/* eslint-disable @next/next/no-img-element */
'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import { ScrollingText, ScrollDirection } from '@/components/ui/scrolling-text';
import { resolvePublicMediaUrl } from '@/lib/mediaUrl';

type MediaType = 'IMAGE' | 'VIDEO';

export type LayoutMedia = {
  id: string;
  name: string;
  type: MediaType;
  url: string;
  originalUrl?: string | null;
  duration?: number | null;
};

export type LayoutSectionItem = {
  id: string;
  order: number;
  duration?: number | null;
  orientation?: 'LANDSCAPE' | 'PORTRAIT';
  resizeMode?: 'FIT' | 'FILL' | 'STRETCH';
  rotation?: number; // 0, 90, 180, 270
  media: LayoutMedia;
};

export type ScrollTextData = {
  id: string;
  text: string;
  direction: ScrollDirection;
  speed: number;
  fontSize: number;
  fontColor: string;
  backgroundColor?: string;
  isOverlay: boolean;
  x: number;
  y: number;
  width: number;
  height: number;
  zIndex: number;
  sectionId?: string;
};

const objectFitFromResizeMode = (mode: string) =>
  mode === 'FILL' ? 'cover' : mode === 'STRETCH' ? 'fill' : 'contain';

export type LayoutSection = {
  id: string;
  name: string;
  order: number;
  x: number; // percentage
  y: number; // percentage
  width: number; // percentage
  height: number; // percentage
  loopEnabled: boolean;
  frequency?: number | null;
  type?: 'media' | 'text';
  textConfig?: {
    text: string;
    direction: ScrollDirection;
    speed: number;
    fontSize: number;
    fontWeight: 'normal' | 'bold' | 'bolder' | 'lighter';
    textColor: string;
    backgroundColor: string;
  };
  items: LayoutSectionItem[];
  scrollTexts?: ScrollTextData[];
};

export type Layout = {
  id: string;
  name: string;
  width: number;
  height: number;
  orientation?: 'LANDSCAPE' | 'PORTRAIT';
  sections: LayoutSection[];
  scrollTexts?: ScrollTextData[];
};

