'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import api from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Plus, Trash2, ShieldAlert, Key, Loader2, Users, Edit, Eye, Building2, Phone, Calendar, HardDrive, Upload, UserCheck, Power, UserX } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AOS from 'aos';
import 'aos/dist/aos.css';

export default function ClientUsersPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  // Form State
  const [isOpen, setIsOpen] = useState(false);
  const [formData, setFormData] = useState({ 
    email: '', 
    password: '',
    companyName: '',
    contactNumber: '',
    maxDisplays: '10',
    maxUsers: '5',
    maxStorageMB: '25',
    maxMonthlyUsageMB: '150',
    licenseExpiry: ''
  });

  // Password Reset State
  const [resetDialogOpen, setResetDialogOpen] = useState(false);
  const [resetUserId, setResetUserId] = useState('');
  const [resetUserEmail, setResetUserEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [resetting, setResetting] = useState(false);

  // Suspend/Reactivate State
  const [suspending, setSuspending] = useState(false);

  // Edit User State
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editUserId, setEditUserId] = useState('');
  const [editFormData, setEditFormData] = useState({
    email: '',
    companyName: '',
    contactNumber: '',
    maxDisplays: '10',
    maxUsers: '5',
    maxStorageMB: '25',
    maxMonthlyUsageMB: '150',
    licenseExpiry: '',
    isActive: true
  });

  // User Details View State
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<any>(null);
  const [userStats, setUserStats] = useState<any>(null);
  const [loadingStats, setLoadingStats] = useState(false);

  // 1. Fetch Users on Mount
  const fetchUsers = async () => {
    try {
      setLoading(true);
      // Calls the endpoint we made: GET /api/users
      const res = await api.get('/users');

      // Backend returns { role, users: [...] } for CLIENT_ADMIN
      const usersArray = Array.isArray(res.data)
        ? res.data
        : res.data.users || [];

      setUsers(usersArray);
      setError('');
    } catch (err: any) {
      console.error('Failed to fetch users:', err);
      setError('Failed to load User Admins.');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    if (!authLoading) {
      if (user?.role !== 'CLIENT_ADMIN') {
        router.push('/login'); // Kick out if not Client Admin
      } else {
        fetchUsers();
      }
    }
  }, [user, authLoading, router]);

  // 2. Handle Create User
  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // Calls POST /api/users (Backend handles linking logic)
      await api.post('/users', {
        email: formData.email,
        password: formData.password,
        companyName: formData.companyName,
        contactNumber: formData.contactNumber,
        maxDisplays: Number(formData.maxDisplays || 10),
        maxUsers: Number(formData.maxUsers || 5),
        maxStorageMB: Number(formData.maxStorageMB || 25),
        maxMonthlyUsageMB: Number(formData.maxMonthlyUsageMB || 150),
        licenseExpiry: formData.licenseExpiry || null,
      });
      
      setIsOpen(false);
      setFormData({ 
        email: '', 
        password: '',
        companyName: '',
        contactNumber: '',
        maxDisplays: '10',
        maxUsers: '5',
        maxStorageMB: '25',
        maxMonthlyUsageMB: '150',
        licenseExpiry: ''
      });
      fetchUsers(); // Refresh list
    } catch (err: any) {
      alert(err.response?.data?.error || 'Failed to create user');
    }
  };

  // 3. Handle Delete (Soft Delete/Suspend)
  const handleDelete = async (id: string) => {
    if (confirm('Are you sure you want to permanently delete this User Admin? This cannot be undone.')) {
      try {
        await api.delete(`/users/${id}`);
        fetchUsers();
      } catch (err: any) {
        const msg =
          err?.response?.data?.message ||
          err?.response?.data?.error ||
          err?.message ||
          'Failed to delete user';
        alert(msg);
      }
    }
  };

  // 4. Handle Password Reset
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPassword) {
      alert('Password is required');
      return;
    }

    try {
      setResetting(true);
      await api.put(`/users/${resetUserId}/reset-password`, {
        newPassword: newPassword
      });
      
      setResetDialogOpen(false);
      setResetUserId('');
      setResetUserEmail('');
      setNewPassword('');
      alert('Password reset successfully');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to reset password');
    } finally {
      setResetting(false);
    }
  };

  const openResetDialog = (userId: string, userEmail: string) => {
    setResetUserId(userId);
    setResetUserEmail(userEmail);
    setNewPassword('');
    setResetDialogOpen(true);
  };

  // 5. Handle Edit User
  const handleEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      setResetting(true); // Reuse loading state
      await api.put(`/users/${editUserId}`, {
        email: editFormData.email,
        companyName: editFormData.companyName,
        contactNumber: editFormData.contactNumber,
        maxDisplays: Number(editFormData.maxDisplays || 10),
        maxUsers: Number(editFormData.maxUsers || 5),
        maxStorageMB: Number(editFormData.maxStorageMB || 25),
        maxMonthlyUsageMB: Number(editFormData.maxMonthlyUsageMB || 150),
        licenseExpiry: editFormData.licenseExpiry || null,
        isActive: editFormData.isActive
      });
      
      setEditDialogOpen(false);
      setEditUserId('');
      setEditFormData({
        email: '',
        companyName: '',
        contactNumber: '',
        maxDisplays: '10',
        maxUsers: '5',
        maxStorageMB: '25',
        maxMonthlyUsageMB: '150',
        licenseExpiry: '',
        isActive: true
      });
      fetchUsers(); // Refresh list
      alert('User updated successfully');
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to update user');
    } finally {
      setResetting(false);
    }
  };

  const openEditDialog = (user: any) => {
    setEditUserId(user.id);
    setEditFormData({
      email: user.email,
      companyName: user.userAdminProfile?.companyName || '',
      contactNumber: user.userAdminProfile?.contactNumber || '',
      maxDisplays: String(user.userAdminProfile?.maxDisplays || 10),
      maxUsers: String(user.userAdminProfile?.maxUsers || 5),
      maxStorageMB: String(user.userAdminProfile?.maxStorageMB || 25),
      maxMonthlyUsageMB: String(user.userAdminProfile?.maxMonthlyUsageMB || 150),
      licenseExpiry: user.userAdminProfile?.licenseExpiry ? 
        new Date(user.userAdminProfile.licenseExpiry).toISOString().split('T')[0] : '',
      isActive: user.isActive
    });
    setEditDialogOpen(true);
  };

  // Open User Details Dialog
  const openDetailsDialog = async (user: any) => {
    setSelectedUser(user);
    setDetailsDialogOpen(true);
    setLoadingStats(true);
    
    try {
      // Fetch user statistics (displays, staff count, storage usage)
      const [displaysRes, staffRes] = await Promise.all([
        api.get(`/displays?managedByUserId=${user.id}`),
        api.get(`/users?createdByUserAdminId=${user.id}`)
      ]);
      
      const displays = Array.isArray(displaysRes.data) ? displaysRes.data : displaysRes.data.displays || [];
      const staff = Array.isArray(staffRes.data) ? staffRes.data : staffRes.data.users || [];
      
      setUserStats({
        displayCount: displays.length,
        staffCount: staff.length,
        // Storage calculation would need to be implemented based on your media storage logic
        storageUsedMB: 0, // TODO: Calculate actual storage usage
      });
    } catch (error) {
      console.error('Failed to fetch user stats:', error);
      setUserStats({
        displayCount: 0,
        staffCount: 0,
        storageUsedMB: 0,
      });
    } finally {
      setLoadingStats(false);
    }
  };

  // Handle Suspend/Reactivate User Admin
  const handleSuspendToggle = async (userId: string, currentStatus: boolean) => {
    const action = currentStatus ? 'suspend' : 'reactivate';
    const confirmMessage = currentStatus 
      ? 'Are you sure you want to suspend this User Admin? This will also suspend all their staff members and unassign their displays.'
      : 'Are you sure you want to reactivate this User Admin? This will also reactivate all their staff members.';

    if (confirm(confirmMessage)) {
      try {
        setSuspending(true);
        await api.patch(`/users/${userId}/suspend`);
        fetchUsers(); // Refresh the list
      } catch (err: any) {
        alert(err.response?.data?.message || `Failed to ${action} user`);
      } finally {
        setSuspending(false);
      }
    }
  };

  if (authLoading || loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
          <p className="ml-3 text-gray-600">Loading users...</p>
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
            <div className="flex items-center justify-between gap-4 flex-wrap">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <Users className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">User Admins</h1>
                </div>
                <p className="text-gray-300 text-lg">
                  Manage the operational managers for your organization.
                </p>
              </div>
              
              {/* CREATE USER MODAL */}
              <Dialog open={isOpen} onOpenChange={setIsOpen}>
                <DialogTrigger asChild>
                  <Button className="h-12 gap-2 bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold shadow-lg hover:shadow-yellow-500/50 transition-all duration-300 hover:scale-105">
                    <Plus className="h-5 w-5" /> Add User Admin
                  </Button>
                </DialogTrigger>
                <DialogContent className="bg-white max-w-2xl">
                  <DialogHeader>
                    <DialogTitle className="text-2xl">Create New User Admin</DialogTitle>
                  </DialogHeader>
                  <form onSubmit={handleCreate} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">Email Address</Label>
                        <Input 
                          required 
                          type="email" 
                          placeholder="manager@branch.com"
                          value={formData.email}
                          onChange={e => setFormData({...formData, email: e.target.value})}
                          className="h-12"
                        />
                      </div>
                      <div className="space-y-2">
                        <Label className="text-sm font-semibold">Password</Label>
                        <Input 
                          required 
                          type="password" 
                          value={formData.password}
                          onChange={e => setFormData({...formData, password: e.target.value})}
                          className="h-12"
                        />
                      </div>
                    </div>

                    <div className="border-t pt-4">
                      <h3 className="text-lg font-semibold mb-3">Company Information</h3>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Company Name</Label>
                          <Input 
                            required
                            type="text" 
                            placeholder="Acme Corporation"
                            value={formData.companyName}
                            onChange={e => setFormData({...formData, companyName: e.target.value})}
                            className="h-12"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Contact Number</Label>
                          <Input 
                            type="tel" 
                            placeholder="+1 (555) 123-4567"
                            value={formData.contactNumber}
                            onChange={e => setFormData({...formData, contactNumber: e.target.value})}
                            className="h-12"
                          />
                        </div>
                      </div>
                    </div>

                    <div className="border-t pt-4">
                      <h3 className="text-lg font-semibold mb-3">Resource Limits</h3>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Max Displays</Label>
                          <Input 
                            type="number"
                            min="0"
                            value={formData.maxDisplays}
                            onChange={e => setFormData({...formData, maxDisplays: e.target.value})}
                            className="h-12"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Max Staff Users</Label>
                          <Input 
                            type="number"
                            min="0"
                            value={formData.maxUsers}
                            onChange={e => setFormData({...formData, maxUsers: e.target.value})}
                            className="h-12"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Storage Limit (MB)</Label>
                          <Input 
                            type="number"
                            min="0"
                            value={formData.maxStorageMB}
                            onChange={e => setFormData({...formData, maxStorageMB: e.target.value})}
                            className="h-12"
                          />
                        </div>
                        <div className="space-y-2">
                          <Label className="text-sm font-semibold">Monthly Upload Limit (MB)</Label>
                          <Input 
                            type="number"
                            min="0"
                            value={formData.maxMonthlyUsageMB}
                            onChange={e => setFormData({...formData, maxMonthlyUsageMB: e.target.value})}
                            className="h-12"
                          />
                        </div>
                      </div>
                    </div>

                    <div className="space-y-2">
                      <Label className="text-sm font-semibold">License Expiry (Optional)</Label>
                      <Input 
                        type="date"
                        value={formData.licenseExpiry}
                        onChange={e => setFormData({...formData, licenseExpiry: e.target.value})}
                        className="h-12"
                      />
                    </div>

                    <Button type="submit" className="w-full signomart-primary hover:signomart-primary">
                      Create User Admin
                    </Button>
                  </form>
                </DialogContent>
          </Dialog>
            </div>
          </div>
        </div>

        {/* PASSWORD RESET MODAL */}
        <Dialog open={resetDialogOpen} onOpenChange={setResetDialogOpen}>
          <DialogContent className="bg-white">
            <DialogHeader>
              <DialogTitle>Reset Password</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleResetPassword} className="space-y-4">
              <div className="space-y-2">
                <Label>User Email</Label>
                <Input 
                  value={resetUserEmail}
                  disabled
                  className="bg-gray-50"
                />
              </div>
              <div className="space-y-2">
                <Label>New Password</Label>
                <Input 
                  required 
                  type="password" 
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  disabled={resetting}
                />
              </div>
              <div className="flex gap-2">
                <Button 
                  type="button" 
                  variant="outline" 
                  onClick={() => setResetDialogOpen(false)}
                  disabled={resetting}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={resetting} className="flex-1">
                  {resetting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Resetting...
                    </>
                  ) : (
                    'Reset Password'
                  )}
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>

        {/* EDIT USER MODAL */}
        <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
          <DialogContent className="bg-white max-w-2xl">
            <DialogHeader>
              <DialogTitle>Edit User Admin</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleEditUser} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Email</Label>
                  <Input 
                    required 
                    type="email" 
                    placeholder="user@example.com"
                    value={editFormData.email}
                    onChange={e => setEditFormData({...editFormData, email: e.target.value})}
                    disabled={resetting}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Company Name</Label>
                  <Input 
                    required 
                    type="text" 
                    placeholder="Company Name"
                    value={editFormData.companyName}
                    onChange={e => setEditFormData({...editFormData, companyName: e.target.value})}
                    disabled={resetting}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Contact Number</Label>
                  <Input 
                    type="tel" 
                    placeholder="+1 (555) 123-4567"
                    value={editFormData.contactNumber}
                    onChange={e => setEditFormData({...editFormData, contactNumber: e.target.value})}
                    disabled={resetting}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Max Displays</Label>
                  <Input 
                    required 
                    type="number" 
                    min="1"
                    placeholder="10"
                    value={editFormData.maxDisplays}
                    onChange={e => setEditFormData({...editFormData, maxDisplays: e.target.value})}
                    disabled={resetting}
                  />
                </div>
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Max Users (Staff)</Label>
                  <Input 
                    required 
                    type="number" 
                    min="1"
                    placeholder="5"
                    value={editFormData.maxUsers}
                    onChange={e => setEditFormData({...editFormData, maxUsers: e.target.value})}
                    disabled={resetting}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Storage Limit (MB)</Label>
                  <Input 
                    required 
                    type="number" 
                    min="1"
                    placeholder="25"
                    value={editFormData.maxStorageMB}
                    onChange={e => setEditFormData({...editFormData, maxStorageMB: e.target.value})}
                    disabled={resetting}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Monthly Usage Limit (MB)</Label>
                  <Input 
                    required 
                    type="number" 
                    min="1"
                    placeholder="150"
                    value={editFormData.maxMonthlyUsageMB}
                    onChange={e => setEditFormData({...editFormData, maxMonthlyUsageMB: e.target.value})}
                    disabled={resetting}
                  />
                </div>
                <div className="space-y-2">
                  <Label>License Expiry (Optional)</Label>
                  <Input 
                    type="date"
                    value={editFormData.licenseExpiry}
                    onChange={e => setEditFormData({...editFormData, licenseExpiry: e.target.value})}
                    disabled={resetting}
                  />
                </div>
              </div>

              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="isActive"
                    checked={editFormData.isActive}
                    onChange={e => setEditFormData({...editFormData, isActive: e.target.checked})}
                    disabled={resetting}
                    className="rounded border-gray-300"
                  />
                  <Label htmlFor="isActive">Account Active</Label>
                </div>
              </div>

              <div className="flex gap-2">
                <Button 
                  type="button" 
                  variant="outline" 
                  onClick={() => setEditDialogOpen(false)}
                  disabled={resetting}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" disabled={resetting} className="flex-1">
                  {resetting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Updating...
                    </>
                  ) : (
                    'Update User Admin'
                  )}
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>

        {/* USER DETAILS MODAL */}
        <Dialog open={detailsDialogOpen} onOpenChange={setDetailsDialogOpen}>
          <DialogContent className="bg-white max-w-4xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="text-2xl flex items-center gap-3">
                <Eye className="h-6 w-6 text-blue-500" />
                User Admin Details
              </DialogTitle>
            </DialogHeader>
            
            {selectedUser && (
              <div className="space-y-6">
                {/* Basic Information */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-lg flex items-center gap-2">
                        <Users className="h-5 w-5 text-blue-500" />
                        Basic Information
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">Email</Label>
                        <p className="text-base font-medium">{selectedUser.email}</p>
                      </div>
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">Status</Label>
                        <div className="mt-1">
                          <Badge className={selectedUser.isActive ? "bg-green-500" : "bg-red-500"}>
                            {selectedUser.isActive ? 'Active' : 'Inactive'}
                          </Badge>
                        </div>
                      </div>
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">Created</Label>
                        <p className="text-base">
                          {selectedUser.createdAt ? new Date(selectedUser.createdAt).toLocaleDateString() : 'Unknown'}
                        </p>
                      </div>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-lg flex items-center gap-2">
                        <Building2 className="h-5 w-5 text-purple-500" />
                        Company Information
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">Company Name</Label>
                        <p className="text-base font-medium">
                          {selectedUser.userAdminProfile?.companyName || 'Not specified'}
                        </p>
                      </div>
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">Contact Number</Label>
                        <p className="text-base flex items-center gap-2">
                          <Phone className="h-4 w-4 text-gray-400" />
                          {selectedUser.userAdminProfile?.contactNumber || 'Not specified'}
                        </p>
                      </div>
                      <div>
                        <Label className="text-sm font-semibold text-gray-600">License Expiry</Label>
                        <p className="text-base flex items-center gap-2">
                          <Calendar className="h-4 w-4 text-gray-400" />
                          {selectedUser.userAdminProfile?.licenseExpiry 
                            ? new Date(selectedUser.userAdminProfile.licenseExpiry).toLocaleDateString()
                            : 'No expiry set'
                          }
                        </p>
                      </div>
                    </CardContent>
                  </Card>
                </div>

                {/* Resource Limits and Usage */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-lg flex items-center gap-2">
                        <HardDrive className="h-5 w-5 text-green-500" />
                        Resource Limits
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-blue-50 p-3 rounded-lg">
                          <Label className="text-xs font-semibold text-blue-600 uppercase">Max Displays</Label>
                          <p className="text-2xl font-bold text-blue-700">
                            {selectedUser.userAdminProfile?.maxDisplays || 0}
                          </p>
                        </div>
                        <div className="bg-green-50 p-3 rounded-lg">
                          <Label className="text-xs font-semibold text-green-600 uppercase">Max Staff</Label>
                          <p className="text-2xl font-bold text-green-700">
                            {selectedUser.userAdminProfile?.maxUsers || 0}
                          </p>
                        </div>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div className="bg-purple-50 p-3 rounded-lg">
                          <Label className="text-xs font-semibold text-purple-600 uppercase">Storage Limit</Label>
                          <p className="text-2xl font-bold text-purple-700">
                            {selectedUser.userAdminProfile?.maxStorageMB || 0} MB
                          </p>
                        </div>
                        <div className="bg-orange-50 p-3 rounded-lg">
                          <Label className="text-xs font-semibold text-orange-600 uppercase">Monthly Limit</Label>
                          <p className="text-2xl font-bold text-orange-700">
                            {selectedUser.userAdminProfile?.maxMonthlyUsageMB || 0} MB
                          </p>
                        </div>
                      </div>
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader className="pb-3">
                      <CardTitle className="text-lg flex items-center gap-2">
                        <UserCheck className="h-5 w-5 text-indigo-500" />
                        Current Usage
                      </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      {loadingStats ? (
                        <div className="flex items-center justify-center py-8">
                          <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                          <span className="ml-2 text-gray-600">Loading usage data...</span>
                        </div>
                      ) : (
                        <>
                          <div className="grid grid-cols-2 gap-4">
                            <div className="bg-blue-50 p-3 rounded-lg">
                              <Label className="text-xs font-semibold text-blue-600 uppercase">Active Displays</Label>
                              <p className="text-2xl font-bold text-blue-700">
                                {userStats?.displayCount || 0}
                              </p>
                              <div className="text-xs text-blue-600 mt-1">
                                of {selectedUser.userAdminProfile?.maxDisplays || 0} allowed
                              </div>
                            </div>
                            <div className="bg-green-50 p-3 rounded-lg">
                              <Label className="text-xs font-semibold text-green-600 uppercase">Staff Users</Label>
                              <p className="text-2xl font-bold text-green-700">
                                {userStats?.staffCount || 0}
                              </p>
                              <div className="text-xs text-green-600 mt-1">
                                of {selectedUser.userAdminProfile?.maxUsers || 0} allowed
                              </div>
                            </div>
                          </div>
                          <div className="grid grid-cols-2 gap-4">
                            <div className="bg-purple-50 p-3 rounded-lg">
                              <Label className="text-xs font-semibold text-purple-600 uppercase">Storage Used</Label>
                              <p className="text-2xl font-bold text-purple-700">
                                {userStats?.storageUsedMB || 0} MB
                              </p>
                              <div className="text-xs text-purple-600 mt-1">
                                of {selectedUser.userAdminProfile?.maxStorageMB || 0} MB
                              </div>
                            </div>
                            <div className="bg-orange-50 p-3 rounded-lg">
                              <Label className="text-xs font-semibold text-orange-600 uppercase">Monthly Upload</Label>
                              <p className="text-2xl font-bold text-orange-700">
                                {Math.round((selectedUser.userAdminProfile?.monthlyUploadedBytes || 0) / (1024 * 1024))} MB
                              </p>
                              <div className="text-xs text-orange-600 mt-1">
                                of {selectedUser.userAdminProfile?.maxMonthlyUsageMB || 0} MB
                              </div>
                            </div>
                          </div>
                        </>
                      )}
                    </CardContent>
                  </Card>
                </div>

                {/* Usage Progress Bars */}
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Upload className="h-5 w-5 text-yellow-500" />
                      Usage Overview
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {!loadingStats && userStats && (
                      <>
                        {/* Displays Usage */}
                        <div>
                          <div className="flex justify-between items-center mb-2">
                            <Label className="text-sm font-semibold">Display Usage</Label>
                            <span className="text-sm text-gray-600">
                              {userStats.displayCount} / {selectedUser.userAdminProfile?.maxDisplays || 0}
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-blue-500 h-2 rounded-full transition-all duration-300"
                              style={{ 
                                width: `${Math.min(100, (userStats.displayCount / (selectedUser.userAdminProfile?.maxDisplays || 1)) * 100)}%` 
                              }}
                            ></div>
                          </div>
                        </div>

                        {/* Staff Usage */}
                        <div>
                          <div className="flex justify-between items-center mb-2">
                            <Label className="text-sm font-semibold">Staff Usage</Label>
                            <span className="text-sm text-gray-600">
                              {userStats.staffCount} / {selectedUser.userAdminProfile?.maxUsers || 0}
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-green-500 h-2 rounded-full transition-all duration-300"
                              style={{ 
                                width: `${Math.min(100, (userStats.staffCount / (selectedUser.userAdminProfile?.maxUsers || 1)) * 100)}%` 
                              }}
                            ></div>
                          </div>
                        </div>

                        {/* Monthly Upload Usage */}
                        <div>
                          <div className="flex justify-between items-center mb-2">
                            <Label className="text-sm font-semibold">Monthly Upload Usage</Label>
                            <span className="text-sm text-gray-600">
                              {Math.round((selectedUser.userAdminProfile?.monthlyUploadedBytes || 0) / (1024 * 1024))} MB / {selectedUser.userAdminProfile?.maxMonthlyUsageMB || 0} MB
                            </span>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-2">
                            <div 
                              className="bg-orange-500 h-2 rounded-full transition-all duration-300"
                              style={{ 
                                width: `${Math.min(100, ((selectedUser.userAdminProfile?.monthlyUploadedBytes || 0) / (1024 * 1024)) / (selectedUser.userAdminProfile?.maxMonthlyUsageMB || 1) * 100)}%` 
                              }}
                            ></div>
                          </div>
                        </div>
                      </>
                    )}
                  </CardContent>
                </Card>

                <div className="flex justify-end">
                  <Button 
                    onClick={() => setDetailsDialogOpen(false)}
                    className="bg-gray-500 hover:bg-gray-600"
                  >
                    Close
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>

        {error && (
          <div className="bg-red-50 text-red-600 p-4 rounded-md flex items-center">
            <ShieldAlert className="mr-2 h-5 w-5" />
            {error}
          </div>
        )}

        {/* USERS TABLE */}
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>User (Email)</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Limits</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center py-8 text-gray-500">
                      No users found. Create one to get started.
                    </TableCell>
                  </TableRow>
                ) : (
                  users.map((u) => (
                    <TableRow key={u.id}>
                      <TableCell className="font-medium">
                        <button
                          onClick={() => openDetailsDialog(u)}
                          className="text-blue-600 hover:text-blue-800 hover:underline cursor-pointer"
                        >
                          {u.email}
                        </button>
                      </TableCell>
                      <TableCell><Badge variant="outline">User Admin</Badge></TableCell>
                      <TableCell>
                        {u.userAdminProfile ? (
                          <div className="text-sm text-gray-600">
                            <div>Displays: {u.userAdminProfile.maxDisplays}</div>
                            <div>Storage: {u.userAdminProfile.maxStorageMB}MB</div>
                          </div>
                        ) : (
                          <span className="text-gray-400">No limits set</span>
                        )}
                      </TableCell>
                      <TableCell>
                        {u.isActive ? (
                          <Badge className="bg-green-500">Active</Badge>
                        ) : (
                          <Badge variant="destructive">Suspended</Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex gap-2 justify-end">
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="text-blue-500 hover:text-blue-700"
                            onClick={() => openDetailsDialog(u)}
                            title="View Details"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="text-green-500 hover:text-green-700"
                            onClick={() => openEditDialog(u)}
                            title="Edit User Admin"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className={u.isActive ? "text-orange-500 hover:text-orange-700" : "text-green-500 hover:text-green-700"}
                            onClick={() => handleSuspendToggle(u.id, u.isActive)}
                            title={u.isActive ? "Suspend User Admin" : "Reactivate User Admin"}
                            disabled={suspending}
                          >
                            {suspending ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : u.isActive ? (
                              <UserX className="h-4 w-4" />
                            ) : (
                              <Power className="h-4 w-4" />
                            )}
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="text-blue-500 hover:text-blue-700"
                            onClick={() => openResetDialog(u.id, u.email)}
                            title="Reset Password"
                          >
                            <Key className="h-4 w-4" />
                          </Button>
                          <Button 
                            variant="ghost" 
                            size="sm" 
                            className="text-red-500 hover:text-red-700"
                            onClick={() => handleDelete(u.id)}
                            title="Delete User Admin"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
