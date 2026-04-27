/**
 * Scrolling Text Player for Tizen
 * Handles scrolling text sections in layouts
 */

class ScrollingTextPlayer {
    constructor(container, config) {
        this.container = container;
        this.config = {
            text: config.text || 'Sample Text',
            direction: config.direction || 'left-to-right',
            speed: config.speed || 50, // pixels per second
            fontSize: config.fontSize || 24,
            fontWeight: config.fontWeight || 'normal',
            textColor: config.textColor || '#000000',
            backgroundColor: config.backgroundColor || 'transparent',
            ...config
        };
        
        this.textElement = null;
        this.animationId = null;
        this.isPlaying = false;
        
        this.init();
    }
    
    init() {
        this.createTextElement();
        this.setupStyles();
    }
    
    createTextElement() {
        // Clear container
        this.container.innerHTML = '';
        
        // Create text element
        this.textElement = document.createElement('div');
        this.textElement.textContent = this.config.text;
        this.textElement.style.position = 'absolute';
        this.textElement.style.whiteSpace = 'nowrap';
        this.textElement.style.fontSize = this.config.fontSize + 'px';
        this.textElement.style.fontWeight = this.config.fontWeight;
        this.textElement.style.color = this.config.textColor;
        this.textElement.style.zIndex = '10';
        
        // Set container styles
        this.container.style.position = 'relative';
        this.container.style.overflow = 'hidden';
        this.container.style.backgroundColor = this.config.backgroundColor;
        
        this.container.appendChild(this.textElement);
    }
    
    setupStyles() {
        const containerRect = this.container.getBoundingClientRect();
        const textRect = this.textElement.getBoundingClientRect();
        
        // Position text based on direction
        switch (this.config.direction) {
            case 'left-to-right':
                this.textElement.style.top = '50%';
                this.textElement.style.transform = 'translateY(-50%)';
                this.textElement.style.left = '-' + textRect.width + 'px';
                break;
                
            case 'right-to-left':
                this.textElement.style.top = '50%';
                this.textElement.style.transform = 'translateY(-50%)';
                this.textElement.style.left = containerRect.width + 'px';
                break;
                
            case 'top-to-bottom':
                this.textElement.style.left = '50%';
                this.textElement.style.transform = 'translateX(-50%)';
                this.textElement.style.top = '-' + textRect.height + 'px';
                break;
                
            case 'bottom-to-top':
                this.textElement.style.left = '50%';
                this.textElement.style.transform = 'translateX(-50%)';
                this.textElement.style.top = containerRect.height + 'px';
                break;
        }
    }
    
    play() {
        if (this.isPlaying) return;
        
        this.isPlaying = true;
        this.startAnimation();
    }
    
    pause() {
        this.isPlaying = false;
        if (this.animationId) {
            cancelAnimationFrame(this.animationId);
            this.animationId = null;
        }
    }
    
    stop() {
        this.pause();
        this.setupStyles(); // Reset position
    }
    
