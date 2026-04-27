/**
 * Config Service - Handles pairing and config polling
 */

const ConfigService = {
    pairingInterval: null,
    configInterval: null,
    state: 'idle',
    onStateChange: null,
    lastConfigHash: null,

    async startPairingFlow() {
        Logger.info('ConfigService', '🚀 Starting pairing flow');
        
        // Check if already paired (has device token)
        const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);
        const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
        
        if (deviceToken && displayId) {
            Logger.info('ConfigService', '✅ Already paired - using existing token');
            this.setState('paired');
            this.startConfigPolling();
            return;
        }
        
        // Check for existing display
        this.setState('checking');
        const existingDisplay = await this.checkExistingDisplay();
        
        if (existingDisplay && existingDisplay.isPaired && existingDisplay.deviceToken) {
            // Already paired, save token and start
            Logger.info('ConfigService', '✅ Found existing pairing');
            Storage.set(STORAGE_KEYS.DEVICE_TOKEN, existingDisplay.deviceToken);
            Storage.set(STORAGE_KEYS.DISPLAY_ID, existingDisplay.displayId);
            this.setState('paired');
            this.startConfigPolling();
            return;
        }
        
        // Generate new pairing code
        const result = await API.generatePairingCode();
        
        if (result.success && result.data) {
            const pairingCode = result.data.pairingCode || result.data.code;
            const displayId = result.data.displayId || result.data.id;
            
            Storage.set(STORAGE_KEYS.PAIRING_CODE, pairingCode);
            Storage.set(STORAGE_KEYS.DISPLAY_ID, displayId);
            
            Logger.info('ConfigService', `✓ Pairing code: ${pairingCode}`);
            this.setState('pairing', pairingCode);
            this.startPairingPoll();
        } else {
            Logger.error('ConfigService', 'Failed to generate pairing code');
            this.setState('error', 'Failed to connect to server');
        }
    },
    
    async checkExistingDisplay() {
        try {
            const deviceId = Storage.get(STORAGE_KEYS.DEVICE_ID);
            if (!deviceId) {
                return null;
            }
            
            Logger.debug('ConfigService', 'Checking existing display...');
            
            const result = await API.request(`/displays/${deviceId}/status`);
            
            if (result.success && result.data) {
                return result.data;
            } else if (result.status === 404) {
                // Display was deleted, clear storage
                Logger.warn('ConfigService', 'Display was deleted - clearing pairing');
                this.clearPairing();
                return null;
            }
            
            return null;
        } catch (error) {
            Logger.error('ConfigService', 'Error checking existing display:', error);
            return null;
        }
    },

    startPairingPoll() {
        this.stopPairingPoll();
        Logger.info('ConfigService', 'Starting pairing poll...');
        
        // Poll immediately
        this.checkPairingStatus();
        
        // Then poll every 5 seconds
        this.pairingInterval = setInterval(() => {
            this.checkPairingStatus();
        }, CONFIG.PAIRING_POLL_INTERVAL);
    },

    async checkPairingStatus() {
        const pairingCode = Storage.get(STORAGE_KEYS.PAIRING_CODE);
        const deviceId = Storage.get(STORAGE_KEYS.DEVICE_ID);
        
        if (!pairingCode || !deviceId) return;

        Logger.debug('ConfigService', 'Checking pairing status...');
        
        const result = await API.request('/displays/check-status', {
            method: 'POST',
            body: JSON.stringify({ deviceId, pairingCode })
        });
        
        if (result.success && result.data) {
            if (result.data.isPaired && result.data.deviceToken) {
                Logger.info('ConfigService', '🎉 Device paired successfully!');
                
                Storage.set(STORAGE_KEYS.DEVICE_TOKEN, result.data.deviceToken);
                Storage.set(STORAGE_KEYS.DISPLAY_ID, result.data.displayId);
                
                this.stopPairingPoll();
                this.setState('paired');
                this.startConfigPolling();
            }
        } else if (result.status === 401) {
            // Unauthorized - pairing expired
            Logger.warn('ConfigService', 'Pairing expired - clearing and restarting');
            this.clearPairing();
            this.stopPairingPoll();
            this.setState('error', 'Pairing expired. Please restart.');
        }
    },

    startConfigPolling() {
        this.stopConfigPolling();
        Logger.info('ConfigService', 'Starting config polling...');
        
        // Fetch immediately
        this.fetchConfig();
        
        // Then poll every 5 seconds
        this.configInterval = setInterval(() => {
            this.fetchConfig();
        }, CONFIG.CONFIG_POLL_INTERVAL);
    },

    async fetchConfig() {
        Logger.debug('ConfigService', 'Fetching config...');
        
        const result = await API.request('/player/config');
        
        if (result.success && result.data) {
            Logger.info('ConfigService', '✓ Config received');
            Logger.debug('ConfigService', 'Full config data:', JSON.stringify(result.data, null, 2));
            
            // Save config for offline use
            Storage.set(STORAGE_KEYS.CACHED_CONFIG, result.data);
            
            this.setState('config-updated', result.data);
            
            // Queue downloads for offline caching
            if (typeof DownloadService !== 'undefined') {
                Logger.info('ConfigService', '📦 Queueing media for download...');
                
                if (result.data.layout) {
                    Logger.info('ConfigService', `📦 Found layout: ${result.data.layout.name}`);
                    Logger.info('ConfigService', `📦 Layout sections: ${result.data.layout.sections?.length || 0}`);
                    
                    // Log all media in layout
                    if (result.data.layout.sections) {
                        result.data.layout.sections.forEach((section, sIndex) => {
                            Logger.info('ConfigService', `📦 Section ${sIndex + 1}: ${section.name} (${section.items?.length || 0} items)`);
                            if (section.items) {
                                section.items.forEach((item, iIndex) => {
                                    if (item.media) {
                                        const media = item.media;
                                        const mediaUrl = (media.type === 'VIDEO' && media.originalUrl) ? media.originalUrl : media.url;
                                        Logger.info('ConfigService', `📦   Item ${iIndex + 1}: ${media.name} (${media.type})`);
                                        Logger.info('ConfigService', `📦   URL: ${mediaUrl}`);
                                        Logger.info('ConfigService', `📦   Size: ${media.fileSize ? CacheService.formatBytes(media.fileSize) : 'unknown'}`);
                                    }
                                });
                            }
                        });
                    }
                    
                    Logger.info('ConfigService', '📦 Calling DownloadService.downloadLayout...');
                    await DownloadService.downloadLayout(result.data.layout);
                    Logger.info('ConfigService', '📦 DownloadService.downloadLayout completed');
                } else if (result.data.playlist) {
                    Logger.info('ConfigService', `📦 Found playlist: ${result.data.playlist.name}`);
                    Logger.info('ConfigService', `📦 Playlist items: ${result.data.playlist.items?.length || 0}`);
                    
                    // Log all media in playlist
                    if (result.data.playlist.items) {
                        result.data.playlist.items.forEach((item, index) => {
                            if (item.media) {
                                const media = item.media;
                                const mediaUrl = (media.type === 'VIDEO' && media.originalUrl) ? media.originalUrl : media.url;
                                Logger.info('ConfigService', `📦 Item ${index + 1}: ${media.name} (${media.type})`);
                                Logger.info('ConfigService', `📦 URL: ${mediaUrl}`);
                                Logger.info('ConfigService', `📦 Size: ${media.fileSize ? CacheService.formatBytes(media.fileSize) : 'unknown'}`);
                            }
                        });
                    }
                    
                    Logger.info('ConfigService', '📦 Calling DownloadService.downloadPlaylist...');
                    await DownloadService.downloadPlaylist(result.data.playlist);
                    Logger.info('ConfigService', '📦 DownloadService.downloadPlaylist completed');
                } else {
                    Logger.info('ConfigService', '📦 No content to download (no layout or playlist)');
                }
            } else {
                Logger.warn('ConfigService', '⚠️ DownloadService not available!');
            }
        } else if (result.status === 401) {
            // Unauthorized - token invalid, restart pairing
            Logger.warn('ConfigService', 'Unauthorized - restarting pairing flow');
            this.clearPairing();
            this.stopConfigPolling();
            this.setState('error', 'Session expired. Restarting...');
            
            // Restart pairing after delay
            setTimeout(() => {
                this.startPairingFlow();
            }, 2000);
        } else {
            // Network error - try to use cached config
            const cachedConfig = Storage.get(STORAGE_KEYS.CACHED_CONFIG);
            if (cachedConfig) {
                Logger.info('ConfigService', '📦 Using cached config (offline mode)');
                this.setState('config-updated', cachedConfig);
            } else {
                Logger.error('ConfigService', 'Config fetch failed and no cache available');
            }
        }
    },
    
    clearPairing() {
        Storage.remove(STORAGE_KEYS.DEVICE_TOKEN);
        Storage.remove(STORAGE_KEYS.DISPLAY_ID);
        Storage.remove(STORAGE_KEYS.PAIRING_CODE);
        Logger.info('ConfigService', 'Pairing cleared');
    },
    
    resetPairing() {
        this.clearPairing();
        this.stopAll();
        this.startPairingFlow();
    },
    
    retryConnection() {
        this.stopAll();
        this.startPairingFlow();
    },

    stopPairingPoll() {
        if (this.pairingInterval) {
            clearInterval(this.pairingInterval);
            this.pairingInterval = null;
        }
    },
    
    stopConfigPolling() {
        if (this.configInterval) {
            clearInterval(this.configInterval);
            this.configInterval = null;
        }
    },
    
    stopAll() {
        this.stopPairingPoll();
        this.stopConfigPolling();
    },

    setState(state, data = null) {
        this.state = state;
        Logger.debug('ConfigService', `State: ${state}`);
        
        if (this.onStateChange) {
            this.onStateChange(state, data);
        }
    }
};

console.log('✅ Config Service loaded');
