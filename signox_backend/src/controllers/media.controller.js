const prisma = require('../config/db');
const path = require('path');
const fs = require('fs');
const os = require('os');
const crypto = require('crypto');
const { spawn } = require('child_process');
const { checkStorageLimit, getClientStorageInfo, getUserStorageInfo, getClientAdminId, incrementMonthlyUpload, incrementUserMonthlyUpload } = require('../utils/storage.utils');
const imageOptimizationService = require('../services/image-optimization.service');
const s3Media = require('../services/s3-media.service');
const paginationService = require('../services/pagination.service');
const { catchAsync } = require('../middleware/error.middleware');
const { getFileMetadata } = require('../utils/file.utils');

function guessContentType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  const map = {
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.png': 'image/png',
    '.webp': 'image/webp',
    '.gif': 'image/gif',
    '.mp4': 'video/mp4',
    '.webm': 'video/webm',
    '.mov': 'video/quicktime',
    '.avi': 'video/x-msvideo',
  };
  return map[ext] || 'application/octet-stream';
}

function safeUnlink(filePath) {
  try {
    if (filePath && fs.existsSync(filePath)) fs.unlinkSync(filePath);
  } catch (e) {
    console.warn('safeUnlink:', filePath, e.message);
  }
}

const publicRoot = path.join(__dirname, '../../public');
const uploadsDir = path.join(publicRoot, 'uploads');

/**
 * Get thumbnail URL for media item
 * @param {Object} media - Media object with filename and type
 * @returns {string|null} Thumbnail URL or null if not available
 */
function getThumbnailUrl(media) {
  if (!media.filename) return null;

  const baseName = path.parse(media.filename).name;

  if (media.s3Key) {
    return s3Media.buildPublicUrl(s3Media.thumbnailKeyFromMainKey(media.s3Key));
  }

  if (media.type === 'IMAGE') {
    const thumbnailPath = path.join(uploadsDir, 'thumbnails', `${baseName}_thumb.jpg`);
    if (fs.existsSync(thumbnailPath)) {
      return `/uploads/thumbnails/${baseName}_thumb.jpg`;
    }
  } else if (media.type === 'VIDEO') {
    const thumbnailPath = path.join(uploadsDir, 'thumbnails', `${baseName}_thumb.jpg`);
    if (fs.existsSync(thumbnailPath)) {
      return `/uploads/thumbnails/${baseName}_thumb.jpg`;
    }
  }

  return null;
}

/**
 * Get preview URL for media item
 * @param {Object} media - Media object with filename and type
 * @returns {string|null} Preview URL or null if not available
 */
function getPreviewUrl(media) {
  if (!media.filename || media.type !== 'IMAGE') return null;

  const baseName = path.parse(media.filename).name;

  if (media.s3Key) {
    return s3Media.buildPublicUrl(s3Media.previewKeyFromMainKey(media.s3Key));
  }

  const previewPath = path.join(uploadsDir, 'optimized', `${baseName}_preview.jpg`);

  if (fs.existsSync(previewPath)) {
    return `/uploads/optimized/${baseName}_preview.jpg`;
  }

  return null;
}

/**
 * Extract video metadata using FFprobe
 * @param {string} inputPath - Full path to input video
 * @returns {Promise<{duration: number, width: number, height: number}>}
 */
