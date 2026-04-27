'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import api from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { 
  ArrowLeft, 
  Monitor, 
  Plus, 
  Trash2, 
  Loader2, 
  Play,
  Pause,
  Settings,
  Users
} from 'lucide-react';

type Display = {
  id: string;
  name: string;
  status: string;
  isPaired: boolean;
  lastHeartbeat?: string;
  playlist?: {
    id: string;
    name: string;
  };
  layout?: {
    id: string;
    name: string;
  };
};

type DisplayGroup = {
  id: string;
  name: string;
  description?: string;
  color: string;
  displays: Display[];
  createdBy?: {
    id: string;
    email: string;
    name?: string;
  };
};

type AvailableDisplay = {
  id: string;
  name: string;
  status: string;
  isPaired: boolean;
  pairingCode?: string;
  location?: string;
  tags?: string[];
  lastHeartbeat?: string;
};

type Playlist = {
  id: string;
  name: string;
};

type Layout = {
  id: string;
  name: string;
};

export default function DisplayGroupDetailPage() {
  const { user } = useAuth();
  const router = useRouter();
  const params = useParams();
  const groupId = params.id as string;

  const [displayGroup, setDisplayGroup] = useState<DisplayGroup | null>(null);
  const [availableDisplays, setAvailableDisplays] = useState<AvailableDisplay[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [layouts, setLayouts] = useState<Layout[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  const [selectedDisplays, setSelectedDisplays] = useState<string[]>([]);
  const [selectedGroupDisplays, setSelectedGroupDisplays] = useState<string[]>([]);
  const [selectedPlaylist, setSelectedPlaylist] = useState<string>('');
  const [selectedLayout, setSelectedLayout] = useState<string>('');
  const [assignmentType, setAssignmentType] = useState<'playlist' | 'layout'>('playlist');
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);
  const [assigning, setAssigning] = useState(false);
  const [bulkRemoving, setBulkRemoving] = useState(false);

  // Check access permissions
  const hasAccess = user?.role === 'USER_ADMIN' || user?.role === 'CLIENT_ADMIN' || user?.role === 'SUPER_ADMIN';
  const canWrite = user?.role === 'USER_ADMIN' || user?.role === 'CLIENT_ADMIN' || user?.role === 'SUPER_ADMIN';

  useEffect(() => {
    // Don't redirect if user is still loading
    if (!user) return;

    if (!hasAccess) {
      router.replace('/user/dashboard');
      return;
    }

    fetchDisplayGroup();
    fetchAvailableDisplays();
    fetchPlaylists();
    fetchLayouts();
  }, [user, hasAccess, router, groupId]);

  const fetchDisplayGroup = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/display-groups/${groupId}`);
      setDisplayGroup(response.data.displayGroup);
    } catch (error: any) {
      console.error('Error fetching display group:', error);
      if (error.response?.status === 404) {
        router.replace('/user/display-groups');
      }
    } finally {
      setLoading(false);
    }
  };

  const fetchAvailableDisplays = async () => {
    try {
      const response = await api.get('/displays');
      // Filter out displays that are already in groups
      const allDisplays = response.data.displays || [];
      const unassignedDisplays = allDisplays.filter((display: any) => !display.displayGroup);
      setAvailableDisplays(unassignedDisplays);
    } catch (error) {
      console.error('Error fetching available displays:', error);
    }
  };

  const fetchPlaylists = async () => {
    try {
      const response = await api.get('/playlists');
      setPlaylists(response.data.data || response.data.playlists || []);
    } catch (error) {
      console.error('Error fetching playlists:', error);
    }
  };

  const fetchLayouts = async () => {
    try {
      const response = await api.get('/layouts');
      setLayouts(response.data.data || response.data.layouts || []);
    } catch (error) {
      console.error('Error fetching layouts:', error);
    }
  };

  const handleAddDisplays = async () => {
    if (selectedDisplays.length === 0) return;

    try {
      setAdding(true);
      await api.post(`/display-groups/${groupId}/displays`, {
        displayIds: selectedDisplays
      });
      setShowAddDialog(false);
      setSelectedDisplays([]);
      fetchDisplayGroup();
      fetchAvailableDisplays();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to add displays to group');
    } finally {
      setAdding(false);
    }
  };

  const handleRemoveDisplay = async (displayId: string) => {
    if (!confirm('Are you sure you want to remove this display from the group?')) {
      return;
    }

    try {
      setRemoving(displayId);
      await api.delete(`/display-groups/${groupId}/displays`, {
        data: { displayIds: [displayId] }
      });
      fetchDisplayGroup();
      fetchAvailableDisplays();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to remove display from group');
    } finally {
      setRemoving(null);
    }
  };

  const handleBulkRemoveDisplays = async () => {
    if (selectedGroupDisplays.length === 0) return;
    
    if (!confirm(`Are you sure you want to remove ${selectedGroupDisplays.length} displays from the group?`)) {
      return;
    }

    try {
      setBulkRemoving(true);
      await api.delete(`/display-groups/${groupId}/displays`, {
        data: { displayIds: selectedGroupDisplays }
      });
      setSelectedGroupDisplays([]);
      fetchDisplayGroup();
      fetchAvailableDisplays();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to remove displays from group');
    } finally {
      setBulkRemoving(false);
    }
  };

  const toggleDisplaySelection = (displayId: string) => {
    setSelectedGroupDisplays(prev => 
      prev.includes(displayId) 
        ? prev.filter(id => id !== displayId)
        : [...prev, displayId]
    );
  };

  const selectAllDisplays = () => {
    if (selectedGroupDisplays.length === displayGroup?.displays.length) {
      setSelectedGroupDisplays([]);
    } else {
      setSelectedGroupDisplays(displayGroup?.displays.map(d => d.id) || []);
    }
  };

  const handleAssignContent = async () => {
    if (!displayGroup) return;

    try {
      setAssigning(true);
      
      if (assignmentType === 'playlist') {
        await api.post(`/display-groups/${groupId}/assign-playlist`, {
          playlistId: selectedPlaylist || null
        });
      } else {
        await api.post(`/display-groups/${groupId}/assign-layout`, {
          layoutId: selectedLayout || null
        });
      }

      setShowAssignDialog(false);
      setSelectedPlaylist('');
      setSelectedLayout('');
      fetchDisplayGroup();
      
      const contentType = assignmentType === 'playlist' ? 'Playlist' : 'Layout';
      const contentName = assignmentType === 'playlist' ? selectedPlaylist : selectedLayout;
      alert(`${contentType} ${contentName ? 'assigned to' : 'removed from'} all displays in the group`);
    } catch (error: any) {
      alert(error.response?.data?.message || `Failed to assign ${assignmentType}`);
    } finally {
      setAssigning(false);
    }
  };

  const openAssignDialog = (type: 'playlist' | 'layout') => {
    setAssignmentType(type);
    setSelectedPlaylist('');
    setSelectedLayout('');
    setShowAssignDialog(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ONLINE': return 'text-green-600 bg-green-100';
      case 'OFFLINE': return 'text-red-600 bg-red-100';
      case 'PAIRING': return 'text-yellow-600 bg-yellow-100';
      default: return 'text-gray-600 bg-gray-100';
    }
  };

  const calculateRealTimeStatus = (display: Display) => {
    if (!display.isPaired) return 'PAIRING';
    if (!display.lastHeartbeat) return 'OFFLINE';
    
    const now = new Date();
    const lastHeartbeat = new Date(display.lastHeartbeat);
    const timeDiff = (now.getTime() - lastHeartbeat.getTime()) / 1000;
    
    return timeDiff < 60 ? 'ONLINE' : 'OFFLINE';
  };

  if (!user) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
          <p className="ml-3 text-gray-600">Loading...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (!hasAccess) {
    return (
      <DashboardLayout>
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          You do not have access to Display Groups.
        </div>
      </DashboardLayout>
    );
  }

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
          <p className="ml-3 text-gray-600">Loading display group...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (!displayGroup) {
    return (
      <DashboardLayout>
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          Display group not found.
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="space-y-8 pb-8">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            onClick={() => router.back()}
            className="gap-2"
          >
            <ArrowLeft className="h-4 w-4" />
            Back
          </Button>
          <div className="flex items-center gap-3">
            <div 
              className="w-6 h-6 rounded-full"
              style={{ backgroundColor: displayGroup.color }}
            />
            <div>
              <h1 className="text-3xl font-bold">{displayGroup.name}</h1>
              {displayGroup.description && (
                <p className="text-gray-600">{displayGroup.description}</p>
              )}
            </div>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-6 sm:grid-cols-3">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-gray-600">Total Displays</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-2">
                <Monitor className="h-5 w-5 text-yellow-500" />
                <span className="text-2xl font-bold">{displayGroup.displays.length}</span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-gray-600">Online</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-green-500 rounded-full" />
                <span className="text-2xl font-bold text-green-600">
                  {displayGroup.displays.filter(d => calculateRealTimeStatus(d) === 'ONLINE').length}
                </span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm font-medium text-gray-600">Offline</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-2">
                <div className="w-3 h-3 bg-red-500 rounded-full" />
                <span className="text-2xl font-bold text-red-600">
                  {displayGroup.displays.filter(d => calculateRealTimeStatus(d) === 'OFFLINE').length}
                </span>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Content Assignment Section */}
        {canWrite && displayGroup.displays.length > 0 && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Play className="h-5 w-5" />
                Assign Content to All Displays
              </CardTitle>
              <p className="text-sm text-gray-600 mt-1">
                Assign playlists or layouts to all {displayGroup.displays.length} displays in this group
              </p>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-3">
                <Button
                  onClick={() => openAssignDialog('playlist')}
                  className="gap-2 bg-blue-600 hover:bg-blue-700"
                >
                  <Play className="h-4 w-4" />
                  Assign Playlist
                </Button>
                <Button
                  onClick={() => openAssignDialog('layout')}
                  className="gap-2 bg-purple-600 hover:bg-purple-700"
                >
                  <Settings className="h-4 w-4" />
                  Assign Layout
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {/* Displays Section */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <Users className="h-5 w-5" />
                Displays in Group
                {selectedGroupDisplays.length > 0 && (
                  <Badge variant="outline" className="ml-2">
                    {selectedGroupDisplays.length} selected
                  </Badge>
                )}
              </CardTitle>
              <div className="flex items-center gap-2">
                {selectedGroupDisplays.length > 0 && canWrite && (
                  <Button
                    onClick={handleBulkRemoveDisplays}
                    disabled={bulkRemoving}
                    variant="destructive"
                    size="sm"
                    className="gap-2"
                  >
                    {bulkRemoving ? <Loader2 className="h-4 w-4 animate-spin" /> : <Trash2 className="h-4 w-4" />}
                    Remove Selected ({selectedGroupDisplays.length})
                  </Button>
                )}
                {canWrite && (
                  <Button
                    onClick={() => setShowAddDialog(true)}
                    className="gap-2"
                    disabled={availableDisplays.length === 0}
                  >
                    <Plus className="h-4 w-4" />
                    Add Displays
                  </Button>
                )}
              </div>
            </div>
            {displayGroup && displayGroup.displays.length > 0 && canWrite && (
              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  checked={selectedGroupDisplays.length === displayGroup.displays.length}
                  onChange={selectAllDisplays}
                  className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500"
                />
                <span className="text-sm text-gray-600">
                  Select all displays
                </span>
              </div>
            )}
          </CardHeader>
          <CardContent>
            {displayGroup.displays.length === 0 ? (
              <div className="text-center py-8">
                <Monitor className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-600 mb-2">No displays in this group</p>
                <p className="text-gray-500 text-sm">Add displays to start managing them as a group</p>
              </div>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {displayGroup.displays.map((display) => {
                  const realTimeStatus = calculateRealTimeStatus(display);
                  return (
                    <Card key={display.id} className="border border-gray-200 relative">
                      {canWrite && (
                        <div className="absolute top-3 left-3 z-10">
                          <input
                            type="checkbox"
                            checked={selectedGroupDisplays.includes(display.id)}
                            onChange={() => toggleDisplaySelection(display.id)}
                            className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500"
                            onClick={(e) => e.stopPropagation()}
                          />
                        </div>
                      )}
                      <CardHeader className="pb-3">
                        <div className="flex items-start justify-between">
                          <div className={canWrite ? "ml-6" : ""}>
                            <CardTitle className="text-lg">{display.name}</CardTitle>
                            <Badge className={`mt-1 ${getStatusColor(realTimeStatus)}`}>
                              {realTimeStatus}
                            </Badge>
                          </div>
                          {canWrite && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleRemoveDisplay(display.id)}
                              disabled={removing === display.id || bulkRemoving}
                              className="h-8 w-8 p-0 hover:bg-red-100"
                            >
                              {removing === display.id ? (
                                <Loader2 className="h-4 w-4 animate-spin" />
                              ) : (
                                <Trash2 className="h-4 w-4" />
                              )}
                            </Button>
                          )}
                        </div>
                      </CardHeader>
                      <CardContent className="space-y-2">
                        {display.playlist && (
                          <div className="flex items-center gap-2 text-sm">
                            <Play className="h-4 w-4 text-green-500" />
                            <span>Playlist: {display.playlist.name}</span>
                          </div>
                        )}
                        {display.layout && (
                          <div className="flex items-center gap-2 text-sm">
                            <Settings className="h-4 w-4 text-yellow-500" />
                            <span>Layout: {display.layout.name}</span>
                          </div>
                        )}
                        {!display.playlist && !display.layout && (
                          <div className="flex items-center gap-2 text-sm text-gray-500">
                            <Pause className="h-4 w-4" />
                            <span>No content assigned</span>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Add Displays Dialog */}
        <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
          <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-hidden">
            <DialogHeader>
              <DialogTitle className="text-xl font-bold">Add Displays to Group</DialogTitle>
              <p className="text-sm text-gray-600 mt-1">
                Select displays to add to "{displayGroup?.name}"
              </p>
            </DialogHeader>
            <div className="space-y-4 py-4 overflow-hidden">
              {availableDisplays.length === 0 ? (
                <div className="text-center py-8">
                  <Monitor className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p className="text-gray-600 font-medium">No available displays</p>
                  <p className="text-gray-500 text-sm">All displays are either already in groups or not paired.</p>
                </div>
              ) : (
                <>
                  <div className="flex items-center justify-between">
                    <p className="text-sm text-gray-700 font-medium">
                      Available displays ({availableDisplays.length}):
                    </p>
                    <p className="text-xs text-gray-500">
                      {selectedDisplays.length} selected
                    </p>
                  </div>
                  <div className="space-y-3 max-h-80 overflow-y-auto">
                    {availableDisplays.map((display) => (
                      <label
                        key={display.id}
                        className="flex items-start gap-3 p-4 rounded-lg border-2 hover:bg-gray-50 cursor-pointer transition-all hover:border-yellow-200"
                      >
                        <input
                          type="checkbox"
                          checked={selectedDisplays.includes(display.id)}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedDisplays([...selectedDisplays, display.id]);
                            } else {
                              setSelectedDisplays(selectedDisplays.filter(id => id !== display.id));
                            }
                          }}
                          className="mt-1 rounded border-gray-300 text-yellow-600 focus:ring-yellow-500"
                        />
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between mb-2">
                            <div className="font-semibold text-gray-900 truncate">
                              {display.name || 'Unnamed Display'}
                            </div>
                            <Badge className={`text-xs ${getStatusColor(display.status)}`}>
                              {display.status}
                            </Badge>
                          </div>
                          
                          <div className="space-y-1 text-sm text-gray-600">
                            {display.pairingCode && (
                              <div className="flex items-center gap-2">
                                <Monitor className="h-3 w-3 text-gray-400" />
                                <span className="font-mono bg-gray-100 px-2 py-0.5 rounded text-xs">
                                  Code: {display.pairingCode}
                                </span>
                              </div>
                            )}
                            
                            {display.location && (
                              <div className="flex items-center gap-2">
                                <svg className="h-3 w-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                                </svg>
                                <span className="truncate">{display.location}</span>
                              </div>
                            )}
                            
                            {display.tags && display.tags.length > 0 && (
                              <div className="flex items-center gap-2">
                                <svg className="h-3 w-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                                </svg>
                                <div className="flex flex-wrap gap-1">
                                  {display.tags.slice(0, 2).map((tag, index) => (
                                    <span key={index} className="bg-blue-100 text-blue-800 text-xs px-1.5 py-0.5 rounded">
                                      {tag}
                                    </span>
                                  ))}
                                  {display.tags.length > 2 && (
                                    <span className="text-xs text-gray-500">
                                      +{display.tags.length - 2} more
                                    </span>
                                  )}
                                </div>
                              </div>
                            )}
                            
                            {display.lastHeartbeat && (
                              <div className="flex items-center gap-2 text-xs text-gray-500">
                                <div className={`w-2 h-2 rounded-full ${
                                  display.status === 'ONLINE' ? 'bg-green-400' : 'bg-gray-400'
                                }`} />
                                <span>
                                  Last seen: {new Date(display.lastHeartbeat).toLocaleString()}
                                </span>
                              </div>
                            )}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                </>
              )}
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => {
                  setShowAddDialog(false);
                  setSelectedDisplays([]);
                }}
                disabled={adding}
              >
                Cancel
              </Button>
              <Button
                onClick={handleAddDisplays}
                disabled={adding || selectedDisplays.length === 0}
              >
                {adding ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                Add {selectedDisplays.length} Display{selectedDisplays.length !== 1 ? 's' : ''}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Assign Content Dialog */}
        <Dialog open={showAssignDialog} onOpenChange={setShowAssignDialog}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>
                Assign {assignmentType === 'playlist' ? 'Playlist' : 'Layout'} to Group
              </DialogTitle>
              <p className="text-sm text-gray-600 mt-1">
                Assign a {assignmentType} to all {displayGroup?.displays.length} displays in "{displayGroup?.name}"
              </p>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="content">
                  Select {assignmentType === 'playlist' ? 'Playlist' : 'Layout'}
                </Label>
                {assignmentType === 'playlist' ? (
                  <select
                    id="content"
                    value={selectedPlaylist}
                    onChange={(e) => setSelectedPlaylist(e.target.value)}
                    className="w-full h-10 rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500"
                    disabled={assigning}
                  >
                    <option value="">Remove current playlist</option>
                    {playlists.map((playlist) => (
                      <option key={playlist.id} value={playlist.id}>
                        {playlist.name}
                      </option>
                    ))}
                  </select>
                ) : (
                  <select
                    id="content"
                    value={selectedLayout}
                    onChange={(e) => setSelectedLayout(e.target.value)}
                    className="w-full h-10 rounded-md border border-gray-300 bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-yellow-500"
                    disabled={assigning}
                  >
                    <option value="">Remove current layout</option>
                    {layouts.map((layout) => (
                      <option key={layout.id} value={layout.id}>
                        {layout.name}
                      </option>
                    ))}
                  </select>
                )}
              </div>
              
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                <div className="flex items-start gap-2">
                  <svg className="h-4 w-4 text-yellow-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 15.5c-.77.833.192 2.5 1.732 2.5z" />
                  </svg>
                  <div className="text-sm">
                    <p className="font-medium text-yellow-800">Important Note</p>
                    <p className="text-yellow-700 mt-1">
                      Assigning a {assignmentType} will {assignmentType === 'playlist' ? 'clear any existing layouts' : 'clear any existing playlists'} from all displays in this group.
                    </p>
                  </div>
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setShowAssignDialog(false)}
                disabled={assigning}
              >
                Cancel
              </Button>
              <Button
                onClick={handleAssignContent}
                disabled={assigning}
                className={assignmentType === 'playlist' ? 'bg-blue-600 hover:bg-blue-700' : 'bg-purple-600 hover:bg-purple-700'}
              >
                {assigning ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                {assignmentType === 'playlist' 
                  ? (selectedPlaylist ? 'Assign Playlist' : 'Remove Playlist')
                  : (selectedLayout ? 'Assign Layout' : 'Remove Layout')
                }
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </DashboardLayout>
  );
}