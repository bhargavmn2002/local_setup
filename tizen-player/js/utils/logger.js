/**
 * Logger Utility
 */

const Logger = {
    levels: {
        debug: 0,
        info: 1,
        warn: 2,
        error: 3
    },
    
    maxOnScreenLogs: 50, // Keep last 50 logs on screen

    getCurrentLevel() {
        return this.levels[CONFIG.LOG_LEVEL] || this.levels.info;
    },

    getTimestamp() {
        const now = new Date();
        return now.toISOString().split('T')[1].split('.')[0];
    },
    
    addToScreen(level, module, message) {
        try {
            const logDisplay = document.getElementById('log-display');
            const logContent = document.getElementById('log-content');
            
            if (!logDisplay || !logContent) return;
            
            // Show log display
            logDisplay.style.display = 'block';
            
            const timestamp = this.getTimestamp();
            const logLine = document.createElement('div');
            logLine.style.marginBottom = '2px';
            logLine.style.fontSize = '11px';
            
            // Color coding
            const colors = {
                debug: '#888',
                info: '#0f0',
                warn: '#fa0',
                error: '#f44'
            };
            
            logLine.style.color = colors[level] || '#0f0';
            logLine.innerHTML = `[${timestamp}] [${module}] ${message}`;
            
            // Add to top
            logContent.insertBefore(logLine, logContent.firstChild);
            
            // Keep only last N logs
            while (logContent.children.length > this.maxOnScreenLogs) {
                logContent.removeChild(logContent.lastChild);
            }
            
        } catch (error) {
            // Ignore errors in on-screen logging
        }
    },

    log(level, module, message, ...args) {
        if (this.levels[level] < this.getCurrentLevel()) {
            return;
        }

        const timestamp = this.getTimestamp();
        const prefix = `[${timestamp}] [${level.toUpperCase()}] [${module}]`;
        
        const styles = {
            debug: 'color: #888',
            info: 'color: #00d4ff',
            warn: 'color: #ffaa00',
            error: 'color: #ff4444; font-weight: bold'
        };

        // Console logging
        if (args.length > 0) {
            console.log(`%c${prefix}`, styles[level], message, ...args);
        } else {
            console.log(`%c${prefix}`, styles[level], message);
        }
        
        // On-screen logging (disabled by default for production)
        // if (level !== 'debug') {
        //     this.addToScreen(level, module, message);
        // }
    },

    debug(module, message, ...args) {
        this.log('debug', module, message, ...args);
    },

    info(module, message, ...args) {
        this.log('info', module, message, ...args);
    },

    warn(module, message, ...args) {
        this.log('warn', module, message, ...args);
    },

    error(module, message, ...args) {
        this.log('error', module, message, ...args);
    }
};

console.log('✅ Logger loaded');
