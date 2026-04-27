'use client';

import { useState, useEffect, useRef } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { Button } from '@/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Plus, Edit, Trash2, Loader2, Calendar, Monitor, Activity, Search, CheckCircle } from 'lucide-react';
import api from '@/lib/api';
import AOS from 'aos';
import 'aos/dist/aos.css';

interface Playlist {
  id: string;
  name: string;
}

interface Layout {
  id: string;
  name: string;
}

interface Display {
  id: string;
  name: string | null;
  pairingCode: string | null;
  location: string | null;
  playlistId?: string | null;
  playlist?: Playlist | null;
  layoutId?: string | null;
  layout?: Layout | null;
  status: 'ONLINE' | 'OFFLINE' | 'PAIRING' | 'ERROR';
  lastHeartbeat: string | null;
  isPaired: boolean;
  createdAt: string;
  activeSchedule?: {
    id: string;
    name: string;
    priority: number;
    contentType: string;
    contentName: string;
  } | null;
}

// Circular Progress Component
function CircularProgress({ progress, size = 24 }: { progress: number; size?: number }) {
  const radius = (size - 4) / 2;
  const circumference = radius * 2 * Math.PI;
  const strokeDashoffset = circumference - (progress / 100) * circumference;

  return (
    <div className="relative inline-flex items-center justify-center">
      <svg
        className="transform -rotate-90"
        width={size}
        height={size}
      >
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth="2"
          fill="transparent"
          className="text-gray-200"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth="2"
          fill="transparent"
          strokeDasharray={circumference}
          strokeDashoffset={strokeDashoffset}
          className="text-blue-500 transition-all duration-300 ease-in-out"
        />
      </svg>
      <span className="absolute text-xs font-medium text-gray-700">
        {Math.round(progress)}%
      </span>
    </div>
  );
}

