/**
 * API Service
 */

const API = {
    baseUrl: CONFIG.SERVER_URL,

    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        const deviceToken = Storage.get(STORAGE_KEYS.DEVICE_TOKEN);

        const defaultOptions = {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        };

        if (deviceToken) {
            defaultOptions.headers['Authorization'] = `Bearer ${deviceToken}`;
        }

        const finalOptions = { ...defaultOptions, ...options };
        if (options.headers) {
            finalOptions.headers = { ...defaultOptions.headers, ...options.headers };
        }

        try {
            Logger.debug('API', `${finalOptions.method} ${endpoint}`);
            
            const response = await fetch(url, {
                method: finalOptions.method,
                headers: finalOptions.headers,
                body: finalOptions.body
            });

            const status = response.status;

            if (!response.ok) {
                Logger.warn('API', `HTTP ${status} for ${endpoint}`);
                return { success: false, status, error: `HTTP ${status}` };
            }

            const data = await response.json();
            Logger.debug('API', '✓ Response received');
            
            return { success: true, status, data };
        } catch (error) {
            Logger.error('API', 'Request failed:', error.message);
            return { success: false, status: 0, error: error.message };
        }
    },

    async generatePairingCode() {
        const deviceId = this.getOrCreateDeviceId();
        Logger.info('API', 'Generating pairing code...');
        
        return await this.request('/displays/pairing-code', {
            method: 'POST',
            body: JSON.stringify({ deviceId })
        });
    },

    getOrCreateDeviceId() {
        let deviceId = Storage.get(STORAGE_KEYS.DEVICE_ID);
        if (!deviceId) {
            deviceId = this.generateDeviceId();
            Storage.set(STORAGE_KEYS.DEVICE_ID, deviceId);
            Logger.info('API', `Generated device ID: ${deviceId}`);
        }
        return deviceId;
    },

    generateDeviceId() {
        const timestamp = Date.now().toString(36);
        const random = Math.random().toString(36).substring(2, 15);
        return `tizen-${timestamp}-${random}`;
    }
};

console.log('✅ API Service loaded');
