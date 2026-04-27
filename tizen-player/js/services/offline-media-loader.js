/**
 * Offline Media Loader - Main interface for loading media with offline support
 * Similar to Android's OfflineMediaLoader
 */

const OfflineMediaLoader = {
    /**
     * Load media URL - returns cached blob URL if available, otherwise streaming URL
     */
    async loadMedia(mediaUrl) {
        Logger.debug('OfflineMediaLoader', `Loading media: ${mediaUrl}`);
        
        // 1. Check if file exists in cache
        const cachedUrl = await CacheService.getCachedMedia(mediaUrl);
        
        if (cachedUrl) {
            Logger.debug('OfflineMediaLoader', `Using cached file`);
            return cachedUrl;
        }
        
        // 2. Check network connectivity
        if (ConnectionMonitor.isOnline()) {
            // Queue for download with HIGH priority (currently playing)
            await DownloadService.queueDownload(mediaUrl, 'HIGH');
            
            // Return streaming URL
            const streamingUrl = this.getStreamingUrl(mediaUrl);
            Logger.debug('OfflineMediaLoader', `Streaming from: ${streamingUrl}`);
            return streamingUrl;
        }
        
        // 3. No cache and no network - return streaming URL anyway (will fail gracefully)
        Logger.warn('OfflineMediaLoader', `Media not cached and no network: ${mediaUrl}`);
        return this.getStreamingUrl(mediaUrl);
    },
    
    /**
     * Get streaming URL
     */
    getStreamingUrl(path) {
        if (path.startsWith('http')) {
            return path;
        }
        
        const cleanPath = path.startsWith('/') ? path : `/${path}`;
        return `${CONFIG.MEDIA_BASE_URL}${cleanPath}`;
    },
    
    /**
     * Preload playlist media files
     */
    async preloadPlaylist(playlist) {
        if (!playlist || !playlist.items) return;
        
        Logger.info('OfflineMediaLoader', `Preloading playlist: ${playlist.name} (${playlist.items.length} items)`);
        
        const mediaUrls = playlist.items
            .filter(item => item.media && item.media.url)
            .map(item => ({
                url: item.media.url,
                checksum: item.media.checksum,
                fileSize: item.media.fileSize
            }));
        
        if (mediaUrls.length === 0) {
            Logger.warn('OfflineMediaLoader', 'No media items in playlist');
            return;
        }
        
        // Queue first item with HIGH priority (will play first)
        if (mediaUrls.length > 0) {
            const first = mediaUrls[0];
            await DownloadService.queueDownload(first.url, 'HIGH', first.checksum, first.fileSize);
        }
        
        // Queue remaining items with MEDIUM priority
        for (let i = 1; i < mediaUrls.length; i++) {
            const media = mediaUrls[i];
            await DownloadService.queueDownload(media.url, 'MEDIUM', media.checksum, media.fileSize);
        }
        
        Logger.info('OfflineMediaLoader', `Queued ${mediaUrls.length} media files for download`);
    },
    
    /**
     * Preload layout media files
     */
    async preloadLayout(layout) {
        if (!layout || !layout.sections) return;
        
        Logger.info('OfflineMediaLoader', `Preloading layout: ${layout.name} (${layout.sections.length} sections)`);
        
        const mediaUrls = [];
        
        // Collect all media URLs from all sections
        layout.sections.forEach(section => {
            if (section.items) {
                section.items.forEach(item => {
                    if (item.media && item.media.url) {
                        mediaUrls.push({
                            url: item.media.url,
                            checksum: item.media.checksum,
                            fileSize: item.media.fileSize
                        });
                    }
                });
            }
        });
        
        if (mediaUrls.length === 0) {
            Logger.warn('OfflineMediaLoader', 'No media items in layout');
            return;
        }
        
        // Queue first 3 items with HIGH priority
        const highPriorityCount = Math.min(3, mediaUrls.length);
        for (let i = 0; i < highPriorityCount; i++) {
            const media = mediaUrls[i];
            await DownloadService.queueDownload(media.url, 'HIGH', media.checksum, media.fileSize);
        }
        
        // Queue remaining items with MEDIUM priority
        for (let i = highPriorityCount; i < mediaUrls.length; i++) {
            const media = mediaUrls[i];
            await DownloadService.queueDownload(media.url, 'MEDIUM', media.checksum, media.fileSize);
        }
        
        Logger.info('OfflineMediaLoader', `Queued ${mediaUrls.length} media files for download`);
    },
    
    /**
     * Check if media is available offline
     */
    async isAvailableOffline(mediaUrl) {
        return await CacheService.isCached(mediaUrl);
    },
    
    /**
     * Check if playlist is fully cached
     */
    async isPlaylistCached(playlist) {
        if (!playlist || !playlist.items) return false;
        
        const mediaUrls = playlist.items
            .filter(item => item.media && item.media.url)
            .map(item => item.media.url);
        
        for (const url of mediaUrls) {
            if (!await CacheService.isCached(url)) {
                return false;
            }
        }
        
        return mediaUrls.length > 0;
    },
    
    /**
     * Check if layout is fully cached
     */
    async isLayoutCached(layout) {
        if (!layout || !layout.sections) return false;
        
        const mediaUrls = [];
        
        layout.sections.forEach(section => {
            if (section.items) {
                section.items.forEach(item => {
                    if (item.media && item.media.url) {
                        mediaUrls.push(item.media.url);
                    }
                });
            }
        });
        
        for (const url of mediaUrls) {
            if (!await CacheService.isCached(url)) {
                return false;
            }
        }
        
        return mediaUrls.length > 0;
    },
    
    /**
     * Get cache statistics
     */
    async getCacheStats() {
        return await CacheService.getStats();
    },
    
    /**
     * Clear cache
     */
    async clearCache() {
        Logger.info('OfflineMediaLoader', 'Clearing cache');
        return await CacheService.clearCache();
    },
    
    /**
     * Clean up old media files not in current config
     */
    async cleanupOldMedia(currentMediaUrls) {
        Logger.info('OfflineMediaLoader', 'Cleaning up old media');
        
        const allMetadata = await CacheService.getAllMetadata();
        const cachedUrls = allMetadata.map(m => m.url);
        const urlsToRemove = cachedUrls.filter(url => !currentMediaUrls.includes(url));
        
        if (urlsToRemove.length > 0) {
            Logger.info('OfflineMediaLoader', `Removing ${urlsToRemove.length} old media files`);
            
            for (const url of urlsToRemove) {
                await CacheService.removeFromCache(url);
            }
        } else {
            Logger.info('OfflineMediaLoader', 'No old media files to remove');
        }
    },
    
    /**
     * Get network state
     */
    getNetworkState() {
        return ConnectionMonitor.isOnline() ? 'ONLINE' : 'OFFLINE';
    },
    
    /**
     * Check if network is available
     */
    isNetworkAvailable() {
        return ConnectionMonitor.isOnline();
    },
    
    /**
     * Set whether to allow downloads on cellular
     */
    setAllowCellularDownloads(allow) {
        DownloadService.setAllowCellularDownloads(allow);
    },
    
    /**
     * Get download progress for media
     */
    getDownloadProgress(mediaUrl) {
        return DownloadService.getProgress(mediaUrl);
    },
    
    /**
     * Check if media is currently downloading
     */
    isDownloading(mediaUrl) {
        return DownloadService.isDownloading(mediaUrl);
    },
    
    /**
     * Cancel download
     */
    cancelDownload(mediaUrl) {
        DownloadService.cancelDownload(mediaUrl);
    },
    
    /**
     * Get download queue status
     */
    getDownloadStatus() {
        return DownloadService.getStatus();
    },
    
    /**
     * Get all download tasks
     */
    getAllDownloadTasks() {
        return DownloadService.getAllTasks();
    },
    
    /**
     * Clear completed downloads
     */
    clearCompletedDownloads() {
        DownloadService.clearCompleted();
    }
};

console.log('✅ Offline Media Loader loaded');
