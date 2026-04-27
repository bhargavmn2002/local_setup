
//app.use('/api/auth', authLimiter);
require('dotenv').config();

try {
  const { isS3MediaEnabled } = require('./services/s3-media.service');
  if (isS3MediaEnabled()) {
    console.log(
      `☁️  CMS media: S3 (bucket=${process.env.AWS_S3_BUCKET}, prefix=${(process.env.AWS_S3_MEDIA_PREFIX || 'media').replace(/^\/+|\/+$/g, '')})`
    );
  } else {
    console.log(
      '💾 CMS media: local disk (public/uploads). Enable S3 with USE_S3_FOR_MEDIA=true and AWS_S3_BUCKET=...'
    );
  }
} catch (e) {
  console.warn('CMS media storage mode: could not read S3 config:', e.message);
}

// Set timezone to India (IST)
process.env.TZ = process.env.TIMEZONE || 'Asia/Kolkata';

const prisma = require('./config/db');
void (async () => {
  try {
    await prisma.$connect();
    console.log('✅ Database: connected');
  } catch (err) {
    const hint = err.message?.split('\n')[0] || String(err);
    console.error(
      '\n⚠️  DATABASE UNREACHABLE — login and most API routes will fail until this is fixed.\n' +
        '   Point DATABASE_URL in .env at a running MongoDB (local install or MongoDB Atlas).\n' +
        `   (${hint})\n`
    );
  }
})();

const express = require('express');
const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const helmet = require('helmet');
const compression = require('compression');

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const displayRoutes = require('./routes/display.routes');
const playerRoutes = require('./routes/player.routes');
const mediaRoutes = require('./routes/media.routes');
const playlistRoutes = require('./routes/playlist.routes');
const layoutRoutes = require('./routes/layout.routes');
const analyticsRoutes = require('./routes/analytics.routes');
const adminRoutes = require('./routes/admin.routes');
const proofOfPlayRoutes = require('./routes/proofOfPlay.routes');
const monitoringRoutes = require('./routes/monitoring.routes');

// Services
const cleanupService = require('./services/cleanup.service');
const backupService = require('./services/backup.service');
const healthService = require('./services/health.service');
const cacheService = require('./services/cache.service');
const imageOptimizationService = require('./services/image-optimization.service');
const databaseOptimizationService = require('./services/database-optimization.service');
const licenseCheckService = require('./services/license-check.service');
const displayStatusService = require('./services/display-status.service');
const licenseExpiryService = require('./services/license-expiry.service');

// Middleware
const { logAuthEvents, logSensitiveAccess, logSuspiciousActivity } = require('./middleware/logging.middleware');
const { globalErrorHandler } = require('./middleware/error.middleware');
const {
  requestLogger,
  performanceMonitor,
  memoryMonitor,
  rateLimitMonitor,
  apiAnalytics,
  securityMonitor,
  healthCheck
} = require('./middleware/monitoring.middleware');
const { auditRequest, auditAuth } = require('./middleware/audit.middleware');

const app = express();

/* =========================
   SECURITY & PERFORMANCE
========================= */

app.use(helmet({
  crossOriginEmbedderPolicy: false,
  crossOriginResourcePolicy: { policy: 'cross-origin' },
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      scriptSrc: ["'self'"],
      imgSrc: ["'self'", 'data:', 'blob:'],
      mediaSrc: ["'self'", 'blob:', '*'],
      connectSrc: [
        "'self'",
        ...(process.env.CORS_ORIGIN?.split(',').map(o => o.trim()) || ['*'])
      ],
    },
  },
}));

app.use(compression());

/* =========================
   RATE LIMITING
========================= */

let limiter, authLimiter;

// Only apply rate limiting in production
if (process.env.NODE_ENV === 'production') {
  limiter = rateLimit({
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW) || 15 * 60 * 1000,
    max: parseInt(process.env.RATE_LIMIT_MAX) || 5000,
    standardHeaders: true,
    legacyHeaders: false,
  });

    authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 minutes
    max: parseInt(process.env.AUTH_RATE_LIMIT_MAX) || 30,
    skipSuccessfulRequests: true,
  });

  //app.use(limiter);
   app.use('/api/auth', authLimiter);
app.use('/api/admin', limiter);
} else {
  // No-op middleware for development
  limiter = (req, res, next) => next();
  authLimiter = (req, res, next) => next();
}

/* =========================
   CORS (RENDER SAFE)
========================= */

