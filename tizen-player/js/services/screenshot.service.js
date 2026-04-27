/**
 * Screenshot Service - Handles screenshot capture and upload
 */

console.log('📸 Screenshot Service script loaded');

const ScreenshotService = {
    isCapturing: false,
    checkInterval: null,
    
    init() {
        console.log('📸 Screenshot service init() called');
        Logger.info('ScreenshotService', '📸 Screenshot service initialized');
        
        // Test if we're properly paired
        const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
        const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);
        
        console.log('📸 Display pairing check:', { displayId: displayId ? 'EXISTS' : 'MISSING', deviceToken: deviceToken ? 'EXISTS' : 'MISSING' });
        
        if (displayId && deviceToken) {
            Logger.info('ScreenshotService', `✅ Display paired: ${displayId}`);
            console.log('📸 Starting polling...');
            this.startPolling();
        } else {
            Logger.warn('ScreenshotService', '⚠️ Display not paired yet, waiting...');
            console.log('📸 Not paired, retrying in 5 seconds...');
            // Check again in 5 seconds
            setTimeout(() => this.init(), 5000);
        }
    },
    
    startPolling() {
        // Don't start if already polling
        if (this.checkInterval) {
            return;
        }
        
        // Check for screenshot requests every 10 seconds
        this.checkInterval = setInterval(() => {
            this.checkForRequests();
        }, 10000);
        
        Logger.info('ScreenshotService', '🔄 Started polling for screenshot requests every 10 seconds');
        
        // Do an immediate check
        this.checkForRequests();
    },
    
    stopPolling() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
        }
        Logger.info('ScreenshotService', '⏹️ Stopped polling for screenshot requests');
    },
    
    async checkForRequests() {
        try {
            const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
            const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);
            
            if (!displayId || !deviceToken) {
                Logger.warn('ScreenshotService', '⚠️ Not paired yet, skipping request check');
                return;
            }
            
            Logger.debug('ScreenshotService', '🔍 Checking for screenshot requests...');
            
            // Check for actual screenshot requests from the backend
            const response = await fetch(`${CONFIG.API_BASE_URL}/displays/${displayId}/screenshot-requests`, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${deviceToken}`
                }
            });
            
            if (response.ok) {
                const data = await response.json();
                Logger.debug('ScreenshotService', '� Request check response:', data);
                
                if (data.hasRequest && !this.isCapturing) {
                    Logger.info('ScreenshotService', '🎯 Screenshot request detected from dashboard!');
                    await this.captureAndUpload();
                } else if (!data.hasRequest) {
                    Logger.debug('ScreenshotService', '✅ No pending screenshot requests');
                }
            } else {
                Logger.warn('ScreenshotService', `❌ Failed to check screenshot requests: ${response.status}`);
            }
            
        } catch (error) {
            Logger.error('ScreenshotService', '❌ Error checking for requests:', error);
        }
    },
    
    async captureAndUpload() {
        if (this.isCapturing) {
            Logger.warn('ScreenshotService', 'Screenshot capture already in progress');
            return;
        }
        
        try {
            this.isCapturing = true;
            Logger.info('ScreenshotService', '📸 Starting screenshot capture...');
            
            // Capture the current screen
            const imageBlob = await this.captureScreen();
            
            if (imageBlob) {
                Logger.info('ScreenshotService', '✅ Screenshot captured, uploading...');
                await this.uploadScreenshot(imageBlob);
                Logger.info('ScreenshotService', '✅ Screenshot uploaded successfully');
            } else {
                Logger.error('ScreenshotService', '❌ Failed to capture screenshot');
            }
            
        } catch (error) {
            Logger.error('ScreenshotService', 'Error capturing/uploading screenshot:', error);
        } finally {
            this.isCapturing = false;
        }
    },
    
    async captureScreen() {
        return new Promise((resolve) => {
            try {
                // Create a canvas element
                const canvas = document.createElement('canvas');
                const ctx = canvas.getContext('2d');
                
                // Set canvas size to screen size
                canvas.width = window.innerWidth;
                canvas.height = window.innerHeight;
                
                // Fill with background color
                ctx.fillStyle = '#000000';
                ctx.fillRect(0, 0, canvas.width, canvas.height);
                
                // Try to capture the current player content
                this.capturePlayerContent(ctx, canvas.width, canvas.height);
                
                // Convert canvas to blob
                canvas.toBlob((blob) => {
                    if (blob) {
                        Logger.info('ScreenshotService', `📸 Screenshot captured: ${blob.size} bytes`);
                        resolve(blob);
                    } else {
                        Logger.error('ScreenshotService', '❌ Failed to create blob from canvas');
                        resolve(null);
                    }
                }, 'image/jpeg', 0.8);
                
            } catch (error) {
                Logger.error('ScreenshotService', 'Error in captureScreen:', error);
                resolve(null);
            }
        });
    },
    
    capturePlayerContent(ctx, width, height) {
        try {
            // Try to capture playlist player content
            const playlistImage = document.getElementById('playlist-image');
            const playlistVideo = document.getElementById('playlist-video');
            
            if (playlistImage && playlistImage.style.display !== 'none' && playlistImage.src) {
                // Draw the current image
                ctx.drawImage(playlistImage, 0, 0, width, height);
                Logger.info('ScreenshotService', '🖼️ Captured playlist image');
                return;
            }
            
            if (playlistVideo && playlistVideo.style.display !== 'none' && !playlistVideo.paused) {
                // Draw the current video frame
                ctx.drawImage(playlistVideo, 0, 0, width, height);
                Logger.info('ScreenshotService', '🎥 Captured playlist video frame');
                return;
            }
            
            // Try to capture layout player content
            const layoutPlayer = document.getElementById('layout-player');
            if (layoutPlayer && layoutPlayer.style.display !== 'none') {
                // For layout player, we'll capture the first visible media element
                const mediaElements = layoutPlayer.querySelectorAll('img, video');
                for (const element of mediaElements) {
                    if (element.style.display !== 'none') {
                        const rect = element.getBoundingClientRect();
                        ctx.drawImage(element, rect.left, rect.top, rect.width, rect.height);
                        Logger.info('ScreenshotService', '🎬 Captured layout content');
                        return;
                    }
                }
            }
            
            // Fallback: Draw current screen info
            ctx.fillStyle = '#ffffff';
            ctx.font = '48px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('SignoX Player', width / 2, height / 2 - 50);
            
            ctx.font = '24px Arial';
            ctx.fillText('Screenshot captured at ' + new Date().toLocaleTimeString(), width / 2, height / 2 + 50);
            
            // Add current content info
            const cachedConfig = Storage.get(STORAGE_KEYS.CACHED_CONFIG);
            if (cachedConfig) {
                let contentInfo = 'No content';
                if (cachedConfig.playlist) {
                    contentInfo = `Playlist: ${cachedConfig.playlist.name}`;
                } else if (cachedConfig.layout) {
                    contentInfo = `Layout: ${cachedConfig.layout.name}`;
                }
                
                ctx.fillText(contentInfo, width / 2, height / 2 + 100);
            }
            
            Logger.info('ScreenshotService', '📝 Drew fallback screenshot content');
            
        } catch (error) {
            Logger.error('ScreenshotService', 'Error capturing player content:', error);
            
            // Ultimate fallback
            ctx.fillStyle = '#ffffff';
            ctx.font = '36px Arial';
            ctx.textAlign = 'center';
            ctx.fillText('Screenshot Error', width / 2, height / 2);
        }
    },
    
    async uploadScreenshot(imageBlob) {
        try {
            const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
            const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);
            
            if (!displayId || !deviceToken) {
                throw new Error('Display not paired');
            }
            
            Logger.info('ScreenshotService', `📤 Uploading screenshot (${imageBlob.size} bytes)...`);
            
            // Create form data
            const formData = new FormData();
            formData.append('screenshot', imageBlob, `screenshot-${Date.now()}.jpg`);
            
            // Add context information
            const cachedConfig = Storage.get(STORAGE_KEYS.CACHED_CONFIG);
            if (cachedConfig) {
                if (cachedConfig.playlist) {
                    formData.append('currentMediaName', this.getCurrentPlaylistMedia());
                } else if (cachedConfig.layout) {
                    formData.append('currentMediaName', 'Layout Content');
                }
            }
            
            formData.append('deviceInfo', 'Tizen Player');
            formData.append('playerVersion', '1.0.0');
            
            // Upload using fetch
            const response = await fetch(`${CONFIG.API_BASE_URL}/displays/${displayId}/screenshot`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${deviceToken}`
                },
                body: formData
            });
            
            const responseText = await response.text();
            Logger.info('ScreenshotService', `📤 Upload response (${response.status}):`, responseText);
            
            if (!response.ok) {
                let errorData = {};
                try {
                    errorData = JSON.parse(responseText);
                } catch (e) {
                    errorData = { error: responseText };
                }
                throw new Error(errorData.error || `HTTP ${response.status}`);
            }
            
            const result = JSON.parse(responseText);
            Logger.info('ScreenshotService', '✅ Screenshot uploaded successfully:', result.screenshot?.id);
            
            // Show success message in debug overlay
            this.showUploadSuccess();
            
        } catch (error) {
            Logger.error('ScreenshotService', '❌ Error uploading screenshot:', error);
            this.showUploadError(error.message);
            throw error;
        }
    },
    
    showUploadSuccess() {
        // Update debug overlay if visible
        const debugOverlay = document.getElementById('debug-overlay');
        const debugScreenshot = document.getElementById('debug-screenshot');
        
        if (debugScreenshot) {
            debugScreenshot.innerHTML = 'Screenshots: ✅ Last uploaded successfully';
            debugScreenshot.style.color = '#0f0';
        }
        
        if (debugOverlay && debugOverlay.style.display !== 'none') {
            const statusDiv = document.createElement('div');
            statusDiv.style.cssText = 'color: #0f0; font-weight: bold; margin-top: 5px;';
            statusDiv.textContent = '✅ Screenshot uploaded to dashboard!';
            debugOverlay.appendChild(statusDiv);
            
            setTimeout(() => {
                if (statusDiv.parentNode) {
                    statusDiv.parentNode.removeChild(statusDiv);
                }
                if (debugScreenshot) {
                    debugScreenshot.innerHTML = 'Screenshots: 🔄 Polling for requests...';
                    debugScreenshot.style.color = '#ff0';
                }
            }, 5000);
        }
    },
    
    showUploadError(message) {
        // Update debug overlay if visible
        const debugOverlay = document.getElementById('debug-overlay');
        if (debugOverlay && debugOverlay.style.display !== 'none') {
            const statusDiv = document.createElement('div');
            statusDiv.style.cssText = 'color: #f44; font-weight: bold; margin-top: 5px;';
            statusDiv.textContent = `❌ Upload failed: ${message}`;
            debugOverlay.appendChild(statusDiv);
            
            setTimeout(() => {
                if (statusDiv.parentNode) {
                    statusDiv.parentNode.removeChild(statusDiv);
                }
            }, 5000);
        }
    },
    
    getCurrentPlaylistMedia() {
        try {
            const cachedConfig = Storage.get(STORAGE_KEYS.CACHED_CONFIG);
            if (cachedConfig?.playlist?.items?.length > 0) {
                // Try to determine current media (simplified)
                const currentItem = cachedConfig.playlist.items[0]; // First item as fallback
                return currentItem.media?.name || 'Unknown Media';
            }
            return 'No Media';
        } catch (error) {
            return 'Unknown Media';
        }
    },
    
    // Manual screenshot trigger (for testing)
    async triggerScreenshot() {
        Logger.info('ScreenshotService', '🎯 Manual screenshot triggered');
        await this.captureAndUpload();
    }
};

// Global function for testing
window.takeScreenshot = () => {
    console.log('🎯 Manual screenshot triggered from window.takeScreenshot');
    if (typeof ScreenshotService !== 'undefined') {
        ScreenshotService.triggerScreenshot();
    } else {
        console.error('❌ ScreenshotService not available');
    }
};

// Global function to check screenshot service status
window.checkScreenshotService = () => {
    console.log('🔍 Screenshot Service Status:');
    console.log('- Service exists:', typeof ScreenshotService !== 'undefined');
    console.log('- Is capturing:', ScreenshotService?.isCapturing);
    console.log('- Polling active:', ScreenshotService?.checkInterval !== null);
    
    const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
    const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);
    console.log('- Display ID:', displayId ? 'EXISTS' : 'MISSING');
    console.log('- Device Token:', deviceToken ? 'EXISTS' : 'MISSING');
    
    return {
        serviceExists: typeof ScreenshotService !== 'undefined',
        isCapturing: ScreenshotService?.isCapturing,
        pollingActive: ScreenshotService?.checkInterval !== null,
        displayId: displayId ? 'EXISTS' : 'MISSING',
        deviceToken: deviceToken ? 'EXISTS' : 'MISSING'
    };
};

console.log('📸 Screenshot service functions registered globally');