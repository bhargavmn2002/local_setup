/**
 * Playlist Player - Sequential playback
 */

const PlaylistPlayer = {
    playlist: null,
    currentIndex: 0,
    timer: null,
    imageElement: null,
    videoElement: null,
    isPlaying: false,
    lastLoggedTime: -1,

    init() {
        this.imageElement = document.getElementById('playlist-image');
        this.videoElement = document.getElementById('playlist-video');
        
        this.videoElement.addEventListener('ended', () => this.onVideoEnded());
        this.videoElement.addEventListener('error', (e) => this.onVideoError(e));
        this.videoElement.addEventListener('timeupdate', () => this.onVideoTimeUpdate());
        this.videoElement.addEventListener('pause', () => this.onVideoPause());
        this.videoElement.addEventListener('stalled', () => this.onVideoStalled());
        this.videoElement.addEventListener('waiting', () => this.onVideoWaiting());
        
        Logger.debug('PlaylistPlayer', 'Initialized');
    },

    async play(playlist) {
        if (!playlist || !playlist.items || playlist.items.length === 0) {
            Logger.warn('PlaylistPlayer', 'Empty playlist');
            return;
        }

        Logger.info('PlaylistPlayer', `Playing playlist: ${playlist.name} (${playlist.items.length} items)`);
        
        this.playlist = playlist;
        this.currentIndex = 0;
        this.isPlaying = true;
        
        await this.playCurrentItem();
    },

    async playCurrentItem() {
        if (!this.isPlaying || !this.playlist) return;

        const item = this.playlist.items[this.currentIndex];
        if (!item || !item.media) {
            Logger.warn('PlaylistPlayer', 'No item or media');
            this.advanceToNext();
            return;
        }

        try {
            Logger.debug('PlaylistPlayer', `Playing item ${this.currentIndex + 1}/${this.playlist.items.length}: ${item.media.name}`);
            
            if (typeof showDebug === 'function') {
                showDebug(`Playing: ${item.media.name} (${item.media.type})`);
            }

            if (item.media.type === 'IMAGE') {
                await this.playImage(item);
            } else if (item.media.type === 'VIDEO') {
                await this.playVideo(item);
            } else {
                Logger.warn('PlaylistPlayer', 'Unknown media type:', item.media.type);
                this.advanceToNext();
            }
        } catch (error) {
            Logger.error('PlaylistPlayer', 'Error playing item:', error);
            if (typeof showDebug === 'function') {
                showDebug('ERROR playing: ' + error.message);
            }
            this.advanceToNext();
        }
    },

    async playImage(item) {
        try {
            Logger.debug('PlaylistPlayer', 'Playing image:', item.media.name);

            // Clear any existing timer
            if (this.timer) {
                clearTimeout(this.timer);
                this.timer = null;
            }

            // Hide video, show image
            this.videoElement.style.display = 'none';
            this.imageElement.style.display = 'block';

            // Check if online
            const isOnline = typeof ConnectionMonitor !== 'undefined' ? ConnectionMonitor.isOnline() : true;
            
            let mediaUrl = null;
            
            if (!isOnline) {
                // Offline: Try cache first
                if (typeof CacheService !== 'undefined') {
                    mediaUrl = await CacheService.getCachedMedia(item.media.url);
                    if (mediaUrl) {
                        Logger.debug('PlaylistPlayer', 'Using cached image (offline)');
                    } else {
                        Logger.warn('PlaylistPlayer', 'Image not cached, cannot play offline');
                        this.advanceToNext();
                        return;
                    }
                }
            } else {
                // Online: Stream and cache in background
                mediaUrl = this.getMediaUrl(item.media.url);
                Logger.debug('PlaylistPlayer', 'Streaming image');
                this.cacheMediaInBackground(item.media.url, mediaUrl);
            }
            
            // Update debug overlay
            if (typeof updateCurrentMedia === 'function') {
                updateCurrentMedia(item.media.name || 'Video', isCached);
            }
            
            this.imageElement.src = mediaUrl;

            // Schedule advance
            const duration = item.duration || CONFIG.DEFAULT_IMAGE_DURATION;
            this.timer = setTimeout(() => {
                this.advanceToNext();
            }, duration * 1000);
            
            if (typeof showDebug === 'function') {
                showDebug(`Image playing (${duration}s)`);
            }
        } catch (error) {
            Logger.error('PlaylistPlayer', 'Image error:', error);
            if (typeof showDebug === 'function') {
                showDebug('Image ERROR: ' + error.message);
            }
            this.advanceToNext();
        }
    },

    async playVideo(item) {
        try {
            // Reset lastLoggedTime for new video
            this.lastLoggedTime = -1;
            
            Logger.debug('PlaylistPlayer', 'Playing video:', item.media.name);
            Logger.debug('PlaylistPlayer', 'Video settings:', {
                loopVideo: item.loopVideo,
                duration: item.duration,
                mediaName: item.media.name
            });

            // Clear any existing timer
            if (this.timer) {
                clearTimeout(this.timer);
                this.timer = null;
            }

            // Hide image, show video
            this.imageElement.style.display = 'none';
            this.videoElement.style.display = 'block';

            // Debug: Show ALL media fields
            Logger.debug('PlaylistPlayer', '=== MEDIA OBJECT DEBUG ===');
            Logger.debug('PlaylistPlayer', 'Full media object:', JSON.stringify(item.media, null, 2));
            Logger.debug('PlaylistPlayer', '=========================');

            // ONLY use direct MP4 files - ignore HLS
            let videoUrl = null;
            
            // Check url field first (this is the primary field from backend)
            if (item.media.url && !item.media.url.includes('/hls/') && !item.media.url.includes('index.m3u8')) {
                videoUrl = item.media.url;
                Logger.debug('PlaylistPlayer', 'Using url field (MP4)');
            }
            // Check originalUrl as fallback
            else if (item.media.originalUrl && !item.media.originalUrl.includes('/hls/')) {
                videoUrl = item.media.originalUrl;
                Logger.debug('PlaylistPlayer', 'Using originalUrl (MP4)');
            } 
            
            if (!videoUrl) {
                Logger.error('PlaylistPlayer', 'No MP4 URL available');
                Logger.error('PlaylistPlayer', 'url:', item.media.url);
                Logger.error('PlaylistPlayer', 'originalUrl:', item.media.originalUrl);
                
                if (typeof showDebug === 'function') {
                    showDebug(`ERROR: No MP4 available`);
                    showDebug(`url: ${item.media.url || 'NULL'}`);
                    showDebug(`orig: ${item.media.originalUrl || 'NULL'}`);
                    showDebug(`Video needs re-upload`);
                }
                
                // Skip to next item after 3 seconds
                setTimeout(() => this.advanceToNext(), 3000);
                return;
            }
            
            // Try to get from cache first
            let mediaUrl = null;
            let isCached = false;
            
            // Check if online
            const isOnline = typeof ConnectionMonitor !== 'undefined' ? ConnectionMonitor.isOnline() : true;
            
            if (!isOnline) {
                // Offline: Try cache first
                Logger.info('PlaylistPlayer', 'OFFLINE MODE - Looking for cached video');
                Logger.info('PlaylistPlayer', `Searching cache for: ${videoUrl}`);
                
                if (typeof CacheService !== 'undefined') {
                    mediaUrl = await CacheService.getCachedMedia(videoUrl);
                    Logger.info('PlaylistPlayer', `Cache lookup result: ${mediaUrl ? 'FOUND' : 'NOT FOUND'}`);
                    
                    if (mediaUrl) {
                        isCached = true;
                        Logger.info('PlaylistPlayer', `✅ Using cached video: ${videoUrl}`);
                        Logger.info('PlaylistPlayer', `Cached blob URL: ${mediaUrl}`);
                    } else {
                        Logger.error('PlaylistPlayer', `❌ Video not in cache: ${videoUrl}`);
                        
                        // List all cached media to debug
                        if (CacheService.getAllMetadata) {
                            const allCached = await CacheService.getAllMetadata();
                            Logger.info('PlaylistPlayer', `All cached media (${allCached.length} items):`);
                            allCached.forEach((item, index) => {
                                Logger.info('PlaylistPlayer', `  ${index + 1}. ${item.url}`);
                            });
                        }
                        
                        if (typeof showDebug === 'function') {
                            showDebug('Video not cached - offline');
                        }
                        setTimeout(() => this.advanceToNext(), 3000);
                        return;
                    }
                } else {
                    Logger.error('PlaylistPlayer', 'CacheService not available');
                    setTimeout(() => this.advanceToNext(), 3000);
                    return;
                }
            } else {
                // Online: Stream and cache in background
                mediaUrl = this.getMediaUrl(videoUrl);
                Logger.info('PlaylistPlayer', `ONLINE MODE - Streaming video: ${videoUrl}`);
                Logger.info('PlaylistPlayer', `Full media URL: ${mediaUrl}`);
                this.cacheMediaInBackground(videoUrl, mediaUrl);
            }
            
            Logger.debug('PlaylistPlayer', '=== VIDEO URL DEBUG ===');
            Logger.debug('PlaylistPlayer', 'Selected MP4:', videoUrl);
            Logger.debug('PlaylistPlayer', 'Final URL:', mediaUrl);
            Logger.debug('PlaylistPlayer', 'Cached:', isCached);
            Logger.debug('PlaylistPlayer', '======================');
            
            if (typeof showDebug === 'function') {
                showDebug(`Video: ${item.media.name}`);
                showDebug(`File: ${videoUrl.substring(videoUrl.lastIndexOf('/') + 1)}`);
                showDebug(`URL: ${mediaUrl}`);
            }
            
            // Test if video URL is accessible (skip test for cached content)
            if (!isCached) {
                try {
                    Logger.debug('PlaylistPlayer', `Testing URL: ${mediaUrl}`);
                    const testResponse = await fetch(mediaUrl, { method: 'HEAD' });
                    Logger.debug('PlaylistPlayer', `URL test: ${testResponse.ok} (${testResponse.status})`);
                    
                    if (!testResponse.ok) {
                        Logger.error('PlaylistPlayer', `❌ MEDIA SERVER ERROR: ${testResponse.status}`);
                        Logger.error('PlaylistPlayer', `❌ URL: ${mediaUrl}`);
                        Logger.error('PlaylistPlayer', `❌ This indicates the live server is not serving media files correctly`);
                        
                        if (typeof showDebug === 'function') {
                            showDebug(`❌ MEDIA SERVER ERROR (${testResponse.status})`);
                            showDebug(`URL: ${mediaUrl}`);
                            showDebug(`Live server uploads not configured`);
                        }
                        throw new Error(`Media server error (${testResponse.status})`);
                    }
                    
                    Logger.info('PlaylistPlayer', `✅ Media URL accessible: ${mediaUrl}`);
                    if (typeof showDebug === 'function') {
                        showDebug(`✅ File found - Loading...`);
                    }
                } catch (fetchError) {
                    Logger.error('PlaylistPlayer', '❌ MEDIA ACCESS FAILED:', fetchError);
                    Logger.error('PlaylistPlayer', '❌ Failed URL:', mediaUrl);
                    Logger.error('PlaylistPlayer', '❌ Check if live server uploads directory is configured');
                    
                    if (typeof showDebug === 'function') {
                        showDebug(`❌ MEDIA ACCESS FAILED`);
                        showDebug(`Error: ${fetchError.message}`);
                        showDebug(`Check live server uploads config`);
                    }
                    // Skip to next after 3 seconds
                    setTimeout(() => this.advanceToNext(), 3000);
                    return;
                }
            } else {
                Logger.info('PlaylistPlayer', 'Skipping URL test for cached video');
                if (typeof showDebug === 'function') {
                    showDebug(`Using cached video - Ready!`);
                }
            }
            
            // Reset video element
            this.videoElement.pause();
            this.videoElement.src = '';
            
            // Set new source and loop settings
            this.videoElement.src = mediaUrl;
            this.videoElement.loop = item.loopVideo || false;
            this.videoElement.preload = 'metadata';
            
            Logger.debug('PlaylistPlayer', `Video loop set to: ${this.videoElement.loop}`);
            
            // Try to load metadata, but don't block on it
            try {
                await new Promise((resolve, reject) => {
                    const timeout = setTimeout(() => {
                        Logger.warn('PlaylistPlayer', 'Metadata load slow, proceeding anyway...');
                        resolve(); // Don't reject, just proceed
                    }, 5000); // 5 second timeout
                    
                    this.videoElement.onloadedmetadata = () => {
                        clearTimeout(timeout);
                        Logger.debug('PlaylistPlayer', `Metadata loaded - Duration: ${this.videoElement.duration}s`);
                        resolve();
                    };
                    
                    this.videoElement.load(); // Start loading
                });
            } catch (err) {
                Logger.warn('PlaylistPlayer', 'Metadata load issue:', err.message);
            }
            
            // Try to play immediately (will buffer as needed)
            try {
                await this.videoElement.play();
                Logger.debug('PlaylistPlayer', 'Video playing');
                Logger.debug('PlaylistPlayer', `Video duration: ${this.videoElement.duration}s`);
                Logger.debug('PlaylistPlayer', `Video paused state: ${this.videoElement.paused}`);
                Logger.debug('PlaylistPlayer', `Video readyState: ${this.videoElement.readyState}`);
                Logger.debug('PlaylistPlayer', `Video networkState: ${this.videoElement.networkState}`);
                
                // Update debug overlay
                if (typeof updateCurrentMedia === 'function') {
                    const isCached = !isOnline || (await CacheService.isCached(videoUrl));
                    updateCurrentMedia(item.media.name, isCached);
                }
                
                if (typeof showDebug === 'function') {
                    showDebug('Video playing');
                    showDebug(`Duration: ${Math.floor(this.videoElement.duration)}s`);
                    showDebug(`Paused: ${this.videoElement.paused}`);
                    if (item.loopVideo && item.duration) {
                        showDebug(`Will loop for ${item.duration}s`);
                    } else if (item.loopVideo) {
                        showDebug('Looping continuously');
                    } else {
                        showDebug('Playing once');
                    }
                }
                
                // Check video state after 2 seconds
                setTimeout(() => {
                    Logger.debug('PlaylistPlayer', `After 2s - currentTime: ${this.videoElement.currentTime}s, paused: ${this.videoElement.paused}`);
                    if (typeof showDebug === 'function') {
                        showDebug(`After 2s: ${Math.floor(this.videoElement.currentTime)}s`);
                    }
                }, 2000);
                
                // If looping with duration, schedule advance
                if (item.loopVideo && item.duration) {
                    Logger.debug('PlaylistPlayer', `Video will loop for ${item.duration} seconds`);
                    this.timer = setTimeout(() => {
                        Logger.debug('PlaylistPlayer', `Timer expired after ${item.duration}s, advancing...`);
                        this.advanceToNext();
                    }, item.duration * 1000);
                }
            } catch (playError) {
                // If play fails, wait for canplay event
                Logger.debug('PlaylistPlayer', 'Waiting for video to buffer...');
                
                if (typeof showDebug === 'function') {
                    showDebug('Buffering video...');
                }
                
                await new Promise((resolve, reject) => {
                    const timeout = setTimeout(() => {
                        reject(new Error('Video buffer timeout (30s)'));
                    }, 30000); // 30 second timeout
                    
                    this.videoElement.oncanplay = async () => {
                        clearTimeout(timeout);
                        try {
                            await this.videoElement.play();
                            resolve();
                        } catch (e) {
                            reject(e);
                        }
                    };
                    
                    this.videoElement.onerror = (e) => {
                        clearTimeout(timeout);
                        const error = this.videoElement.error;
                        let errorMsg = 'Video error';
                        if (error) {
                            switch(error.code) {
                                case 1: errorMsg = 'MEDIA_ERR_ABORTED'; break;
                                case 2: errorMsg = 'MEDIA_ERR_NETWORK'; break;
                                case 3: errorMsg = 'MEDIA_ERR_DECODE'; break;
                                case 4: errorMsg = 'MEDIA_ERR_SRC_NOT_SUPPORTED'; break;
                                default: errorMsg = 'Unknown error';
                            }
                            errorMsg += ` (${error.code})`;
                        }
                        reject(new Error(errorMsg));
                    };
                });
                
                Logger.debug('PlaylistPlayer', 'Video playing after buffer');
                
                if (typeof showDebug === 'function') {
                    showDebug('Video playing');
                }
                
                // If looping with duration, schedule advance
                if (item.loopVideo && item.duration) {
                    this.timer = setTimeout(() => {
                        this.advanceToNext();
                    }, item.duration * 1000);
                }
            }
        } catch (error) {
            Logger.error('PlaylistPlayer', 'Video play error:', error);
            if (typeof showDebug === 'function') {
                showDebug('Video ERROR: ' + error.message);
            }
            // Skip to next item
            setTimeout(() => this.advanceToNext(), 2000);
        }
    },

    getMediaUrl(path) {
        if (!path) return '';
        if (path.startsWith('http')) return path;
        
        // Ensure path starts with /
        const cleanPath = path.startsWith('/') ? path : `/${path}`;
        
        // Log the full URL for debugging
        const fullUrl = `${CONFIG.MEDIA_BASE_URL}${cleanPath}`;
        Logger.debug('PlaylistPlayer', `Full media URL: ${fullUrl}`);
        
        return fullUrl;
    },

    advanceToNext() {
        if (!this.isPlaying) return;

        // Clear any existing timer
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }

        this.currentIndex = (this.currentIndex + 1) % this.playlist.items.length;
        Logger.debug('PlaylistPlayer', `Advancing to item ${this.currentIndex + 1}`);
        
        if (typeof showDebug === 'function') {
            showDebug(`Next item: ${this.currentIndex + 1}`);
        }
        
        this.playCurrentItem();
    },

    onVideoEnded() {
        Logger.debug('PlaylistPlayer', 'Video ended naturally');
        if (typeof showDebug === 'function') {
            showDebug('Video ended');
        }
        if (!this.videoElement.loop) {
            this.advanceToNext();
        }
    },

    onVideoTimeUpdate() {
        // Log every second to track playback closely
        const currentTime = Math.floor(this.videoElement.currentTime);
        const duration = Math.floor(this.videoElement.duration);
        
        if (currentTime !== this.lastLoggedTime) {
            this.lastLoggedTime = currentTime;
            
            // Log every second
            if (currentTime % 1 === 0) {
                Logger.debug('PlaylistPlayer', `Video time: ${currentTime}s / ${duration}s`);
                
                // Show on screen every 5 seconds to avoid clutter
                if (currentTime % 5 === 0 && typeof showDebug === 'function') {
                    showDebug(`Playing: ${currentTime}s / ${duration}s`);
                }
            }
        }
        
        // Reset lastLoggedTime when video loops back to start
        if (currentTime < this.lastLoggedTime - 5) {
            Logger.debug('PlaylistPlayer', '🔄 Video looped back to start');
            if (typeof showDebug === 'function') {
                showDebug('🔄 Video looped');
            }
            this.lastLoggedTime = -1;
        }
    },

    onVideoError(error) {
        Logger.error('PlaylistPlayer', 'Video error event:', error);
        if (typeof showDebug === 'function') {
            showDebug('Video ERROR event');
        }
        this.advanceToNext();
    },

    onVideoPause() {
        // Check if this is an intentional pause or unexpected
        if (this.isPlaying && this.videoElement.currentTime < this.videoElement.duration - 1) {
            Logger.warn('PlaylistPlayer', `Video paused unexpectedly at ${Math.floor(this.videoElement.currentTime)}s`);
            if (typeof showDebug === 'function') {
                showDebug(`⚠️ Paused at ${Math.floor(this.videoElement.currentTime)}s`);
            }
            
            // Try to resume playback
            Logger.debug('PlaylistPlayer', 'Attempting to resume playback...');
            this.videoElement.play().catch(err => {
                Logger.error('PlaylistPlayer', 'Failed to resume:', err.message);
            });
        }
    },

    onVideoStalled() {
        Logger.warn('PlaylistPlayer', 'Video stalled (network issue)');
        if (typeof showDebug === 'function') {
            showDebug('⚠️ Video stalled');
        }
    },

    onVideoWaiting() {
        Logger.debug('PlaylistPlayer', 'Video waiting for data');
    },

    stop() {
        Logger.debug('PlaylistPlayer', 'Stopping');
        
        this.isPlaying = false;
        
        if (this.timer) {
            clearTimeout(this.timer);
            this.timer = null;
        }
        
        this.videoElement.pause();
        this.videoElement.src = '';
        this.imageElement.src = '';
        
        this.videoElement.style.display = 'none';
        this.imageElement.style.display = 'none';
    },
    
    /**
     * Cache media in background while streaming
     */
    async cacheMediaInBackground(mediaPath, fullUrl) {
        if (typeof CacheService === 'undefined' || typeof DownloadService === 'undefined') {
            return;
        }
        
        try {
            // Check if already cached or queued
            if (await CacheService.isCached(mediaPath)) {
                return;
            }
            
            Logger.debug('PlaylistPlayer', `Caching in background: ${mediaPath}`);
            
            // Fetch and cache
            const response = await fetch(fullUrl);
            if (!response.ok) {
                Logger.warn('PlaylistPlayer', `Failed to fetch for caching: ${response.status}`);
                return;
            }
            
            const blob = await response.blob();
            const mediaType = CacheService.getMediaType(mediaPath);
            
            await CacheService.addToCache(mediaPath, blob, mediaType, null, blob.size);
            
            Logger.info('PlaylistPlayer', `✅ Cached: ${mediaPath}`);
        } catch (error) {
            Logger.error('PlaylistPlayer', 'Background caching error:', error);
        }
    }
};

console.log('✅ Playlist Player loaded');