export function LayoutPlayer({
  layout,
  publicBaseUrl,
}: {
  layout: Layout;
  publicBaseUrl: string;
}) {
  const [sectionStates, setSectionStates] = useState<Record<string, { currentIdx: number }>>({});
  const timerRefs = useRef<Record<string, number>>({});
  const layoutRef = useRef(layout);
  const videoRefs = useRef<Record<string, HTMLVideoElement | null>>({});
  const hlsRefs = useRef<Record<string, Hls | null>>({});

  // Keep layout ref updated
  useEffect(() => {
    layoutRef.current = layout;
  }, [layout]);

  // Initialize section states - only for sections with items (media sections)
  useEffect(() => {
    const initialStates: Record<string, { currentIdx: number }> = {};
    layout.sections.forEach((section) => {
      // Only initialize state for media sections with items
      if (section.type !== 'text' && section.items && section.items.length > 0) {
        initialStates[section.id] = {
          currentIdx: 0,
        };
      }
    });
    setSectionStates(initialStates);
  }, [layout.id]); // Only re-initialize when layout changes

  // Setup timers for image items
  useEffect(() => {
    // Clear all existing timers
    Object.values(timerRefs.current).forEach((timer) => {
      if (timer) window.clearTimeout(timer);
    });
    timerRefs.current = {};

    const currentLayout = layoutRef.current;
    
    currentLayout.sections.forEach((section) => {
      // Skip text sections - they don't need timers
      if (section.type === 'text' || !section.items || section.items.length === 0) return;

      const state = sectionStates[section.id];
      if (!state) return;

      const currentItem = section.items[state.currentIdx];
      if (!currentItem || !currentItem.media) return;

      // Only set timer for images
      if (currentItem.media.type === 'IMAGE') {
        const updateSection = () => {
          setSectionStates((prev) => {
            const sectionState = prev[section.id];
            if (!sectionState) return prev;

            // Keep single-item sections continuously active.
            const nextIdx =
              section.items.length <= 1
                ? 0
                : section.loopEnabled
                ? (sectionState.currentIdx + 1) % section.items.length
                : Math.min(sectionState.currentIdx + 1, section.items.length - 1);

            return {
              ...prev,
              [section.id]: {
                currentIdx: nextIdx,
              },
            };
          });
        };

        const duration = Math.max(
          1,
          Number(currentItem.duration ?? 0) ||
            Number(currentItem.media.duration ?? 0) ||
            10
        );
        timerRefs.current[section.id] = window.setTimeout(updateSection, duration * 1000);
      }
    });

    return () => {
      Object.values(timerRefs.current).forEach((timer) => {
        if (timer) window.clearTimeout(timer);
      });
      timerRefs.current = {};
    };
  }, [sectionStates]);

  const getCurrentItem = useCallback((section: LayoutSection): LayoutSectionItem | null => {
    const state = sectionStates[section.id];
    if (!state || !section.items || section.items.length === 0) return null;
    
    return section.items[state.currentIdx] || null;
  }, [sectionStates]);

  const handleVideoEnd = useCallback((sectionId: string) => {
    setSectionStates((prev) => {
      const sectionState = prev[sectionId];
      if (!sectionState) return prev;

      const section = layoutRef.current.sections.find(s => s.id === sectionId);
      if (!section || !section.items || section.items.length === 0) return prev;

      // Keep single-item sections continuously active.
      const nextIdx =
        section.items.length <= 1
          ? 0
          : section.loopEnabled
          ? (sectionState.currentIdx + 1) % section.items.length
          : Math.min(sectionState.currentIdx + 1, section.items.length - 1);

      return {
        ...prev,
        [sectionId]: {
          currentIdx: nextIdx,
        },
      };
    });
  }, []);

  // Hide Next.js dev tools when media is playing
  useEffect(() => {
    // Inject style to hide Next.js dev tools
    const style = document.createElement('style');
    style.textContent = `
      [data-nextjs-dialog-overlay],
      [data-nextjs-dialog],
      [data-nextjs-toast],
      [data-nextjs-toast-root],
      [id^="__nextjs"],
      [class*="nextjs-toast"],
      [class*="__nextjs-toast"],
      button[aria-label*="Next.js"],
      div[role="dialog"][data-nextjs],
      div[data-nextjs-portal] {
        display: none !important;
        visibility: hidden !important;
        opacity: 0 !important;
        pointer-events: none !important;
      }
    `;
    document.head.appendChild(style);

    // Also hide elements directly
    const hideDevTools = () => {
      const selectors = [
        '[data-nextjs-dialog-overlay]',
        '[data-nextjs-dialog]',
        '[data-nextjs-toast]',
        '[data-nextjs-toast-root]',
        '[id^="__nextjs"]',
        'button[aria-label*="Next.js"]',
        'div[role="dialog"][data-nextjs]',
        'div[data-nextjs-portal]'
      ];
      
      selectors.forEach(selector => {
        document.querySelectorAll(selector).forEach((el) => {
          (el as HTMLElement).style.cssText = 'display: none !important; visibility: hidden !important; opacity: 0 !important; pointer-events: none !important;';
        });
      });
    };

    hideDevTools();
    const interval = setInterval(hideDevTools, 500);

    return () => {
      clearInterval(interval);
      if (style.parentNode) {
        style.parentNode.removeChild(style);
      }
    };
  }, []);

  // Setup HLS for video sections with debounce
  useEffect(() => {
    const timeoutId = setTimeout(() => {
      const setupVideos = async () => {
        for (const section of layout.sections) {
          // Skip text sections
          if (section.type === 'text') continue;
          
          const currentItem = getCurrentItem(section);
          if (!currentItem || currentItem.media.type !== 'VIDEO') continue;

          const videoEl = videoRefs.current[section.id];
          if (!videoEl) continue;

          const streamUrl =
            currentItem.media.originalUrl || currentItem.media.url;
          const url = resolvePublicMediaUrl(streamUrl, publicBaseUrl) ?? '';
          if (!url) continue;
          const isHLS = url.includes('.m3u8');

          // Cleanup existing HLS instance
          if (hlsRefs.current[section.id]) {
            hlsRefs.current[section.id]?.destroy();
            hlsRefs.current[section.id] = null;
          }

          // Reset video element
          videoEl.pause();
          videoEl.removeAttribute('src');
          videoEl.load();

          try {
            if (isHLS && Hls.isSupported()) {
              const hls = new Hls({
                enableWorker: true,
                lowLatencyMode: false,
                backBufferLength: 90,
                maxBufferLength: 30,
                maxMaxBufferLength: 60,
              });

              hlsRefs.current[section.id] = hls;
              hls.loadSource(url);
              hls.attachMedia(videoEl);

              hls.on(Hls.Events.MANIFEST_PARSED, () => {
                videoEl.play().catch(err => {
                  console.warn('HLS playback delayed:', err.message);
                });
              });

              hls.on(Hls.Events.ERROR, (event, data) => {
                console.error('HLS error:', data);
                if (data.fatal) {
                  switch (data.type) {
                    case Hls.ErrorTypes.NETWORK_ERROR:
                      console.error('Fatal network error, trying to recover');
                      hls.startLoad();
                      break;
                    case Hls.ErrorTypes.MEDIA_ERROR:
                      console.error('Fatal media error, trying to recover');
                      hls.recoverMediaError();
                      break;
                    default:
                      console.error('Fatal error, cannot recover');
                      handleVideoEnd(section.id);
                      break;
                  }
                }
              });
            } else if (isHLS && videoEl.canPlayType('application/vnd.apple.mpegurl')) {
              // Native HLS support (Safari)
              videoEl.src = url;
              await new Promise(resolve => setTimeout(resolve, 100)); // Small delay
              videoEl.play().catch(err => {
                console.warn('Native HLS playback delayed:', err.message);
              });
            } else if (!isHLS) {
              // Regular video file
              videoEl.src = url;
              await new Promise(resolve => setTimeout(resolve, 100)); // Small delay
              videoEl.play().catch(err => {
                console.warn('Video playback delayed:', err.message);
              });
            }
          } catch (error) {
            console.error('Video setup error:', error);
          }
        }
      };

      setupVideos();
    }, 100); // 100ms debounce

    // Cleanup on unmount
    return () => {
      clearTimeout(timeoutId);
      Object.values(hlsRefs.current).forEach(hls => {
        if (hls) {
          try {
            hls.destroy();
          } catch (e) {
            console.warn('HLS cleanup error:', e);
          }
        }
      });
      hlsRefs.current = {};
    };
  }, [sectionStates, layout.sections, publicBaseUrl, getCurrentItem, handleVideoEnd]);

  const renderSectionMedia = (section: LayoutSection, currentItem: LayoutSectionItem | null) => {
    if (!currentItem?.media) return null;

    const resizeMode = currentItem.resizeMode || 'FIT';
    const rotation = currentItem.rotation ?? 0;
    const objectFit = objectFitFromResizeMode(resizeMode);
    const is90or270 = rotation === 90 || rotation === 270;

    const mediaStyle: React.CSSProperties = {
      objectFit,
      transform: is90or270 ? undefined : `rotate(${rotation}deg)`,
      transformOrigin: 'center center',
      width: '100%',
      height: '100%',
    };

    const wrapperStyle90_270: React.CSSProperties = is90or270
      ? {
          position: 'absolute',
          left: '50%',
          top: '50%',
          width: '100%',
          height: '100%',
          transform: `translate(-50%, -50%) rotate(${rotation === 90 ? -90 : 90}deg)`,
          transformOrigin: 'center center',
        }
      : {};

    const streamUrl =
      currentItem.media.type === 'VIDEO' && currentItem.media.originalUrl
        ? currentItem.media.originalUrl
        : currentItem.media.url;
    const url = resolvePublicMediaUrl(streamUrl, publicBaseUrl) ?? '';
    if (!url) return null;

    const inner =
      currentItem.media.type === 'IMAGE' ? (
        <img
          key={`${section.id}-${currentItem.id}-${currentItem.media.id}`}
          src={url}
          alt={currentItem.media.name}
          className="h-full w-full bg-black"
          style={mediaStyle}
          draggable={false}
          onError={() => {
            console.error('Failed to load image:', currentItem.media?.url);
            handleVideoEnd(section.id);
          }}
          onContextMenu={(e) => e.preventDefault()}
        />
      ) : (
        <video
          ref={(el) => {
            videoRefs.current[section.id] = el;
          }}
          key={`${section.id}-${currentItem.id}-${currentItem.media.id}`}
          className="h-full w-full bg-black"
          style={{ ...mediaStyle, pointerEvents: 'none', cursor: 'none' }}
          autoPlay
          muted
          playsInline
          controls={false}
          disablePictureInPicture
          disableRemotePlayback
          loop={section.items.length === 1}
          onEnded={() => handleVideoEnd(section.id)}
          onError={(e) => {
            console.warn('Video error (will retry):', currentItem.media?.url, e.currentTarget.error);
            // Don't immediately fail, let it try to recover
            setTimeout(() => handleVideoEnd(section.id), 1000);
          }}
          onAbort={(e) => {
            console.warn('Video aborted (normal during transitions):', currentItem.media?.url);
            // Don't treat abort as an error - it's normal during component updates
          }}
          onContextMenu={(e) => e.preventDefault()}
        />
      );

    return is90or270 ? (
      <div style={wrapperStyle90_270}>{inner}</div>
    ) : (
      <div className="h-full w-full flex items-center justify-center overflow-hidden">{inner}</div>
    );
  };

  return (
    <div className="relative h-screen w-screen bg-black overflow-hidden" style={{ cursor: 'none' }}>
      {layout.sections.map((section) => {
        const currentItem = getCurrentItem(section);

        return (
          <div
            key={section.id}
            className="absolute"
            style={{
              left: `${section.x}%`,
              top: `${section.y}%`,
              width: `${section.width}%`,
              height: `${section.height}%`,
              overflow: 'hidden',
              zIndex: section.order + 10, // Ensure proper layering for overlays
            }}
          >
            {section.type === 'text' && section.textConfig ? (
              <>
                {console.log(`[DEBUG] Rendering text section: ${section.name}`, section.textConfig)}
                <ScrollingText
                  text={section.textConfig.text}
                  direction={section.textConfig.direction}
                  speed={section.textConfig.speed}
                  fontSize={section.textConfig.fontSize}
                  fontWeight={section.textConfig.fontWeight}
                  textColor={section.textConfig.textColor}
                  backgroundColor={section.textConfig.backgroundColor}
                  className="w-full h-full"
                />
              </>
            ) : (
              currentItem && currentItem.media ? renderSectionMedia(section, currentItem) : null
            )}
          </div>
        );
      })}
    </div>
  );
}
