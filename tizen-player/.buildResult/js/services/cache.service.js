/**
 * Cache Service - Manages offline media caching
 * Uses IndexedDB for metadata and Tizen File System for media files
 */

const CacheService = {
    db: null,
    DB_NAME: 'SignoXCache',
    DB_VERSION: 1,
    STORE_NAME: 'media',
    MAX_CACHE_SIZE: 1 * 1024 * 1024 * 1024, // 1GB
    currentCacheSize: 0,
    
    /**
     * Initialize cache service
     */
    async init() {
        try {
            Logger.info('CacheService', 'Initializing...');
            
            // Open IndexedDB
            await this.openDatabase();
            
            // Calculate current cache size
            await this.calculateCacheSize();
            
            Logger.info('CacheService', `Initialized - Cache size: ${this.formatBytes(this.currentCacheSize)}`);
            
            return true;
        } catch (error) {
            Logger.error('CacheService', 'Init failed:', error);
            return false;
        }
    },
    
    /**
     * Open IndexedDB database
     */
    openDatabase() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
            
            request.onerror = () => {
                Logger.error('CacheService', 'Database open failed');
                reject(request.error);
            };
            
            request.onsuccess = () => {
                this.db = request.result;
                Logger.debug('CacheService', 'Database opened');
                resolve();
            };
            
            request.onupgradeneeded = (event) => {
                Logger.debug('CacheService', 'Database upgrade needed');
                const db = event.target.result;
                
                // Create object store if it doesn't exist
                if (!db.objectStoreNames.contains(this.STORE_NAME)) {
                    const store = db.createObjectStore(this.STORE_NAME, { keyPath: 'url' });
                    store.createIndex('lastUsed', 'lastUsed', { unique: false });
                    store.createIndex('type', 'type', { unique: false });
                    Logger.debug('CacheService', 'Object store created');
                }
            };
        });
    },
    
    /**
     * Calculate total cache size
     */
    async calculateCacheSize() {
        try {
            const allMedia = await this.getAllMetadata();
            this.currentCacheSize = allMedia.reduce((sum, item) => sum + (item.fileSize || 0), 0);
            Logger.debug('CacheService', `Cache size: ${this.formatBytes(this.currentCacheSize)}`);
        } catch (error) {
            Logger.error('CacheService', 'Error calculating cache size:', error);
            this.currentCacheSize = 0;
        }
    },
    
    /**
     * Get cached media blob URL
     */
    async getCachedMedia(url) {
        try {
            const metadata = await this.getMetadata(url);
            
            if (!metadata || !metadata.blob) {
                Logger.debug('CacheService', `Cache MISS: ${url}`);
                return null;
            }
            
            // Update last used timestamp
            await this.updateLastUsed(url);
            
            Logger.debug('CacheService', `Cache HIT: ${url}`);
            return metadata.blobUrl;
        } catch (error) {
            Logger.error('CacheService', 'Error getting cached media:', error);
            return null;
        }
    },
    
    /**
     * Add media to cache
     */
    async addToCache(url, blob, mediaType, checksum = null, fileSize = null) {
        try {
            const actualSize = fileSize || blob.size;
            const sizeMB = Math.round(actualSize / 1024 / 1024 * 100) / 100;
            
            Logger.info('CacheService', `💾 Caching: ${url.split('/').pop()} (${sizeMB}MB, ${mediaType})`);
            
            // Check if we need to evict
            if (this.needsEviction(actualSize)) {
                const requiredMB = Math.round(actualSize / 1024 / 1024 * 100) / 100;
                Logger.info('CacheService', `🗑️ Need to evict ${requiredMB}MB for new file`);
                await this.evictLRU(actualSize);
            }
            
            // Create blob URL for playback
            const blobUrl = URL.createObjectURL(blob);
            
            // Store metadata
            const metadata = {
                url: url,
                blob: blob,
                blobUrl: blobUrl,
                fileSize: actualSize,
                checksum: checksum,
                type: mediaType,
                downloadedAt: Date.now(),
                lastUsed: Date.now()
            };
            
            await this.saveMetadata(metadata);
            
            this.currentCacheSize += actualSize;
            
            // Log cache status after adding
            const totalMB = Math.round(this.currentCacheSize / 1024 / 1024 * 100) / 100;
            const maxMB = Math.round(this.MAX_CACHE_SIZE / 1024 / 1024 * 100) / 100;
            const usagePercent = Math.round((this.currentCacheSize / this.MAX_CACHE_SIZE) * 100);
            
            Logger.info('CacheService', `✅ CACHED: ${url.split('/').pop()} (${sizeMB}MB)`);
            Logger.info('CacheService', `📊 TOTAL CACHE: ${totalMB}MB / ${maxMB}MB (${usagePercent}%)`);
            
            return blobUrl;
        } catch (error) {
            Logger.error('CacheService', 'Error adding to cache:', error);
            return null;
        }
    },
    
    /**
     * Remove media from cache
     */
    async removeFromCache(url) {
        try {
            const metadata = await this.getMetadata(url);
            
            if (!metadata) {
                return false;
            }
            
            // Revoke blob URL
            if (metadata.blobUrl) {
                URL.revokeObjectURL(metadata.blobUrl);
            }
            
            // Remove from database
            await this.deleteMetadata(url);
            
            this.currentCacheSize -= (metadata.fileSize || 0);
            
            Logger.debug('CacheService', `Removed from cache: ${url}`);
            
            return true;
        } catch (error) {
            Logger.error('CacheService', 'Error removing from cache:', error);
            return false;
        }
    },
    
    /**
     * Check if media is cached
     */
    async isCached(url) {
        const metadata = await this.getMetadata(url);
        return metadata !== null && metadata.blob !== null;
    },
    
    /**
     * Get metadata for a URL
     */
    getMetadata(url) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.STORE_NAME], 'readonly');
            const store = transaction.objectStore(this.STORE_NAME);
            const request = store.get(url);
            
            request.onsuccess = () => resolve(request.result || null);
            request.onerror = () => reject(request.error);
        });
    },
    
    /**
     * Get all metadata
     */
    getAllMetadata() {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.STORE_NAME], 'readonly');
            const store = transaction.objectStore(this.STORE_NAME);
            const request = store.getAll();
            
            request.onsuccess = () => resolve(request.result || []);
            request.onerror = () => reject(request.error);
        });
    },
    
    /**
     * Save metadata
     */
    saveMetadata(metadata) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.STORE_NAME], 'readwrite');
            const store = transaction.objectStore(this.STORE_NAME);
            const request = store.put(metadata);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    },
    
    /**
     * Delete metadata
     */
    deleteMetadata(url) {
        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.STORE_NAME], 'readwrite');
            const store = transaction.objectStore(this.STORE_NAME);
            const request = store.delete(url);
            
            request.onsuccess = () => resolve();
            request.onerror = () => reject(request.error);
        });
    },
    
    /**
     * Update last used timestamp
     */
    async updateLastUsed(url) {
        try {
            const metadata = await this.getMetadata(url);
            if (metadata) {
                metadata.lastUsed = Date.now();
                await this.saveMetadata(metadata);
            }
        } catch (error) {
            Logger.error('CacheService', 'Error updating last used:', error);
        }
    },
    
    /**
     * Check if eviction is needed
     */
    needsEviction(requiredSpace) {
        const availableSpace = this.MAX_CACHE_SIZE - this.currentCacheSize;
        return availableSpace < requiredSpace;
    },
    
    /**
     * Evict least recently used media
     */
    async evictLRU(requiredSpace) {
        try {
            const requiredMB = Math.round(requiredSpace / 1024 / 1024 * 100) / 100;
            Logger.info('CacheService', `🗑️ Evicting LRU to free ${requiredMB}MB`);
            
            const allMedia = await this.getAllMetadata();
            
            // Sort by last used (oldest first)
            allMedia.sort((a, b) => a.lastUsed - b.lastUsed);
            
            let freedSpace = 0;
            let evictedCount = 0;
            
            for (const media of allMedia) {
                if (freedSpace >= requiredSpace) {
                    break;
                }
                
                const fileSizeMB = Math.round(media.fileSize / 1024 / 1024 * 100) / 100;
                Logger.info('CacheService', `🗑️ Evicting: ${media.url.split('/').pop()} (${fileSizeMB}MB)`);
                
                if (await this.removeFromCache(media.url)) {
                    freedSpace += media.fileSize;
                    evictedCount++;
                }
            }
            
            const freedMB = Math.round(freedSpace / 1024 / 1024 * 100) / 100;
            Logger.info('CacheService', `✅ Evicted ${evictedCount} files, freed ${freedMB}MB`);
        } catch (error) {
            Logger.error('CacheService', 'Error during eviction:', error);
        }
    },
    
    /**
     * Clear entire cache
     */
    async clearCache() {
        try {
            Logger.info('CacheService', 'Clearing cache...');
            
            const allMedia = await this.getAllMetadata();
            
            // Revoke all blob URLs
            for (const media of allMedia) {
                if (media.blobUrl) {
                    URL.revokeObjectURL(media.blobUrl);
                }
            }
            
            // Clear database
            const transaction = this.db.transaction([this.STORE_NAME], 'readwrite');
            const store = transaction.objectStore(this.STORE_NAME);
            await new Promise((resolve, reject) => {
                const request = store.clear();
                request.onsuccess = () => resolve();
                request.onerror = () => reject(request.error);
            });
            
            this.currentCacheSize = 0;
            
            Logger.info('CacheService', 'Cache cleared');
            
            return true;
        } catch (error) {
            Logger.error('CacheService', 'Error clearing cache:', error);
            return false;
        }
    },
    
    /**
     * Get cache statistics
     */
    async getStats() {
        const allMedia = await this.getAllMetadata();
        
        const imageCount = allMedia.filter(m => m.type === 'IMAGE').length;
        const videoCount = allMedia.filter(m => m.type === 'VIDEO').length;
        
        return {
            totalFiles: allMedia.length,
            totalSize: this.currentCacheSize,
            maxSize: this.MAX_CACHE_SIZE,
            availableSpace: this.MAX_CACHE_SIZE - this.currentCacheSize,
            usagePercentage: (this.currentCacheSize / this.MAX_CACHE_SIZE) * 100,
            imageCount: imageCount,
            videoCount: videoCount
        };
    },
    
    /**
     * Format bytes to human readable
     */
    formatBytes(bytes) {
        if (bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
    },
    
    /**
     * Determine media type from URL
     */
    getMediaType(url) {
        const lower = url.toLowerCase();
        
        if (lower.match(/\.(jpg|jpeg|png|gif|webp)$/)) {
            return 'IMAGE';
        } else if (lower.match(/\.(mp4|webm|mkv|avi)$/)) {
            return 'VIDEO';
        }
        
        return 'UNKNOWN';
    }
};

console.log('✅ Cache Service loaded');
