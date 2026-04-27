/**
 * Download Service - Manages media downloads with queue and retry logic
 */

const DownloadService = {
    queue: [],
    activeDownloads: new Map(),
    maxConcurrentDownloads: 2,
    isProcessing: false,
    allowCellularDownloads: true,
    downloadTasks: new Map(), // Track all tasks with progress
    
    /**
     * Initialize download service
     */
    init() {
        Logger.info('DownloadService', 'Initialized');
    },
    
    /**
     * Add media to download queue
     */
    async queueDownload(url, priority = 'MEDIUM', checksum = null, fileSize = null) {
        try {
            Logger.info('DownloadService', `🔍 queueDownload called:`);
            Logger.info('DownloadService', `  URL: ${url}`);
            Logger.info('DownloadService', `  Priority: ${priority}`);
            Logger.info('DownloadService', `  Checksum: ${checksum || 'none'}`);
            Logger.info('DownloadService', `  FileSize: ${fileSize ? this.formatBytes(fileSize) : 'unknown'}`);
            
            // Check if already cached
            Logger.debug('DownloadService', 'Checking if already cached...');
            const isCached = await CacheService.isCached(url);
            Logger.info('DownloadService', `Cache check result: ${isCached ? 'CACHED ✅' : 'NOT CACHED ❌'}`);
            
            if (isCached) {
                Logger.info('DownloadService', `✅ Already cached: ${url}`);
                return true;
            }
            
            // Check if already in queue
            const inQueue = this.queue.find(item => item.url === url);
            Logger.info('DownloadService', `Queue check result: ${inQueue ? 'IN QUEUE ⏳' : 'NOT IN QUEUE ✅'}`);
            
            if (inQueue) {
                Logger.info('DownloadService', `⏳ Already in queue: ${url}`);
                return true;
            }
            
            // Check if currently downloading
            const isDownloading = this.activeDownloads.has(url);
            Logger.info('DownloadService', `Active download check: ${isDownloading ? 'DOWNLOADING ⬇️' : 'NOT DOWNLOADING ✅'}`);
            
            if (isDownloading) {
                Logger.info('DownloadService', `⬇️ Already downloading: ${url}`);
                return true;
            }
            
            // Add to queue
            const downloadItem = {
                id: this.generateId(),
                url: url,
                priority: priority,
                checksum: checksum,
                fileSize: fileSize,
                retries: 0,
                maxRetries: 3,
                status: 'PENDING',
                progress: 0,
                downloadedBytes: 0,
                totalBytes: 0,
                error: null,
                addedAt: Date.now(),
                startedAt: null,
                completedAt: null
            };
            
            Logger.info('DownloadService', `📥 Adding to queue: ${url}`);
            this.queue.push(downloadItem);
            this.downloadTasks.set(url, downloadItem);
            
            // Sort queue by priority
            this.sortQueue();
            
            Logger.info('DownloadService', `📥 Successfully queued: ${url}`);
            Logger.info('DownloadService', `📥 Queue size after add: ${this.queue.length}`);
            Logger.info('DownloadService', `📥 Priority: ${priority}, Total tasks: ${this.downloadTasks.size}`);
            
            // Start processing if not already
            Logger.info('DownloadService', `Processing status: ${this.isProcessing ? 'ALREADY RUNNING' : 'STARTING NOW'}`);
            
            if (!this.isProcessing) {
                Logger.info('DownloadService', '🚀 Starting queue processor...');
                // Don't await - let it run in background
                this.processQueue().catch(err => {
                    Logger.error('DownloadService', 'Queue processing error:', err);
                });
            } else {
                Logger.info('DownloadService', '🔄 Queue processor already running');
            }
            
            return true;
        } catch (error) {
            Logger.error('DownloadService', 'Error in queueDownload:', error);
            return false;
        }
    },
    
    /**
     * Format bytes helper
     */
    formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    },
    
    /**
     * Generate unique ID
     */
    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2);
    },
    
    /**
     * Sort queue by priority
     */
    sortQueue() {
        const priorityOrder = { 'HIGH': 0, 'MEDIUM': 1, 'LOW': 2 };
        
        this.queue.sort((a, b) => {
            const priorityDiff = priorityOrder[a.priority] - priorityOrder[b.priority];
            if (priorityDiff !== 0) return priorityDiff;
            return a.addedAt - b.addedAt;
        });
    },
    
    /**
     * Process download queue
     */
    async processQueue() {
        if (this.isProcessing) {
            Logger.debug('DownloadService', 'processQueue: Already processing');
            return;
        }
        
        this.isProcessing = true;
        
        Logger.info('DownloadService', `🔄 Processing queue (${this.queue.length} items)...`);
        
        while (this.queue.length > 0 || this.activeDownloads.size > 0) {
            // Check network connectivity
            if (!ConnectionMonitor.isOnline()) {
                Logger.info('DownloadService', '📡 Offline - pausing downloads');
                await this.waitForOnline();
            }
            
            // Check if we should download on current network
            if (!this.shouldDownload()) {
                Logger.info('DownloadService', '📱 Cellular network - pausing downloads');
                await new Promise(resolve => setTimeout(resolve, 5000));
                continue;
            }
            
            // Start new downloads if under limit
            while (this.queue.length > 0 && this.activeDownloads.size < this.maxConcurrentDownloads) {
                const item = this.queue.shift();
                Logger.info('DownloadService', `⬇️ Starting download: ${item.url} (${this.activeDownloads.size + 1}/${this.maxConcurrentDownloads})`);
                this.downloadMedia(item);
            }
            
            // Wait a bit before checking again
            await new Promise(resolve => setTimeout(resolve, 1000));
            
            // Log status periodically
            if (this.queue.length > 0 || this.activeDownloads.size > 0) {
                Logger.debug('DownloadService', `Status: ${this.queue.length} queued, ${this.activeDownloads.size} active`);
            }
        }
        
        this.isProcessing = false;
        Logger.info('DownloadService', '✅ Queue processing complete');
    },
    
    /**
     * Check if downloads should proceed
     */
    shouldDownload() {
        // For Tizen, we assume WiFi/Ethernet connection
        // In production, you could add network type detection
        return ConnectionMonitor.isOnline() && this.allowCellularDownloads;
    },
    
    /**
     * Download a single media file
     */
    async downloadMedia(item) {
        try {
            Logger.info('DownloadService', `Downloading: ${item.url}`);
            
            item.status = 'DOWNLOADING';
            item.startedAt = Date.now();
            this.activeDownloads.set(item.url, item);
            this.downloadTasks.set(item.url, item);
            
            // Build full URL
            const fullUrl = this.getFullUrl(item.url);
            
            // Download with timeout and progress tracking
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 60000); // 60s timeout
            
            const response = await fetch(fullUrl, {
                signal: controller.signal
            });
            
            clearTimeout(timeout);
            
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            
            // Get content length
            const contentLength = parseInt(response.headers.get('content-length') || '0');
            item.totalBytes = contentLength;
            
            // Read response with progress tracking
            const reader = response.body.getReader();
            const chunks = [];
            let downloadedBytes = 0;
            let lastLoggedPercent = 0;
            
            while (true) {
                const { done, value } = await reader.read();
                
                if (done) break;
                
                chunks.push(value);
                downloadedBytes += value.length;
                
                // Update progress
                item.downloadedBytes = downloadedBytes;
                item.progress = contentLength > 0 ? Math.round((downloadedBytes / contentLength) * 100) : 0;
                this.downloadTasks.set(item.url, item);
                
                // Log progress every 10% and show MB progress
                const progressPercent = item.progress;
                const downloadedMB = Math.round(downloadedBytes / 1024 / 1024 * 100) / 100;
                const totalMB = contentLength > 0 ? Math.round(contentLength / 1024 / 1024 * 100) / 100 : 0;
                
                // Log every 10% but avoid duplicate logs
                if (progressPercent >= lastLoggedPercent + 10 && progressPercent > 0) {
                    Logger.info('DownloadService', `📊 Progress: ${item.url.split('/').pop()} - ${progressPercent}% (${downloadedMB}MB / ${totalMB}MB)`);
                    lastLoggedPercent = Math.floor(progressPercent / 10) * 10;
                }
            }
            
            // Verify we got all the data
            if (contentLength > 0 && downloadedBytes !== contentLength) {
                Logger.warn('DownloadService', `⚠️ Size mismatch: expected ${contentLength}, got ${downloadedBytes}`);
            }
            
            Logger.info('DownloadService', `📦 Download complete: ${item.url.split('/').pop()} - ${downloadedBytes} bytes`);
            
            // Combine chunks into blob
            const blob = new Blob(chunks);
            const fileSizeMB = Math.round(blob.size / 1024 / 1024 * 100) / 100;
            
            // Verify blob size matches downloaded bytes
            if (blob.size !== downloadedBytes) {
                Logger.error('DownloadService', `❌ Blob size mismatch: downloaded ${downloadedBytes}, blob ${blob.size}`);
                throw new Error(`Blob size mismatch: downloaded ${downloadedBytes}, blob ${blob.size}`);
            }
            
            Logger.info('DownloadService', `📦 Downloaded: ${item.url.split('/').pop()} (${fileSizeMB}MB) - COMPLETE`);
            
            // Determine media type
            const mediaType = CacheService.getMediaType(item.url);
            
            Logger.info('DownloadService', `💾 Adding to cache: ${item.url.split('/').pop()} (${mediaType})`);
            
            // Add to cache
            const blobUrl = await CacheService.addToCache(
                item.url,
                blob,
                mediaType,
                item.checksum,
                item.fileSize
            );
            
            if (blobUrl) {
                item.status = 'COMPLETED';
                item.progress = 100;
                item.completedAt = Date.now();
                this.downloadTasks.set(item.url, item);
                
                // Get updated cache stats
                const cacheStats = await CacheService.getStats();
                const totalCacheMB = Math.round(cacheStats.totalSize / 1024 / 1024 * 100) / 100;
                const maxCacheMB = Math.round(cacheStats.maxSize / 1024 / 1024 * 100) / 100;
                const usagePercent = Math.round(cacheStats.usagePercentage);
                
                Logger.info('DownloadService', `✅ CACHED: ${item.url.split('/').pop()} (${fileSizeMB}MB) - READY FOR OFFLINE`);
                Logger.info('DownloadService', `📊 CACHE STATUS: ${cacheStats.totalFiles} files, ${totalCacheMB}MB / ${maxCacheMB}MB (${usagePercent}%)`);
            } else {
                throw new Error('Failed to cache');
            }
            
        } catch (error) {
            Logger.error('DownloadService', `Download failed: ${item.url}`, error);
            
            item.retries++;
            
            if (item.retries < item.maxRetries) {
                Logger.info('DownloadService', `Retrying (${item.retries}/${item.maxRetries}): ${item.url}`);
                
                // Add back to queue with delay
                setTimeout(() => {
                    item.status = 'PENDING';
                    this.queue.push(item);
                    this.sortQueue();
                }, 5000 * item.retries); // Exponential backoff
            } else {
                Logger.error('DownloadService', `Max retries reached: ${item.url}`);
                item.status = 'FAILED';
                item.error = error.message;
                item.completedAt = Date.now();
                this.downloadTasks.set(item.url, item);
            }
        } finally {
            this.activeDownloads.delete(item.url);
        }
    },
    
    /**
     * Wait for online connectivity
     */
    async waitForOnline() {
        return new Promise(resolve => {
            if (ConnectionMonitor.isOnline()) {
                resolve();
                return;
            }
            
            const checkInterval = setInterval(() => {
                if (ConnectionMonitor.isOnline()) {
                    clearInterval(checkInterval);
                    Logger.info('DownloadService', 'Back online - resuming downloads');
                    resolve();
                }
            }, 2000);
        });
    },
    
    /**
     * Get full URL from path
     */
    getFullUrl(path) {
        if (path.startsWith('http')) {
            return path;
        }
        
        const cleanPath = path.startsWith('/') ? path : `/${path}`;
        return `${CONFIG.MEDIA_BASE_URL}${cleanPath}`;
    },
    
    /**
     * Download playlist media
     */
    async downloadPlaylist(playlist) {
        if (!playlist || !playlist.items) {
            Logger.warn('DownloadService', 'downloadPlaylist: No playlist or items');
            return;
        }
        
        Logger.info('DownloadService', `📦 Downloading playlist: ${playlist.name} (${playlist.items.length} items)`);
        
        let queuedCount = 0;
        
        // First item with HIGH priority (will play first)
        if (playlist.items.length > 0 && playlist.items[0].media) {
            const media = playlist.items[0].media;
            // Prefer originalUrl for videos (direct MP4), fallback to url
            const mediaUrl = (media.type === 'VIDEO' && media.originalUrl) ? media.originalUrl : media.url;
            
            if (mediaUrl) {
                Logger.info('DownloadService', `Queueing first item (HIGH): ${mediaUrl}`);
                await this.queueDownload(
                    mediaUrl,
                    'HIGH',
                    media.checksum,
                    media.fileSize
                );
                queuedCount++;
            }
        }
        
        // Remaining items with MEDIUM priority
        for (let i = 1; i < playlist.items.length; i++) {
            const item = playlist.items[i];
            if (item.media) {
                const media = item.media;
                // Prefer originalUrl for videos (direct MP4), fallback to url
                const mediaUrl = (media.type === 'VIDEO' && media.originalUrl) ? media.originalUrl : media.url;
                
                if (mediaUrl) {
                    Logger.debug('DownloadService', `Queueing item ${i+1} (MEDIUM): ${mediaUrl}`);
                    await this.queueDownload(
                        mediaUrl,
                        'MEDIUM',
                        media.checksum,
                        media.fileSize
                    );
                    queuedCount++;
                }
            }
        }
        
        Logger.info('DownloadService', `✅ Queued ${queuedCount} items from playlist`);
    },
    
    /**
     * Download layout media
     */
    async downloadLayout(layout) {
        if (!layout || !layout.sections) {
            Logger.warn('DownloadService', 'downloadLayout: No layout or sections');
            return;
        }
        
        Logger.info('DownloadService', `📦 Downloading layout: ${layout.name} (${layout.sections.length} sections)`);
        
        const mediaUrls = [];
        
        // Collect all media URLs
        for (const section of layout.sections) {
            if (section.items) {
                for (const item of section.items) {
                    if (item.media) {
                        const media = item.media;
                        // Prefer originalUrl for videos (direct MP4), fallback to url
                        const mediaUrl = (media.type === 'VIDEO' && media.originalUrl) ? media.originalUrl : media.url;
                        
                        if (mediaUrl) {
                            mediaUrls.push({
                                url: mediaUrl,
                                checksum: media.checksum,
                                fileSize: media.fileSize
                            });
                        }
                    }
                }
            }
        }
        
        Logger.info('DownloadService', `Found ${mediaUrls.length} media items in layout`);
        
        let queuedCount = 0;
        
        // First 3 items with HIGH priority
        const highPriorityCount = Math.min(3, mediaUrls.length);
        for (let i = 0; i < highPriorityCount; i++) {
            const media = mediaUrls[i];
            Logger.info('DownloadService', `Queueing item ${i+1} (HIGH): ${media.url}`);
            await this.queueDownload(media.url, 'HIGH', media.checksum, media.fileSize);
            queuedCount++;
        }
        
        // Remaining items with MEDIUM priority
        for (let i = highPriorityCount; i < mediaUrls.length; i++) {
            const media = mediaUrls[i];
            Logger.debug('DownloadService', `Queueing item ${i+1} (MEDIUM): ${media.url}`);
            await this.queueDownload(media.url, 'MEDIUM', media.checksum, media.fileSize);
            queuedCount++;
        }
        
        Logger.info('DownloadService', `✅ Queued ${queuedCount} items from layout`);
    },
    
    /**
     * Get download progress for a URL
     */
    getProgress(url) {
        const task = this.downloadTasks.get(url);
        return task ? task.progress : 0;
    },
    
    /**
     * Check if media is downloading
     */
    isDownloading(url) {
        const task = this.downloadTasks.get(url);
        return task && task.status === 'DOWNLOADING';
    },
    
    /**
     * Cancel download
     */
    cancelDownload(url) {
        // Remove from queue
        this.queue = this.queue.filter(item => item.url !== url);
        
        // Mark as cancelled
        const task = this.downloadTasks.get(url);
        if (task) {
            task.status = 'CANCELLED';
            task.completedAt = Date.now();
            this.downloadTasks.set(url, task);
        }
        
        // Remove from active downloads
        this.activeDownloads.delete(url);
        
        Logger.info('DownloadService', `Cancelled: ${url}`);
    },
    
    /**
     * Get queue status
     */
    getStatus() {
        const pending = this.queue.filter(i => i.status === 'PENDING').length;
        const failed = Array.from(this.downloadTasks.values()).filter(i => i.status === 'FAILED').length;
        
        return {
            queueLength: this.queue.length,
            activeDownloads: this.activeDownloads.size,
            isProcessing: this.isProcessing,
            pending: pending,
            downloading: this.activeDownloads.size,
            failed: failed,
            totalTasks: this.downloadTasks.size
        };
    },
    
    /**
     * Get all download tasks
     */
    getAllTasks() {
        return Array.from(this.downloadTasks.values());
    },
    
    /**
     * Clear completed tasks
     */
    clearCompleted() {
        for (const [url, task] of this.downloadTasks.entries()) {
            if (task.status === 'COMPLETED') {
                this.downloadTasks.delete(url);
            }
        }
        Logger.info('DownloadService', 'Cleared completed tasks');
    },
    
    /**
     * Set cellular downloads
     */
    setAllowCellularDownloads(allow) {
        this.allowCellularDownloads = allow;
        Logger.info('DownloadService', `Allow cellular downloads: ${allow}`);
    },
    
    /**
     * Clear queue
     */
    clearQueue() {
        this.queue = [];
        Logger.info('DownloadService', 'Queue cleared');
    }
};

console.log('✅ Download Service loaded');
