/**
 * Layout Player - Multi-zone content playback
 * Phase 6: Handles layouts with multiple sections playing simultaneously
 */

const LayoutPlayer = {
    container: null,
    layout: null,
    sections: [],
    isPlaying: false,

    init() {
        this.container = document.getElementById('layout-player');
        if (!this.container) {
            Logger.error('LayoutPlayer', 'Container not found');
            return;
        }
        Logger.info('LayoutPlayer', 'Initialized');
    },

    async play(layout) {
        try {
            Logger.info('LayoutPlayer', `Playing layout: ${layout.name}`);
            Logger.debug('LayoutPlayer', `Layout has ${layout.sections?.length || 0} sections`);

            this.stop();
            this.layout = layout;
            this.isPlaying = true;

            // Clear container
            this.container.innerHTML = '';

            // Set container dimensions
            this.container.style.width = `${layout.width || 1920}px`;
            this.container.style.height = `${layout.height || 1080}px`;

            // Create and play each section
            if (layout.sections && layout.sections.length > 0) {
                for (const section of layout.sections) {
                    await this.createSection(section);
                }
            } else {
                Logger.warn('LayoutPlayer', 'No sections in layout');
            }

        } catch (error) {
            Logger.error('LayoutPlayer', 'Play error:', error);
        }
    },

    async createSection(section) {
        try {
            Logger.debug('LayoutPlayer', `Creating section: ${section.name}`);

            // Create section container
            const sectionDiv = document.createElement('div');
            sectionDiv.id = `section-${section.id}`;
            sectionDiv.className = 'layout-section';
            sectionDiv.style.position = 'absolute';
            sectionDiv.style.left = `${section.x}%`;
            sectionDiv.style.top = `${section.y}%`;
            sectionDiv.style.width = `${section.width}%`;
            sectionDiv.style.height = `${section.height}%`;
            sectionDiv.style.overflow = 'hidden';

            // Create media elements for this section
            const img = document.createElement('img');
            img.className = 'section-image';
            img.style.width = '100%';
            img.style.height = '100%';
            img.style.objectFit = 'contain'; // Default to FIT mode
            img.style.display = 'none';

            const video = document.createElement('video');
            video.className = 'section-video';
            video.style.width = '100%';
            video.style.height = '100%';
            video.style.objectFit = 'contain'; // Default to FIT mode
            video.style.display = 'none';
            video.muted = false;  // Enable audio
            video.autoplay = false;
            video.preload = 'auto';
            video.playsInline = true;

            sectionDiv.appendChild(img);
            sectionDiv.appendChild(video);
            this.container.appendChild(sectionDiv);

            // Store section data
            const sectionData = {
                id: section.id,
                name: section.name,
                element: sectionDiv,
                imageElement: img,
                videoElement: video,
                items: section.items || [],
                currentIndex: 0,
                loopEnabled: section.loopEnabled !== false,
                isPlaying: false
            };

            this.sections.push(sectionData);

            // Start playing this section
            if (sectionData.items.length > 0) {
                await this.playSection(sectionData);
            } else {
                Logger.warn('LayoutPlayer', `Section ${section.name} has no items`);
            }

        } catch (error) {
            Logger.error('LayoutPlayer', `Error creating section ${section.name}:`, error);
        }
    },

    async playSection(sectionData) {
        if (!this.isPlaying || sectionData.items.length === 0) {
            return;
        }

        sectionData.isPlaying = true;
        await this.playNextItem(sectionData);
    },

    async playNextItem(sectionData) {
        if (!this.isPlaying || !sectionData.isPlaying) {
            return;
        }

        const item = sectionData.items[sectionData.currentIndex];
        if (!item || !item.media) {
            Logger.warn('LayoutPlayer', `No media for item in section ${sectionData.name}`);
            this.advanceSection(sectionData);
            return;
        }

        const media = item.media;
        const resizeMode = item.resizeMode || 'FIT'; // FIT, FILL, STRETCH
        
        Logger.info('LayoutPlayer', `Section ${sectionData.name}: resizeMode = ${resizeMode}`);
        Logger.info('LayoutPlayer', `Section ${sectionData.name}: Full item data:`, JSON.stringify(item));
        
        // Apply resize mode to media elements with !important via cssText
        const objectFitValue = this.getObjectFit(resizeMode);
        sectionData.imageElement.style.cssText += `object-fit: ${objectFitValue} !important;`;
        sectionData.videoElement.style.cssText += `object-fit: ${objectFitValue} !important;`;
        
        Logger.info('LayoutPlayer', `Section ${sectionData.name}: Applied object-fit: ${objectFitValue}`);
        Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Playing ${media.type} - ${media.name}`);
        Logger.debug('LayoutPlayer', `Section ${sectionData.name}: media.url = ${media.url}`);
        Logger.debug('LayoutPlayer', `Section ${sectionData.name}: media.originalUrl = ${media.originalUrl}`);

        // Update debug overlay with current media
        if (typeof updateCurrentMedia === 'function') {
            updateCurrentMedia(media.name || 'Unknown Media', false);
        }

        try {
            if (media.type === 'IMAGE') {
                const mediaUrl = `${CONFIG.MEDIA_BASE_URL}${media.url}`;
                await this.playImage(sectionData, media.url, mediaUrl, item.duration || media.duration || CONFIG.DEFAULT_IMAGE_DURATION);
            } else if (media.type === 'VIDEO') {
                // Handle video URL properly - PREFER originalUrl over url
                let videoPath = null;
                
                // Try originalUrl first (this is the direct MP4)
                if (media.originalUrl && !media.originalUrl.includes('/hls/') && !media.originalUrl.includes('index.m3u8')) {
                    videoPath = media.originalUrl;
                    Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Using originalUrl (MP4)`);
                }
                // Fallback to url if it's not HLS
                else if (media.url && !media.url.includes('/hls/') && !media.url.includes('index.m3u8')) {
                    videoPath = media.url;
                    Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Using url field (MP4)`);
                }
                
                if (!videoPath) {
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: No MP4 URL available`);
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: url: ${media.url}`);
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: originalUrl: ${media.originalUrl}`);
                    this.advanceSection(sectionData);
                    return;
                }
                
                const mediaUrl = `${CONFIG.MEDIA_BASE_URL}${videoPath}`;
                Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Final video path: ${videoPath}`);
                await this.playVideo(sectionData, videoPath, mediaUrl);
            } else {
                Logger.warn('LayoutPlayer', `Unknown media type: ${media.type}`);
                this.advanceSection(sectionData);
            }
        } catch (error) {
            Logger.error('LayoutPlayer', `Error playing item in section ${sectionData.name}:`, error);
            this.advanceSection(sectionData);
        }
    },

    async playImage(sectionData, mediaPath, fullUrl, duration) {
        return new Promise(async (resolve) => {
            const img = sectionData.imageElement;
            const video = sectionData.videoElement;

            // Hide video, show image
            video.style.display = 'none';
            video.pause();
            video.src = '';

            // Check if online
            const isOnline = typeof ConnectionMonitor !== 'undefined' ? ConnectionMonitor.isOnline() : true;
            
            let mediaUrl = fullUrl;
            
            if (!isOnline) {
                // Offline: Try cache
                if (typeof CacheService !== 'undefined') {
                    const cachedUrl = await CacheService.getCachedMedia(mediaPath);
                    if (cachedUrl) {
                        mediaUrl = cachedUrl;
                        Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Using cached image (offline)`);
                    } else {
                        Logger.warn('LayoutPlayer', `Section ${sectionData.name}: Image not cached, skipping`);
                        this.advanceSection(sectionData);
                        resolve();
                        return;
                    }
                }
            } else {
                // Online: Stream and cache in background
                Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Streaming image`);
                this.cacheMediaInBackground(mediaPath, fullUrl);
            }

            img.onload = () => {
                img.style.display = 'block';
                Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Image loaded, showing for ${duration}s`);

                setTimeout(() => {
                    this.advanceSection(sectionData);
                    resolve();
                }, duration * 1000);
            };

            img.onerror = () => {
                Logger.error('LayoutPlayer', `Section ${sectionData.name}: Image load failed`);
                this.advanceSection(sectionData);
                resolve();
            };

            img.src = mediaUrl;
        });
    },

    async playVideo(sectionData, mediaPath, fullUrl) {
        return new Promise(async (resolve) => {
            const video = sectionData.videoElement;
            const img = sectionData.imageElement;

            Logger.info('LayoutPlayer', `Section ${sectionData.name}: Starting video playback`);

            // Hide image, show video
            img.style.display = 'none';
            img.src = '';

            // Check if online
            const isOnline = typeof ConnectionMonitor !== 'undefined' ? ConnectionMonitor.isOnline() : true;
            
            let mediaUrl = fullUrl;
            
            if (!isOnline) {
                // Offline: Try cache
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: OFFLINE MODE - Looking for cached video`);
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: Searching cache for: ${mediaPath}`);
                
                if (typeof CacheService !== 'undefined') {
                    const cachedUrl = await CacheService.getCachedMedia(mediaPath);
                    Logger.info('LayoutPlayer', `Section ${sectionData.name}: Cache lookup result: ${cachedUrl ? 'FOUND' : 'NOT FOUND'}`);
                    
                    if (cachedUrl) {
                        mediaUrl = cachedUrl;
                        Logger.info('LayoutPlayer', `Section ${sectionData.name}: ✅ Using cached video: ${mediaPath}`);
                        Logger.info('LayoutPlayer', `Section ${sectionData.name}: Cached blob URL: ${cachedUrl}`);
                        
                        // Update debug overlay to show cached
                        if (typeof updateCurrentMedia === 'function') {
                            updateCurrentMedia(media.name || 'Video', true);
                        }
                    } else {
                        Logger.error('LayoutPlayer', `Section ${sectionData.name}: ❌ Video not in cache: ${mediaPath}`);
                        
                        // List all cached media to debug
                        if (CacheService.getAllMetadata) {
                            const allCached = await CacheService.getAllMetadata();
                            Logger.info('LayoutPlayer', `Section ${sectionData.name}: All cached media (${allCached.length} items):`);
                            allCached.forEach((item, index) => {
                                Logger.info('LayoutPlayer', `  ${index + 1}. ${item.url}`);
                            });
                        }
                        
                        this.advanceSection(sectionData);
                        resolve();
                        return;
                    }
                } else {
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: CacheService not available`);
                    this.advanceSection(sectionData);
                    resolve();
                    return;
                }
            } else {
                // Online: Stream and cache in background
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: ONLINE MODE - Streaming video: ${mediaPath}`);
                
                // Update debug overlay to show streaming
                if (typeof updateCurrentMedia === 'function') {
                    updateCurrentMedia(media.name || 'Video', false);
                }
                
                this.cacheMediaInBackground(mediaPath, fullUrl);
            }

            // Reset video element completely
            video.pause();
            video.removeAttribute('src');
            video.load();

            video.style.display = 'block';
            video.src = mediaUrl;

            let hasResolved = false;

            const onEnded = () => {
                if (hasResolved) return;
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: Video ended`);
                cleanup();
                hasResolved = true;
                this.advanceSection(sectionData);
                resolve();
            };

            const onError = (e) => {
                if (hasResolved) return;
                Logger.error('LayoutPlayer', `Section ${sectionData.name}: Video error`);
                if (video.error) {
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: Error code: ${video.error.code}`);
                }
                
                // If using cached URL and it failed, try streaming instead (if online)
                if (!isOnline) {
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: Offline and cache failed`);
                    cleanup();
                    hasResolved = true;
                    this.advanceSection(sectionData);
                    resolve();
                    return;
                }
                
                cleanup();
                hasResolved = true;
                this.advanceSection(sectionData);
                resolve();
            };

            const onCanPlay = () => {
                if (hasResolved) return;
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: Video can play`);
                
                video.play().then(() => {
                    Logger.info('LayoutPlayer', `Section ${sectionData.name}: Video playing`);
                }).catch(err => {
                    if (hasResolved) return;
                    Logger.error('LayoutPlayer', `Section ${sectionData.name}: Play failed: ${err.message}`);
                    cleanup();
                    hasResolved = true;
                    this.advanceSection(sectionData);
                    resolve();
                });
            };

            const cleanup = () => {
                video.removeEventListener('ended', onEnded);
                video.removeEventListener('error', onError);
                video.removeEventListener('canplay', onCanPlay);
            };

            video.addEventListener('ended', onEnded);
            video.addEventListener('error', onError);
            video.addEventListener('canplay', onCanPlay);

            video.load();
        });
    },

    getObjectFit(resizeMode) {
        // Convert backend resizeMode to CSS object-fit
        switch (resizeMode) {
            case 'FIT':
                return 'contain'; // Fit entire content, may have letterboxing
            case 'FILL':
                return 'cover';   // Fill entire area, may crop content
            case 'STRETCH':
                return 'fill';    // Stretch to fill, may distort
            default:
                return 'contain'; // Default to FIT
        }
    },

    advanceSection(sectionData) {
        if (!this.isPlaying || !sectionData.isPlaying) {
            return;
        }

        sectionData.currentIndex++;

        // Loop or stop
        if (sectionData.currentIndex >= sectionData.items.length) {
            if (sectionData.loopEnabled) {
                Logger.debug('LayoutPlayer', `Section ${sectionData.name}: Looping`);
                sectionData.currentIndex = 0;
            } else {
                Logger.info('LayoutPlayer', `Section ${sectionData.name}: Finished (no loop)`);
                sectionData.isPlaying = false;
                return;
            }
        }

        // Play next item
        this.playNextItem(sectionData);
    },

    stop() {
        Logger.info('LayoutPlayer', 'Stopping');
        this.isPlaying = false;

        // Stop all sections
        this.sections.forEach(section => {
            section.isPlaying = false;
            if (section.videoElement) {
                section.videoElement.pause();
                section.videoElement.src = '';
            }
            if (section.imageElement) {
                section.imageElement.src = '';
            }
        });

        this.sections = [];
        
        if (this.container) {
            this.container.innerHTML = '';
        }

        this.layout = null;
    },
    
    /**
     * Cache media in background while streaming
     */
    async cacheMediaInBackground(mediaPath, fullUrl) {
        if (typeof CacheService === 'undefined') {
            Logger.warn('LayoutPlayer', 'CacheService not available for background caching');
            return;
        }
        
        try {
            Logger.info('LayoutPlayer', `🔍 Checking cache for: ${mediaPath}`);
            
            // Check if already cached
            if (await CacheService.isCached(mediaPath)) {
                Logger.info('LayoutPlayer', `✅ Already cached: ${mediaPath}`);
                return;
            }
            
            Logger.info('LayoutPlayer', `📥 Starting background cache: ${mediaPath}`);
            Logger.debug('LayoutPlayer', `Full URL: ${fullUrl}`);
            
            // Fetch and cache
            const response = await fetch(fullUrl);
            if (!response.ok) {
                Logger.warn('LayoutPlayer', `❌ Failed to fetch for caching: ${response.status} - ${fullUrl}`);
                return;
            }
            
            Logger.debug('LayoutPlayer', `Fetched, converting to blob...`);
            const blob = await response.blob();
            const mediaType = CacheService.getMediaType(mediaPath);
            
            Logger.info('LayoutPlayer', `💾 Adding to cache: ${mediaPath} (${CacheService.formatBytes(blob.size)})`);
            
            const blobUrl = await CacheService.addToCache(mediaPath, blob, mediaType, null, blob.size);
            
            if (blobUrl) {
                Logger.info('LayoutPlayer', `✅ Successfully cached: ${mediaPath}`);
            } else {
                Logger.error('LayoutPlayer', `❌ Failed to add to cache: ${mediaPath}`);
            }
        } catch (error) {
            Logger.error('LayoutPlayer', `Background caching error for ${mediaPath}:`, error);
        }
    }
};

console.log('✅ Layout Player loaded');
