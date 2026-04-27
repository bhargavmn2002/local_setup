/**
 * Kiosk Mode Service - Phase 7
 * Prevents user from exiting the app and keeps it in fullscreen
 */

const KioskService = {
    isEnabled: false,
    exitTapCount: 0,
    lastTapTime: 0,
    exitTapRequired: 5,
    tapTimeout: 3000, // 3 seconds
    pinCode: '0000', // Default PIN

    enable() {
        if (this.isEnabled) {
            Logger.warn('KioskService', 'Already enabled');
            return;
        }

        Logger.info('KioskService', 'Enabling kiosk mode');
        this.isEnabled = true;

        // Enter fullscreen
        this.enterFullscreen();

        // Setup exit gesture (tap 5 times in top-right corner)
        this.setupExitGesture();

        // Handle back button (if available on Tizen)
        this.setupBackButton();

        // Keep screen on
        this.keepScreenOn();

        // Show kiosk mode indicator
        this.showKioskIndicator();
    },

    disable() {
        if (!this.isEnabled) return;

        Logger.info('KioskService', 'Disabling kiosk mode');
        this.isEnabled = false;

        // Exit fullscreen
        this.exitFullscreen();

        // Remove event listeners
        document.removeEventListener('click', this.handleExitGesture, true);
        document.removeEventListener('touchstart', this.handleExitGesture, true);
        document.removeEventListener('keydown', this.handleKeyPress, true);

        // Hide kiosk mode indicator
        this.hideKioskIndicator();
    },

    showKioskIndicator() {
        // Don't show indicator - kiosk mode is invisible
        // Users can still exit using:
        // 1. Tap 5 times in top-right corner
        // 2. Press ESC or Back button
        // 3. Press X key 5 times
    },

    hideKioskIndicator() {
        // No indicator to hide
    },

    enterFullscreen() {
        try {
            const elem = document.documentElement;

            if (elem.requestFullscreen) {
                elem.requestFullscreen();
            } else if (elem.webkitRequestFullscreen) {
                elem.webkitRequestFullscreen();
            } else if (elem.mozRequestFullScreen) {
                elem.mozRequestFullScreen();
            } else if (elem.msRequestFullscreen) {
                elem.msRequestFullscreen();
            }

            Logger.info('KioskService', 'Entered fullscreen');
        } catch (error) {
            Logger.error('KioskService', 'Failed to enter fullscreen:', error);
        }
    },

    exitFullscreen() {
        try {
            if (document.exitFullscreen) {
                document.exitFullscreen();
            } else if (document.webkitExitFullscreen) {
                document.webkitExitFullscreen();
            } else if (document.mozCancelFullScreen) {
                document.mozCancelFullScreen();
            } else if (document.msExitFullscreen) {
                document.msExitFullscreen();
            }

            Logger.info('KioskService', 'Exited fullscreen');
        } catch (error) {
            Logger.error('KioskService', 'Failed to exit fullscreen:', error);
        }
    },

    setupExitGesture() {
        this.handleExitGesture = (event) => {
            if (!this.isEnabled) return;

            // Get coordinates from mouse or touch event
            const x = event.clientX || event.touches?.[0]?.clientX || 0;
            const y = event.clientY || event.touches?.[0]?.clientY || 0;

            // Check if tap is in top-right corner (100x100 px area)
            const cornerSize = 100;
            const isInCorner = x > window.innerWidth - cornerSize && y < cornerSize;

            Logger.debug('KioskService', `Tap at (${x}, ${y}), window: ${window.innerWidth}x${window.innerHeight}, inCorner: ${isInCorner}`);

            if (isInCorner) {
                const currentTime = Date.now();

                // Reset counter if too much time has passed
                if (currentTime - this.lastTapTime > this.tapTimeout) {
                    this.exitTapCount = 0;
                }

                this.exitTapCount++;
                this.lastTapTime = currentTime;

                Logger.info('KioskService', `Exit gesture tap: ${this.exitTapCount}/${this.exitTapRequired}`);

                // Show visual feedback
                this.showTapFeedback(x, y);

                if (this.exitTapCount >= this.exitTapRequired) {
                    this.exitTapCount = 0;
                    this.showPinDialog();
                }
            }
        };

        // Listen to both click and touchstart events
        document.addEventListener('click', this.handleExitGesture, true);
        document.addEventListener('touchstart', this.handleExitGesture, true);
        
        Logger.info('KioskService', 'Exit gesture setup complete');
    },

    showTapFeedback(x, y) {
        // Disable visual feedback - keep kiosk mode invisible
        // Just log for debugging
        Logger.debug('KioskService', `Tap registered at (${x}, ${y})`);
    },

    setupBackButton() {
        this.handleKeyPress = (event) => {
            if (!this.isEnabled) return;

            // Tizen back button key code
            if (event.keyCode === 10009 || event.key === 'Back') {
                event.preventDefault();
                event.stopPropagation();
                Logger.info('KioskService', 'Back button pressed - showing PIN dialog');
                this.showPinDialog();
                return false;
            }

            // Also block ESC key
            if (event.keyCode === 27 || event.key === 'Escape') {
                event.preventDefault();
                event.stopPropagation();
                Logger.info('KioskService', 'ESC key pressed - showing PIN dialog');
                this.showPinDialog();
                return false;
            }

            // Secret key combo: Press 'X' key 5 times quickly
            if (event.key === 'x' || event.key === 'X') {
                const currentTime = Date.now();
                if (currentTime - this.lastKeyTime > this.tapTimeout) {
                    this.keyPressCount = 0;
                }
                this.keyPressCount++;
                this.lastKeyTime = currentTime;
                
                Logger.info('KioskService', `X key pressed: ${this.keyPressCount}/5`);
                
                if (this.keyPressCount >= 5) {
                    this.keyPressCount = 0;
                    this.showPinDialog();
                }
            }
        };

        this.keyPressCount = 0;
        this.lastKeyTime = 0;
        document.addEventListener('keydown', this.handleKeyPress, true);
        
        Logger.info('KioskService', 'Back button and key handlers setup complete');
    },

    showPinDialog() {
        Logger.info('KioskService', 'Showing PIN dialog');

        // Create overlay
        const overlay = document.createElement('div');
        overlay.id = 'pin-dialog-overlay';
        overlay.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.8);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 99999;
        `;

        // Create dialog
        const dialog = document.createElement('div');
        dialog.style.cssText = `
            background: white;
            padding: 40px;
            border-radius: 10px;
            text-align: center;
            max-width: 400px;
        `;

        dialog.innerHTML = `
            <h2 style="color: #333; margin-bottom: 20px;">Exit Application</h2>
            <p style="color: #666; margin-bottom: 30px;">Enter PIN to exit SignoX Player</p>
            <input 
                type="password" 
                id="pin-input" 
                placeholder="Enter PIN"
                maxlength="4"
                tabindex="1"
                style="
                    width: 100%;
                    padding: 15px;
                    font-size: 32px;
                    text-align: center;
                    border: 2px solid #00d4ff;
                    border-radius: 5px;
                    margin-bottom: 20px;
                    letter-spacing: 15px;
                    outline: none;
                    box-shadow: 0 0 5px rgba(0, 212, 255, 0.3);
                "
            />
            <p style="color: #999; font-size: 14px;">Enter 4-digit PIN or press Back to cancel</p>
            <p id="pin-error" style="color: red; margin-top: 10px; display: none;">Incorrect PIN. Try again.</p>
        `;

        overlay.appendChild(dialog);
        document.body.appendChild(overlay);

        // Focus input
        const input = document.getElementById('pin-input');
        setTimeout(() => input.focus(), 100);

        // Auto-check PIN as user types
        input.oninput = (e) => {
            const enteredPin = input.value;
            Logger.info('KioskService', `PIN entered: ${enteredPin} (${enteredPin.length}/4 digits)`);
            
            // Check PIN when 4 digits are entered
            if (enteredPin.length === 4) {
                Logger.info('KioskService', `Checking PIN: ${enteredPin} vs ${this.pinCode}`);
                
                if (enteredPin === this.pinCode) {
                    Logger.info('KioskService', '✅ Correct PIN - exiting app');
                    const overlayElement = document.getElementById('pin-dialog-overlay');
                    if (overlayElement) {
                        document.body.removeChild(overlayElement);
                    }
                    this.exitApp();
                } else {
                    Logger.warn('KioskService', '❌ Incorrect PIN - closing dialog');
                    const errorElement = document.getElementById('pin-error');
                    if (errorElement) {
                        errorElement.style.display = 'block';
                        errorElement.textContent = 'Incorrect PIN. Returning to content...';
                    }
                    
                    // Close dialog after 2 seconds and return to content
                    setTimeout(() => {
                        const overlayElement = document.getElementById('pin-dialog-overlay');
                        if (overlayElement) {
                            document.body.removeChild(overlayElement);
                        }
                    }, 2000);
                }
            }
        };

        // Handle remote control navigation
        input.onkeydown = (e) => {
            Logger.info('KioskService', `Key pressed: ${e.key} (${e.keyCode})`);
            
            if (e.key === 'Escape' || e.keyCode === 27 || e.keyCode === 10009) {
                // Back/Escape key - cancel and return to content
                e.preventDefault();
                Logger.info('KioskService', 'Back key pressed - closing dialog');
                const overlayElement = document.getElementById('pin-dialog-overlay');
                if (overlayElement) {
                    document.body.removeChild(overlayElement);
                }
            } else if (e.key === 'Enter') {
                // Enter key - check current PIN
                e.preventDefault();
                const enteredPin = input.value;
                if (enteredPin.length === 4) {
                    // Trigger the same logic as oninput
                    input.oninput({ target: input });
                }
            }
        };

        // Handle overlay click (close on background click)
        overlay.onclick = (e) => {
            if (e.target === overlay) {
                Logger.info('KioskService', 'Overlay clicked - closing dialog');
                const overlayElement = document.getElementById('pin-dialog-overlay');
                if (overlayElement) {
                    document.body.removeChild(overlayElement);
                }
            }
        };
    },

    exitApp() {
        Logger.info('KioskService', 'Exiting application');
        
        // Disable kiosk mode
        this.disable();

        // Try to close the app (Tizen specific)
        try {
            if (typeof tizen !== 'undefined' && tizen.application) {
                tizen.application.getCurrentApplication().exit();
            } else {
                // Fallback: show exit message
                alert('Please close the application manually');
            }
        } catch (error) {
            Logger.error('KioskService', 'Failed to exit app:', error);
            alert('Please close the application manually');
        }
    },

    keepScreenOn() {
        // Request wake lock to keep screen on
        try {
            if ('wakeLock' in navigator) {
                navigator.wakeLock.request('screen').then(wakeLock => {
                    Logger.info('KioskService', 'Screen wake lock acquired');
                    this.wakeLock = wakeLock;
                }).catch(err => {
                    Logger.warn('KioskService', 'Failed to acquire wake lock:', err);
                });
            }
        } catch (error) {
            Logger.warn('KioskService', 'Wake lock not supported');
        }
    },

    releaseWakeLock() {
        if (this.wakeLock) {
            this.wakeLock.release();
            this.wakeLock = null;
            Logger.info('KioskService', 'Screen wake lock released');
        }
    }
};

console.log('✅ Kiosk Service loaded');
