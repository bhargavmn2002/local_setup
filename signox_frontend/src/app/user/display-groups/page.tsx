'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import api from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { 
  Users, 
  Plus, 
  Edit, 
  Trash2, 
  Monitor, 
  Loader2, 
  Settings,
  Play,
  Palette
} from 'lucide-react';
import AOS from 'aos';
import 'aos/dist/aos.css';

type DisplayGroup = {
  id: string;
  name: string;
  description?: string;
  color: string;
  displayCount: number;
  onlineCount: number;
  offlineCount: number;
  createdAt: string;
  createdBy?: {
    id: string;
    email: string;
    name?: string;
  };
};

type Playlist = {
  id: string;
  name: string;
};

type Layout = {
  id: string;
  name: string;
};

const DEFAULT_COLORS = [
  '#F59E0B', // Yellow
  '#3B82F6', // Blue
  '#10B981', // Green
  '#EF4444', // Red
  '#8B5CF6', // Purple
  '#06B6D4', // Cyan
  '#F97316', // Orange
  '#84CC16', // Lime
];

export default function DisplayGroupsPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [displayGroups, setDisplayGroups] = useState<DisplayGroup[]>([]);
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [layouts, setLayouts] = useState<Layout[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showAssignDialog, setShowAssignDialog] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<DisplayGroup | null>(null);
  const [selectedPlaylist, setSelectedPlaylist] = useState<string>('');
  const [selectedLayout, setSelectedLayout] = useState<string>('');
  const [assignmentType, setAssignmentType] = useState<'playlist' | 'layout'>('playlist');
  
  const [form, setForm] = useState({
    name: '',
    description: '',
    color: DEFAULT_COLORS[0]
  });
  
  const [creating, setCreating] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [deleting, setDeleting] = useState<string | null>(null);
  const [assigning, setAssigning] = useState(false);

  // Check access permissions
  const hasAccess = user?.role === 'USER_ADMIN' || user?.role === 'CLIENT_ADMIN' || user?.role === 'SUPER_ADMIN';
  const canWrite = user?.role === 'USER_ADMIN' || user?.role === 'CLIENT_ADMIN' || user?.role === 'SUPER_ADMIN';

  useEffect(() => {
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    // Don't redirect if user is still loading
    if (!user) return;

    if (!hasAccess) {
      router.replace('/user/dashboard');
      return;
    }

    fetchDisplayGroups();
    fetchPlaylists();
    fetchLayouts();
  }, [user, hasAccess, router]);

  const fetchDisplayGroups = async () => {
    try {
      setLoading(true);
      const response = await api.get('/display-groups');
      setDisplayGroups(response.data.displayGroups || []);
    } catch (error) {
      console.error('Error fetching display groups:', error);
    } finally {
      setLoading(false);
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

  const handleCreate = async () => {
    if (!form.name.trim()) return;

    try {
      setCreating(true);
      await api.post('/display-groups', form);
      setShowCreateDialog(false);
      setForm({ name: '', description: '', color: DEFAULT_COLORS[0] });
      fetchDisplayGroups();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to create display group');
    } finally {
      setCreating(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedGroup || !form.name.trim()) return;

    try {
      setUpdating(true);
      await api.patch(`/display-groups/${selectedGroup.id}`, form);
      setShowEditDialog(false);
      setSelectedGroup(null);
      setForm({ name: '', description: '', color: DEFAULT_COLORS[0] });
      fetchDisplayGroups();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to update display group');
    } finally {
      setUpdating(false);
    }
  };

  const handleDelete = async (group: DisplayGroup) => {
    if (!confirm(`Are you sure you want to delete "${group.name}"? Displays will be unassigned from this group.`)) {
      return;
    }

    try {
      setDeleting(group.id);
      await api.delete(`/display-groups/${group.id}`);
      fetchDisplayGroups();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to delete display group');
    } finally {
      setDeleting(null);
    }
  };

  const handleAssignPlaylist = async () => {
    if (!selectedGroup) return;

    try {
      setAssigning(true);
      
      if (assignmentType === 'playlist') {
        await api.post(`/display-groups/${selectedGroup.id}/assign-playlist`, {
          playlistId: selectedPlaylist || null
        });
      } else {
        await api.post(`/display-groups/${selectedGroup.id}/assign-layout`, {
          layoutId: selectedLayout || null
        });
      }
      
      setShowAssignDialog(false);
      setSelectedGroup(null);
      setSelectedPlaylist('');
      setSelectedLayout('');
      
      const contentType = assignmentType === 'playlist' ? 'Playlist' : 'Layout';
      const contentName = assignmentType === 'playlist' ? selectedPlaylist : selectedLayout;
      alert(`${contentType} ${contentName ? 'assigned to' : 'removed from'} all displays in the group`);
    } catch (error: any) {
      alert(error.response?.data?.message || `Failed to assign ${assignmentType}`);
    } finally {
      setAssigning(false);
    }
  };

  const openEditDialog = (group: DisplayGroup) => {
    setSelectedGroup(group);
    setForm({
      name: group.name,
      description: group.description || '',
      color: group.color
    });
    setShowEditDialog(true);
  };

  const openAssignDialog = (group: DisplayGroup, type: 'playlist' | 'layout' = 'playlist') => {
    setSelectedGroup(group);
    setAssignmentType(type);
    setSelectedPlaylist('');
    setSelectedLayout('');
    setShowAssignDialog(true);
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

  return (
    <DashboardLayout>
      <div className="space-y-8 pb-8">
        {/* Header Section */}
        <div className="relative" data-aos="fade-down">
          <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/10 to-orange-500/10 rounded-3xl blur-3xl"></div>
          <div className="relative bg-gradient-to-br from-gray-900 to-black rounded-3xl p-8 border border-gray-800 shadow-2xl">
            <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <Users className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Display Groups</h1>
                </div>
                <p className="text-gray-300 text-lg">Organize displays and manage content in bulk</p>
              </div>

              {canWrite && (
                <Button
                  onClick={() => setShowCreateDialog(true)}
                  className="h-12 gap-2 bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold shadow-lg hover:shadow-yellow-500/50 transition-all duration-300 hover:scale-105"
                >
                  <Plus className="h-5 w-5" />
                  Create Group
                </Button>
              )}
            </div>
          </div>
        </div>

        {/* Groups Grid */}
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
            <p className="ml-3 text-gray-600">Loading display groups...</p>
          </div>
        ) : displayGroups.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center shadow-sm">
            <Users className="h-16 w-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-600 text-lg mb-2 font-medium">No display groups found</p>
            <p className="text-gray-500 text-sm mb-6">Create your first group to organize displays and manage content efficiently</p>
            {canWrite && (
              <Button
                onClick={() => setShowCreateDialog(true)}
                className="gap-2 bg-yellow-600 hover:bg-yellow-700 text-white"
              >
                <Plus className="h-4 w-4" />
                Create First Group
              </Button>
            )}
          </div>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3" data-aos="fade-up">
            {displayGroups.map((group, index) => (
              <Card 
                key={group.id} 
                className="overflow-hidden hover:shadow-2xl transition-all duration-300 hover:scale-105 border-gray-200 group"
                data-aos="fade-up"
                data-aos-delay={index * 100}
              >
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center gap-3">
                      <div 
                        className="w-4 h-4 rounded-full flex-shrink-0"
                        style={{ backgroundColor: group.color }}
                      />
                      <CardTitle className="text-lg font-bold line-clamp-1">
                        {group.name}
                      </CardTitle>
                    </div>
                    {canWrite && (
                      <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => openEditDialog(group)}
                          className="h-8 w-8 p-0 hover:bg-yellow-100"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDelete(group)}
                          disabled={deleting === group.id}
                          className="h-8 w-8 p-0 hover:bg-red-100"
                        >
                          {deleting === group.id ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Trash2 className="h-4 w-4" />
                          )}
                        </Button>
                      </div>
                    )}
                  </div>
                  {group.description && (
                    <p className="text-sm text-gray-600 line-clamp-2 mt-2">
                      {group.description}
                    </p>
                  )}
                </CardHeader>

                <CardContent className="space-y-4">
                  {/* Display Stats */}
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <Monitor className="h-4 w-4 text-gray-500" />
                      <span className="text-sm font-medium">
                        {group.displayCount} Display{group.displayCount !== 1 ? 's' : ''}
                      </span>
                    </div>
                    <div className="flex gap-2">
                      <Badge variant="outline" className="text-green-600 border-green-200">
                        {group.onlineCount} Online
                      </Badge>
                      {group.offlineCount > 0 && (
                        <Badge variant="outline" className="text-red-600 border-red-200">
                          {group.offlineCount} Offline
                        </Badge>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => router.push(`/user/display-groups/${group.id}`)}
                      className="flex-1 gap-2"
                    >
                      <Settings className="h-4 w-4" />
                      Manage
                    </Button>
                    {canWrite && group.displayCount > 0 && (
                      <div className="flex gap-1">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openAssignDialog(group, 'playlist')}
                          className="flex-1 gap-1 text-xs"
                        >
                          <Play className="h-3 w-3" />
                          Playlist
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openAssignDialog(group, 'layout')}
                          className="flex-1 gap-1 text-xs"
                        >
                          <Settings className="h-3 w-3" />
                          Layout
                        </Button>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Create Group Dialog */}
        <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Create Display Group</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="name">Group Name *</Label>
                <Input
                  id="name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Enter group name"
                  disabled={creating}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Optional description"
                  disabled={creating}
                />
              </div>
              <div className="space-y-2">
                <Label>Color</Label>
                <div className="flex gap-2 flex-wrap">
                  {DEFAULT_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setForm({ ...form, color })}
                      className={cn(
                        "w-8 h-8 rounded-full border-2 transition-all",
                        form.color === color ? "border-gray-900 scale-110" : "border-gray-300"
                      )}
                      style={{ backgroundColor: color }}
                      disabled={creating}
                    />
                  ))}
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setShowCreateDialog(false)}
                disabled={creating}
              >
                Cancel
              </Button>
              <Button
                onClick={handleCreate}
                disabled={creating || !form.name.trim()}
              >
                {creating ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                Create Group
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Edit Group Dialog */}
        <Dialog open={showEditDialog} onOpenChange={setShowEditDialog}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Edit Display Group</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="edit-name">Group Name *</Label>
                <Input
                  id="edit-name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Enter group name"
                  disabled={updating}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-description">Description</Label>
                <Input
                  id="edit-description"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Optional description"
                  disabled={updating}
                />
              </div>
              <div className="space-y-2">
                <Label>Color</Label>
                <div className="flex gap-2 flex-wrap">
                  {DEFAULT_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setForm({ ...form, color })}
                      className={cn(
                        "w-8 h-8 rounded-full border-2 transition-all",
                        form.color === color ? "border-gray-900 scale-110" : "border-gray-300"
                      )}
                      style={{ backgroundColor: color }}
                      disabled={updating}
                    />
                  ))}
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setShowEditDialog(false)}
                disabled={updating}
              >
                Cancel
              </Button>
              <Button
                onClick={handleEdit}
                disabled={updating || !form.name.trim()}
              >
                {updating ? <Loader2 className="h-4 w-4 animate-spin mr-2" /> : null}
                Update Group
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
                Assign a {assignmentType} to all {selectedGroup?.displayCount} displays in "{selectedGroup?.name}"
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
                onClick={handleAssignPlaylist}
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