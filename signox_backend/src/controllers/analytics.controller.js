const prisma = require('../config/db');

/**
 * Calculate display status based on last heartbeat
 * @param {Object} display - Display object with lastHeartbeat and isPaired
 * @returns {string} Status: 'ONLINE', 'OFFLINE', 'PAIRING', 'ERROR'
 */
const calculateDisplayStatus = (display) => {
  if (!display.isPaired) {
    return 'PAIRING';
  }
  
  if (!display.lastHeartbeat) {
    return 'OFFLINE';
  }
  
  const now = new Date();
  const lastHeartbeat = new Date(display.lastHeartbeat);
  const timeDiff = (now - lastHeartbeat) / 1000; // seconds
  
  // Consider display offline if no heartbeat for 60 seconds or more
  // (heartbeat interval is 30 seconds, so 60 seconds gives some buffer)
  if (timeDiff >= 60) {
    return 'OFFLINE';
  }
  
  return 'ONLINE';
};

/**
 * GET /api/analytics/summary
 * Returns role-aware analytics for dashboards.
 */
exports.getSummary = async (req, res) => {
  try {
    const user = req.user;

    if (!user) {
      return res.status(401).json({ message: 'Authentication required' });
    }

    if (user.role === 'SUPER_ADMIN') {
      console.log('📊 [ANALYTICS DEBUG] Super Admin requesting summary');
      
      // Get all paired displays and calculate real-time status
      const allDisplays = await prisma.display.findMany({
        where: { isPaired: true },
        select: { lastHeartbeat: true, isPaired: true }
      });
      
      const clientCount = await prisma.user.count({ where: { role: 'CLIENT_ADMIN' } });
      const displayCount = allDisplays.length;
      const onlineDisplays = allDisplays.filter(display => 
        calculateDisplayStatus(display) === 'ONLINE'
      ).length;

      console.log('📊 [ANALYTICS DEBUG] Super Admin counts:', {
        clientCount,
        displayCount,
        onlineDisplays
      });

      return res.json({
        role: 'SUPER_ADMIN',
        totalClients: clientCount,
        totalDisplays: displayCount,
        onlineDisplays,
        offlineDisplays: displayCount - onlineDisplays,
        totalContent: 0,
        totalPlaylists: 0,
        averageUptime: 0,
        totalPlaybackTime: 0,
        systemHealth: {
          status: 'OK',
          uptime: process.uptime(),
        },
      });
    }

    if (user.role === 'CLIENT_ADMIN') {
      // Get all paired displays for this client and calculate real-time status
      const clientDisplays = await prisma.display.findMany({
        where: { 
          clientAdminId: user.id,
          isPaired: true 
        },
        select: { lastHeartbeat: true, isPaired: true }
      });

      const [profile, userAdminCount] = await Promise.all([
        prisma.clientProfile.findUnique({
          where: { clientAdminId: user.id },
        }),
        prisma.user.count({
          where: {
            role: 'USER_ADMIN',
            managedByClientAdminId: user.id,
          },
        }),
      ]);

      const displayCount = clientDisplays.length;
      const onlineCount = clientDisplays.filter(display => 
        calculateDisplayStatus(display) === 'ONLINE'
      ).length;

      // After migration, CLIENT_ADMIN doesn't have limits - they're at USER_ADMIN level
      // So we don't show display limits for CLIENT_ADMIN anymore
      const licenseExpiry = profile?.licenseExpiry ?? null;
      const licenseActive = profile?.isActive ?? true;

      return res.json({
        role: 'CLIENT_ADMIN',
        userAdmins: userAdminCount,
        totalDisplays: displayCount,
        onlineDisplays: onlineCount,
        offlineDisplays: displayCount - onlineCount,
        // Remove displayLimit since it's now managed at USER_ADMIN level
        totalContent: 0,
        totalPlaylists: 0,
        averageUptime: 0,
        totalPlaybackTime: 0,
        license: {
          status: licenseActive ? 'ACTIVE' : 'SUSPENDED',
          expiry: licenseExpiry,
        },
      });
    }

    if (user.role === 'USER_ADMIN') {
      console.log('📊 [ANALYTICS DEBUG] User Admin requesting summary:', user.email);
      const [myDisplays, mediaCount, playlistCount, userAdminProfile] = await Promise.all([
        // Only get paired displays with heartbeat data
        prisma.display.findMany({
          where: { 
            managedByUserId: user.id,
            isPaired: true 
          },
          select: { lastHeartbeat: true, isPaired: true },
        }),
        prisma.media.count({
          where: { createdById: user.id },
        }),
        prisma.playlist.count({
          where: { createdById: user.id },
        }),
        // Get USER_ADMIN profile with limits and license info
        prisma.userAdminProfile.findUnique({
          where: { userAdminId: user.id },
        }),
      ]);

      console.log('📊 [ANALYTICS DEBUG] User Admin counts:', {
        userId: user.id,
        displayCount: myDisplays.length,
        mediaCount,
        playlistCount,
        maxDisplays: userAdminProfile?.maxDisplays
      });

      // Calculate real-time online/offline status
      const online = myDisplays.filter(display => 
        calculateDisplayStatus(display) === 'ONLINE'
      ).length;
      const offline = myDisplays.length - online;

      // Approximate storage: sum fileSize where available
      const storageAgg = await prisma.media.aggregate({
        where: { createdById: user.id },
        _sum: { fileSize: true },
      });
      const bytes = storageAgg._sum.fileSize ?? 0;

      // License status based on USER_ADMIN profile
      const licenseActive = userAdminProfile?.isActive ?? true;
      const licenseExpiry = userAdminProfile?.licenseExpiry ?? null;
      
      let licenseStatus = 'ACTIVE';
      if (!licenseActive) {
        licenseStatus = 'SUSPENDED';
      } else if (licenseExpiry) {
        const now = new Date();
        const expiry = new Date(licenseExpiry);
        const daysUntilExpiry = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
        
        if (daysUntilExpiry < 0) {
          licenseStatus = 'EXPIRED';
        } else if (daysUntilExpiry <= 7) {
          licenseStatus = 'EXPIRING_SOON';
        }
      }

      return res.json({
        role: 'USER_ADMIN',
        totalDisplays: myDisplays.length,
        onlineDisplays: online,
        offlineDisplays: offline,
        displayLimit: userAdminProfile?.maxDisplays ?? 0,
        totalContent: mediaCount,
        totalPlaylists: playlistCount,
        averageUptime: 0,
        totalPlaybackTime: 0,
        displays: {
          total: myDisplays.length,
          online,
          offline,
        },
        mediaCount,
        playlistCount,
        storageBytes: bytes,
        // Add license and limits info for USER_ADMIN
        license: {
          status: licenseStatus,
          expiry: licenseExpiry,
        },
        limits: {
          maxDisplays: userAdminProfile?.maxDisplays ?? 0,
          maxUsers: userAdminProfile?.maxUsers ?? 0,
          maxStorageMB: userAdminProfile?.maxStorageMB ?? 0,
          maxMonthlyUsageMB: userAdminProfile?.maxMonthlyUsageMB ?? 0,
          monthlyUploadedBytes: Number(userAdminProfile?.monthlyUploadedBytes ?? 0),
        },
      });
    }

    // Other roles (STAFF, etc.) can receive a minimal payload or 403
    return res.status(403).json({ message: 'Analytics not available for this role' });
  } catch (error) {
    console.error('Analytics Summary Error:', error);
    res.status(500).json({ message: 'Failed to load analytics summary', error: error.message });
  }
};

