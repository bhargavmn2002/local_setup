'use client';

/* eslint-disable react-hooks/exhaustive-deps */

import { useState, useEffect, useRef } from 'react';
import { PlaylistPlayer, type PlaylistItem as RenderItem } from '@/components/player/PlaylistPlayer';
import {
  LayoutPlayer,
  type Layout as LayoutType,
  type LayoutSection,
} from '@/components/player/LayoutPlayer';

const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000/api';
const PUBLIC_BASE_URL = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000/api').replace('/api', '');

/** Timeout for initial connection (status + pairing-code). Prevents infinite "Initializing..." when server is unreachable. */
const CONNECTION_TIMEOUT_MS = 15000;

function fetchWithTimeout(
  url: string,
  options: RequestInit,
  timeoutMs: number
): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => {
    controller.abort(new DOMException('Connection timeout', 'AbortError'));
  }, timeoutMs);
  return fetch(url, { ...options, signal: controller.signal }).finally(() =>
    clearTimeout(timeoutId)
  );
}

type PairingState = 'checking' | 'pairing' | 'paired' | 'error';

export default function PlayerPage() {
  const [pairingState, setPairingState] = useState<PairingState>('checking');
  const [pairingCode, setPairingCode] = useState<string>('');
  const [displayId, setDisplayId] = useState<string>('');
  const [deviceToken, setDeviceToken] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [playlist, setPlaylist] = useState<{ id: string; name: string; items: RenderItem[] } | null>(null);
  const [layout, setLayout] = useState<LayoutType | null>(null);
  const [isPaused, setIsPaused] = useState<boolean>(false);

  const pairingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const heartbeatIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const configIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const consecutive401Ref = useRef<number>(0);
  const CONSECUTIVE_401_BEFORE_RESET = 2;

  // Mark body so we can hide Next.js dev indicator (circular N logo) on player only
  useEffect(() => {
    document.body.classList.add('player-page');
    return () => document.body.classList.remove('player-page');
  }, []);

  // Check for existing display ID on mount
  useEffect(() => {
    const checkExistingDisplay = async () => {
      const storedDisplayId = localStorage.getItem('signox_display_id');
      const token = localStorage.getItem('deviceToken');

      if (storedDisplayId) {
        try {
          // Check if display still exists and get its status (with timeout so we don't hang forever)
          const response = await fetchWithTimeout(
            `${API_URL}/displays/${storedDisplayId}/status`,
            { method: 'GET' },
            CONNECTION_TIMEOUT_MS
          );
          
          if (response.status === 404) {
            // Display was deleted, clear localStorage and generate new
            console.log('🔧 Display not found, clearing localStorage and generating new pairing code...');
            localStorage.removeItem('signox_display_id');
            localStorage.removeItem('deviceToken');
            localStorage.removeItem('displayId');
            generatePairingCode();
            return;
          }

          if (!response.ok) {
            throw new Error('Failed to get display status');
          }

          const data = await response.json();
          
          // Use existing display data
          setDisplayId(data.id);
          setPairingCode(data.pairingCode);
          
          if (data.isPaired && data.deviceToken) {
            // Display is paired, use existing token
            localStorage.setItem('deviceToken', data.deviceToken);
            localStorage.setItem('displayId', data.id);
            setDeviceToken(data.deviceToken);
            setPairingState('paired');
            startHeartbeat(data.id, data.deviceToken);
            startConfigPoll(data.deviceToken);
          } else {
            // Display exists but not paired yet, show pairing code
            setPairingState('pairing');
            startPairingPoll(data.pairingCode);
          }
        } catch (error: unknown) {
          const isTimeout = error instanceof Error && error.name === 'AbortError';
          if (!isTimeout) console.error('Error checking display status:', error);
          if (isTimeout) {
            setError('Connection timed out. Check that the server is on and reachable.');
            setPairingState('error');
            return;
          }
          // On other error, clear localStorage and generate new pairing code
          localStorage.removeItem('signox_display_id');
          localStorage.removeItem('deviceToken');
          localStorage.removeItem('displayId');
          generatePairingCode();
        }
      } else {
        // No stored display ID, generate new pairing code
        generatePairingCode();
      }
    };

    checkExistingDisplay();

    return () => {
      // Cleanup intervals on unmount
      if (pairingIntervalRef.current) {
        clearInterval(pairingIntervalRef.current);
      }
      if (heartbeatIntervalRef.current) {
        clearInterval(heartbeatIntervalRef.current);
      }
      if (configIntervalRef.current) {
        clearInterval(configIntervalRef.current);
      }
    };
  }, []);

  const generatePairingCode = async () => {
    try {
      setError('');
      const response = await fetchWithTimeout(
        `${API_URL}/displays/pairing-code`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        },
        CONNECTION_TIMEOUT_MS
      );

      if (!response.ok) {
        throw new Error('Failed to generate pairing code');
      }

      const data = await response.json();
      setPairingCode(data.pairingCode);
      setDisplayId(data.displayId);
      setPairingState('pairing');

      // Save display ID to localStorage to prevent duplicates
      localStorage.setItem('signox_display_id', data.displayId);

      // Start polling for pairing status
      startPairingPoll(data.pairingCode);
    } catch (err: unknown) {
      const isTimeout = err instanceof Error && err.name === 'AbortError';
      if (!isTimeout) console.error('Failed to generate pairing code:', err);
      setError(
        isTimeout
          ? 'Connection timed out. Check server and network, then tap Retry.'
          : 'Failed to connect to server. Please refresh the page.'
      );
      setPairingState('error');
    }
  };

  const startPairingPoll = (code: string) => {
    // Clear any existing interval
    if (pairingIntervalRef.current) {
      clearInterval(pairingIntervalRef.current);
    }

    // Poll every 5 seconds
    pairingIntervalRef.current = setInterval(async () => {
      try {
        const response = await fetch(`${API_URL}/displays/check-status`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ pairingCode: code }),
        });

        // Handle 401 - only reset after consecutive 401s (avoids reset on transient/server restart)
        if (response.status === 401) {
          consecutive401Ref.current += 1;
          if (consecutive401Ref.current >= CONSECUTIVE_401_BEFORE_RESET) {
            handleUnauthorized();
          }
          return;
        }

        consecutive401Ref.current = 0;

        if (!response.ok) {
          return; // Continue polling on other errors
        }

        const data = await response.json();

        if (data.isPaired && data.deviceToken) {
          // Paired! Store token and switch to paired state
          localStorage.setItem('deviceToken', data.deviceToken);
          localStorage.setItem('displayId', data.displayId);
          localStorage.setItem('signox_display_id', data.displayId);
          setDeviceToken(data.deviceToken);
          setDisplayId(data.displayId);
          setPairingState('paired');

          // Stop pairing poll
          if (pairingIntervalRef.current) {
            clearInterval(pairingIntervalRef.current);
            pairingIntervalRef.current = null;
          }

          // Start heartbeat
          startHeartbeat(data.displayId, data.deviceToken);
          startConfigPoll(data.deviceToken);
        }
      } catch (err) {
        console.error('Pairing poll error:', err);
        // Continue polling on error
      }
    }, 5000); // Poll every 5 seconds
  };

  const startHeartbeat = (id: string, token: string) => {
    // Clear any existing heartbeat interval
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }

    // Send heartbeat every 30 seconds
    heartbeatIntervalRef.current = setInterval(async () => {
      try {
        const response = await fetch(`${API_URL}/displays/${id}/heartbeat`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
        });

        // Handle 401 - only reset after consecutive 401s
        if (response.status === 401) {
          consecutive401Ref.current += 1;
          if (consecutive401Ref.current >= CONSECUTIVE_401_BEFORE_RESET) {
            handleUnauthorized();
          }
          return;
        }

        consecutive401Ref.current = 0;

        if (!response.ok) {
          console.error('Heartbeat failed:', response.status);
        }
      } catch (err) {
        console.error('Heartbeat error:', err);
      }
    }, 30000); // Every 30 seconds
  };

  const fetchConfigOnce = async (token: string) => {
    try {
      const response = await fetch(`${API_URL}/player/config`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 401) {
        consecutive401Ref.current += 1;
        if (consecutive401Ref.current >= CONSECUTIVE_401_BEFORE_RESET) {
          handleUnauthorized();
        }
        return;
      }

      consecutive401Ref.current = 0;

      if (!response.ok) return;

      const data = await response.json();
      
      // Log config changes for debugging
      const newPlaylist = data.playlist ?? null;
      const newLayout = data.layout ?? null;
      const activeSchedule = data.activeSchedule ?? null;
      const newIsPaused = data.isPaused ?? false;
      
      // Check if content has changed
      const playlistChanged = (playlist?.id !== newPlaylist?.id);
      const layoutChanged = (layout?.id !== newLayout?.id);
      const pausedChanged = (isPaused !== newIsPaused);
      
      if (playlistChanged || layoutChanged) {
        console.log('🔄 Content changed:', {
          from: {
            playlist: playlist?.name || 'none',
            layout: layout?.name || 'none'
          },
          to: {
            playlist: newPlaylist?.name || 'none',
            layout: newLayout?.name || 'none'
          },
          activeSchedule: activeSchedule ? `${activeSchedule.name} (priority: ${activeSchedule.priority})` : 'none'
        });
      }
      
      if (pausedChanged) {
        console.log(newIsPaused ? '⏸️ Display paused' : '▶️ Display resumed');
      }
      
      setPlaylist(newPlaylist);
      setLayout(newLayout);
      setIsPaused(newIsPaused);

      // Debug logging for layout sections
      if (newLayout && newLayout.sections) {
        console.log('[DEBUG] Layout received:', newLayout.name);
        newLayout.sections.forEach((section: LayoutSection) => {
          console.log(`[DEBUG] Section: ${section.name}`, {
            type: section.type,
            hasTextConfig: !!section.textConfig,
            textConfig: section.textConfig
          });
        });
      }
    } catch (err) {
      // ignore transient errors; next poll will retry
    }
  };

  const startConfigPoll = (token: string) => {
    if (configIntervalRef.current) {
      clearInterval(configIntervalRef.current);
      configIntervalRef.current = null;
    }

    // Initial fetch immediately
    fetchConfigOnce(token);

    // Poll every 5 seconds for more responsive schedule changes
    configIntervalRef.current = setInterval(() => {
      fetchConfigOnce(token);
    }, 5000);
  };

  const handleUnauthorized = () => {
    // Self-healing: Clear localStorage and reset to pairing mode (after consecutive 401s)
    consecutive401Ref.current = 0;
    console.log('🔧 Self-healing: Display unauthorized, resetting to pairing mode...');
    localStorage.removeItem('deviceToken');
    localStorage.removeItem('displayId');
    localStorage.removeItem('signox_display_id');

    // Clear intervals
    if (pairingIntervalRef.current) {
      clearInterval(pairingIntervalRef.current);
      pairingIntervalRef.current = null;
    }
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
    if (configIntervalRef.current) {
      clearInterval(configIntervalRef.current);
      configIntervalRef.current = null;
    }

    // Reset state and generate new pairing code
    setDeviceToken('');
    setDisplayId('');
    setPairingCode('');
    setPlaylist(null);
    setLayout(null);
    setPairingState('checking');
    generatePairingCode();
  };

  const handleReset = () => {
    // Clear all localStorage and reload page
    localStorage.removeItem('signox_display_id');
    localStorage.removeItem('deviceToken');
    localStorage.removeItem('displayId');
    window.location.reload();
  };

  // Pairing Screen UI
  if (pairingState === 'pairing' || pairingState === 'checking') {
    return (
      <div className="flex min-h-screen items-center justify-center bg-black text-white">
        <div className="text-center">
          {pairingState === 'checking' ? (
            <div className="space-y-6">
              <div className="inline-block h-16 w-16 animate-spin rounded-full border-4 border-solid border-white border-t-transparent"></div>
              <p className="text-xl">Initializing...</p>
              <p className="text-sm text-gray-400 max-w-xs mx-auto">
                Connecting to server. If this doesn’t load in ~15s, check Wi‑Fi and that the server is on.
              </p>
            </div>
          ) : (
            <div className="space-y-8">
              <div className="space-y-4">
                <h1 className="text-5xl font-bold">SignoX</h1>
                <p className="text-xl text-gray-400">Digital Signage Player</p>
              </div>

              <div className="space-y-6">
                <div>
                  <p className="mb-4 text-lg text-gray-300">
                    Enter this code on your admin dashboard to pair this device:
                  </p>
                  <div className="inline-block rounded-lg bg-gray-900 px-8 py-6">
                    <code className="text-6xl font-mono font-bold tracking-wider">
                      {pairingCode}
                    </code>
                  </div>
                </div>

                <div className="flex items-center justify-center gap-2 text-gray-400">
                  <div className="h-3 w-3 animate-pulse rounded-full bg-yellow-400 shadow-lg shadow-yellow-400/50"></div>
                  <p className="text-sm font-medium">Waiting for pairing...</p>
                </div>
              </div>

              {error && (
                <div className="mt-6 rounded-xl bg-red-900/50 px-6 py-3 text-red-200 border border-red-500/30 shadow-lg">
                  {error}
                </div>
              )}

              {/* Reset/Unpair Button */}
              <div className="mt-8">
                <button
                  onClick={handleReset}
                  className="rounded-xl bg-gray-800 px-8 py-3 text-sm text-gray-300 hover:bg-gray-700 transition-all hover:shadow-lg font-medium"
                >
                  Reset / Unpair
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  // Paired / Standby Screen
  if (pairingState === 'paired') {

    // Render layout if assigned (layouts take priority)
    if (layout && layout.sections && layout.sections.length > 0) {
      // Check if any section has valid media items OR is a text section
      const sectionsWithContent = layout.sections.filter(section => 
        (section.type === 'text' && section.textConfig) || // Text sections with config
        (section.items && section.items.length > 0 && section.items.some(item => item.media !== null && item.media !== undefined)) // Media sections with items
      );
      
      if (sectionsWithContent.length > 0) {
        return (
          <LayoutPlayer layout={layout} publicBaseUrl={PUBLIC_BASE_URL} />
        );
      }
    }

    // Fall back to playlist if no layout or layout has no media
    if (playlist && playlist.items && playlist.items.length > 0) {
      return (
        <PlaylistPlayer items={playlist.items} publicBaseUrl={PUBLIC_BASE_URL} isPaused={isPaused} />
      );
    }

    return (
      <div className="flex min-h-screen items-center justify-center bg-black text-white">
        <div className="flex items-center justify-center w-full h-full">
          {/* Try to load ArhamWorld logo first, fallback to text */}
          <div className="text-center space-y-6">
            <img 
              src="/arhamworld-logo.png" 
              alt="ArhamWorld Group of Companies" 
              className="max-w-[80vw] max-h-[60vh] object-contain mx-auto"
              style={{ 
                filter: 'drop-shadow(0 0 20px rgba(255, 255, 255, 0.1))'
              }}
              onError={(e) => {
                // Hide image and show text fallback
                const target = e.target as HTMLImageElement;
                target.style.display = 'none';
                const textFallback = target.nextElementSibling as HTMLElement;
                if (textFallback) {
                  textFallback.style.display = 'block';
                }
              }}
            />
            {/* Text fallback - hidden by default, shown if image fails */}
            <div className="hidden space-y-4" style={{ display: 'none' }}>
              <div className="space-y-2">
                <h1 className="text-8xl font-bold bg-gradient-to-r from-blue-400 to-green-400 bg-clip-text text-transparent">
                  Arham<span className="text-green-400">World</span>
                </h1>
                <p className="text-2xl text-gray-400 font-light tracking-wide">
                  Group of Companies
                </p>
              </div>
              <div className="flex items-center justify-center space-x-4 mt-8">
                <div className="w-16 h-1 bg-gradient-to-r from-blue-400 to-green-400 rounded-full"></div>
                <div className="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
                <div className="w-16 h-1 bg-gradient-to-r from-green-400 to-blue-400 rounded-full"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Error State
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-gray-900 to-black text-white">
      <div className="text-center space-y-6">
        <div className="text-7xl">⚠️</div>
        <h1 className="text-4xl font-black bg-gradient-to-r from-red-400 to-orange-500 bg-clip-text text-transparent">Connection Error</h1>
        <p className="text-gray-400 text-lg font-medium">{error || 'Failed to connect to server'}</p>
        <button
          onClick={() => {
            setPairingState('checking');
            generatePairingCode();
          }}
          className="mt-4 rounded-xl bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 px-8 py-4 text-gray-900 font-bold shadow-lg hover:shadow-xl transition-all"
        >
          Retry
        </button>
      </div>
    </div>
  );
}