export default function DisplaysPage() {
  const { user } = useAuth();
  const [displays, setDisplays] = useState<Display[]>([]);
  const [filteredDisplays, setFilteredDisplays] = useState<Display[]>([]);
  const [selectedDisplayIds, setSelectedDisplayIds] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [displayLimits, setDisplayLimits] = useState<{maxDisplays: number, currentCount: number} | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [pairingCode, setPairingCode] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [pairingLoading, setPairingLoading] = useState(false);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const [assignOpen, setAssignOpen] = useState(false);
  const [assignLoading, setAssignLoading] = useState(false);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [layouts, setLayouts] = useState<Layout[]>([]);
  const [selectedDisplay, setSelectedDisplay] = useState<Display | null>(null);
  const [selectedPlaylistId, setSelectedPlaylistId] = useState<string>('');
  const [selectedLayoutId, setSelectedLayoutId] = useState<string>('');
  const [contentType, setContentType] = useState<'playlist' | 'layout'>('playlist');

  const [editOpen, setEditOpen] = useState(false);
  const [editDisplay, setEditDisplay] = useState<Display | null>(null);
  const [editName, setEditName] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editLoading, setEditLoading] = useState(false);
  const editNameInputRef = useRef<HTMLInputElement>(null);

  // Progress tracking for content push
  const [pushProgress, setPushProgress] = useState<Record<string, { progress: number; isComplete: boolean; isPermanent: boolean }>>({});

  // Check if user has access
  const hasAccess =
    user?.role === 'USER_ADMIN' ||
    (user?.role === 'STAFF' && user?.staffRole === 'DISPLAY_MANAGER');

  const canDelete = user?.role === 'USER_ADMIN';
  const canAssign = user?.role === 'USER_ADMIN';

  // Filter displays based on search query
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredDisplays(displays);
      return;
    }

    const query = searchQuery.toLowerCase().trim();
    const filtered = displays.filter((display) => {
      const name = (display.name || '').toLowerCase();
      const id = display.id.toLowerCase();
      const pairingCode = (display.pairingCode || '').toLowerCase();
      const location = (display.location || '').toLowerCase();
      
      return (
        name.includes(query) ||
        id.includes(query) ||
        pairingCode.includes(query) ||
        location.includes(query)
      );
    });

    setFilteredDisplays(filtered);
  }, [searchQuery, displays]);

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    if (hasAccess) {
      // Initial fetch
      fetchDisplays(false);
      
      // Set up polling every 10 seconds for status updates
      const interval = setInterval(() => {
        fetchDisplays(true); // Pass true to indicate this is a refresh
      }, 10000);
      
      // Cleanup interval on unmount
      return () => clearInterval(interval);
    }
  }, [hasAccess]);

  useEffect(() => {
    if (displays.length > 0) {
      fetchDisplayLimits();
    }
  }, [displays, user]);

  const fetchDisplays = async (isRefresh = false) => {
    try {
      if (!isRefresh) {
        setLoading(true);
      } else {
        setRefreshing(true);
      }
      const response = await api.get('/displays');
      const displaysData = response.data.displays || response.data || [];
      setDisplays(displaysData);
      
      // Update display limits info
      const pairedDisplays = displaysData.filter((d: Display) => d.isPaired).length;
      if (displayLimits) {
        setDisplayLimits({
          ...displayLimits,
          currentCount: pairedDisplays
        });
      }
    } catch (error: any) {
      console.error('Failed to fetch displays:', error);
      if (!isRefresh) {
        setError('Failed to load displays. Please try again.');
      }
    } finally {
      if (!isRefresh) {
        setLoading(false);
      } else {
        setRefreshing(false);
      }
    }
  };

  const fetchDisplayLimits = async () => {
    try {
      // For USER_ADMIN, check their own userAdminProfile limits
      if (user?.role === 'USER_ADMIN' && user?.userAdminProfile) {
        const pairedDisplays = displays.filter(d => d.isPaired).length;
        setDisplayLimits({
          maxDisplays: user.userAdminProfile.maxDisplays || 10,
          currentCount: pairedDisplays
        });
      }
      // For CLIENT_ADMIN, check their clientProfile limits (if they still have limits)
      else if (user?.role === 'CLIENT_ADMIN' && user?.clientProfile) {
        const pairedDisplays = displays.filter(d => d.isPaired).length;
        setDisplayLimits({
          maxDisplays: user.clientProfile.maxDisplays || 10,
          currentCount: pairedDisplays
        });
      }
      // For STAFF, we need to get their parent USER_ADMIN's limits
      else if (user?.role === 'STAFF') {
        // For now, set a default limit - you might want to fetch from API
        const pairedDisplays = displays.filter(d => d.isPaired).length;
        setDisplayLimits({
          maxDisplays: 10, // Default limit for STAFF
          currentCount: pairedDisplays
        });
      }
    } catch (error) {
      console.error('Failed to fetch display limits:', error);
    }
  };

  const fetchPlaylists = async () => {
    const res = await api.get('/playlists');
    const playlistsData = res.data.playlists ?? [];
    setPlaylists(playlistsData);
    
    // If no playlist is currently selected and we have playlists, select the first one
    if (!selectedPlaylistId && playlistsData.length > 0 && contentType === 'playlist') {
      setSelectedPlaylistId(playlistsData[0].id);
    }
  };

  const fetchLayouts = async () => {
    const res = await api.get('/layouts');
    const layoutsData = res.data.layouts ?? [];
    setLayouts(layoutsData);
    
    // If no layout is currently selected and we have layouts, select the first one
    if (!selectedLayoutId && layoutsData.length > 0 && contentType === 'layout') {
      setSelectedLayoutId(layoutsData[0].id);
    }
  };

  const openEditDialog = (display: Display) => {
    setEditDisplay(display);
    setEditName(display.name ?? '');
    setEditLocation(display.location ?? '');
    setError('');
    setEditOpen(true);
  };

  const saveEdit = async () => {
    if (!editDisplay) return;
    const nameTrim = editName.trim();
    if (!nameTrim) {
      setError('Display name is required');
      return;
    }
    try {
      setEditLoading(true);
      setError('');
      await api.patch(`/displays/${editDisplay.id}`, {
        name: nameTrim,
        location: editLocation.trim() || null,
      });
      setEditOpen(false);
      setEditDisplay(null);
      setEditName('');
      setEditLocation('');
      await fetchDisplays(false);
    } catch (e: any) {
      const msg = e?.response?.data?.error || e?.response?.data?.message || 'Failed to update display';
      setError(msg);
    } finally {
      setEditLoading(false);
    }
  };

  const openAssignDialog = async (display: Display) => {
    try {
      setError('');
      setSelectedDisplay(display);
      
      // Determine content type based on what's assigned
      if (display.layoutId) {
        setContentType('layout');
      } else if (display.playlistId) {
        setContentType('playlist');
      } else {
        setContentType('playlist'); // Default to playlist
      }
      
      // Fetch playlists and layouts first
      await Promise.all([fetchPlaylists(), fetchLayouts()]);
      
      // Set the selected values after fetching data
      if (display.playlistId) {
        setSelectedPlaylistId(display.playlistId);
      } else if (display.layoutId) {
        setSelectedLayoutId(display.layoutId);
      } else {
        // If no content is assigned, select the first available item
        const playlistsRes = await api.get('/playlists');
        const layoutsRes = await api.get('/layouts');
        const playlistsData = playlistsRes.data.playlists ?? [];
        const layoutsData = layoutsRes.data.layouts ?? [];
        
        if (contentType === 'playlist' && playlistsData.length > 0) {
          setSelectedPlaylistId(playlistsData[0].id);
        } else if (contentType === 'layout' && layoutsData.length > 0) {
          setSelectedLayoutId(layoutsData[0].id);
        }
      }
      
      setAssignOpen(true);
    } catch (e: any) {
      console.error('Failed to fetch content:', e);
      setError('Failed to load content. Please try again.');
    }
  };

  const saveAssignment = async () => {
    if (!selectedDisplay) return;
    
    try {
      setAssignLoading(true);
      setError('');
      const updateData: { playlistId?: string | null; layoutId?: string | null } = {};
      
      if (contentType === 'playlist') {
        // Ensure we have a valid playlist ID selected
        if (!selectedPlaylistId || selectedPlaylistId.trim() === '') {
          setError('Please select a playlist');
          return;
        }
        updateData.playlistId = selectedPlaylistId;
        updateData.layoutId = null; // Clear layout when assigning playlist
      } else {
        // Ensure we have a valid layout ID selected
        if (!selectedLayoutId || selectedLayoutId.trim() === '') {
          setError('Please select a layout');
          return;
        }
        updateData.layoutId = selectedLayoutId;
        updateData.playlistId = null; // Clear playlist when assigning layout
      }
      
      console.log('Assigning content to display:', {
        displayId: selectedDisplay.id,
        updateData
      });

      // Start progress tracking - clear any existing permanent status
      const displayId = selectedDisplay.id;
      setPushProgress(prev => ({
        ...prev,
        [displayId]: { progress: 0, isComplete: false, isPermanent: false }
      }));

      // Simulate progress updates
      const progressInterval = setInterval(() => {
        setPushProgress(prev => {
          const current = prev[displayId]?.progress || 0;
          if (current >= 100) {
            clearInterval(progressInterval);
            return {
              ...prev,
              [displayId]: { progress: 100, isComplete: true, isPermanent: false }
            };
          }
          return {
            ...prev,
            [displayId]: { progress: current + 10, isComplete: false, isPermanent: false }
          };
        });
      }, 200);
      
      const response = await api.patch(`/displays/${selectedDisplay.id}`, updateData);
      console.log('Assignment successful:', response.data);

      // Complete the progress and make it permanent
      setTimeout(() => {
        setPushProgress(prev => ({
          ...prev,
          [displayId]: { progress: 100, isComplete: true, isPermanent: true }
        }));
      }, 2000);
      
      setAssignOpen(false);
      setSelectedDisplay(null);
      setSelectedPlaylistId('');
      setSelectedLayoutId('');
      await fetchDisplays(false);
    } catch (e: any) {
      console.error('Failed to assign content:', e);
      const errorMessage = e?.response?.data?.error || e?.response?.data?.message || e?.response?.data?.details || 'Failed to assign content';
      setError(errorMessage);
      
      // Clear progress on error
      if (selectedDisplay) {
        setPushProgress(prev => {
          const newState = { ...prev };
          delete newState[selectedDisplay.id];
          return newState;
        });
      }
    } finally {
      setAssignLoading(false);
    }
  };

  const unassign = async () => {
    if (!selectedDisplay) return;
    try {
      setAssignLoading(true);
      setError('');
      setSuccessMessage('');
      console.log('Unassigning display:', selectedDisplay.id);
      
      const response = await api.patch(`/displays/${selectedDisplay.id}`, {
        playlistId: null,
        layoutId: null,
      });
      
      console.log('Unassign response:', response.data);
      
      // Wait a moment before closing to ensure update completes
      await fetchDisplays(false);
      
      setSuccessMessage('Display unassigned successfully');
      
      // Close dialog after a brief delay to show success message
      setTimeout(() => {
        setAssignOpen(false);
        setSelectedDisplay(null);
        setSelectedPlaylistId('');
        setSelectedLayoutId('');
        setSuccessMessage('');
      }, 1500);
    } catch (e: any) {
      console.error('Failed to unassign content:', e);
      const errorMessage = e?.response?.data?.error || e?.response?.data?.message || e?.response?.data?.details || 'Failed to unassign content';
      setError(errorMessage);
    } finally {
      setAssignLoading(false);
    }
  };

  const handlePairDisplay = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // Validate pairing code (must be 6 digits)
    if (!pairingCode || !/^\d{6}$/.test(pairingCode)) {
      setError('Pairing code must be exactly 6 digits');
      return;
    }

    if (!displayName || displayName.trim().length === 0) {
      setError('Display name is required');
      return;
    }

    try {
      setPairingLoading(true);
      setError(''); // Clear previous errors
      
      const response = await api.post('/displays/pair', {
        pairingCode,
        name: displayName.trim(),
      });

      // Reset form
      setPairingCode('');
      setDisplayName('');
      setIsDialogOpen(false);

      // Auto-refresh the list
      await fetchDisplays(false);
    } catch (error: any) {
      console.error('Failed to pair display:', error);
      
      // Handle different error scenarios
      if (error.response?.status === 403) {
        // SaaS limit reached or permission denied
        setError(
          error.response?.data?.error ||
            'License limit reached. Contact Super Admin.'
        );
      } else if (error.response?.status === 404) {
        // Invalid pairing code
        setError(
          error.response?.data?.error ||
            'Invalid pairing code. Please check and try again.'
        );
      } else if (error.response?.status === 400) {
        // Bad request (missing fields, already paired, etc.)
        setError(
          error.response?.data?.error ||
            error.response?.data?.message ||
            'Invalid request. Please check your input.'
        );
      } else if (error.response?.status === 401) {
        // Unauthorized
        setError('You are not authorized. Please log in again.');
      } else {
        // Network error or other issues
        setError(
          error.response?.data?.error ||
            error.response?.data?.message ||
            error.message ||
            'Failed to pair display. Please check the pairing code and try again.'
        );
      }
    } finally {
      setPairingLoading(false);
    }
  };

  const handleDelete = async (id: string) => {
    const display = displays.find((d) => d.id === id);
    const displayName = display?.name || 'this display';
    const isPaired = display?.isPaired;
    
    const confirmMessage = isPaired
      ? `Are you sure you want to delete "${displayName}"? This display is currently paired and will be disconnected. This action cannot be undone.`
      : `Are you sure you want to delete "${displayName}"? This action cannot be undone.`;

    if (!confirm(confirmMessage)) {
      return;
    }

    try {
      await api.delete(`/displays/${id}`);
      // Refresh the list
      await fetchDisplays(false);
    } catch (error: any) {
      console.error('Failed to delete display:', error);
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || 'Failed to delete display. Please try again.';
      alert(errorMessage);
    }
  };

  const handleBulkDelete = async () => {
    if (selectedDisplayIds.length === 0) return;
    
    const selectedDisplays = displays.filter(d => selectedDisplayIds.includes(d.id));
    const pairedCount = selectedDisplays.filter(d => d.isPaired).length;
    
    let confirmMessage = `Are you sure you want to delete ${selectedDisplayIds.length} displays?`;
    if (pairedCount > 0) {
      confirmMessage += ` ${pairedCount} of these displays are currently paired and will be disconnected.`;
    }
    confirmMessage += ' This action cannot be undone.';

    if (!confirm(confirmMessage)) {
      return;
    }

    try {
      setBulkDeleting(true);
      // Delete displays one by one (you could also create a bulk delete API endpoint)
      for (const displayId of selectedDisplayIds) {
        await api.delete(`/displays/${displayId}`);
      }
      setSelectedDisplayIds([]);
      await fetchDisplays(false);
    } catch (error: any) {
      console.error('Failed to delete displays:', error);
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || 'Failed to delete displays. Please try again.';
      alert(errorMessage);
    } finally {
      setBulkDeleting(false);
    }
  };

  const toggleDisplaySelection = (displayId: string) => {
    setSelectedDisplayIds(prev => 
      prev.includes(displayId) 
        ? prev.filter(id => id !== displayId)
        : [...prev, displayId]
    );
  };

  const selectAllDisplays = () => {
    if (selectedDisplayIds.length === filteredDisplays.length) {
      setSelectedDisplayIds([]);
    } else {
      setSelectedDisplayIds(filteredDisplays.map(d => d.id));
    }
  };

  const getStatusColor = (display: Display) => {
    if (display.status === 'ONLINE') return 'bg-green-500';
    if (display.status === 'OFFLINE') return 'bg-red-500';
    if (display.status === 'PAIRING') return 'bg-yellow-500';
    return 'bg-gray-500';
  };

  const getStatusText = (display: Display) => {
    if (display.status === 'ONLINE') return 'Online';
    if (display.status === 'OFFLINE') return 'Offline';
    if (display.status === 'PAIRING') return 'Pairing';
    return 'Error';
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleString();
  };

  if (!hasAccess) {
    return (
      <DashboardLayout>
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          <p>You do not have access to this page.</p>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-8 pb-8">
        {/* Header Section */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/10 to-orange-500/10 rounded-3xl blur-3xl"></div>
          <div className="relative bg-gradient-to-br from-gray-900 to-black rounded-3xl p-8 border border-gray-800 shadow-2xl">
            <div className="flex items-center justify-between gap-4 flex-wrap">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <Monitor className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Display Management</h1>
                  {refreshing && (
                    <div className="flex items-center gap-2 bg-white/10 px-3 py-1 rounded-xl border border-white/20">
                      <Loader2 className="h-4 w-4 animate-spin text-yellow-400" />
                      <span className="text-sm text-gray-300">Updating...</span>
                    </div>
                  )}
                </div>
                <div className="flex items-center gap-4 flex-wrap">
                  <p className="text-gray-300 text-lg">Manage and monitor your displays</p>
                  {displayLimits && (
                    <div className="flex items-center gap-2">
                      <div className={`rounded-xl px-4 py-2 text-sm font-bold shadow-lg ${
                        displayLimits.currentCount >= displayLimits.maxDisplays 
                          ? 'bg-red-500 text-white' 
                          : displayLimits.currentCount >= displayLimits.maxDisplays * 0.8
                          ? 'bg-yellow-400 text-black'
                          : 'bg-green-500 text-white'
                      }`}>
                        <Activity className="inline h-4 w-4 mr-2" />
                        {displayLimits.currentCount} / {displayLimits.maxDisplays} displays
                      </div>
                      {displayLimits.currentCount >= displayLimits.maxDisplays && (
                        <span className="text-sm text-red-400 font-semibold bg-red-500/20 px-3 py-1 rounded-lg">
                          Limit reached
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-3">
                {selectedDisplayIds.length > 0 && canDelete && (
                  <Button
                    onClick={handleBulkDelete}
                    disabled={bulkDeleting}
                    variant="destructive"
                    className="h-12 gap-2 font-bold shadow-lg"
                  >
                    {bulkDeleting ? <Loader2 className="h-5 w-5 animate-spin" /> : <Trash2 className="h-5 w-5" />}
                    Delete Selected ({selectedDisplayIds.length})
                  </Button>
                )}
                <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                  <DialogTrigger asChild>
                    <Button 
                      disabled={
                        !!displayLimits &&
                        displayLimits.currentCount >= displayLimits.maxDisplays
                      }
                      className="h-12 gap-2 bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold shadow-lg hover:shadow-yellow-500/50 transition-all duration-300 hover:scale-105"
                    >
                      <Plus className="h-5 w-5" />
                      Add Display
                    </Button>
                  </DialogTrigger>
                <DialogContent className="sm:max-w-[425px] bg-white">
                  <DialogHeader>
                    <DialogTitle className="text-2xl">Pair New Display</DialogTitle>
                    <DialogDescription className="text-base">
                      Enter the 6-digit pairing code from your display device.
                    </DialogDescription>
                  </DialogHeader>
                  <form onSubmit={handlePairDisplay}>
                    <div className="grid gap-4 py-4">
                      <div className="space-y-2">
                        <Label htmlFor="pairingCode" className="text-sm font-semibold">Pairing Code</Label>
                        <Input
                          id="pairingCode"
                          placeholder="123456"
                          value={pairingCode}
                          onChange={(e) => {
                            const value = e.target.value.replace(/\D/g, '').slice(0, 6);
                            setPairingCode(value);
                            setError('');
                          }}
                          maxLength={6}
                          required
                          className="h-12 text-center text-2xl font-bold tracking-widest"
                      disabled={pairingLoading}
                    />
                    <p className="text-xs text-gray-500">
                      Enter exactly 6 digits
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="displayName">Display Name</Label>
                    <Input
                      id="displayName"
                      placeholder="Main Lobby Display"
                      value={displayName}
                      onChange={(e) => {
                        setDisplayName(e.target.value);
                        setError('');
                      }}
                      required
                      disabled={pairingLoading}
                    />
                  </div>
                  {error && (
                    <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">
                      {error}
                    </div>
                  )}
                </div>
                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsDialogOpen(false);
                      setPairingCode('');
                      setDisplayName('');
                      setError('');
                    }}
                    disabled={pairingLoading}
                  >
                    Cancel
                  </Button>
                  <Button type="submit" disabled={pairingLoading} className="signomart-primary hover:signomart-primary">
                    {pairingLoading ? (
                      <>
                        <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                        Pairing...
                      </>
                    ) : (
                      'Pair Display'
                    )}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
              </div>
            </div>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative" data-aos="fade-up" data-aos-delay="100">
          <div className="relative">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <Input
              type="text"
              placeholder="Search displays by name, ID, pairing code, or location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-12 h-12 text-base border-gray-300 focus:border-yellow-400 focus:ring-yellow-400"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            )}
          </div>
          {searchQuery && (
            <p className="mt-2 text-sm text-gray-600">
              Found {filteredDisplays.length} display{filteredDisplays.length !== 1 ? 's' : ''} matching "{searchQuery}"
            </p>
          )}
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
            <p className="ml-3 text-gray-600">Loading displays...</p>
          </div>
        ) : filteredDisplays.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
            {searchQuery ? (
              <>
                <p className="text-gray-600">No displays found matching "{searchQuery}"</p>
                <p className="mt-2 text-sm text-gray-500">
                  Try a different search term or clear the search.
                </p>
              </>
            ) : (
              <>
                <p className="text-gray-600">No displays found.</p>
                <p className="mt-2 text-sm text-gray-500">
                  Click Add Display to pair a new device.
                </p>
              </>
            )}
          </div>
        ) : (
          <>
            {/* Floating Action Bar - appears when displays are selected */}
            {selectedDisplayIds.length > 0 && (
              <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-4 shadow-lg" data-aos="fade-down">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-2">
                      <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                        <Trash2 className="h-4 w-4 text-red-600" />
                      </div>
                      <span className="font-semibold text-red-900">
                        {selectedDisplayIds.length} display{selectedDisplayIds.length !== 1 ? 's' : ''} selected
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <Button
                      variant="outline"
                      onClick={() => setSelectedDisplayIds([])}
                      className="border-gray-300 text-gray-700 hover:bg-gray-50"
                    >
                      Cancel
                    </Button>
                    <Button
                      onClick={handleBulkDelete}
                      disabled={bulkDeleting}
                      className="bg-red-600 hover:bg-red-700 text-white font-semibold gap-2"
                    >
                      {bulkDeleting ? (
                        <>
                          <Loader2 className="h-4 w-4 animate-spin" />
                          Deleting...
                        </>
                      ) : (
                        <>
                          <Trash2 className="h-4 w-4" />
                          Delete Selected
                        </>
                      )}
                    </Button>
                  </div>
                </div>
              </div>
            )}
            
            <div className="rounded-2xl border border-gray-200 bg-white shadow-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  {canDelete && (
                    <TableHead className="w-[50px]">
                      <input
                        type="checkbox"
                        checked={selectedDisplayIds.length === filteredDisplays.length && filteredDisplays.length > 0}
                        onChange={selectAllDisplays}
                        className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500"
                      />
                    </TableHead>
                  )}
                  <TableHead className="w-[100px]">Status</TableHead>
                  <TableHead className="min-w-[150px]">Name</TableHead>
                  <TableHead className="min-w-[180px]">Current Content</TableHead>
                  <TableHead className="w-[120px]">Pairing Code</TableHead>
                  <TableHead className="min-w-[120px]">Location</TableHead>
                  <TableHead className="w-[100px]">Push Status</TableHead>
                  <TableHead className="w-[140px]">Last Seen</TableHead>
                  {selectedDisplayIds.length === 0 && (
                    <TableHead className="text-right w-[120px]">Actions</TableHead>
                  )}
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredDisplays.map((display) => (
                  <TableRow key={display.id}>
                    {canDelete && (
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedDisplayIds.includes(display.id)}
                          onChange={() => toggleDisplaySelection(display.id)}
                          className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500"
                        />
                      </TableCell>
                    )}
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div
                          className={`h-3 w-3 rounded-full ${getStatusColor(display)}`}
                        />
                        <span className="text-sm text-gray-600">
                          {getStatusText(display)}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell className="font-medium">
                      {display.name || 'Unnamed Display'}
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-col gap-1">
                        {display.activeSchedule && (
                          <div className="flex items-center gap-2">
                            <div className="h-2 w-2 rounded-full bg-blue-500"></div>
                            <span className="text-sm text-blue-700 font-medium">
                              Schedule: {display.activeSchedule.name}
                            </span>
                            <span className="text-xs text-gray-500">
                              ({display.activeSchedule.contentType}: {display.activeSchedule.contentName})
                            </span>
                          </div>
                        )}
                        {display.layout && !display.activeSchedule && (
                          <span className="text-sm text-gray-700 font-medium">
                            Layout: {display.layout.name}
                          </span>
                        )}
                        {display.playlist && !display.activeSchedule && (
                          <span className="text-sm text-gray-700">
                            Playlist: {display.playlist.name}
                          </span>
                        )}
                        {!display.layout && !display.playlist && !display.activeSchedule && (
                          <span className="text-sm text-gray-500">—</span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <code className="rounded bg-gray-100 px-2 py-1 text-sm">
                        {display.pairingCode || 'N/A'}
                      </code>
                    </TableCell>
                    <TableCell>{display.location || '—'}</TableCell>
                    <TableCell>
                      <div className="flex items-center justify-center">
                        {pushProgress[display.id] && (
                          <>
                            {pushProgress[display.id].isComplete ? (
                              <CheckCircle className="h-5 w-5 text-green-500" />
                            ) : (
                              <CircularProgress progress={pushProgress[display.id].progress} size={20} />
                            )}
                          </>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-sm text-gray-500">
                      {formatDate(display.lastHeartbeat)}
                    </TableCell>
                    {selectedDisplayIds.length === 0 && (
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="icon"
                            onClick={() => openEditDialog(display)}
                            title="Edit display"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          {canAssign && (
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => openAssignDialog(display)}
                              title="Assign Content"
                            >
                              <Calendar className="h-4 w-4" />
                            </Button>
                          )}
                          {canDelete && (
                            <Button
                              variant="outline"
                              size="icon"
                              onClick={() => handleDelete(display.id)}
                            >
                              <Trash2 className="h-4 w-4 text-red-600" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            </div>
          </>
        )}
      </div>

      {/* Edit Display Dialog */}
      <Dialog open={editOpen} onOpenChange={(v) => !editLoading && (setEditOpen(v), !v && setError(''))}>
        <DialogContent
          className="sm:max-w-[425px] bg-white"
          onOpenAutoFocus={(e) => {
            e.preventDefault();
            editNameInputRef.current?.focus();
          }}
        >
          <DialogHeader>
            <DialogTitle>Edit Display</DialogTitle>
            <DialogDescription>
              Update the display name and location.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="editName">Display Name</Label>
              <Input
                ref={editNameInputRef}
                id="editName"
                type="text"
                value={editName}
                onChange={(e) => { setEditName(e.target.value); setError(''); }}
                placeholder="e.g. Main Lobby Display"
                disabled={editLoading}
                readOnly={false}
                autoComplete="off"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="editLocation">Location (optional)</Label>
              <Input
                id="editLocation"
                type="text"
                value={editLocation}
                onChange={(e) => setEditLocation(e.target.value)}
                placeholder="e.g. Building A, Floor 1"
                disabled={editLoading}
                readOnly={false}
                autoComplete="off"
              />
            </div>
            {error && (
              <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">
                {error}
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditOpen(false)} disabled={editLoading}>
              Cancel
            </Button>
            <Button onClick={saveEdit} disabled={editLoading} className="signomart-primary hover:signomart-primary">
              {editLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Saving…
                </>
              ) : (
                'Save'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Assign Content Dialog */}
      <Dialog open={assignOpen} onOpenChange={(v) => !assignLoading && setAssignOpen(v)}>
        <DialogContent className="sm:max-w-[500px] bg-white">
          <DialogHeader>
            <DialogTitle>Assign Content</DialogTitle>
            <DialogDescription>
              Select a playlist or layout to assign to this display.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label>Display</Label>
              <div className="text-sm text-gray-700">
                {selectedDisplay?.name || '—'}
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="contentType">Content Type</Label>
              <select
                id="contentType"
                className="h-10 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                value={contentType}
                onChange={(e) => {
                  setContentType(e.target.value as 'playlist' | 'layout');
                  // Clear selections when switching types
                  if (e.target.value === 'playlist') {
                    setSelectedLayoutId('');
                  } else {
                    setSelectedPlaylistId('');
                  }
                }}
                disabled={assignLoading}
              >
                <option value="playlist">Playlist</option>
                <option value="layout">Layout</option>
              </select>
            </div>

            {contentType === 'playlist' ? (
              <div className="space-y-2">
                <Label htmlFor="playlistSelect">Playlist</Label>
                <select
                  id="playlistSelect"
                  className="h-10 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={selectedPlaylistId}
                  onChange={(e) => setSelectedPlaylistId(e.target.value)}
                  disabled={assignLoading}
                >
                  {playlists.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
                {playlists.length === 0 && (
                  <p className="text-xs text-gray-500">No playlists available. Create one first.</p>
                )}
              </div>
            ) : (
              <div className="space-y-2">
                <Label htmlFor="layoutSelect">Layout</Label>
                <select
                  id="layoutSelect"
                  className="h-10 w-full rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  value={selectedLayoutId}
                  onChange={(e) => setSelectedLayoutId(e.target.value)}
                  disabled={assignLoading}
                >
                  {layouts.map((l) => (
                    <option key={l.id} value={l.id}>
                      {l.name}
                    </option>
                  ))}
                </select>
                {layouts.length === 0 && (
                  <p className="text-xs text-gray-500">No layouts available. Create one first.</p>
                )}
              </div>
            )}

            {error && (
              <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">
                {error}
              </div>
            )}
            
            {successMessage && (
              <div className="rounded-md bg-green-50 p-3 text-sm text-green-800">
                {successMessage}
              </div>
            )}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setAssignOpen(false)}
              disabled={assignLoading}
            >
              Cancel
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={unassign}
              disabled={assignLoading}
            >
              Unassign / Stop
            </Button>
            <Button type="button" onClick={saveAssignment} disabled={assignLoading}>
              {assignLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Saving…
                </>
              ) : (
                'Save'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}
