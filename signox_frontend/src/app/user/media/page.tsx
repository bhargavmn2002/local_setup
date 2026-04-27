'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import api from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent } from '@/components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { StorageIndicator } from '@/components/ui/storage-indicator';
import { cn } from '@/lib/utils';
import { resolvePublicMediaUrl } from '@/lib/mediaUrl';
import { Loader2, Trash2, Upload, Video, Calendar, HardDrive, Maximize2, Monitor, Image as ImageIcon, FileText, Check, Film } from 'lucide-react';
import AOS from 'aos';
import 'aos/dist/aos.css';

type MediaType = 'IMAGE' | 'VIDEO';

type Media = {
  id: string;
  name: string;
  originalName?: string | null;
  filename?: string | null;
  type: MediaType;
  url: string;
  fileSize?: number | null;
  mimeType?: string | null;
  endDate?: string | null;
  createdAt: string;
  thumbnailUrl?: string | null;
  previewUrl?: string | null;
};

type StorageInfo = {
  limitMB: number;
  usedMB: number;
  availableMB: number;
};

function formatBytes(bytes?: number | null) {
  if (!bytes || bytes <= 0) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let n = bytes;
  while (n >= 1024 && i < units.length - 1) {
    n /= 1024;
    i++;
  }
  return `${n.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

export default function MediaLibraryPage() {
  const { user } = useAuth();
  const router = useRouter();
  const publicBaseUrl = (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000/api').replace('/api', '');
  const mediaUrl = (u: string | null | undefined) => resolvePublicMediaUrl(u, publicBaseUrl) ?? '';

  const [media, setMedia] = useState<Media[]>([]);
  const [storageInfo, setStorageInfo] = useState<StorageInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<'all' | 'images' | 'videos'>('all');

  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [showUploadDialog, setShowUploadDialog] = useState(false);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [endDate, setEndDate] = useState<string>('');
  const [orientation, setOrientation] = useState<'LANDSCAPE' | 'PORTRAIT'>('LANDSCAPE');
  const [displayMode, setDisplayMode] = useState<'fit' | 'fill' | 'stretch'>('fit');
  const [deletingIds, setDeletingIds] = useState<Set<string>>(new Set());
  const [currentUploadIndex, setCurrentUploadIndex] = useState(0);
  const [uploadResults, setUploadResults] = useState<{success: number, failed: number}>({success: 0, failed: 0});
  const [isDragOver, setIsDragOver] = useState(false);
  const [selectedMediaIds, setSelectedMediaIds] = useState<string[]>([]);
  const [bulkDeleting, setBulkDeleting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // Role access:
  // USER_ADMIN: full access
  // STAFF CONTENT_MANAGER: full access
  // STAFF DISPLAY_MANAGER: no access
  const hasAccess = useMemo(() => {
    if (!user) return false;
    if (user.role === 'USER_ADMIN') return true;
    if (user.role === 'STAFF' && user.staffRole === 'CONTENT_MANAGER') return true;
    // (Optional) allow Broadcast Manager too, since backend requireContentManagement allows it
    if (user.role === 'STAFF' && user.staffRole === 'BROADCAST_MANAGER') return true;
    return false;
  }, [user]);

  const canWrite = useMemo(() => {
    if (!user) return false;
    if (user.role === 'USER_ADMIN') return true;
    if (user.role === 'STAFF' && (user.staffRole === 'CONTENT_MANAGER' || user.staffRole === 'BROADCAST_MANAGER'))
      return true;
    return false;
  }, [user]);

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    if (!user) return;
    if (user.role === 'STAFF' && user.staffRole === 'DISPLAY_MANAGER') {
      router.replace('/user/dashboard');
      return;
    }
    if (hasAccess) {
      fetchMedia();
    }
  }, [user, hasAccess]);

  async function fetchMedia() {
    try {
      setLoading(true);
      console.log('🔍 [FRONTEND DEBUG] Starting fetchMedia...');
      console.log('🔍 [FRONTEND DEBUG] Current user:', {
        id: user?.id,
        email: user?.email,
        role: user?.role,
        staffRole: user?.staffRole
      });
      
      // Add cache-busting parameter to ensure fresh data
      const timestamp = Date.now();
      const res = await api.get(`/media?_t=${timestamp}`);
      console.log('📊 [FRONTEND DEBUG] Raw API response:', res);
      console.log('📊 [FRONTEND DEBUG] Response status:', res.status);
      console.log('📊 [FRONTEND DEBUG] Response headers:', res.headers);
      console.log('📊 [FRONTEND DEBUG] Response data structure:', {
        hasData: !!res.data,
        dataKeys: res.data ? Object.keys(res.data) : [],
        dataType: typeof res.data
      });
      console.log('📊 [FRONTEND DEBUG] Full response data:', res.data);
      
      // Check if response has the expected structure
      if (res.data) {
        console.log('📁 [FRONTEND DEBUG] Media array details:', {
          hasMediaProperty: 'media' in res.data,
          hasDataProperty: 'data' in res.data,
          mediaValue: res.data.media,
          dataValue: res.data.data,
          mediaType: typeof res.data.media,
          mediaLength: Array.isArray(res.data.media) ? res.data.media.length : 'not array',
          dataType: typeof res.data.data,
          dataLength: Array.isArray(res.data.data) ? res.data.data.length : 'not array'
        });
        
        console.log('💾 [FRONTEND DEBUG] Storage info:', res.data.storageInfo);
      }
      
      // Use the correct property based on pagination service format
      const mediaArray = res.data.data || res.data.media || [];
      console.log('📁 [FRONTEND DEBUG] Final media array:', mediaArray);
      console.log('📁 [FRONTEND DEBUG] Media array length:', mediaArray.length);
      
      if (mediaArray.length > 0) {
        console.log('📁 [FRONTEND DEBUG] First media item:', mediaArray[0]);
        console.log('📁 [FRONTEND DEBUG] Sample media items:', mediaArray.slice(0, 3).map((m: any) => ({     
          id: m.id,
          name: m.name,
          type: m.type,
          url: m.url,
          fileSize: m.fileSize
        })));
      }
      
      setMedia(mediaArray);
      setStorageInfo(res.data.storageInfo || null);
      } catch (e: any) {
     console.error('❌ [FRONTEND DEBUG] Error fetching media:', e);
     console.error('❌ [FRONTEND DEBUG] Error details:', {
       message: e?.message,
        response: e.response?.data,
        status: e.response?.status,
        statusText: e.response?.statusText
      });
      setMedia([]);
      setStorageInfo(null);
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    // Clear selection when tab changes
    setSelectedMediaIds([]);
    
    if (tab === 'images') return media.filter((m) => m.type === 'IMAGE');
    if (tab === 'videos') return media.filter((m) => m.type === 'VIDEO');
    return media;
  }, [media, tab]);

  async function onPickFiles(files: FileList) {
    if (!canWrite) return;

    const validFiles: File[] = [];
    const errors: string[] = [];
    let totalSize = 0;

    // Validate each file
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      
      // Check file type
      const allowed = ['image/jpeg', 'image/png', 'video/mp4'];
      if (!allowed.includes(file.type)) {
        errors.push(`${file.name}: Only JPEG/PNG images and MP4 videos are allowed`);
        continue;
      }

      // Check individual file size (600MB limit)
      const maxFileSizeBytes = 600 * 1024 * 1024;
      if (file.size > maxFileSizeBytes) {
        errors.push(`${file.name}: File too large. Max size is 600MB`);
        continue;
      }

      totalSize += file.size;
      validFiles.push(file);
    }

    // Check total storage limit
    if (storageInfo) {
      const totalSizeMB = totalSize / (1024 * 1024);
      if (totalSizeMB > storageInfo.availableMB) {
        errors.push(`Total file size (${totalSizeMB.toFixed(1)}MB) exceeds available storage (${storageInfo.availableMB.toFixed(1)}MB)`);
      }
    }

    // Show errors if any
    if (errors.length > 0) {
      alert(`Upload errors:\n\n${errors.join('\n')}`);
      if (validFiles.length === 0) return;
    }

    // Set valid files and show dialog
    setSelectedFiles(validFiles);
    setEndDate('');
    setOrientation('LANDSCAPE');
    setDisplayMode('fit');
    setUploadResults({success: 0, failed: 0});
    setCurrentUploadIndex(0);
    setShowUploadDialog(true);
  }

  // Drag and drop handlers
  function handleDragOver(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    if (canWrite && !uploading) {
      setIsDragOver(true);
    }
  }

  function handleDragLeave(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    
    if (!canWrite || uploading) return;
    
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      onPickFiles(files);
    }
  }

  async function onUpload() {
    if (selectedFiles.length === 0 || !canWrite) return;

    console.log('📤 [UPLOAD DEBUG] Starting batch upload process...');
    console.log('📤 [UPLOAD DEBUG] Selected files:', selectedFiles.map(f => ({
      name: f.name,
      size: f.size,
      type: f.type
    })));

    setUploading(true);
    setUploadProgress(0);
    setCurrentUploadIndex(0);
    setUploadResults({success: 0, failed: 0});

    let successCount = 0;
    let failedCount = 0;

    for (let i = 0; i < selectedFiles.length; i++) {
      const file = selectedFiles[i];
      setCurrentUploadIndex(i);
      
      console.log(`📤 [UPLOAD DEBUG] Uploading file ${i + 1}/${selectedFiles.length}: ${file.name}`);

      const form = new FormData();
      form.append('file', file);
      form.append('name', file.name);
      
      if (endDate) {
        form.append('endDate', endDate);
      }
      
      // Add orientation and display mode metadata
      form.append('orientation', orientation);
      form.append('displayMode', displayMode);

      try {
        const response = await api.post('/media', form, {
          timeout: 300000, // 5 minutes for large video files
          onUploadProgress: (evt) => {
            const total = evt.total ?? 0;
            if (!total) return;
            const fileProgress = Math.round((evt.loaded / total) * 100);
            const overallProgress = Math.round(((i + (evt.loaded / total)) / selectedFiles.length) * 100);
            setUploadProgress(overallProgress);
            console.log(`📤 [UPLOAD DEBUG] File ${i + 1} progress: ${fileProgress}%, Overall: ${overallProgress}%`);
          },
        });

        console.log(`✅ [UPLOAD DEBUG] File ${i + 1} uploaded successfully!`);
        successCount++;
        setUploadResults({success: successCount, failed: failedCount});

      } catch (e: any) {
        console.error(`❌ [UPLOAD DEBUG] File ${i + 1} upload failed:`, e);
        failedCount++;
        setUploadResults({success: successCount, failed: failedCount});
        
        // Continue with next file instead of stopping
        console.log(`⏭️ [UPLOAD DEBUG] Continuing with next file...`);
      }
    }

    console.log(`🎉 [UPLOAD DEBUG] Batch upload completed! Success: ${successCount}, Failed: ${failedCount}`);

    // Refresh media list and close dialog
    await fetchMedia();
    
    // Show completion message
    if (failedCount === 0) {
      alert(`✅ All ${successCount} files uploaded successfully!`);
    } else if (successCount === 0) {
      alert(`❌ All ${selectedFiles.length} files failed to upload.`);
    } else {
      alert(`⚠️ Upload completed: ${successCount} successful, ${failedCount} failed.`);
    }

    // Reset state
    setShowUploadDialog(false);
    setSelectedFiles([]);
    setEndDate('');
    setOrientation('LANDSCAPE');
    setDisplayMode('fit');
    setUploading(false);
    setUploadProgress(0);
    setCurrentUploadIndex(0);
    setUploadResults({success: 0, failed: 0});
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  async function onDelete(id: string) {
    if (!canWrite) return;
    if (deletingIds.has(id)) {
      console.log('🔄 [DELETE DEBUG] Delete already in progress for:', id);
      return;
    }
    
    let confirmMessage = 'Are you sure you want to delete this media file?';
    confirmMessage += '\n\nNote: Media files that are currently used in playlists, layouts, or widgets cannot be deleted.';
    confirmMessage += '\n\nThis action cannot be undone.';
    
    if (!confirm(confirmMessage)) return;
    
    console.log('🗑️ [DELETE DEBUG] Starting delete process for media ID:', id);
    
    // Mark as deleting to prevent double-clicks
    setDeletingIds(prev => new Set(prev).add(id));
    
    // Optimistically remove from UI immediately
    const originalMedia = media;
    setMedia(prevMedia => prevMedia.filter(item => item.id !== id));
    
    try {
      console.log('🗑️ [DELETE DEBUG] Sending delete request...');
      const response = await api.delete(`/media/${id}`);
      console.log('✅ [DELETE DEBUG] Delete successful!');
      console.log('✅ [DELETE DEBUG] Response:', response.data);
      
      // Update storage info if provided in response
      if (response.data?.storageInfo) {
        console.log('💾 [DELETE DEBUG] Updating storage info:', response.data.storageInfo);
        setStorageInfo(response.data.storageInfo);
      }

      // Refresh the full list to ensure consistency
      console.log('🔄 [DELETE DEBUG] Refreshing media list');
      await fetchMedia();
    } catch (e: any) {
      console.error('❌ [DELETE DEBUG] Delete failed:', e);
      console.error('❌ [DELETE DEBUG] Error details:', {
        status: e.response?.status,
        statusText: e.response?.statusText,
        data: e.response?.data,
        message: e.message
      });
      
      // Don't show error if it's just "not found" (already deleted)
      if (e.response?.status === 404) {
        console.log('ℹ️ [DELETE DEBUG] Item was already deleted, refreshing list');
        await fetchMedia();
      } else {
        // Restore the original media list on error
        console.log('🔄 [DELETE DEBUG] Restoring original media list due to error');
        setMedia(originalMedia);
        
        // Show more detailed error message
        let errorMessage = 'Delete failed';
        if (e.response?.status === 400) {
          const backendMessage = e.response?.data?.message || e.response?.data?.error || '';
          if (backendMessage.includes('playlists')) {
            errorMessage = 'Cannot delete: This media is currently used in one or more playlists. Remove it from playlists first.';
          } else if (backendMessage.includes('layouts')) {
            errorMessage = 'Cannot delete: This media is currently used in one or more layouts. Remove it from layouts first.';
          } else if (backendMessage.includes('widgets')) {
            errorMessage = 'Cannot delete: This media is currently used in one or more widgets. Remove it from widgets first.';
          } else {
            errorMessage = backendMessage || 'Bad request - the media file may be in use or corrupted';
          }
        } else if (e.response?.status === 403) {
          errorMessage = 'Permission denied - you may not have access to delete this file';
        } else if (e.response?.status === 500) {
          errorMessage = 'Server error - please try again later';
        } else {
          errorMessage = e.response?.data?.message || e.response?.data?.error || e.message || 'Delete failed';
        }
        
        alert(errorMessage);
      }
    } finally {
      // Remove from deleting set
      setDeletingIds(prev => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  }

  async function handleBulkDelete() {
    if (selectedMediaIds.length === 0 || !canWrite) return;
    
    const selectedMedia = media.filter(m => selectedMediaIds.includes(m.id));
    let confirmMessage = `Are you sure you want to delete ${selectedMediaIds.length} media file${selectedMediaIds.length !== 1 ? 's' : ''}?`;
    confirmMessage += '\n\nNote: Media files that are currently used in playlists, layouts, or widgets cannot be deleted and will be skipped.';
    confirmMessage += '\n\nThis action cannot be undone.';

    if (!confirm(confirmMessage)) {
      return;
    }

    try {
      setBulkDeleting(true);
      console.log('🗑️ [BULK DELETE DEBUG] Starting bulk delete for:', selectedMediaIds);
      
      let successCount = 0;
      let failedCount = 0;
      const failedItems: string[] = [];
      
      // Delete media files one by one
      for (const mediaId of selectedMediaIds) {
        try {
          console.log('🗑️ [BULK DELETE DEBUG] Attempting to delete:', mediaId);
          const response = await api.delete(`/media/${mediaId}`);
          console.log('✅ [BULK DELETE DEBUG] Successfully deleted:', mediaId, response.status);
          successCount++;
        } catch (e: any) {
          console.error('❌ [BULK DELETE DEBUG] Failed to delete:', mediaId);
          console.error('❌ [BULK DELETE DEBUG] Error details:', {
            status: e.response?.status,
            statusText: e.response?.statusText,
            data: e.response?.data,
            message: e.message
          });
          
          failedCount++;
          const mediaItem = selectedMedia.find(m => m.id === mediaId);
          const mediaName = mediaItem?.originalName || mediaItem?.name || mediaId;
          
          // Provide specific error messages based on status and response
          let errorReason = 'Unknown error';
          if (e.response?.status === 400) {
            errorReason = e.response?.data?.message || 'Bad request';
            // Common 400 errors from backend:
            if (errorReason.includes('playlists')) {
              errorReason = 'Used in playlists';
            } else if (errorReason.includes('layouts')) {
              errorReason = 'Used in layouts';
            } else if (errorReason.includes('widgets')) {
              errorReason = 'Used in widgets';
            }
          } else if (e.response?.status === 403) {
            errorReason = 'Permission denied';
          } else if (e.response?.status === 404) {
            errorReason = 'File not found';
          } else if (e.response?.status === 500) {
            errorReason = 'Server error';
          } else {
            errorReason = e.response?.data?.message || e.response?.data?.error || e.message || 'Unknown error';
          }
          
          failedItems.push(`${mediaName}: ${errorReason}`);
        }
      }
      
      // Clear selection and refresh
      setSelectedMediaIds([]);
      await fetchMedia();
      
      // Show results
      if (failedCount === 0) {
        console.log('🎉 [BULK DELETE DEBUG] All items deleted successfully');
        alert(`✅ Successfully deleted ${successCount} media file${successCount !== 1 ? 's' : ''}!`);
      } else if (successCount === 0) {
        console.log('❌ [BULK DELETE DEBUG] All deletions failed');
        alert(`❌ Failed to delete all ${failedCount} media files:\n\n${failedItems.join('\n')}\n\nTip: Remove media from playlists, layouts, and widgets before deleting.`);
      } else {
        console.log('⚠️ [BULK DELETE DEBUG] Partial success:', { successCount, failedCount });
        alert(`⚠️ Bulk delete completed with mixed results:\n\n✅ Successfully deleted: ${successCount}\n❌ Failed to delete: ${failedCount}\n\nFailed items:\n${failedItems.join('\n')}\n\nTip: Remove media from playlists, layouts, and widgets before deleting.`);
      }
      
    } catch (error: any) {
      console.error('❌ [BULK DELETE DEBUG] Bulk delete operation failed:', error);
      const errorMessage = error?.response?.data?.error || error?.response?.data?.message || 'Failed to delete media files. Please try again.';
      alert(errorMessage);
    } finally {
      setBulkDeleting(false);
    }
  }

  function toggleMediaSelection(mediaId: string) {
    setSelectedMediaIds(prev => 
      prev.includes(mediaId) 
        ? prev.filter(id => id !== mediaId)
        : [...prev, mediaId]
    );
  }

  function selectAllMedia() {
    if (selectedMediaIds.length === filtered.length) {
      setSelectedMediaIds([]);
    } else {
      setSelectedMediaIds(filtered.map(m => m.id));
    }
  }

  if (!hasAccess) {
    return (
      <DashboardLayout>
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          You do not have access to the Media Library.
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div 
        className="space-y-8 pb-8"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {/* Drag Overlay */}
        {isDragOver && (
          <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center">
            <div className="bg-white rounded-2xl p-8 shadow-2xl border-4 border-dashed border-yellow-400 max-w-md mx-4">
              <div className="text-center">
                <Upload className="h-16 w-16 text-yellow-500 mx-auto mb-4" />
                <h3 className="text-xl font-bold text-gray-900 mb-2">Drop files to upload</h3>
                <p className="text-gray-600">
                  Drop your JPEG/PNG images or MP4 videos here
                </p>
              </div>
            </div>
          </div>
        )}
        {/* Header Section */}
        <div className="relative">
          <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/10 to-orange-500/10 rounded-3xl blur-3xl"></div>
          <div className="relative bg-gradient-to-br from-gray-900 to-black rounded-3xl p-8 border border-gray-800 shadow-2xl">
            <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <Film className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Media Library</h1>
                </div>
                <p className="text-gray-300 text-lg">Upload multiple images and videos at once</p>
              </div>

              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full lg:w-auto">
                {storageInfo && (
                  <Card className="border-white/20 bg-white/10 backdrop-blur-xl shadow-lg">
                    <CardContent className="p-4">
                      <div className="flex items-center gap-2 mb-3">
                        <HardDrive className="h-5 w-5 text-yellow-400" />
                        <span className="text-sm font-bold text-white">Storage Usage</span>
                      </div>
                      <StorageIndicator storageInfo={storageInfo} />
                    </CardContent>
                  </Card>
                )}

                <div className="flex items-center gap-3">
                  {selectedMediaIds.length > 0 && canWrite && (
                    <Button
                      onClick={handleBulkDelete}
                      disabled={bulkDeleting}
                      variant="destructive"
                      className="h-12 gap-2 font-bold shadow-lg"
                    >
                      {bulkDeleting ? <Loader2 className="h-5 w-5 animate-spin" /> : <Trash2 className="h-5 w-5" />}
                      Delete Selected ({selectedMediaIds.length})
                    </Button>
                  )}
                  <input
                    ref={fileInputRef}
                    type="file"
                    className="hidden"
                    accept="image/jpeg,image/png,video/mp4"
                    multiple
                    onChange={(e) => {
                      const files = e.target.files;
                      if (files && files.length > 0) onPickFiles(files);
                    }}
                  />
                  <Button
                    onClick={() => {
                      console.log('🔄 [DEBUG] Force refreshing media list...');
                      fetchMedia();
                    }}
                    variant="outline"
                    className="h-12 gap-2 border-white/20 text-white hover:bg-white/10 font-semibold"
                  >
                    🔄 Refresh
                  </Button>
                  <Button
                    onClick={() => fileInputRef.current?.click()}
                    disabled={!canWrite || uploading || (storageInfo?.availableMB ?? 0) <= 0}
                    className="h-12 gap-2 bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold shadow-lg hover:shadow-yellow-500/50 transition-all duration-300 hover:scale-105"
                  >
                    {uploading ? <Loader2 className="h-5 w-5 animate-spin" /> : <Upload className="h-5 w-5" />}
                    Upload Media Files
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>

        {uploading && (
          <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-lg">
            <div className="flex items-center justify-between text-sm mb-4">
              <span className="text-gray-700 font-bold text-lg">Uploading…</span>
              <span className="text-gray-900 font-black text-2xl">{uploadProgress}%</span>
            </div>
            <div className="h-4 w-full rounded-full bg-gray-200 overflow-hidden shadow-inner">
              <div
                className="h-4 rounded-full bg-gradient-to-r from-yellow-400 to-orange-500 transition-all duration-300 ease-out shadow-lg"
                style={{ width: `${uploadProgress}%` }}
              />
            </div>
          </div>
        )}

        <Tabs value={tab} onValueChange={(v) => setTab(v as any)}>
          <TabsList className="bg-gray-100 p-1">
            <TabsTrigger value="all" className="data-[state=active]:bg-gradient-to-r data-[state=active]:from-yellow-400 data-[state=active]:to-yellow-500 data-[state=active]:text-black font-semibold">
              All Media
              {selectedMediaIds.length > 0 && tab === 'all' && (
                <span className="ml-2 bg-yellow-600 text-white text-xs px-2 py-0.5 rounded-full">
                  {selectedMediaIds.length}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="images" className="data-[state=active]:bg-gradient-to-r data-[state=active]:from-yellow-400 data-[state=active]:to-yellow-500 data-[state=active]:text-black font-semibold">
              Images
              {selectedMediaIds.length > 0 && tab === 'images' && (
                <span className="ml-2 bg-yellow-600 text-white text-xs px-2 py-0.5 rounded-full">
                  {selectedMediaIds.length}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="videos" className="data-[state=active]:bg-gradient-to-r data-[state=active]:from-yellow-400 data-[state=active]:to-yellow-500 data-[state=active]:text-black font-semibold">
              Videos
              {selectedMediaIds.length > 0 && tab === 'videos' && (
                <span className="ml-2 bg-yellow-600 text-white text-xs px-2 py-0.5 rounded-full">
                  {selectedMediaIds.length}
                </span>
              )}
            </TabsTrigger>
          </TabsList>
        </Tabs>

        {/* Floating Action Bar - appears when media items are selected */}
        {selectedMediaIds.length > 0 && (
          <div className="mb-4 rounded-xl border border-red-200 bg-red-50 p-4 shadow-lg" data-aos="fade-down">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                    <Trash2 className="h-4 w-4 text-red-600" />
                  </div>
                  <span className="font-semibold text-red-900">
                    {selectedMediaIds.length} media file{selectedMediaIds.length !== 1 ? 's' : ''} selected
                  </span>
                </div>
              </div>
              <div className="flex items-center gap-3">
                <Button
                  variant="outline"
                  onClick={() => setSelectedMediaIds([])}
                  className="border-gray-300 text-gray-700 hover:bg-gray-50"
                >
                  Cancel
                </Button>
                {canWrite && (
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
                )}
              </div>
            </div>
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
            <p className="ml-3 text-gray-600">Loading media…</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className="rounded-lg border-2 border-dashed border-gray-300 bg-gray-50 p-12 text-center hover:border-yellow-400 hover:bg-yellow-50 transition-all duration-300">
            <div className="space-y-4">
              <Upload className="h-12 w-12 text-gray-400 mx-auto" />
              <div>
                <p className="text-gray-600 font-medium">No media found.</p>
                <p className="mt-2 text-sm text-gray-500">
                  Upload JPEG/PNG images or MP4 videos (max 600MB per file).
                </p>
                <p className="mt-1 text-sm text-gray-500">
                  You can drag and drop files here or click the upload button.
                </p>
                {storageInfo && (
                  <p className="mt-1 text-sm text-gray-500">
                    Available storage: {storageInfo.availableMB.toFixed(1)}MB of {storageInfo.limitMB}MB
                  </p>
                )}
              </div>
              {canWrite && (
                <Button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploading || (storageInfo?.availableMB ?? 0) <= 0}
                  className="bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold"
                >
                  <Upload className="h-4 w-4 mr-2" />
                  Choose Files
                </Button>
              )}
            </div>
          </div>
        ) : (
          <>
            {/* Select All Section */}
            {canWrite && filtered.length > 0 && (
              <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg border border-gray-200">
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={selectedMediaIds.length === filtered.length && filtered.length > 0}
                    onChange={selectAllMedia}
                    className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500 h-4 w-4"
                  />
                  <span className="text-sm font-medium text-gray-700">
                    Select All ({filtered.length} items)
                  </span>
                </div>
                {selectedMediaIds.length > 0 && (
                  <span className="text-sm text-gray-600">
                    {selectedMediaIds.length} selected
                  </span>
                )}
              </div>
            )}
            
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((m) => {
              const title = m.originalName || m.name;
              const isImage = m.type === 'IMAGE';
              const isSelected = selectedMediaIds.includes(m.id);
              return (
                <Card key={m.id} className={cn(
                  "overflow-hidden hover:shadow-2xl transition-all duration-300 hover:scale-105 border-gray-200 group relative cursor-pointer",
                  isSelected && "ring-2 ring-yellow-400 border-yellow-400"
                )}
                onClick={() => {
                  if (canWrite && selectedMediaIds.length > 0) {
                    toggleMediaSelection(m.id);
                  }
                }}
                >
                  <CardContent className="p-0">
                    {/* Selection Checkbox */}
                    {canWrite && (
                      <div className="absolute top-2 left-2 z-20">
                        <div className={cn(
                          "rounded-full p-1 transition-all duration-200",
                          isSelected 
                            ? "bg-yellow-400 shadow-lg" 
                            : "bg-white/90 backdrop-blur-sm shadow-lg hover:bg-yellow-100"
                        )}>
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={(e) => {
                              e.stopPropagation();
                              toggleMediaSelection(m.id);
                            }}
                            className="rounded border-gray-300 text-yellow-600 focus:ring-yellow-500 h-4 w-4"
                          />
                        </div>
                      </div>
                    )}

                    <div className="relative aspect-video bg-gray-900 group">
                      {isImage ? (
                        // Use thumbnail if available, fallback to original image
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={m.thumbnailUrl ? mediaUrl(m.thumbnailUrl) : mediaUrl(m.url)}
                          alt={title}
                          className="h-full w-full object-cover"
                          onError={(e) => {
                            console.warn('🖼️ [IMAGE DEBUG] Failed to load image:', m.thumbnailUrl ? mediaUrl(m.thumbnailUrl) : mediaUrl(m.url));
                            const target = e.target as HTMLImageElement;
                            
                            // If thumbnail failed, try original image
                            if (m.thumbnailUrl && target.src.includes(m.thumbnailUrl)) {
                              target.src = mediaUrl(m.url);
                              return;
                            }
                            
                            // If original also failed, show placeholder
                            target.style.display = 'none';
                            const placeholder = target.parentElement?.querySelector('.image-placeholder');
                            if (placeholder) {
                              (placeholder as HTMLElement).style.display = 'flex';
                            }
                          }}
                        />
                      ) : (
                        // For videos, show thumbnail if available, otherwise show video icon
                        m.thumbnailUrl ? (
                          <div className="relative h-full w-full">
                            {/* eslint-disable-next-line @next/next/no-img-element */}
                            <img
                              src={mediaUrl(m.thumbnailUrl)}
                              alt={title}
                              className="h-full w-full object-cover"
                              onError={(e) => {
                                console.warn('🎬 [VIDEO DEBUG] Failed to load video thumbnail:', mediaUrl(m.thumbnailUrl));
                                const target = e.target as HTMLImageElement;
                                target.style.display = 'none';
                                const placeholder = target.parentElement?.querySelector('.video-placeholder');
                                if (placeholder) {
                                  (placeholder as HTMLElement).style.display = 'flex';
                                }
                              }}
                            />
                            {/* Video play overlay */}
                            <div className="absolute inset-0 flex items-center justify-center bg-black/20">
                              <div className="rounded-full bg-black/60 p-3">
                                <Video className="h-8 w-8 text-white" />
                              </div>
                            </div>
                            {/* Video placeholder for failed thumbnail loads */}
                            <div className="video-placeholder absolute inset-0 hidden items-center justify-center bg-gray-800 text-gray-300">
                              <Video className="h-12 w-12 opacity-70" />
                            </div>
                          </div>
                        ) : (
                          <div className="flex h-full w-full items-center justify-center text-gray-300">
                            <Video className="h-12 w-12 opacity-70" />
                          </div>
                        )
                      )}

                      {/* Image placeholder for failed loads */}
                      {isImage && (
                        <div className="image-placeholder absolute inset-0 hidden items-center justify-center bg-gray-100 text-gray-400">
                          <div className="text-center">
                            <ImageIcon className="h-12 w-12 mx-auto mb-2 opacity-50" />
                            <p className="text-xs">Image not found</p>
                          </div>
                        </div>
                      )}

                      {canWrite && selectedMediaIds.length === 0 && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            e.preventDefault();
                            onDelete(m.id);
                          }}
                          disabled={deletingIds.has(m.id)}
                          className={cn(
                            'absolute right-2 top-2 rounded-md p-2 text-white transition-all shadow-lg z-10',
                            deletingIds.has(m.id) 
                              ? 'bg-gray-400 cursor-not-allowed' 
                              : 'bg-red-600 hover:bg-red-700 active:bg-red-800'
                          )}
                          aria-label="Delete media"
                          title={deletingIds.has(m.id) ? "Deleting..." : "Delete media file"}
                        >
                          {deletingIds.has(m.id) ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Trash2 className="h-4 w-4" />
                          )}
                        </button>
                      )}
                    </div>

                    <div className="space-y-2 p-4 bg-white">
                      <div className="line-clamp-2 text-sm font-medium text-gray-900 min-h-[2.5rem]">
                        {title}
                      </div>
                      <div className="flex items-center justify-between text-xs text-gray-500">
                        <span>{m.type} • {formatBytes(m.fileSize)}</span>
                      </div>
                      {m.endDate && (
                        <div className="flex items-center gap-1.5 text-xs text-orange-600 bg-orange-50 px-2 py-1 rounded">
                          <Calendar className="h-3 w-3" />
                          <span>Expires: {new Date(m.endDate).toLocaleDateString()}</span>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
          </>
        )}
      </div>

      {/* Upload Dialog */}
      <Dialog open={showUploadDialog} onOpenChange={(open) => {
        if (!uploading) {
          setShowUploadDialog(open);
          if (!open) {
            setSelectedFiles([]);
            setEndDate('');
            setOrientation('LANDSCAPE');
            setDisplayMode('fit');
            setUploadResults({success: 0, failed: 0});
            setCurrentUploadIndex(0);
            if (fileInputRef.current) fileInputRef.current.value = '';
          }
        }
      }}>
        <DialogContent className="sm:max-w-3xl max-h-[90vh] overflow-y-auto bg-white">
          <DialogHeader className="border-b pb-4">
            <DialogTitle className="text-2xl font-bold">
              Upload Media ({selectedFiles.length} file{selectedFiles.length !== 1 ? 's' : ''})
            </DialogTitle>
            <p className="text-sm text-gray-500 mt-1">Configure settings for all selected files</p>
          </DialogHeader>
          
          <div className="space-y-6 py-4">
            {/* Files Preview Section */}
            {selectedFiles.length > 0 && (
              <div className="rounded-lg border-2 border-gray-200 bg-gradient-to-br from-gray-50 to-gray-100 p-5">
                <div className="flex items-center gap-2 mb-4">
                  <FileText className="h-5 w-5 text-gray-600" />
                  <span className="font-semibold text-gray-900">
                    Selected Files ({selectedFiles.length})
                  </span>
                </div>
                
                <div className="space-y-3 max-h-60 overflow-y-auto">
                  {selectedFiles.map((file, index) => (
                    <div key={index} className="flex items-start gap-4 p-3 bg-white rounded-lg border">
                      <div className="flex-shrink-0">
                        {file.type.startsWith('image/') ? (
                          <div className="w-12 h-12 rounded-lg bg-blue-100 flex items-center justify-center">
                            <ImageIcon className="h-6 w-6 text-blue-600" />
                          </div>
                        ) : (
                          <div className="w-12 h-12 rounded-lg bg-purple-100 flex items-center justify-center">
                            <Video className="h-6 w-6 text-purple-600" />
                          </div>
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-sm font-semibold text-gray-900 break-words">
                          {file.name}
                        </div>
                        <div className="flex flex-wrap items-center gap-3 text-xs text-gray-600 mt-1">
                          <span className="flex items-center gap-1">
                            <span className="font-medium">Type:</span> {file.type}
                          </span>
                          <span className="flex items-center gap-1">
                            <span className="font-medium">Size:</span> {formatBytes(file.size)}
                          </span>
                        </div>
                      </div>
                      {uploading && currentUploadIndex === index && (
                        <div className="flex-shrink-0">
                          <Loader2 className="h-4 w-4 animate-spin text-blue-600" />
                        </div>
                      )}
                      {uploading && currentUploadIndex > index && (
                        <div className="flex-shrink-0">
                          <Check className="h-4 w-4 text-green-600" />
                        </div>
                      )}
                    </div>
                  ))}
                </div>
                
                {selectedFiles.length > 0 && (
                  <div className="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
                    <div className="text-sm text-blue-800">
                      <span className="font-medium">Total size:</span> {formatBytes(selectedFiles.reduce((sum, file) => sum + file.size, 0))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Display Settings */}
            <div className="space-y-4">
              <div className="border-t pt-4">
                <h3 className="text-sm font-semibold text-gray-900 mb-4 flex items-center gap-2">
                  <Monitor className="h-4 w-4" />
                  Display Settings
                </h3>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Orientation */}
                  <div className="space-y-2">
                    <Label className="text-sm font-medium text-gray-700">Orientation</Label>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => !uploading && setOrientation('LANDSCAPE')}
                        disabled={uploading}
                        className={cn(
                          "flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border-2 transition-all",
                          orientation === 'LANDSCAPE'
                            ? "border-yellow-500 bg-yellow-50 text-yellow-900 font-medium"
                            : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50"
                        )}
                      >
                        {orientation === 'LANDSCAPE' && <Check className="h-4 w-4" />}
                        <span>Landscape</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => !uploading && setOrientation('PORTRAIT')}
                        disabled={uploading}
                        className={cn(
                          "flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg border-2 transition-all",
                          orientation === 'PORTRAIT'
                            ? "border-yellow-500 bg-yellow-50 text-yellow-900 font-medium"
                            : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50"
                        )}
                      >
                        {orientation === 'PORTRAIT' && <Check className="h-4 w-4" />}
                        <span>Portrait</span>
                      </button>
                    </div>
                    <p className="text-xs text-gray-500">
                      Preferred display orientation for this media
                    </p>
                  </div>

                  {/* Display Mode */}
                  <div className="space-y-2">
                    <Label className="text-sm font-medium text-gray-700">Display Mode</Label>
                    <div className="grid grid-cols-3 gap-2">
                      <button
                        type="button"
                        onClick={() => !uploading && setDisplayMode('fit')}
                        disabled={uploading}
                        className={cn(
                          "flex flex-col items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg border-2 transition-all",
                          displayMode === 'fit'
                            ? "border-yellow-500 bg-yellow-50 text-yellow-900 font-medium"
                            : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50"
                        )}
                        title="Fit to Screen - Maintains aspect ratio, fits within screen"
                      >
                        {displayMode === 'fit' && <Check className="h-3 w-3" />}
                        <Maximize2 className="h-4 w-4" />
                        <span className="text-xs">Fit</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => !uploading && setDisplayMode('fill')}
                        disabled={uploading}
                        className={cn(
                          "flex flex-col items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg border-2 transition-all",
                          displayMode === 'fill'
                            ? "border-yellow-500 bg-yellow-50 text-yellow-900 font-medium"
                            : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50"
                        )}
                        title="Fill Screen - Fills entire screen, may crop edges"
                      >
                        {displayMode === 'fill' && <Check className="h-3 w-3" />}
                        <Monitor className="h-4 w-4" />
                        <span className="text-xs">Fill</span>
                      </button>
                      <button
                        type="button"
                        onClick={() => !uploading && setDisplayMode('stretch')}
                        disabled={uploading}
                        className={cn(
                          "flex flex-col items-center justify-center gap-1.5 px-3 py-2.5 rounded-lg border-2 transition-all",
                          displayMode === 'stretch'
                            ? "border-yellow-500 bg-yellow-50 text-yellow-900 font-medium"
                            : "border-gray-200 bg-white text-gray-700 hover:border-gray-300 hover:bg-gray-50"
                        )}
                        title="Stretch to Fit - Stretches to fill screen, may distort"
                      >
                        {displayMode === 'stretch' && <Check className="h-3 w-3" />}
                        <Maximize2 className="h-4 w-4" />
                        <span className="text-xs">Stretch</span>
                      </button>
                    </div>
                    <p className="text-xs text-gray-500">
                      How the media should be displayed on screen
                    </p>
                  </div>
                </div>
              </div>

              {/* Auto-delete Date */}
              <div className="space-y-2 border-t pt-4">
                <Label htmlFor="endDate" className="text-sm font-semibold text-gray-900 flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  Auto-delete Date (Optional)
                </Label>
                <div className="relative">
                  <Input
                    id="endDate"
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    min={new Date(Date.now() + 60000).toISOString().slice(0, 16)}
                    placeholder="Select when to automatically delete this media"
                    disabled={uploading}
                    className="pr-10 h-11"
                  />
                  <Calendar className="absolute right-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
                </div>
                <p className="text-xs text-gray-500 bg-blue-50 p-2 rounded border border-blue-100">
                  💡 Leave empty to keep the media indefinitely. The file will be automatically deleted at the specified date and time.
                </p>
              </div>
            </div>

            {/* Upload Progress */}
            {uploading && (
              <div className="space-y-3 rounded-lg bg-green-50 border border-green-200 p-4">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-gray-700 font-semibold flex items-center gap-2">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Uploading file {currentUploadIndex + 1} of {selectedFiles.length}…
                  </span>
                  <span className="text-gray-700 font-bold">{uploadProgress}%</span>
                </div>
                <div className="h-3 w-full rounded-full bg-green-100 overflow-hidden shadow-inner">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-green-500 to-green-600 transition-all duration-300 ease-out shadow-sm"
                    style={{ width: `${uploadProgress}%` }}
                  />
                </div>
                
                {/* Batch Progress Summary */}
                {(uploadResults.success > 0 || uploadResults.failed > 0) && (
                  <div className="flex items-center justify-between text-xs bg-white rounded-lg p-3 border">
                    <div className="flex items-center gap-4">
                      {uploadResults.success > 0 && (
                        <span className="flex items-center gap-1 text-green-700">
                          <Check className="h-3 w-3" />
                          {uploadResults.success} successful
                        </span>
                      )}
                      {uploadResults.failed > 0 && (
                        <span className="flex items-center gap-1 text-red-700">
                          <span className="w-3 h-3 rounded-full bg-red-500 flex items-center justify-center text-white text-xs">✕</span>
                          {uploadResults.failed} failed
                        </span>
                      )}
                    </div>
                    <span className="text-gray-600 font-medium">
                      {uploadResults.success + uploadResults.failed} / {selectedFiles.length} processed
                    </span>
                  </div>
                )}
              </div>
            )}
          </div>

          <DialogFooter className="border-t pt-4 gap-3">
            <Button
              variant="outline"
              onClick={() => {
                if (!uploading) {
                  setShowUploadDialog(false);
                  setSelectedFiles([]);
                  setEndDate('');
                  setOrientation('LANDSCAPE');
                  setDisplayMode('fit');
                  setUploadResults({success: 0, failed: 0});
                  setCurrentUploadIndex(0);
                  if (fileInputRef.current) fileInputRef.current.value = '';
                }
              }}
              disabled={uploading}
              className="min-w-[100px]"
            >
              Cancel
            </Button>
            <Button 
              onClick={onUpload} 
              disabled={selectedFiles.length === 0 || uploading} 
              className="bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold min-w-[120px]"
            >
              {uploading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Uploading {currentUploadIndex + 1}/{selectedFiles.length}...
                </>
              ) : (
                <>
                  <Upload className="mr-2 h-4 w-4" />
                  Upload {selectedFiles.length} File{selectedFiles.length !== 1 ? 's' : ''}
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}

