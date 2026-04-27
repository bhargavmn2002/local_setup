/**
 * Connection Monitor - Monitors network connectivity
 */

const ConnectionMonitor = {
    online: true,
    checkInterval: null,
    listeners: [],

    /**
     * Start monitoring
     */
    start() {
        Logger.info('ConnectionMonitor', 'Starting connection monitoring');

        // Listen to online/offline events
        window.addEventListener('online', () => this.handleOnline());
        window.addEventListener('offline', () => this.handleOffline());

        // Periodic connectivity check
        this.checkInterval = setInterval(() => {
            this.checkConnectivity();
        }, 30000); // Check every 30 seconds

        // Initial check
        this.checkConnectivity();
    },
    
    /**
     * Stop monitoring
     */
    stop() {
        if (this.checkInterval) {
            clearInterval(this.checkInterval);
            this.checkInterval = null;
        }
        
        window.removeEventListener('online', this.handleOnline);
        window.removeEventListener('offline', this.handleOffline);
        
        Logger.info('ConnectionMonitor', 'Stopped');
    },
    
    /**
     * Check connectivity
     */
    async checkConnectivity() {
        try {
            // Try to reach the server
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 5000);
            
            const response = await fetch(`${CONFIG.SERVER_URL}/health`, {
                method: 'HEAD',
                signal: controller.signal
            });
            
            clearTimeout(timeout);
            
            if (response.ok && !this.online) {
                this.handleOnline();
            } else if (!response.ok && this.online) {
                this.handleOffline();
            }
        } catch (error) {
            if (this.online) {
                this.handleOffline();
            }
        }
    },
    
    /**
     * Handle online event
     */
    handleOnline() {
        if (!this.online) {
            Logger.info('ConnectionMonitor', '✅ Connection restored');
            this.online = true;
            this.notifyListeners(true);
        }
    },
    
    /**
     * Handle offline event
     */
    handleOffline() {
        if (this.online) {
            Logger.warn('ConnectionMonitor', '⚠️ Connection lost');
            this.online = false;
            this.notifyListeners(false);
        }
    },
    
    /**
     * Check if online
     */
    isOnline() {
        return this.online;
    },
    
    /**
     * Add listener for connection changes
     */
    addListener(callback) {
        this.listeners.push(callback);
    },
    
    /**
     * Remove listener
     */
    removeListener(callback) {
        this.listeners = this.listeners.filter(cb => cb !== callback);
    },
    
    /**
     * Notify all listeners
     */
    notifyListeners(isOnline) {
        this.listeners.forEach(callback => {
            try {
                callback(isOnline);
            } catch (error) {
                Logger.error('ConnectionMonitor', 'Listener error:', error);
            }
        });
    }
};

console.log('✅ Connection Monitor loaded');
