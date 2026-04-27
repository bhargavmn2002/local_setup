/**
 * SignoX Player Configuration
 */

const CONFIG = {
    // Server Configuration
    SERVER_URL: 'https://signoxcms.com/api',
    MEDIA_BASE_URL: 'https://signoxcms.com',
    
    // Polling Intervals (milliseconds)
    PAIRING_POLL_INTERVAL: 5000,
    CONFIG_POLL_INTERVAL: 5000,
    HEARTBEAT_INTERVAL: 30000,
    
    // Player Settings
    DEFAULT_IMAGE_DURATION: 10,
    
    // Debug
    DEBUG_MODE: false,   // Disabled for production
    LOG_LEVEL: 'info'    // Reduced logging for production
};

// Storage Keys
const STORAGE_KEYS = {
    DEVICE_ID: 'signox_device_id',
    DEVICE_TOKEN: 'signox_device_token',
    DISPLAY_ID: 'signox_display_id',
    PAIRING_CODE: 'signox_pairing_code',
    CACHED_CONFIG: 'signox_cached_config'
};

Object.freeze(CONFIG);
Object.freeze(STORAGE_KEYS);

console.log('✅ Config loaded');