    startAnimation() {
        const containerRect = this.container.getBoundingClientRect();
        const textRect = this.textElement.getBoundingClientRect();
        
        // Check if this is a vertical direction that needs letter-by-letter scrolling
        const isVerticalLetterByLetter = this.config.direction === 'top-to-bottom' || this.config.direction === 'bottom-to-top';
        
        if (isVerticalLetterByLetter) {
            // Letter-by-letter scrolling for vertical directions
            let currentIndex = 0;
            let startTime = null;
            const letterDuration = 1000 / this.config.speed * 20; // Adjust timing for letter-by-letter
            
            // Position text in center for vertical letter-by-letter
            this.textElement.style.position = 'absolute';
            this.textElement.style.left = '50%';
            this.textElement.style.top = '50%';
            this.textElement.style.transform = 'translate(-50%, -50%)';
            this.textElement.style.textAlign = 'center';
            this.textElement.textContent = '';
            
            const animateLetterByLetter = (currentTime) => {
                if (!this.isPlaying) return;
                
                if (!startTime) startTime = currentTime;
                
                const elapsed = currentTime - startTime;
                
                if (elapsed >= letterDuration) {
                    currentIndex++;
                    startTime = currentTime;
                    
                    if (currentIndex <= this.config.text.length) {
                        const visibleText = this.config.direction === 'top-to-bottom' 
                            ? this.config.text.slice(0, currentIndex)
                            : this.config.text.slice(-currentIndex);
                        this.textElement.textContent = visibleText;
                    } else {
                        // Animation complete, restart after a brief pause
                        setTimeout(() => {
                            if (this.isPlaying) {
                                currentIndex = 0;
                                this.textElement.textContent = '';
                                startTime = null;
                                this.animationId = requestAnimationFrame(animateLetterByLetter);
                            }
                        }, 1000);
                        return;
                    }
                }
                
                this.animationId = requestAnimationFrame(animateLetterByLetter);
            };
            
            this.animationId = requestAnimationFrame(animateLetterByLetter);
        } else {
            // Original scrolling behavior for horizontal directions
            let startTime = null;
            let startPosition = this.getCurrentPosition();
            let endPosition = this.getEndPosition();
            let distance = Math.abs(endPosition - startPosition);
            let duration = (distance / this.config.speed) * 1000; // Convert to milliseconds
            
            const animate = (currentTime) => {
                if (!this.isPlaying) return;
                
                if (!startTime) startTime = currentTime;
                
                const elapsed = currentTime - startTime;
                const progress = Math.min(elapsed / duration, 1);
                
                // Calculate current position
                let currentPosition;
                if (this.config.direction === 'left-to-right') {
                    currentPosition = startPosition + (endPosition - startPosition) * progress;
                } else {
                    currentPosition = startPosition - (startPosition - endPosition) * progress;
                }
                
                // Update position
                this.setPosition(currentPosition);
                
                if (progress < 1) {
                    this.animationId = requestAnimationFrame(animate);
                } else {
                    // Animation complete, restart
                    this.setupStyles();
                    setTimeout(() => {
                        if (this.isPlaying) {
                            this.startAnimation();
                        }
                    }, 100); // Small delay before restart
                }
            };
            
            this.animationId = requestAnimationFrame(animate);
        }
    }
    
    getCurrentPosition() {
        const rect = this.textElement.getBoundingClientRect();
        const containerRect = this.container.getBoundingClientRect();
        
        switch (this.config.direction) {
            case 'left-to-right':
            case 'right-to-left':
                return rect.left - containerRect.left;
                
            case 'top-to-bottom':
            case 'bottom-to-top':
                return rect.top - containerRect.top;
                
            default:
                return 0;
        }
    }
    
    getEndPosition() {
        const containerRect = this.container.getBoundingClientRect();
        const textRect = this.textElement.getBoundingClientRect();
        
        switch (this.config.direction) {
            case 'left-to-right':
                return containerRect.width;
                
            case 'right-to-left':
                return -textRect.width;
                
            case 'top-to-bottom':
                return containerRect.height;
                
            case 'bottom-to-top':
                return -textRect.height;
                
            default:
                return 0;
        }
    }
    
    setPosition(position) {
        switch (this.config.direction) {
            case 'left-to-right':
            case 'right-to-left':
                this.textElement.style.left = position + 'px';
                break;
                
            case 'top-to-bottom':
            case 'bottom-to-top':
                this.textElement.style.top = position + 'px';
                break;
        }
    }
    
    updateConfig(newConfig) {
        this.config = { ...this.config, ...newConfig };
        this.stop();
        this.createTextElement();
        this.setupStyles();
        if (this.isPlaying) {
            this.play();
        }
    }
    
    destroy() {
        this.stop();
        if (this.container) {
            this.container.innerHTML = '';
        }
    }
}

// Export for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ScrollingTextPlayer;
} else {
    window.ScrollingTextPlayer = ScrollingTextPlayer;
}