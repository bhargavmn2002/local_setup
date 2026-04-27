/* eslint-disable react-hooks/exhaustive-deps */
'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';
import { useAuth } from '@/contexts/AuthContext';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
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
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Loader2, Plus, Search, Edit, Trash2, Users, Building2, Eye, Phone, Calendar, HardDrive, Upload, UserCheck, Monitor } from 'lucide-react';
import AOS from 'aos';
import 'aos/dist/aos.css';

type ClientProfile = {
  clientId?: string;
  maxDisplays: number;
  maxUsers: number;
  maxStorageMB?: number;
  licenseExpiry?: string | null;
  companyName?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
};

type ClientAdmin = {
  id: string;
  email: string;
  isActive: boolean;
  createdAt: string;
  clientProfile?: ClientProfile | null;
  displaysUsed?: number;
  licenseStatus?: 'active' | 'expired' | 'suspended' | 'expiring_soon';
  daysUntilExpiry?: number | null;
  isExpired?: boolean;
};

function fmtDate(d?: string | null) {
  if (!d) return '—';
  const dt = new Date(d);
  if (Number.isNaN(dt.getTime())) return '—';
  return dt.toLocaleDateString();
}

export default function SuperAdminClientsPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [clients, setClients] = useState<ClientAdmin[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [error, setError] = useState<string>('');

  const [open, setOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);
  const [selectedClient, setSelectedClient] = useState<ClientAdmin | null>(null);
  const [clientStats, setClientStats] = useState<any>(null);
  const [loadingStats, setLoadingStats] = useState(false);
  
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    companyName: '',
  });

  const [editForm, setEditForm] = useState({
    companyName: '',
    contactEmail: '',
    contactPhone: '',
  });

  const isSuperAdmin = useMemo(() => user?.role === 'SUPER_ADMIN', [user?.role]);

  // Filter clients based on search term
  const filteredClients = useMemo(() => {
    if (!searchTerm.trim()) return clients;
    
    const term = searchTerm.toLowerCase().trim();
    return clients.filter((client) => {
      const clientId = client.clientProfile?.clientId?.toLowerCase() || '';
      const companyName = client.clientProfile?.companyName?.toLowerCase() || '';
      const email = client.email.toLowerCase();
      
      return (
        clientId.includes(term) ||
        companyName.includes(term) ||
        email.includes(term)
      );
    });
  }, [clients, searchTerm]);

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    if (!user) return;
    if (!isSuperAdmin) {
      router.replace('/login');
      return;
    }
    fetchClients();
  }, [user, isSuperAdmin]);

  async function fetchClients() {
    try {
      setLoading(true);
      setError('');
      console.log('📋 Fetching client admins list...');
      
      const res = await api.get('/users/client-admins');
      console.log('✅ Fetched clients:', res.data.clientAdmins?.length || 0);
      
      setClients(res.data.clientAdmins ?? []);
    } catch (e: any) {
      console.error('❌ Fetch clients error:', {
        status: e?.response?.status,
        message: e?.response?.data?.message
      });
      
      setError(e?.response?.data?.message || 'Failed to load client admins');
      setClients([]);
    } finally {
      setLoading(false);
    }
  }

  async function createClient() {
    try {
      setSaving(true);
      setError('');

      if (!form.email || !form.password || !form.companyName) {
        setError('Email, Password, and Company Name are required.');
        return;
      }

      await api.post('/users/client-admin', {
        name: form.name || undefined,
        email: form.email,
        password: form.password,
        companyName: form.companyName,
      });

      setOpen(false);
      setForm({
        name: '',
        email: '',
        password: '',
        companyName: '',
      });

      await fetchClients();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to create client admin');
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(id: string) {
    // This functionality has been removed
    setError('Client Admin suspension has been disabled. Only Client Admins can manage their User Admins.');
    return;
  }

  // Open Client Details Dialog
  const openDetailsDialog = async (client: ClientAdmin) => {
    setSelectedClient(client);
    setDetailsDialogOpen(true);
    setLoadingStats(true);
    
    try {
      // Fetch client statistics (user admins, displays, total displays)
      const [userAdminsRes, displaysRes] = await Promise.all([
        api.get(`/users?managedByClientAdminId=${client.id}`),
        api.get(`/displays?clientAdminId=${client.id}`)
      ]);
      
      const userAdmins = Array.isArray(userAdminsRes.data) ? userAdminsRes.data : userAdminsRes.data.users || [];
      const displays = Array.isArray(displaysRes.data) ? displaysRes.data : displaysRes.data.displays || [];
      
      // Calculate statistics
      const activeUserAdmins = userAdmins.filter((ua: any) => ua.isActive);
      const suspendedUserAdmins = userAdmins.filter((ua: any) => !ua.isActive);
      const totalDisplays = displays.length;
      const activeDisplays = displays.filter((d: any) => d.isOnline).length;
      
      setClientStats({
        totalUserAdmins: userAdmins.length,
        activeUserAdmins: activeUserAdmins.length,
        suspendedUserAdmins: suspendedUserAdmins.length,
        totalDisplays,
        activeDisplays,
        userAdminsList: userAdmins,
      });
    } catch (error) {
      console.error('Failed to fetch client stats:', error);
      setClientStats({
        totalUserAdmins: 0,
        activeUserAdmins: 0,
        suspendedUserAdmins: 0,
        totalDisplays: 0,
        activeDisplays: 0,
        userAdminsList: [],
      });
    } finally {
      setLoadingStats(false);
    }
  };

  const openEditDialog = (client: ClientAdmin) => {
    setSelectedClient(client);
    setEditForm({
      companyName: client.clientProfile?.companyName || '',
      contactEmail: client.clientProfile?.contactEmail || '',
      contactPhone: client.clientProfile?.contactPhone || '',
    });
    setEditOpen(true);
    setError('');
  };

  const updateClient = async () => {
    if (!selectedClient) return;

    try {
      setSaving(true);
      setError('');

      if (!editForm.companyName) {
        setError('Company Name is required.');
        return;
      }

      await api.put(`/users/client-admins/${selectedClient.id}`, {
        companyName: editForm.companyName,
        contactEmail: editForm.contactEmail || null,
        contactPhone: editForm.contactPhone || null,
      });

      setEditOpen(false);
      setSelectedClient(null);
      setEditForm({
        companyName: '',
        contactEmail: '',
        contactPhone: '',
      });

      await fetchClients();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Failed to update client');
    } finally {
      setSaving(false);
    }
  };

  const openDeleteDialog = (client: ClientAdmin) => {
    setSelectedClient(client);
    setDeleteOpen(true);
  };

  const deleteClient = async () => {
    if (!selectedClient) return;

    try {
      setSaving(true);
      
      await api.delete(`/users/client-admins/${selectedClient.id}`);
      
      setDeleteOpen(false);
      setSelectedClient(null);
      await fetchClients();
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Failed to delete client');
    } finally {
      setSaving(false);
    }
  };

  if (!isSuperAdmin) {
    return (
      <DashboardLayout>
        <div />
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
                  <Users className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Manage Clients</h1>
                </div>
                <p className="text-gray-300 text-lg">
                  Create and manage tenant (Client Admin) accounts. Suspension is now handled at the User Admin level by Client Admins.
                </p>
              </div>

              <Dialog open={open} onOpenChange={setOpen}>
                <DialogTrigger asChild>
                  <Button className="h-12 gap-2 bg-gradient-to-r from-yellow-400 to-yellow-500 hover:from-yellow-500 hover:to-yellow-600 text-black font-bold shadow-lg hover:shadow-yellow-500/50 transition-all duration-300 hover:scale-105">
                    <Plus className="h-5 w-5" />
                    Add New Tenant
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[560px] bg-white border border-gray-200 shadow-lg">
              <DialogHeader>
                <DialogTitle className="text-gray-900">Add New Tenant (Client Admin)</DialogTitle>
                <DialogDescription className="text-gray-600">
                  Create a Client Admin user. License expiry and limits are managed at the User Admin level.
                </DialogDescription>
              </DialogHeader>

              <div className="grid gap-4 py-4">
                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label>Name</Label>
                    <Input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
                  </div>
                  <div className="space-y-2">
                    <Label>Company Name</Label>
                    <Input
                      value={form.companyName}
                      onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                      required
                    />
                  </div>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label>Email</Label>
                    <Input
                      type="email"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Password</Label>
                    <Input
                      type="password"
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                      required
                    />
                  </div>
                </div>

                {error && (
                  <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">{error}</div>
                )}
              </div>

              <DialogFooter>
                <Button variant="outline" onClick={() => setOpen(false)} disabled={saving}>
                  Cancel
                </Button>
                <Button onClick={createClient} disabled={saving} className="signomart-primary hover:signomart-primary">
                  {saving ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Creating…
                    </>
                  ) : (
                    'Create Tenant'
                  )}
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
            </div>
          </div>
        </div>

        {/* Search Bar */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400" />
            <Input
              placeholder="Search by Client ID, Company Name, or Email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 h-12 border-gray-300 focus:border-yellow-400 focus:ring-yellow-400"
            />
          </div>
          {searchTerm && (
            <div className="flex items-center gap-2 bg-yellow-50 px-4 py-2 rounded-xl border border-yellow-200">
              <Building2 className="h-4 w-4 text-yellow-600" />
              <span className="text-sm font-semibold text-yellow-900">
                {filteredClients.length} of {clients.length} clients
              </span>
            </div>
          )}
        </div>

        {error && !loading && (
          <div className="rounded-md bg-red-50 p-4 border border-red-200">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">Error</h3>
                <div className="mt-2 text-sm text-red-700">{error}</div>
              </div>
              <div className="ml-auto pl-3">
                <button
                  onClick={() => setError('')}
                  className="inline-flex rounded-md bg-red-50 p-1.5 text-red-500 hover:bg-red-100"
                >
                  <span className="sr-only">Dismiss</span>
                  <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-gray-400" />
            <p className="ml-3 text-gray-600">Loading client admins…</p>
          </div>
        ) : (
          <div className="rounded-2xl border border-gray-200 bg-white shadow-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Client ID</TableHead>
                  <TableHead>Company Name</TableHead>
                  <TableHead>Admin Email</TableHead>
                  <TableHead className="w-[140px]">Status</TableHead>
                  <TableHead className="w-[140px] text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredClients.map((c) => {
                  const maxDisplays = c.clientProfile?.maxDisplays ?? 0;
                  const displaysUsed = c.displaysUsed ?? 0;
                  const maxStorageMB = c.clientProfile?.maxStorageMB ?? 25;
                  
                  // Determine license status badge
                  let licenseBadge = null;
                  if (c.licenseStatus === 'suspended') {
                    licenseBadge = <Badge variant="destructive" className="ml-2">Suspended</Badge>;
                  }

                  return (
                    <TableRow key={c.id}>
                      <TableCell className="font-mono text-sm font-medium">
                        {c.clientProfile?.clientId || '—'}
                      </TableCell>
                      <TableCell className="font-medium">
                        <button
                          onClick={() => openDetailsDialog(c)}
                          className="text-blue-600 hover:text-blue-800 hover:underline cursor-pointer"
                        >
                          {c.clientProfile?.companyName || '—'}
                        </button>
                      </TableCell>
                      <TableCell>{c.email}</TableCell>
                      <TableCell>
                        <Badge variant={c.isActive ? "default" : "destructive"}>
                          {c.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            className="gap-2 text-blue-500 hover:text-blue-700"
                            onClick={() => openDetailsDialog(c)}
                            disabled={saving}
                          >
                            <Eye className="h-4 w-4" />
                            View
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="gap-2"
                            onClick={() => openEditDialog(c)}
                            disabled={saving}
                          >
                            <Edit className="h-4 w-4" />
                            Edit
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            className="gap-2 text-red-600 hover:text-red-700"
                            onClick={() => openDeleteDialog(c)}
                            disabled={saving}
                          >
                            <Trash2 className="h-4 w-4" />
                            Delete
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
                {filteredClients.length === 0 && clients.length > 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-gray-500">
                      No clients found matching "{searchTerm}".
                    </TableCell>
                  </TableRow>
                )}
                {clients.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-gray-500">
                      No Client Admins found.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {/* CLIENT DETAILS MODAL */}
      <Dialog open={detailsDialogOpen} onOpenChange={setDetailsDialogOpen}>
        <DialogContent className="bg-white max-w-6xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="text-2xl flex items-center gap-3">
              <Eye className="h-6 w-6 text-blue-500" />
              Client Admin Details
            </DialogTitle>
          </DialogHeader>
          
          {selectedClient && (
            <div className="space-y-6">
              {/* Basic Information */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Building2 className="h-5 w-5 text-blue-500" />
                      Basic Information
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Client ID</Label>
                      <p className="text-base font-medium font-mono">{selectedClient.clientProfile?.clientId || 'Not assigned'}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Admin Email</Label>
                      <p className="text-base font-medium">{selectedClient.email}</p>
                    </div>
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Status</Label>
                      <div className="mt-1">
                        <Badge className={selectedClient.isActive ? "bg-green-500" : "bg-red-500"}>
                          {selectedClient.isActive ? 'Active' : 'Inactive'}
                        </Badge>
                      </div>
                    </div>
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Created</Label>
                      <p className="text-base">
                        {selectedClient.createdAt ? new Date(selectedClient.createdAt).toLocaleDateString() : 'Unknown'}
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
                        {selectedClient.clientProfile?.companyName || 'Not specified'}
                      </p>
                    </div>
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Contact Email</Label>
                      <p className="text-base flex items-center gap-2">
                        <Phone className="h-4 w-4 text-gray-400" />
                        {selectedClient.clientProfile?.contactEmail || 'Not specified'}
                      </p>
                    </div>
                    <div>
                      <Label className="text-sm font-semibold text-gray-600">Contact Phone</Label>
                      <p className="text-base flex items-center gap-2">
                        <Phone className="h-4 w-4 text-gray-400" />
                        {selectedClient.clientProfile?.contactPhone || 'Not specified'}
                      </p>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Statistics Overview */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Users className="h-5 w-5 text-green-500" />
                      User Admin Statistics
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {loadingStats ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                        <span className="ml-2 text-gray-600">Loading statistics...</span>
                      </div>
                    ) : (
                      <>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="bg-blue-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-blue-600 uppercase">Total User Admins</Label>
                            <p className="text-2xl font-bold text-blue-700">
                              {clientStats?.totalUserAdmins || 0}
                            </p>
                          </div>
                          <div className="bg-green-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-green-600 uppercase">Active</Label>
                            <p className="text-2xl font-bold text-green-700">
                              {clientStats?.activeUserAdmins || 0}
                            </p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="bg-orange-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-orange-600 uppercase">Suspended</Label>
                            <p className="text-2xl font-bold text-orange-700">
                              {clientStats?.suspendedUserAdmins || 0}
                            </p>
                          </div>
                          <div className="bg-purple-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-purple-600 uppercase">Active Rate</Label>
                            <p className="text-2xl font-bold text-purple-700">
                              {clientStats?.totalUserAdmins > 0 
                                ? Math.round((clientStats.activeUserAdmins / clientStats.totalUserAdmins) * 100)
                                : 0}%
                            </p>
                          </div>
                        </div>
                      </>
                    )}
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Monitor className="h-5 w-5 text-indigo-500" />
                      Display Statistics
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {loadingStats ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                        <span className="ml-2 text-gray-600">Loading display data...</span>
                      </div>
                    ) : (
                      <>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="bg-blue-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-blue-600 uppercase">Total Displays</Label>
                            <p className="text-2xl font-bold text-blue-700">
                              {clientStats?.totalDisplays || 0}
                            </p>
                          </div>
                          <div className="bg-green-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-green-600 uppercase">Online</Label>
                            <p className="text-2xl font-bold text-green-700">
                              {clientStats?.activeDisplays || 0}
                            </p>
                          </div>
                        </div>
                        <div className="grid grid-cols-2 gap-4">
                          <div className="bg-red-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-red-600 uppercase">Offline</Label>
                            <p className="text-2xl font-bold text-red-700">
                              {(clientStats?.totalDisplays || 0) - (clientStats?.activeDisplays || 0)}
                            </p>
                          </div>
                          <div className="bg-purple-50 p-3 rounded-lg">
                            <Label className="text-xs font-semibold text-purple-600 uppercase">Online Rate</Label>
                            <p className="text-2xl font-bold text-purple-700">
                              {clientStats?.totalDisplays > 0 
                                ? Math.round((clientStats.activeDisplays / clientStats.totalDisplays) * 100)
                                : 0}%
                            </p>
                          </div>
                        </div>
                      </>
                    )}
                  </CardContent>
                </Card>
              </div>

              {/* User Admins List */}
              <Card>
                <CardHeader className="pb-3">
                  <CardTitle className="text-lg flex items-center gap-2">
                    <UserCheck className="h-5 w-5 text-yellow-500" />
                    User Admins Under This Client
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  {loadingStats ? (
                    <div className="flex items-center justify-center py-8">
                      <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                      <span className="ml-2 text-gray-600">Loading user admins...</span>
                    </div>
                  ) : clientStats?.userAdminsList?.length > 0 ? (
                    <div className="space-y-3">
                      {clientStats.userAdminsList.map((userAdmin: any, index: number) => (
                        <div key={userAdmin.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg border">
                          <div className="flex items-center gap-4">
                            <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                              <span className="text-sm font-semibold text-blue-600">{index + 1}</span>
                            </div>
                            <div>
                              <p className="font-medium text-gray-900">{userAdmin.email}</p>
                              <div className="flex items-center gap-2 mt-1">
                                <Badge className={userAdmin.isActive ? "bg-green-500" : "bg-red-500"}>
                                  {userAdmin.isActive ? 'Active' : 'Suspended'}
                                </Badge>
                                {userAdmin.userAdminProfile?.companyName && (
                                  <span className="text-sm text-gray-600">
                                    {userAdmin.userAdminProfile.companyName}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                          <div className="text-right text-sm text-gray-600">
                            <div>Displays: {userAdmin.userAdminProfile?.maxDisplays || 0}</div>
                            <div>Staff: {userAdmin.userAdminProfile?.maxUsers || 0}</div>
                            {userAdmin.userAdminProfile?.licenseExpiry && (
                              <div className="text-xs mt-1">
                                License: {new Date(userAdmin.userAdminProfile.licenseExpiry).toLocaleDateString()}
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-8 text-gray-500">
                      <Users className="h-12 w-12 mx-auto mb-4 text-gray-300" />
                      <p>No User Admins found under this client.</p>
                    </div>
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

      {/* Edit Client Dialog */}
      <Dialog open={editOpen} onOpenChange={(v) => !saving && setEditOpen(v)}>
        <DialogContent className="sm:max-w-[560px] bg-white border border-gray-200 shadow-lg">
          <DialogHeader>
            <DialogTitle className="text-gray-900">Edit Client</DialogTitle>
            <DialogDescription className="text-gray-600">
              Update client information. License expiry and limits are managed at the User Admin level.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label>Company Name</Label>
                <Input
                  value={editForm.companyName}
                  onChange={(e) => setEditForm({ ...editForm, companyName: e.target.value })}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label>Contact Email</Label>
                <Input
                  type="email"
                  value={editForm.contactEmail}
                  onChange={(e) => setEditForm({ ...editForm, contactEmail: e.target.value })}
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="space-y-2">
                <Label>Contact Phone</Label>
                <Input
                  value={editForm.contactPhone}
                  onChange={(e) => setEditForm({ ...editForm, contactPhone: e.target.value })}
                />
              </div>
            </div>

            {error && (
              <div className="rounded-md bg-red-50 p-3 text-sm text-red-800">{error}</div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setEditOpen(false)} disabled={saving}>
              Cancel
            </Button>
            <Button onClick={updateClient} disabled={saving} className="signomart-primary hover:signomart-primary">
              {saving ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Updating…
                </>
              ) : (
                'Update Client'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Client Dialog */}
      <Dialog open={deleteOpen} onOpenChange={(v) => !saving && setDeleteOpen(v)}>
        <DialogContent className="sm:max-w-[420px] bg-white border border-gray-200 shadow-lg">
          <DialogHeader>
            <DialogTitle className="text-gray-900">Delete Client</DialogTitle>
            <DialogDescription className="text-gray-600">
              This action cannot be undone. This will permanently delete the client and all associated data.
            </DialogDescription>
          </DialogHeader>

          <div className="py-4">
            <div className="rounded-md bg-red-50 p-4 border border-red-200">
              <div className="flex">
                <div className="flex-shrink-0">
                  <Trash2 className="h-5 w-5 text-red-400" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-red-800">
                    Warning: This will delete all data for:
                  </h3>
                  <div className="mt-2 text-sm text-red-700">
                    <ul className="list-disc list-inside space-y-1">
                      <li><strong>{selectedClient?.clientProfile?.companyName}</strong></li>
                      <li>Client ID: {selectedClient?.clientProfile?.clientId}</li>
                      <li>All user accounts under this client</li>
                      <li>All displays, media, and playlists</li>
                      <li>All associated data and settings</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <DialogFooter className="bg-gray-50 -mx-6 -mb-6 px-6 py-4 rounded-b-lg">
            <Button variant="outline" onClick={() => setDeleteOpen(false)} disabled={saving}>
              Cancel
            </Button>
            <Button 
              variant="destructive" 
              onClick={deleteClient} 
              disabled={saving}
            >
              {saving ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Deleting…
                </>
              ) : (
                'Delete Client'
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
}

