/**
 * Storage Utility
 */

const Storage = {
    isAvailable() {
        try {
            const test = '__test__';
            localStorage.setItem(test, test);
            localStorage.removeItem(test);
            return true;
        } catch (e) {
            return false;
        }
    },

    set(key, value) {
        if (!this.isAvailable()) return false;
        try {
            localStorage.setItem(key, JSON.stringify(value));
            return true;
        } catch (e) {
            Logger.error('Storage', 'Set failed:', e);
            return false;
        }
    },

    get(key, defaultValue = null) {
        if (!this.isAvailable()) return defaultValue;
        try {
            const item = localStorage.getItem(key);
            return item === null ? defaultValue : JSON.parse(item);
        } catch (e) {
            Logger.error('Storage', 'Get failed:', e);
            return defaultValue;
        }
    },

    remove(key) {
        if (!this.isAvailable()) return false;
        try {
            localStorage.removeItem(key);
            return true;
        } catch (e) {
            return false;
        }
    },

    has(key) {
        return this.isAvailable() && localStorage.getItem(key) !== null;
    }
};

console.log('✅ Storage loaded');
