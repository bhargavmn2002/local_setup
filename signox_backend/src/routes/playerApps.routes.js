const express = require('express');
const router = express.Router();
const { getDownloadUrl, trackDownload, getSSSPWidget } = require('../controllers/playerApps.controller');
const { requireAuth } = require('../middleware/auth.middleware');

/**
 * @route   GET /api/player-apps/download-url
 * @desc    Get download URL for player apps (APK or WGT)
 * @access  Private (Authenticated users)
 * @query   type - 'android' or 'tizen'
 */
router.get('/download-url', requireAuth, getDownloadUrl);

/**
 * @route   GET /api/player-apps/sssp-widget
 * @desc    Get SSSP widget configuration for Samsung displays
 * @access  Public (Samsung displays need direct access)
 */
router.get('/sssp-widget', getSSSPWidget);

/**
 * @route   POST /api/player-apps/track-download
 * @desc    Track download analytics
 * @access  Private
 */
router.post('/track-download', requireAuth, trackDownload);

module.exports = router;
