/**
 * Heartbeat Service - Keeps display status updated
 */

const HeartbeatService = {
    interval: null,
    isRunning: false,

    start() {
        if (this.isRunning) {
            Logger.warn('HeartbeatService', 'Already running');
            return;
        }

        Logger.info('HeartbeatService', 'Starting heartbeat service');
        this.isRunning = true;

        // Send heartbeat immediately
        this.sendHeartbeat();

        // Then send every 30 seconds
        this.interval = setInterval(() => {
            this.sendHeartbeat();
        }, CONFIG.HEARTBEAT_INTERVAL);
    },

    async sendHeartbeat() {
        try {
            const displayId = Storage.get(STORAGE_KEYS.DISPLAY_ID);
            
            if (!displayId) {
                Logger.warn('HeartbeatService', 'No display ID - skipping heartbeat');
                return;
            }
            
            Logger.info('HeartbeatService', '💓 Sending heartbeat...');
            
            const result = await API.request(`/displays/${displayId}/heartbeat`, {
                method: 'POST'
            });

            if (result.success) {
                Logger.info('HeartbeatService', '✅ Heartbeat sent successfully');
            } else {
                Logger.warn('HeartbeatService', `⚠️ Heartbeat failed: ${result.error}`);
            }
        } catch (error) {
            Logger.error('HeartbeatService', '❌ Heartbeat error:', error.message);
        }
    },

    stop() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
        this.isRunning = false;
        Logger.info('HeartbeatService', 'Stopped');
    }
};

console.log('✅ Heartbeat Service loaded');