function extractVideoMetadata(inputPath) {
  return new Promise((resolve) => {
    const args = [
      '-v', 'quiet',
      '-print_format', 'json',
      '-show_format',
      '-show_streams',
      inputPath
    ];
    
    const proc = spawn('ffprobe', args, {
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    
    let stdout = '';
    let stderr = '';
    
    proc.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    proc.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    
    proc.on('close', (code) => {
      if (code === 0 && stdout) {
        try {
          const data = JSON.parse(stdout);
          const videoStream = data.streams?.find(s => s.codec_type === 'video');
          
          if (videoStream) {
            const duration = parseFloat(data.format?.duration || videoStream.duration || 0);
            const width = parseInt(videoStream.width || 0);
            const height = parseInt(videoStream.height || 0);
            
            resolve({ duration, width, height });
          } else {
            resolve({ duration: 0, width: 0, height: 0 });
          }
        } catch (parseError) {
          console.warn('Failed to parse ffprobe output:', parseError.message);
          resolve({ duration: 0, width: 0, height: 0 });
        }
      } else {
        console.warn('FFprobe failed:', stderr || `Exit code ${code}`);
        resolve({ duration: 0, width: 0, height: 0 });
      }
    });
    
    proc.on('error', (err) => {
      console.warn('FFprobe error:', err.message);
      resolve({ duration: 0, width: 0, height: 0 });
    });
  });
}

/**
 * Generate video thumbnail using FFmpeg
 * @param {string} inputPath - Full path to input video
 * @param {string} outputPath - Full path for thumbnail output
 * @returns {Promise<boolean>} Success status
 */
function generateVideoThumbnail(inputPath, outputPath) {
  return new Promise((resolve) => {
    const args = [
      '-i', inputPath,
      '-ss', '00:00:01', // Seek to 1 second
      '-vframes', '1', // Extract 1 frame
      '-vf', 'scale=300:300:force_original_aspect_ratio=decrease,pad=300:300:(ow-iw)/2:(oh-ih)/2:black', // Scale and pad to 300x300
      '-q:v', '2', // High quality
      '-y', // Overwrite output
      outputPath
    ];
    
    console.log(`🎬 Generating video thumbnail: ${inputPath} -> ${outputPath}`);
    
    const proc = spawn('ffmpeg', args, {
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    
    let stderr = '';
    proc.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    
    proc.on('close', (code) => {
      if (code === 0 && fs.existsSync(outputPath)) {
        console.log(`✅ Video thumbnail generated: ${outputPath}`);
        resolve(true);
      } else {
        console.warn(`❌ Video thumbnail generation failed: ${stderr || `Exit code ${code}`}`);
        resolve(false);
      }
    });
    
    proc.on('error', (err) => {
      console.warn('FFmpeg thumbnail error:', err.message);
      resolve(false);
    });
  });
}

/**
 * Convert a video file to HLS (index.m3u8 + .ts segments) using FFmpeg.
 * @param {string} inputPath - Full path to input video (e.g. uploaded MP4)
 * @param {string} outputDir - Full path to output directory (e.g. public/uploads/hls/<mediaId>)
 * @returns {Promise<{ success: boolean, error?: string }>}
 */
function convertToHLS(inputPath, outputDir) {
  return new Promise((resolve) => {
    const args = [
      '-i', inputPath,
      '-c:v', 'libx264',
      '-c:a', 'aac',
      '-profile:v', 'baseline',
      '-level', '3.0',
      '-start_number', '0',
      '-hls_time', '4',
      '-hls_list_size', '0',
      '-hls_segment_filename', 'segment_%03d.ts',
      '-f', 'hls',
      'index.m3u8',
    ];
    
    console.log(`🎬 Starting HLS conversion: ${inputPath} -> ${outputDir}`);
    
    const proc = spawn('ffmpeg', args, {
      cwd: outputDir,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    
    let stderr = '';
    let stdout = '';
    
    proc.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    proc.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    
    proc.on('close', (code) => {
      if (code === 0) {
        console.log(`✅ HLS conversion successful: ${outputDir}`);
        resolve({ success: true });
      } else {
        console.error(`❌ HLS conversion failed with code ${code}:`, stderr.slice(-500));
        resolve({ success: false, error: stderr.slice(-500) || `FFmpeg exited with code ${code}` });
      }
    });
    
    proc.on('error', (err) => {
      console.error(`❌ HLS conversion error:`, err.message);
      resolve({ success: false, error: err.message });
    });
  });
}

exports.createMedia = catchAsync(async (req, res) => {
  console.log('📤 [MEDIA CREATE DEBUG] Starting media creation...');
  console.log('📤 [MEDIA CREATE DEBUG] Request user:', {
    id: req.user?.id,
    email: req.user?.email,
    role: req.user?.role
  });
  console.log('📤 [MEDIA CREATE DEBUG] Request body:', req.body);
  console.log('📤 [MEDIA CREATE DEBUG] Request file:', req.file ? {
    originalname: req.file.originalname,
    filename: req.file.filename,
    mimetype: req.file.mimetype,
    size: req.file.size,
    path: req.file.path
  } : 'No file');

  if (!req.file) {
    console.log('❌ [MEDIA CREATE DEBUG] No file provided');
    return res.status(400).json({ message: 'File is required' });
  }

  if (!req.user?.id) {
    console.log('❌ [MEDIA CREATE DEBUG] No user authentication');
    return res.status(401).json({ message: 'User authentication required' });
  }

  const mimeType = req.file.mimetype;
  const originalName = req.file.originalname;
  const filename =
    req.file.filename ||
    `file-${Date.now()}-${Math.round(Math.random() * 1e9)}${path.extname(req.file.originalname)}`;
  const fileSize = req.file.size;

  // Check storage limit before processing
  const clientAdminId = await getClientAdminId(req.user.id);
  console.log('📤 [MEDIA CREATE DEBUG] Client admin ID:', clientAdminId);
  
  if (!clientAdminId) {
    console.log('❌ [MEDIA CREATE DEBUG] No client admin ID found');
    const filePath = req.file.path;
    try {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (deleteError) {
      console.warn('⚠️ Failed to delete rejected file:', deleteError.message);
    }
    return res.status(400).json({ 
      message: 'Unable to determine client association',
      details: 'Your account may not be properly associated with an organization. Please contact your administrator.'
    });
  }

  const storageCheck = await checkStorageLimit(req.user.id, fileSize);
  console.log('📤 [MEDIA CREATE DEBUG] Storage check result:', storageCheck);
  
  if (!storageCheck.canUpload) {
    const filePath = req.file.path;
    try {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (deleteError) {
      console.warn('⚠️ Failed to delete rejected file:', deleteError.message);
    }

    return res.status(413).json({ 
      message: 'Storage limit exceeded',
      details: storageCheck.reason,
      storageInfo: storageCheck.storageInfo
    });
  }

  const type = mimeType.startsWith('image/')
    ? 'IMAGE'
    : (mimeType.startsWith('video/') || mimeType === 'video/quicktime' || mimeType === 'video/x-msvideo')
      ? 'VIDEO'
      : null;

  console.log('📤 [MEDIA CREATE DEBUG] Determined media type:', type);

  if (!type) {
    console.log('❌ [MEDIA CREATE DEBUG] Unsupported file type:', mimeType);
    const filePath = req.file.path;
    try {
      if (filePath && fs.existsSync(filePath)) {
        fs.unlinkSync(filePath);
      }
    } catch (deleteError) {
      console.warn('⚠️ Failed to delete unsupported file:', deleteError.message);
    }

    return res.status(400).json({ message: 'Unsupported file type' });
  }

  let endDate = null;
  if (req.body.endDate) {
    endDate = new Date(req.body.endDate);
    if (isNaN(endDate.getTime())) {
      return res.status(400).json({ message: 'Invalid endDate format. Use ISO 8601 format.' });
    }
    if (endDate <= new Date()) {
      return res.status(400).json({ message: 'endDate must be in the future' });
    }
  }

  const useS3 = s3Media.isS3MediaEnabled();
  let filePathForProcessing = req.file.path;
  if (useS3 && req.file.buffer) {
    const safeName = filename.replace(/[^a-zA-Z0-9._-]/g, '_');
    filePathForProcessing = path.join(os.tmpdir(), `signox-${Date.now()}-${safeName}`);
    fs.writeFileSync(filePathForProcessing, req.file.buffer);
  }

  let url;
  let originalUrl = null;
  const finalFilename = filename;
  let duration = null;
  let width = null;
  let height = null;
  let optimizationResults = null;
  let s3Key = null;
  let s3ExtraKeys = [];

  if (type === 'IMAGE') {
    try {
      optimizationResults = await imageOptimizationService.optimizeImage(
        filePathForProcessing,
        filename
      );
      width = optimizationResults.original.width;
      height = optimizationResults.original.height;

      console.log(
        `🖼️ Image optimized: ${filename} - Original: ${optimizationResults.original.size} bytes`
      );
    } catch (optimizationError) {
      console.warn('⚠️ Image optimization failed:', optimizationError.message);
    }

    if (useS3) {
      const prefix = s3Media.getPrefix();
      const mainKey = `${prefix}/${filename}`;
      await s3Media.uploadFile(mainKey, filePathForProcessing, mimeType);
      s3Key = mainKey;
      const baseName = path.parse(filename).name;
      const extra = [];

      const uploadDerived = async (localPath, key) => {
        if (localPath && fs.existsSync(localPath)) {
          await s3Media.uploadFile(key, localPath, guessContentType(localPath));
          extra.push(key);
        }
      };

      if (optimizationResults?.thumbnail?.path) {
        await uploadDerived(
          optimizationResults.thumbnail.path,
          `${prefix}/thumbnails/${baseName}_thumb.jpg`
        );
      }
      if (optimizationResults?.preview?.path) {
        await uploadDerived(
          optimizationResults.preview.path,
          `${prefix}/optimized/${baseName}_preview.jpg`
        );
      }
      if (optimizationResults?.optimized?.jpeg?.path) {
        await uploadDerived(
          optimizationResults.optimized.jpeg.path,
          `${prefix}/optimized/${baseName}_optimized.jpg`
        );
      }
      if (optimizationResults?.optimized?.webp?.path) {
        await uploadDerived(
          optimizationResults.optimized.webp.path,
          `${prefix}/optimized/${baseName}_optimized.webp`
        );
      }

      s3ExtraKeys = extra;
      url = s3Media.buildPublicUrl(mainKey);

      safeUnlink(filePathForProcessing);
      if (optimizationResults?.thumbnail?.path) safeUnlink(optimizationResults.thumbnail.path);
      if (optimizationResults?.preview?.path) safeUnlink(optimizationResults.preview.path);
      if (optimizationResults?.optimized?.jpeg?.path) safeUnlink(optimizationResults.optimized.jpeg.path);
      if (optimizationResults?.optimized?.webp?.path) safeUnlink(optimizationResults.optimized.webp.path);
    } else {
      url = `/uploads/${filename}`;
    }
  } else if (type === 'VIDEO') {
    try {
      const metadata = await extractVideoMetadata(filePathForProcessing);
      duration = metadata.duration > 0 ? Math.round(metadata.duration) : null;
      width = metadata.width > 0 ? metadata.width : null;
      height = metadata.height > 0 ? metadata.height : null;
    } catch (e) {
      console.warn('⚠️ Failed to extract video metadata:', e.message);
    }

    const baseName = path.parse(filename).name;
    const thumbnailsDir = path.join(uploadsDir, 'thumbnails');
    if (!fs.existsSync(thumbnailsDir)) {
      fs.mkdirSync(thumbnailsDir, { recursive: true });
    }

    const thumbnailPath = path.join(thumbnailsDir, `${baseName}_thumb.jpg`);
    try {
      const thumbnailGenerated = await generateVideoThumbnail(
        filePathForProcessing,
        thumbnailPath
      );
      if (thumbnailGenerated) {
        console.log(`✅ Video thumbnail generated: ${baseName}_thumb.jpg`);
      }
    } catch (thumbnailError) {
      console.warn('⚠️ Video thumbnail generation failed:', thumbnailError.message);
    }

    if (useS3) {
      const prefix = s3Media.getPrefix();
      const mainKey = `${prefix}/${filename}`;
      const thumbKey = `${prefix}/thumbnails/${baseName}_thumb.jpg`;
      await s3Media.uploadFile(mainKey, filePathForProcessing, mimeType);
      s3Key = mainKey;
      if (fs.existsSync(thumbnailPath)) {
        await s3Media.uploadFile(thumbKey, thumbnailPath, 'image/jpeg');
        s3ExtraKeys = [thumbKey];
      }
      url = s3Media.buildPublicUrl(mainKey);

      safeUnlink(filePathForProcessing);
      safeUnlink(thumbnailPath);
    } else {
      url = `/uploads/${filename}`;
    }
  }

  // Collect tags including orientation and displayMode
  const tags = [];
  if (req.body.orientation) {
    tags.push(`orientation:${req.body.orientation}`);
  }
  if (req.body.displayMode) {
    tags.push(`displayMode:${req.body.displayMode}`);
  }

  // Create media record in database
  console.log('📤 [MEDIA CREATE DEBUG] Creating media record with data:', {
    name: req.body.name || originalName,
    originalName,
    filename: finalFilename,
    type,
    url,
    originalUrl,
    s3Key,
    s3ExtraKeys,
    fileSize,
    mimeType,
    duration,
    width,
    height,
    tags,
    endDate,
    createdById: req.user?.id || null,
  });

  const media = await prisma.media.create({
    data: {
      name: req.body.name || originalName,
      originalName,
      filename: finalFilename,
      type,
      url,
      originalUrl,
      s3Key,
      s3ExtraKeys,
      fileSize,
      mimeType,
      duration,
      width,
      height,
      tags,
      endDate,
      createdById: req.user?.id || null,
    },
  });

  console.log('✅ [MEDIA CREATE DEBUG] Media record created:', {
    id: media.id,
    name: media.name,
    type: media.type,
    url: media.url,
    originalUrl: media.originalUrl,   
    fileSize: media.fileSize
  });

  // Increment monthly upload counter for the specific user (not client-wide)
  await incrementUserMonthlyUpload(req.user.id, fileSize);
  console.log('📊 [MEDIA CREATE DEBUG] Monthly upload counter incremented for user:', req.user.id);

  // Include updated storage info for the current user (individual USER_ADMIN storage)
  let updatedStorageInfo;
  if (req.user.role === 'USER_ADMIN') {
    updatedStorageInfo = await getUserStorageInfo(req.user.id);
  } else {
    updatedStorageInfo = clientAdminId ? await getClientStorageInfo(clientAdminId) : null;
  }
  console.log('📤 [MEDIA CREATE DEBUG] Updated storage info:', updatedStorageInfo);

  const response = { 
    media,
    storageInfo: updatedStorageInfo
  };

  // Include optimization results if available
  if (optimizationResults) {
    response.optimization = {
      thumbnail: optimizationResults.thumbnail,
      preview: optimizationResults.preview,
      optimized: optimizationResults.optimized
    };
  }

  console.log('✅ [MEDIA CREATE DEBUG] Sending response with media ID:', media.id);
  res.status(201).json(response);
});

exports.listMedia = catchAsync(async (req, res) => {
  console.log('🔍 [MEDIA DEBUG] Starting listMedia request');
  console.log('🔍 [MEDIA DEBUG] Request user:', {
    id: req.user?.id,
    email: req.user?.email,
    role: req.user?.role,
    staffRole: req.user?.staffRole
  });
  console.log('🔍 [MEDIA DEBUG] Request query params:', req.query);

  // Parse pagination and filtering parameters
  const { page, limit, skip } = paginationService.parsePaginationParams(req);
  const { search, searchFields } = paginationService.parseSearchParams(req);
  const filters = paginationService.parseFilterParams(req);
  const sort = paginationService.parseSortParams(req, { createdAt: 'desc' });

  console.log('🔍 [MEDIA DEBUG] Parsed params:', { page, limit, skip, search, filters, sort });

  // Get the current user's client admin ID to filter media
  const clientAdminId = await getClientAdminId(req.user?.id);
  console.log('🔍 [MEDIA DEBUG] Client admin ID:', clientAdminId);
  
  if (!clientAdminId) {
    console.log('❌ [MEDIA DEBUG] No client admin ID found');
    return res.status(400).json({ message: 'Unable to determine client association' });
  }

  let userIds = [];

  // Different filtering logic based on user role
  if (req.user.role === 'CLIENT_ADMIN') {
    // CLIENT_ADMIN sees all media from all USER_ADMINs under them (for management)
    const userAdmins = await prisma.user.findMany({
      where: {
        role: 'USER_ADMIN',
        managedByClientAdminId: clientAdminId
      },
      select: { id: true }
    });

    const userAdminIds = userAdmins.map(ua => ua.id);

    // Get all STAFF users created by these USER_ADMINs
    const staffUsers = await prisma.user.findMany({
      where: {
        role: 'STAFF',
        createdByUserAdminId: {
          in: userAdminIds
        }
      },
      select: { id: true }
    });

    // CLIENT_ADMIN sees media from: client admin + all user admins + all staff
    userIds = [
      clientAdminId,
      ...userAdminIds,
      ...staffUsers.map(s => s.id)
    ];
    
    console.log('🔍 [MEDIA DEBUG] CLIENT_ADMIN - All user IDs:', userIds);
    
  } else if (req.user.role === 'USER_ADMIN') {
    // USER_ADMIN only sees their own media + their staff's media (STRICT ISOLATION)
    const staffUsers = await prisma.user.findMany({
      where: {
        role: 'STAFF',
        createdByUserAdminId: req.user.id
      },
      select: { id: true }
    });

    // USER_ADMIN sees media from: themselves + their staff only
    userIds = [
      req.user.id,
      ...staffUsers.map(s => s.id)
    ];
    
    console.log('🔍 [MEDIA DEBUG] USER_ADMIN - Own user IDs only:', userIds);
    
  } else if (req.user.role === 'STAFF') {
    // STAFF sees media from their USER_ADMIN + other staff under same USER_ADMIN
    const staffUser = await prisma.user.findUnique({
      where: { id: req.user.id },
      select: { createdByUserAdminId: true }
    });

    if (staffUser?.createdByUserAdminId) {
      const siblingStaff = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: staffUser.createdByUserAdminId
        },
        select: { id: true }
      });

      // STAFF sees media from: their USER_ADMIN + all staff under same USER_ADMIN
      userIds = [
        staffUser.createdByUserAdminId,
        ...siblingStaff.map(s => s.id)
      ];
    } else {
      userIds = [req.user.id]; // Fallback to own media only
    }
    
    console.log('🔍 [MEDIA DEBUG] STAFF - USER_ADMIN group IDs:', userIds);
    
  } else {
    // SUPER_ADMIN or other roles - see all media (no filtering)
    console.log('🔍 [MEDIA DEBUG] SUPER_ADMIN - No filtering applied');
    userIds = null; // Will skip the createdById filter
  }

  // Build where clause
  const where = {
    AND: [
      {
        OR: [{ type: 'IMAGE' }, { type: 'VIDEO' }], // Filter to only known-good enum values
      },
      filters
    ]
  };

  // Add user-based filtering (skip for SUPER_ADMIN)
  if (userIds !== null) {
    where.AND.push({
      createdById: {
        in: userIds // Only show media created by users in the same group
      }
    });
  }

  console.log('🔍 [MEDIA DEBUG] Where clause:', JSON.stringify(where, null, 2));

  // Add search conditions
  if (search) {
    where.AND.push({
      OR: [
        { name: { contains: search, mode: 'insensitive' } },
        { originalName: { contains: search, mode: 'insensitive' } },
        { tags: { hasSome: [search] } }
      ]
    });
    console.log('🔍 [MEDIA DEBUG] Added search conditions for:', search);
  }

  console.log('🔍 [MEDIA DEBUG] Final where clause:', JSON.stringify(where, null, 2));

  // Execute paginated query
  console.log('🔍 [MEDIA DEBUG] Executing database queries...');
  const [media, total] = await Promise.all([
    prisma.media.findMany({
      where,
      orderBy: sort,
      skip,
      take: limit,
      include: {
        createdBy: {
          select: {
            id: true,
            email: true,
            role: true
          }
        }
      }
    }),
    prisma.media.count({ where })
  ]);

  console.log('🔍 [MEDIA DEBUG] Database results:');
  console.log('🔍 [MEDIA DEBUG] - Total count:', total);
  console.log('🔍 [MEDIA DEBUG] - Media items returned:', media.length);
  console.log('🔍 [MEDIA DEBUG] - Media items:', media.map(m => ({
    id: m.id,
    name: m.name,
    type: m.type,
    createdById: m.createdById,
    createdBy: m.createdBy?.email,
    url: m.url,
    fileSize: m.fileSize
  })));

  // Enrich media items with file metadata and thumbnail URLs
  const enrichedMedia = await Promise.all(media.map(async (item) => {
    try {
      const thumbnailUrl = getThumbnailUrl(item);
      const previewUrl = getPreviewUrl(item);

      const isRemote =
        item.s3Key ||
        (item.url && /^https?:\/\//i.test(String(item.url)));

      if (isRemote) {
        return {
          ...item,
          thumbnailUrl,
          previewUrl,
          fileSize: item.fileSize,
        };
      }

      const urlPath = item.url.replace(/^\//, '');
      const filePath = path.join(__dirname, '../../public', urlPath);

      if (fs.existsSync(filePath)) {
        const metadata = await getFileMetadata(filePath);
        return {
          ...item,
          fileSize: metadata.fileSize,
          checksum: metadata.checksum,
          thumbnailUrl,
          previewUrl,
        };
      }

      return {
        ...item,
        thumbnailUrl,
        previewUrl,
      };
    } catch (error) {
      console.error(`Error enriching media ${item.id}:`, error.message);
      return {
        ...item,
        thumbnailUrl: getThumbnailUrl(item),
        previewUrl: getPreviewUrl(item),
      };
    }
  }));

  console.log('🔍 [MEDIA DEBUG] Enriched media with file metadata');

  // Include storage info for the current user (individual USER_ADMIN storage)
  let storageInfo;
  if (req.user.role === 'USER_ADMIN') {
    storageInfo = await getUserStorageInfo(req.user.id);
  } else {
    storageInfo = await getClientStorageInfo(clientAdminId);
  }
  console.log('🔍 [MEDIA DEBUG] Storage info:', storageInfo);

  const response = paginationService.formatPaginatedResponse(enrichedMedia, total, page, limit);
  response.storageInfo = storageInfo;

  console.log('🔍 [MEDIA DEBUG] Final response structure:', {
    mediaCount: response.data?.length || 0,
    total: response.total,
    page: response.page,
    totalPages: response.totalPages,
    hasStorageInfo: !!response.storageInfo
  });

  res.json(response);
});

exports.getStorageInfo = async (req, res) => {
  try {
    const user = req.user;
    
    if (!user) {
      return res.status(401).json({ message: 'Authentication required' });
    }

    let storageInfo;

    if (user.role === 'USER_ADMIN') {
      // For USER_ADMIN, get their individual storage info
      storageInfo = await getUserStorageInfo(user.id);
    } else {
      // For CLIENT_ADMIN and others, use the existing logic
      const clientAdminId = await getClientAdminId(user.id);
      
      if (!clientAdminId) {
        return res.status(400).json({ message: 'Unable to determine client association' });
      }

      storageInfo = await getClientStorageInfo(clientAdminId);
    }
    
    res.json({ storageInfo });
  } catch (error) {
    console.error('Error getting storage info:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.deleteMedia = async (req, res) => {
  try {
    const mediaId = req.params.id;
    console.log('🗑️ [DELETE DEBUG] Starting delete process for media ID:', mediaId);

    // Get the current user's client admin ID
    const clientAdminId = await getClientAdminId(req.user?.id);
    console.log('🗑️ [DELETE DEBUG] Client admin ID:', clientAdminId);
    
    if (!clientAdminId) {
      console.log('❌ [DELETE DEBUG] No client admin ID found');
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as listMedia)
    if (req.user.role === 'CLIENT_ADMIN') {
      // CLIENT_ADMIN can delete media from all USER_ADMINs under them
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      // USER_ADMIN can only delete their own media + their staff's media
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      // STAFF can delete media from their USER_ADMIN group
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id];
      }
      
    } else {
      // SUPER_ADMIN can delete any media
      userIds = null; // Will skip the access check
    }

    console.log('🗑️ [DELETE DEBUG] Authorized user IDs:', userIds);

    // Get media and verify it belongs to the same client
    const media = await prisma.media.findUnique({
      where: { id: mediaId },
      include: {
        createdBy: {
          select: { id: true }
        }
      }
    });

    console.log('🗑️ [DELETE DEBUG] Found media:', media ? {
      id: media.id,
      name: media.name,
      url: media.url,
      filename: media.filename,
      createdById: media.createdById
    } : 'null');

    if (!media) {
      console.log('❌ [DELETE DEBUG] Media not found');
      return res.status(404).json({ message: 'Media not found' });
    }

    // Check if the media was created by someone in the authorized group (skip check for SUPER_ADMIN)
    if (userIds !== null && media.createdById && !userIds.includes(media.createdById)) {
      console.log('❌ [DELETE DEBUG] Access denied - media created by user not in authorized group');
      return res.status(403).json({ message: 'Access denied. You can only delete media from your organization.' });
    }

    // Check if media is used in any playlists
    const playlistItem = await prisma.playlistItem.findFirst({
      where: { mediaId },
      select: { id: true },
    });

    if (playlistItem) {
      console.log('❌ [DELETE DEBUG] Media is used in playlists');
      return res.status(400).json({
        message: 'Cannot delete media that is used in playlists. Remove it from playlists first.',
      });
    }

    // Check if media is used in any layout sections
    const layoutSectionItem = await prisma.layoutSectionItem.findFirst({
      where: { mediaId },
      select: { id: true },
    });

    if (layoutSectionItem) {
      console.log('❌ [DELETE DEBUG] Media is used in layouts');
      return res.status(400).json({
        message: 'Cannot delete media that is used in layouts. Remove it from layouts first.',
      });
    }

    // Check if media is used in any widgets
    const widget = await prisma.widget.findFirst({
      where: { mediaId },
      select: { id: true },
    });

    if (widget) {
      console.log('❌ [DELETE DEBUG] Media is used in widgets');
      return res.status(400).json({
        message: 'Cannot delete media that is used in widgets. Remove it from widgets first.',
      });
    }

    console.log('🗑️ [DELETE DEBUG] All checks passed, proceeding with deletion');

    // Delete the media record
    await prisma.media.delete({
      where: { id: mediaId },
    });

    console.log('✅ [DELETE DEBUG] Media record deleted from database');

    let s3KeysToDelete = s3Media.keysFromMediaRecord(media);
    if (s3Media.isS3MediaEnabled() && s3KeysToDelete.length === 0) {
      s3KeysToDelete = s3Media.tryExtractKeysFromUrl(media.url);
    }
    if (s3Media.isS3MediaEnabled() && s3KeysToDelete.length > 0) {
      await s3Media.deleteKeys(s3KeysToDelete);
    }

    // Delete the file (or HLS directory) from filesystem (best-effort)
    if (media.url && media.url.startsWith('/uploads/')) {
      const diskPath = path.join(__dirname, '../../public', media.url);
      console.log('🗑️ [DELETE DEBUG] Attempting to delete file at:', diskPath);
      
      try {
        if (media.url.includes('/hls/') && media.url.endsWith('/index.m3u8')) {
          const hlsDir = path.dirname(diskPath);
          console.log('🗑️ [DELETE DEBUG] Deleting HLS directory:', hlsDir);
          if (fs.existsSync(hlsDir)) {
            fs.rmSync(hlsDir, { recursive: true });
            console.log('✅ [DELETE DEBUG] HLS directory deleted successfully');
          } else {
            console.log('⚠️ [DELETE DEBUG] HLS directory does not exist:', hlsDir);
          }
        } else {
          console.log('🗑️ [DELETE DEBUG] Deleting single file:', diskPath);
          if (fs.existsSync(diskPath)) {
            fs.unlinkSync(diskPath);
            console.log('✅ [DELETE DEBUG] File deleted successfully');
          } else {
            console.log('⚠️ [DELETE DEBUG] File does not exist:', diskPath);
          }
        }

        // Also try to delete optimized versions if they exist
        if (media.type === 'IMAGE') {
          const optimizedDir = path.join(__dirname, '../../public/uploads/optimized');
          const thumbnailDir = path.join(__dirname, '../../public/uploads/thumbnails');
          const baseFilename = media.filename?.replace(/\.[^/.]+$/, '') || '';
          
          // Delete optimized versions
          const optimizedFiles = [
            path.join(optimizedDir, `${baseFilename}_optimized.jpg`),
            path.join(optimizedDir, `${baseFilename}_optimized.webp`),
            path.join(optimizedDir, `${baseFilename}_preview.jpg`),
            path.join(thumbnailDir, `${baseFilename}_thumb.jpg`)
          ];

          optimizedFiles.forEach(filePath => {
            try {
              if (fs.existsSync(filePath)) {
                fs.unlinkSync(filePath);
                console.log('✅ [DELETE DEBUG] Deleted optimized file:', filePath);
              }
            } catch (optimizedError) {
              console.warn('⚠️ [DELETE DEBUG] Failed to delete optimized file:', filePath, optimizedError.message);
            }
          });
        }

      } catch (e) {
        console.error('❌ [DELETE DEBUG] Failed to delete file from disk:', e.message);
        console.error('❌ [DELETE DEBUG] File path was:', diskPath);
      }
    } else {
      console.log('⚠️ [DELETE DEBUG] No file to delete (invalid URL):', media.url);
    }

    // Include updated storage info for the current user (individual USER_ADMIN storage)
    let updatedStorageInfo;
    if (req.user.role === 'USER_ADMIN') {
      updatedStorageInfo = await getUserStorageInfo(req.user.id);
    } else {
      updatedStorageInfo = await getClientStorageInfo(clientAdminId);
    }
    console.log('🗑️ [DELETE DEBUG] Updated storage info:', updatedStorageInfo);

    console.log('✅ [DELETE DEBUG] Delete operation completed successfully');
    res.json({ 
      message: 'Media deleted successfully',
      storageInfo: updatedStorageInfo
    });
  } catch (error) {
    console.error('❌ [DELETE DEBUG] Error deleting media:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.updateMedia = async (req, res) => {
  try {
    const mediaId = req.params.id;
    const { name, description, tags, endDate } = req.body;

    // Get the current user's client admin ID
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as listMedia and deleteMedia)
    if (req.user.role === 'CLIENT_ADMIN') {
      // CLIENT_ADMIN can update media from all USER_ADMINs under them
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      // USER_ADMIN can only update their own media + their staff's media
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      // STAFF can update media from their USER_ADMIN group
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id];
      }
      
    } else {
      // SUPER_ADMIN can update any media
      userIds = null; // Will skip the access check
    }

    // Check if media exists and belongs to the same client
    const existingMedia = await prisma.media.findUnique({
      where: { id: mediaId },
      include: {
        createdBy: {
          select: { id: true }
        }
      }
    });

    if (!existingMedia) {
      return res.status(404).json({ message: 'Media not found' });
    }

    // Check if the media was created by someone in the authorized group (skip check for SUPER_ADMIN)
    if (userIds !== null && (!existingMedia.createdById || !userIds.includes(existingMedia.createdById))) {
      return res.status(403).json({ message: 'Access denied. You can only modify media from your organization.' });
    }

    // Validate endDate if provided
    let parsedEndDate = existingMedia.endDate;
    if (endDate !== undefined) {
      if (endDate === null) {
        parsedEndDate = null;
      } else {
        parsedEndDate = new Date(endDate);
        if (isNaN(parsedEndDate.getTime())) {
          return res.status(400).json({ message: 'Invalid endDate format. Use ISO 8601 format.' });
        }
        if (parsedEndDate <= new Date()) {
          return res.status(400).json({ message: 'endDate must be in the future' });
        }
      }
    }

    // Update media
    const updatedMedia = await prisma.media.update({
      where: { id: mediaId },
      data: {
        ...(name !== undefined && { name }),
        ...(description !== undefined && { description }),
        ...(tags !== undefined && { tags }),
        endDate: parsedEndDate,
      },
    });

    res.json({ media: updatedMedia });
  } catch (error) {
    console.error('Error updating media:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.cleanupExpiredMedia = async () => {
  try {
    // Find all expired media
    const expiredMedia = await prisma.media.findMany({
      where: {
        endDate: {
        not: null,  
	lte: new Date(),
        },
      },
      include: {
        playlistItems: true,
        widgets: true,
      },
    });

    if (expiredMedia.length === 0) {
      return { deleted: 0, errors: [] };
    }
    
    const results = {
      deleted: 0,
      errors: [],
    };

    for (const media of expiredMedia) {
      try {
        // Remove from playlists first
        if (media.playlistItems.length > 0) {
          await prisma.playlistItem.deleteMany({
            where: { mediaId: media.id },
          });
        }

        // Remove from widgets
        if (media.widgets.length > 0) {
          await prisma.widget.updateMany({
            where: { mediaId: media.id },
            data: { mediaId: null },
          });
        }

        // Delete the media record
        await prisma.media.delete({
          where: { id: media.id },
        });

        let s3KeysToDelete = s3Media.keysFromMediaRecord(media);
        if (s3Media.isS3MediaEnabled() && s3KeysToDelete.length === 0) {
          s3KeysToDelete = s3Media.tryExtractKeysFromUrl(media.url);
        }
        if (s3Media.isS3MediaEnabled() && s3KeysToDelete.length > 0) {
          await s3Media.deleteKeys(s3KeysToDelete);
        }

        // Delete the file (or HLS directory) from filesystem
        if (media.url && media.url.startsWith('/uploads/')) {
          const diskPath = path.join(__dirname, '../../public', media.url);
          try {
            if (media.url.includes('/hls/') && media.url.endsWith('/index.m3u8')) {
              const hlsDir = path.dirname(diskPath);
              if (fs.existsSync(hlsDir)) {
                fs.rmSync(hlsDir, { recursive: true });
              }
            } else if (fs.existsSync(diskPath)) {
              fs.unlinkSync(diskPath);
            }
          } catch (fileError) {
            results.errors.push(`Failed to delete file ${media.filename}: ${fileError.message}`);
          }
        }

        results.deleted++;
        
      } catch (mediaError) {
        results.errors.push(`Failed to delete media ${media.name}: ${mediaError.message}`);
      }
    }

    return results;
    
  } catch (error) {
    console.error('Error during media cleanup:', error);
    throw error;
  }
};