const corsOptions = {
  origin: (origin, callback) => {
    // Allow requests with no origin (mobile apps, Postman, etc.)
    if (!origin) return callback(null, true);

    // In development, allow all origins
    if (process.env.NODE_ENV !== 'production') {
      return callback(null, true);
    }

    // In production, check allowed origins
    const allowed = process.env.CORS_ORIGIN?.split(',').map(o => o.trim()) || [];
    
    // Check if origin matches any allowed origin
    const isAllowed = allowed.some(allowedOrigin => {
      // Exact match
      if (allowedOrigin === origin) return true;
      // Wildcard match (e.g., *.onrender.com)
      if (allowedOrigin.includes('*')) {
        const pattern = allowedOrigin.replace(/\*/g, '.*');
        return new RegExp(`^${pattern}$`).test(origin);
      }
      return false;
    });

    if (isAllowed) {
      return callback(null, true);
    }
    
    return callback(new Error('CORS blocked'));
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
};

app.use(cors(corsOptions));

/* =========================
   BODY & LOGGING
========================= */

app.use(express.json({ limit: '500mb' }));
app.use(express.urlencoded({ extended: true, limit: '500mb' }));

app.use(requestLogger);
app.use(performanceMonitor);
app.use(memoryMonitor);
app.use(rateLimitMonitor);
app.use(apiAnalytics);
app.use(securityMonitor);
app.use(healthCheck);

app.use(logAuthEvents);
app.use(logSuspiciousActivity);
app.use(auditRequest);

/* =========================
   STATIC UPLOADS & DEPLOYS
   • URLs: GET https://signoxcms.com/uploads/<filename> — served from the folder below.
   • Path: always signox_backend/public/uploads (resolved from this file, not cwd), so PM2 cwd does not break /uploads.
   • Deploying new code (git pull, rsync) does NOT move media files. You must either:
       – keep this folder on the server as-is, or
       – copy/rsync public/uploads from a backup or old server when you migrate.
   • After deploy, restart Node (e.g. pm2 restart) so changes apply.
   • AWS S3: (1) Player-app APK/WGT signed URLs when USE_S3_FOR_PLAYER_APPS=true
     (playerApps.controller.js). (2) CMS Media Library uploads when USE_S3_FOR_MEDIA=true
     (same AWS_S3_BUCKET/region/credentials; objects under AWS_S3_MEDIA_PREFIX, default media/).
     If both flags are off, CMS files stay under public/uploads and /uploads static above applies.
========================= */

const uploadsStaticRoot = path.join(__dirname, '../public/uploads');
const downloadsStaticRoot = path.join(__dirname, '../../signox_frontend/public/downloads');

app.use('/uploads', express.static(uploadsStaticRoot, {
  setHeaders: (res, filePath) => {
    const map = {
      '.mp4': 'video/mp4',
      '.webm': 'video/webm',
      '.mov': 'video/quicktime',
      '.avi': 'video/x-msvideo',
      '.m3u8': 'application/vnd.apple.mpegurl',
      '.ts': 'video/MP2T',
      '.jpg': 'image/jpeg',
      '.jpeg': 'image/jpeg',
      '.png': 'image/png',
      '.gif': 'image/gif',
      '.webp': 'image/webp',
      '.wgt': 'application/zip',  // .wgt files are ZIP archives
      '.xml': 'application/xml; charset=utf-8',
    };

    for (const ext in map) {
      if (filePath.endsWith(ext)) {
        res.setHeader('Content-Type', map[ext]);
      }
    }

    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Cache-Control', 'public, max-age=3600');
  },
}));

// Serve player app downloads (APK, WGT, widget.xml)
app.use('/downloads', express.static(downloadsStaticRoot, {
  setHeaders: (res, filePath) => {
    if (filePath.endsWith('.wgt')) {
      // Samsung SSSP accepts multiple MIME types for .wgt files
      // Using application/zip since .wgt files are ZIP archives
      res.setHeader('Content-Type', 'application/zip');
      res.setHeader('Content-Disposition', 'attachment; filename="signox-player.wgt"');
    } else if (filePath.endsWith('.xml')) {
      res.setHeader('Content-Type', 'application/xml; charset=utf-8');
    } else if (filePath.endsWith('.apk')) {
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      res.setHeader('Content-Disposition', 'attachment; filename="signox-player.apk"');
    }
    
    // Essential headers for Samsung SSSP
    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Cache-Control', 'public, max-age=3600');
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Range, Content-Length');
  },
}));

/* =========================
   ROUTES
========================= */

app.use('/api/auth', authLimiter, auditAuth, authRoutes);
app.use('/api/users', logSensitiveAccess, userRoutes);
app.use('/api/displays', displayRoutes);
app.use('/api/player', playerRoutes);
app.use('/api/media', mediaRoutes);
app.use('/api/playlists', playlistRoutes);
app.use('/api/layouts', layoutRoutes);
app.use('/api/schedules', require('./routes/schedule.routes'));
app.use('/api/analytics', analyticsRoutes);
app.use('/api/admin', logSensitiveAccess, adminRoutes);
app.use('/api/proof-of-play', proofOfPlayRoutes);
app.use('/api/monitoring', monitoringRoutes);
app.use('/api/player-apps', require('./routes/playerApps.routes'));
app.use('/api/display-groups', require('./routes/displayGroup.routes'));

/* =========================
   HEALTH & ROOT
========================= */

app.get('/', (req, res) => {
  res.send('SignoX Backend Running');
});

app.get('/health', (req, res) => {
  res.json({
    status: 'OK',
    service: 'SignoX Backend',
    uptime: process.uptime(),
    environment: process.env.NODE_ENV,
  });
});

/* =========================
   ERROR HANDLER
========================= */

app.use(globalErrorHandler);

/* =========================
   SERVER START
========================= */

/* =========================
   LICENSE EXPIRY CHECK
========================= */

let licenseExpiryInterval;

function startLicenseExpiryCheck() {
  console.log('🔄 [LICENSE EXPIRY] Starting license expiry check service...');
  
  // Run immediately on startup
  licenseExpiryService.checkAndSuspendExpiredLicenses()
    .then(result => {
      console.log(`✅ [LICENSE EXPIRY] Initial check completed: ${result.message}`);
    })
    .catch(error => {
      console.error('❌ [LICENSE EXPIRY] Initial check failed:', error);
    });
  
  // Run every 24 hours (86400000 ms)
  licenseExpiryInterval = setInterval(async () => {
    try {
      const result = await licenseExpiryService.checkAndSuspendExpiredLicenses();
      console.log(`✅ [LICENSE EXPIRY] Daily check completed: ${result.message}`);
    } catch (error) {
      console.error('❌ [LICENSE EXPIRY] Daily check failed:', error);
    }
  }, 24 * 60 * 60 * 1000); // 24 hours
}

const PORT = process.env.PORT || 5000;
const HTTPS_PORT = process.env.HTTPS_PORT || 5443;
const HOST = '0.0.0.0';
const ENABLE_HTTPS = process.env.ENABLE_HTTPS === 'true';

let server;
let displayStatusInterval;

if (ENABLE_HTTPS) {
  // HTTPS Server with SSL/TLS
  try {
    const sslOptions = {
      key: fs.readFileSync(path.resolve(process.env.SSL_KEY_PATH)),
      cert: fs.readFileSync(path.resolve(process.env.SSL_CERT_PATH)),
    };

    server = https.createServer(sslOptions, app);
    
    server.listen(HTTPS_PORT, HOST, () => {
      console.log(`🔒 SignoX Backend running on HTTPS at ${HOST}:${HTTPS_PORT}`);
      
      if (process.env.NODE_ENV === 'production') {
        cleanupService.start();
        healthService.start();
        licenseCheckService.start();
        databaseOptimizationService.optimizeConnectionPool();
      }
      
      // Start display status service in all environments
      displayStatusInterval = displayStatusService.startDisplayStatusService();
      
      // Start license expiry check (runs daily)
      startLicenseExpiryCheck();
    });

    // Optional: Redirect HTTP to HTTPS
    const httpApp = express();
    httpApp.use((req, res) => {
      res.redirect(301, `https://${req.headers.host.replace(PORT, HTTPS_PORT)}${req.url}`);
    });
    
    http.createServer(httpApp).listen(PORT, HOST);

  } catch (error) {
    console.error('❌ Failed to start HTTPS server:', error.message);
    process.exit(1);
  }
} else {
  // HTTP Server (for development or when behind load balancer)
  server = http.createServer(app);
  
  server.listen(PORT, HOST, () => {
    console.log(`🚀 SignoX Backend running on HTTP at ${HOST}:${PORT}`);

    if (process.env.NODE_ENV === 'production') {
      cleanupService.start();
      healthService.start();
      licenseCheckService.start();
      databaseOptimizationService.optimizeConnectionPool();
    }
    
    // Start display status service in all environments
    displayStatusInterval = displayStatusService.startDisplayStatusService();
    
    // Start license expiry check (runs daily)
    startLicenseExpiryCheck();
  });
}

/* =========================
   GRACEFUL SHUTDOWN
========================= */

const shutdown = () => {
  cleanupService.stop();
  healthService.stop();
  licenseCheckService.stop();
  displayStatusService.stopDisplayStatusService(displayStatusInterval);
  cacheService.disconnect();
  databaseOptimizationService.disconnect();
  server.close(() => process.exit(0));
};

process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
