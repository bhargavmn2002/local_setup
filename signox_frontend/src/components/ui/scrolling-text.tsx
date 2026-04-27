'use client';

import { useEffect, useRef } from 'react';

export type ScrollDirection = 'left-to-right' | 'right-to-left' | 'top-to-bottom' | 'bottom-to-top';

interface ScrollingTextProps {
  text: string;
  direction?: ScrollDirection;
  speed?: number; // pixels per second
  fontSize?: number;
  fontWeight?: 'normal' | 'bold' | 'bolder' | 'lighter';
  textColor?: string;
  backgroundColor?: string;
  className?: string;
  style?: React.CSSProperties;
}

export function ScrollingText({
  text,
  direction = 'left-to-right',
  speed = 50,
  fontSize = 24,
  fontWeight = 'normal',
  textColor = '#000000',
  backgroundColor = 'transparent',
  className = '',
  style = {},
}: ScrollingTextProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const textRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    if (!containerRef.current || !textRef.current || !text) return;

    const container = containerRef.current;
    const textElement = textRef.current;

    const rawSpeed = Number(speed);
    const pxPerSec =
      Number.isFinite(rawSpeed) && rawSpeed > 0 ? Math.max(rawSpeed, 5) : 50;
    
    // Cancel any existing animation
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }
    
    // Force reflow to get accurate measurements
    container.offsetHeight;
    textElement.offsetHeight;

    const containerRect = container.getBoundingClientRect();
    const isVertical = direction === 'top-to-bottom' || direction === 'bottom-to-top';
    const displayText = isVertical ? text.split('').join('\n') : text;
    textElement.textContent = displayText;
    const textRect = textElement.getBoundingClientRect();

    let startTime: number | null = null;
    let startPos: { x: number; y: number };
    let endPos: { x: number; y: number };
    let distance: number;

    switch (direction) {
      case 'left-to-right':
        startPos = { x: -textRect.width, y: (containerRect.height - textRect.height) / 2 };
        endPos = { x: containerRect.width, y: startPos.y };
        distance = containerRect.width + textRect.width;
        break;
      case 'right-to-left':
        startPos = { x: containerRect.width, y: (containerRect.height - textRect.height) / 2 };
        endPos = { x: -textRect.width, y: startPos.y };
        distance = containerRect.width + textRect.width;
        break;
      case 'top-to-bottom':
        startPos = { x: (containerRect.width - textRect.width) / 2, y: -textRect.height };
        endPos = { x: startPos.x, y: containerRect.height };
        distance = containerRect.height + textRect.height;
        break;
      case 'bottom-to-top':
        startPos = { x: (containerRect.width - textRect.width) / 2, y: containerRect.height };
        endPos = { x: startPos.x, y: -textRect.height };
        distance = containerRect.height + textRect.height;
        break;
      default:
        startPos = { x: containerRect.width, y: (containerRect.height - textRect.height) / 2 };
        endPos = { x: -textRect.width, y: startPos.y };
        distance = containerRect.width + textRect.width;
        break;
    }

    const duration = (distance / pxPerSec) * 1000; // Convert to milliseconds

    const animate = (currentTime: number) => {
      if (!startTime) startTime = currentTime;
      
      // Continuous loop (train-like) without pause/jump artifacts.
      const elapsed = currentTime - startTime;
      const progress = (elapsed % duration) / duration;
      
      // Calculate current position
      const currentX = startPos.x + (endPos.x - startPos.x) * progress;
      const currentY = startPos.y + (endPos.y - startPos.y) * progress;
      
      // Update position
      textElement.style.left = `${currentX}px`;
      textElement.style.top = `${currentY}px`;
      animationRef.current = requestAnimationFrame(animate);
    };

    // Set initial position and start animation
    textElement.style.position = 'absolute';
    textElement.style.left = `${startPos.x}px`;
    textElement.style.top = `${startPos.y}px`;
    textElement.style.transform = 'none';
    textElement.style.textAlign = direction === 'top-to-bottom' || direction === 'bottom-to-top' ? 'center' : 'left';
    textElement.textContent = displayText;
    animationRef.current = requestAnimationFrame(animate);

    // Cleanup function
    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [text, direction, speed, fontSize, fontWeight]);

  if (!text) {
    return (
      <div 
        className={`flex items-center justify-center text-gray-400 ${className}`}
        style={{ backgroundColor, ...style }}
      >
        <span style={{ fontSize: `${fontSize}px`, fontWeight }}>
          No text to display
        </span>
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      className={`relative overflow-hidden ${className}`}
      style={{ backgroundColor, ...style }}
    >
      <div
        ref={textRef}
        className="absolute"
        style={{
          fontSize: `${fontSize}px`,
          fontWeight,
          color: textColor,
          pointerEvents: 'none',
          whiteSpace: direction === 'top-to-bottom' || direction === 'bottom-to-top' ? 'pre' : 'nowrap',
          lineHeight: direction === 'top-to-bottom' || direction === 'bottom-to-top' ? '1' : 'normal',
          textAlign: direction === 'top-to-bottom' || direction === 'bottom-to-top' ? 'center' : 'left',
        }}
      >
        {text}
      </div>
    </div>
  );
}