/* eslint-disable @next/next/no-img-element */
'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Hls from 'hls.js';
import { resolvePublicMediaUrl } from '@/lib/mediaUrl';

type MediaType = 'IMAGE' | 'VIDEO';

export type PlaylistMedia = {
  id: string;
  name: string;
  type: MediaType;
  url: string;
  /** MP4 or direct file; preferred over HLS `url` for playback when set */
  originalUrl?: string | null;
  duration?: number | null;
};

export type PlaylistItem = {
  id: string;
  order: number;
  duration?: number | null; // override (seconds)
  loopVideo?: boolean; // if true, video loops for duration then next item
  orientation?: 'LANDSCAPE' | 'PORTRAIT';
  resizeMode?: 'FIT' | 'FILL' | 'STRETCH';
  rotation?: number; // 0, 90, 180, 270
  media: PlaylistMedia;
};

const objectFitFromResizeMode = (mode: string) =>
  mode === 'FILL' ? 'cover' : mode === 'STRETCH' ? 'fill' : 'contain';

export function PlaylistPlayer({
  items,
  publicBaseUrl,
  isPaused = false,
}: {
  items: PlaylistItem[];
  publicBaseUrl: string; // e.g. http://localhost:5000
  isPaused?: boolean;
}) {
  const ordered = useMemo(
    () =>
      [...items]
        .filter((it) => !!it.media)
        .sort((a, b) => a.order - b.order),
    [items]
  );

  const [idx, setIdx] = useState(0);
  const timerRef = useRef<number | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hlsRef = useRef<Hls | null>(null);

  const current = ordered.length > 0 ? ordered[idx % ordered.length] : null;

  useEffect(() => {
    setIdx(0);
  }, [ordered.length]);

  // Handle pause/play state
  useEffect(() => {
    if (!videoRef.current) return;

    const video = videoRef.current;

    if (isPaused) {
      console.log('⏸️ Pausing video playback');
      // Only pause if video is not already paused
      if (!video.paused) {
        video.pause();
      }
      // Clear timer to prevent advancing to next item
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    } else {
      console.log('▶️ Resuming video playback');
      // Only play if video is paused and has a valid source
      if (video.paused && video.readyState >= 2) {
        video.play().catch(err => {
          // Ignore abort errors - they happen when switching videos
          if (err.name !== 'AbortError') {
            console.error('Failed to resume playback:', err);
          }
        });
      }
    }
  }, [isPaused]);

  // Setup HLS for video playback
  useEffect(() => {
    if (!current || current.media.type !== 'VIDEO' || !videoRef.current) return;

    const streamUrl =
      current.media.type === 'VIDEO' && current.media.originalUrl
        ? current.media.originalUrl
        : current.media.url;
    const src = resolvePublicMediaUrl(streamUrl, publicBaseUrl);
    if (!src) return;

    // Check if it's an HLS stream
    const isHLS = src.includes('.m3u8');

    if (isHLS && Hls.isSupported()) {
      // Use HLS.js for HLS streams
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: false,
        backBufferLength: 90,
      });
      
      hlsRef.current = hls;
      hls.loadSource(src);
      hls.attachMedia(videoRef.current);
      
      hls.on(Hls.Events.MANIFEST_PARSED, async () => {
        if (videoRef.current) {
          videoRef.current.volume = 1.0;
          videoRef.current.muted = false;
          
          try {
            await videoRef.current.play();
          } catch (err: unknown) {
            const errName = err instanceof Error ? err.name : '';
            // If audio autoplay is blocked, try muted playback
            if (errName === 'NotAllowedError') {
              console.log('Audio autoplay blocked, falling back to muted playback');
              videoRef.current.muted = true;
              try {
                await videoRef.current.play();
              } catch (mutedErr) {
                console.error('Even muted playback failed:', mutedErr);
              }
            } else if (errName !== 'AbortError') {
              console.log('Autoplay with audio blocked by browser policy');
            }
          }
        }
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
              setIdx((v) => (v + 1) % ordered.length);
              break;
          }
        }
      });

      return () => {
        if (hlsRef.current) {
          hlsRef.current.destroy();
          hlsRef.current = null;
        }
      };
    } else if (isHLS && videoRef.current.canPlayType('application/vnd.apple.mpegurl')) {
      // Native HLS support (Safari)
      videoRef.current.src = src;
      videoRef.current.volume = 1.0;
      videoRef.current.muted = false;
      
      videoRef.current.play().catch(async (err) => {
        // If audio autoplay is blocked, try muted playback
        if (err.name === 'NotAllowedError' && videoRef.current) {
          console.log('Audio autoplay blocked, falling back to muted playback');
          videoRef.current.muted = true;
          try {
            await videoRef.current.play();
          } catch (mutedErr) {
            console.error('Even muted playback failed:', mutedErr);
          }
        } else if (err.name !== 'AbortError') {
          console.log('Autoplay with audio blocked by browser policy');
        }
      });
    } else if (!isHLS) {
      // Regular video file - abort any previous loading
      if (videoRef.current.src && videoRef.current.src !== src) {
        videoRef.current.pause();
        videoRef.current.removeAttribute('src');
        videoRef.current.load(); // This will abort any pending requests
      }
      
      videoRef.current.src = src;
      videoRef.current.volume = 1.0;
      videoRef.current.muted = false;
      
      videoRef.current.play().catch(async (err) => {
        // If audio autoplay is blocked, try muted playback
        if (err.name === 'NotAllowedError' && videoRef.current) {
          console.log('Audio autoplay blocked, falling back to muted playback');
          videoRef.current.muted = true;
          try {
            await videoRef.current.play();
          } catch (mutedErr) {
            console.error('Even muted playback failed:', mutedErr);
          }
        } else if (err.name !== 'AbortError') {
          console.log('Autoplay with audio blocked by browser policy');
        }
      });
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
      
      // Clean up video element to prevent abort errors
      if (videoRef.current) {
        videoRef.current.pause();
        videoRef.current.removeAttribute('src');
        videoRef.current.load(); // This will abort any pending requests
      }
    };
  }, [current?.media.id, current?.media.url, current?.media.originalUrl, publicBaseUrl, ordered.length]);

  // Timer for advancing to next item. Depend only on idx and ordered.length so config polling
  // (which replaces items/ordered) doesn't clear the timer and prevent advancement.
  useEffect(() => {
    if (timerRef.current) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }

    // Don't set timer if paused
    if (isPaused) {
      console.log('⏸️ Timer paused');
      return;
    }

    if (ordered.length === 0) return;

    const currentItem = ordered[idx % ordered.length];
    if (!currentItem?.media) return;

    const seconds = Math.max(
      1,
      Number(currentItem.duration ?? 0) || Number(currentItem.media.duration ?? 0) || 10
    );

    if (currentItem.media.type === 'IMAGE') {
      timerRef.current = window.setTimeout(() => {
        setIdx((v) => (v + 1) % ordered.length);
      }, seconds * 1000);
    } else if (currentItem.media.type === 'VIDEO' && currentItem.loopVideo) {
      timerRef.current = window.setTimeout(() => {
        setIdx((v) => (v + 1) % ordered.length);
      }, seconds * 1000);
    }
    // Non-looped video: onEnded advances (no timer)

    return () => {
      if (timerRef.current) {
        window.clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [idx, ordered.length, isPaused]);

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

  // No playable items (avoid silent black screen)
  if (ordered.length === 0) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-black p-8 text-center text-white">
        <div className="max-w-md space-y-2">
          <p className="text-xl font-semibold">No playable media</p>
          <p className="text-sm text-gray-400">
            {items.length === 0
              ? 'This playlist has no items. Add media in the dashboard.'
              : 'Playlist items have no media attached, or URLs are missing.'}
          </p>
        </div>
      </div>
    );
  }

  if (!current) return null;

  const streamUrlForPlayback =
    current.media.type === 'VIDEO' && current.media.originalUrl
      ? current.media.originalUrl
      : current.media.url;
  const src = resolvePublicMediaUrl(streamUrlForPlayback, publicBaseUrl);
  if (!src) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-black p-8 text-center text-white">
        <div className="max-w-md space-y-2">
          <p className="text-xl font-semibold">Invalid media URL</p>
          <p className="text-sm text-gray-400">{current.media.name}</p>
        </div>
      </div>
    );
  }

  const resizeMode = current.resizeMode || 'FIT';
  const rotation = current.rotation ?? 0;
  const objectFit = objectFitFromResizeMode(resizeMode);
  const is90or270 = rotation === 90 || rotation === 270;

  const mediaStyle: React.CSSProperties = {
    objectFit,
    transform: is90or270 ? undefined : `rotate(${rotation}deg)`,
    transformOrigin: 'center center',
    width: '100%',
    height: '100%',
  };

  const wrapperStyle90_270: React.CSSProperties = {
    position: 'absolute',
    left: '50%',
    top: '50%',
    width: '100vh',
    height: '100vw',
    transform: `translate(-50%, -50%) rotate(${rotation === 90 ? -90 : 90}deg)`,
    transformOrigin: 'center center',
  };

  const inner = current.media.type === 'IMAGE' ? (
    <img
      key={current.media.id}
      src={src}
      alt={current.media.name}
      className="h-full w-full bg-black"
      style={mediaStyle}
      draggable={false}
      onContextMenu={(e) => e.preventDefault()}
    />
  ) : (
    <video
      ref={videoRef}
      key={current.media.id}
      className="h-full w-full bg-black"
      style={{ ...mediaStyle, pointerEvents: 'none', cursor: 'none' }}
      autoPlay
      playsInline
      preload="auto"
      loop={!!current.loopVideo}
      controls={false}
      disablePictureInPicture
      disableRemotePlayback
      onEnded={() => {
        if (!current.loopVideo) setIdx((v) => (v + 1) % ordered.length);
      }}
      onError={() => setIdx((v) => (v + 1) % ordered.length)}
      onContextMenu={(e) => e.preventDefault()}
    />
  );

  return (
    <div className="h-screen w-screen bg-black overflow-hidden" style={{ cursor: 'none', position: 'relative' }}>
      {is90or270 ? (
        <div style={wrapperStyle90_270}>{inner}</div>
      ) : (
        <div className="h-screen w-screen flex items-center justify-center overflow-hidden">
          {inner}
        </div>
      )}
    </div>
  );
}

