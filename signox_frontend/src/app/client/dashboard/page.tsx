/* eslint-disable react-hooks/exhaustive-deps */
'use client';

import { useEffect, useState } from 'react';
import { Activity, Monitor, Users, Building2, User, Crown, TrendingUp, HardDrive } from 'lucide-react';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import { StatCard } from '@/components/dashboard/StatCard';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { StorageIndicator } from '@/components/ui/storage-indicator';
import api from '@/lib/api';
import AOS from 'aos';
import 'aos/dist/aos.css';

interface ClientSummary {
  userAdmins: number;
  totalDisplays: number;
  onlineDisplays: number;
  offlineDisplays: number;
  license: {
    status: string;
    expiry: string | null;
  };
}

interface StorageInfo {
  limitMB: number;
  usedMB: number;
  availableMB: number;
  maxMonthlyUsageMB?: number;
  monthlyUploadedMB?: number;
  monthlyQuotaRemainingMB?: number;
  quotaResetDate?: Date | string;
}

type HierarchyInfo = {
  clientAdmin: {
    id: string;
    email: string;
    name: string;
  } | null;
  userAdmin: {
    id: string;
    email: string;
  } | null;
  companyName: string | null;
};

type UserProfile = {
  id: string;
  email: string;
  role: string;
  staffRole?: string;
  isActive: boolean;
  createdAt: string;
};

export default function ClientDashboard() {
  const [summary, setSummary] = useState<ClientSummary | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [hierarchy, setHierarchy] = useState<HierarchyInfo | null>(null);
  const [storageInfo, setStorageInfo] = useState<StorageInfo | null>(null);

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });

    // Fetch analytics summary
    api
      .get('/analytics/summary')
      .then((res) => {
        if (res.data.role === 'CLIENT_ADMIN') {
          setSummary(res.data);
        }
      })
      .catch(() => setSummary(null));

    // Fetch user profile and hierarchy
    api
      .get('/users/profile')
      .then((res) => {
        setProfile(res.data.user);
        setHierarchy(res.data.hierarchy);
      })
      .catch(() => {
        setProfile(null);
        setHierarchy(null);
      });

    // Fetch storage info
    api
      .get('/media/storage-info')
      .then((res) => {
        setStorageInfo(res.data.storageInfo);
      })
      .catch(() => setStorageInfo(null));
  }, []);

  const getRoleDisplayName = (role: string, staffRole?: string) => {
    const roleNames = {
      SUPER_ADMIN: 'Super Administrator',
      CLIENT_ADMIN: 'Client Administrator',
      USER_ADMIN: 'User Administrator',
      STAFF: 'Staff Member'
    };

    const staffRoleNames = {
      DISPLAY_MANAGER: 'Display Manager',
      BROADCAST_MANAGER: 'Broadcast Manager',
      CONTENT_MANAGER: 'Content Manager',
      CMS_VIEWER: 'CMS Viewer',
      POP_MANAGER: 'Proof of Play Manager'
    };

    let displayName = roleNames[role as keyof typeof roleNames] || role;
    if (role === 'STAFF' && staffRole) {
      displayName += ` - ${staffRoleNames[staffRole as keyof typeof staffRoleNames] || staffRole}`;
    }
    return displayName;
  };

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
                  <Crown className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Client Dashboard</h1>
                  {profile && (
                    <Badge className="bg-yellow-400 text-black font-bold px-4 py-1">
                      {getRoleDisplayName(profile.role, profile.staffRole)}
                    </Badge>
                  )}
                </div>
                <div className="flex items-center gap-4 flex-wrap">
                  <p className="text-gray-300 text-lg">Tenant analytics and quick actions</p>
                  {hierarchy?.companyName && (
                    <div className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/20">
                      <Building2 className="h-4 w-4 text-yellow-400" />
                      <span className="text-white font-semibold">{hierarchy.companyName}</span>
                    </div>
                  )}
                </div>
              </div>
              {summary?.license && (
                <div className="bg-blue-500 px-6 py-3 rounded-xl shadow-lg">
                  <p className="text-white font-bold text-sm">Organization License: {summary.license.status}</p>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* User Information Card */}
        {(profile || hierarchy) && (
          <Card className="border-gray-200 shadow-lg" data-aos="fade-up">
            <CardHeader className="bg-gradient-to-r from-gray-50 to-white">
              <CardTitle className="flex items-center gap-3 text-2xl">
                <div className="bg-gradient-to-br from-yellow-400 to-orange-500 p-3 rounded-xl">
                  <User className="h-6 w-6 text-white" />
                </div>
                Administrator Information
              </CardTitle>
              <CardDescription className="text-base">Your account and organization details</CardDescription>
            </CardHeader>
            <CardContent className="pt-6">
              <div className="grid gap-6 md:grid-cols-2">
                <div className="bg-gray-50 p-4 rounded-xl">
                  <p className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Email</p>
                  <p className="text-lg font-bold text-gray-900">{profile?.email || 'Loading...'}</p>
                </div>
                {hierarchy?.companyName && (
                  <div className="bg-gray-50 p-4 rounded-xl">
                    <p className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Company</p>
                    <p className="text-lg font-bold text-gray-900">{hierarchy.companyName}</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        )}

        {/* Stats Grid */}
        <div className="grid gap-6 md:grid-cols-2">
          <StatCard
            title="User Admins"
            value={summary?.userAdmins ?? '—'}
            icon={<Users className="h-8 w-8" />}
            gradient="from-blue-400 to-blue-600"
          />
          <StatCard
            title="Total Displays"
            value={summary?.totalDisplays ?? '—'}
            subtitle={summary ? `${summary.onlineDisplays} online · ${summary.offlineDisplays} offline` : undefined}
            icon={<Monitor className="h-8 w-8" />}
            gradient="from-yellow-400 to-orange-500"
          />
        </div>

        {/* User Management Overview */}
        <div className="grid gap-6 md:grid-cols-1" data-aos="fade-up" data-aos-delay="200">
          <Card className="border-gray-200 shadow-lg hover:shadow-xl transition-shadow duration-300">
            <CardHeader className="bg-gradient-to-r from-blue-50 to-purple-50">
              <CardTitle className="flex items-center gap-3 text-xl">
                <div className="bg-gradient-to-br from-blue-400 to-purple-500 p-3 rounded-xl">
                  <TrendingUp className="h-6 w-6 text-white" />
                </div>
                Organization Overview
              </CardTitle>
              <CardDescription>
                Manage your User Admins who control displays, content, and have individual limits and licenses.
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-6">
              <div className="grid gap-4 md:grid-cols-3">
                <div className="bg-gray-50 p-4 rounded-xl text-center">
                  <div className="text-2xl font-bold text-blue-600 mb-1">{summary?.userAdmins ?? 0}</div>
                  <div className="text-sm text-gray-600">User Admins</div>
                </div>
                <div className="bg-gray-50 p-4 rounded-xl text-center">
                  <div className="text-2xl font-bold text-yellow-600 mb-1">{summary?.totalDisplays ?? 0}</div>
                  <div className="text-sm text-gray-600">Total Displays</div>
                </div>
                <div className="bg-gray-50 p-4 rounded-xl text-center">
                  <div className="text-2xl font-bold text-green-600 mb-1">{summary?.onlineDisplays ?? 0}</div>
                  <div className="text-sm text-gray-600">Online Displays</div>
                </div>
              </div>
              <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-xl">
                <p className="text-sm text-yellow-800">
                  <strong>Note:</strong> Display limits, storage quotas, and license expiry are now managed at the User Admin level. 
                  Each User Admin has individual limits and license terms.
                </p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Storage & Usage */}
        <Card className="border-gray-200 shadow-lg hover:shadow-xl transition-shadow duration-300" data-aos="fade-up" data-aos-delay="300">
          <CardHeader className="bg-gradient-to-r from-green-50 to-emerald-50">
            <CardTitle className="flex items-center gap-3 text-xl">
              <div className="bg-gradient-to-br from-green-400 to-green-600 p-3 rounded-xl">
                <HardDrive className="h-6 w-6 text-white" />
              </div>
              Storage & Monthly Usage
            </CardTitle>
            <CardDescription>Organization-wide storage and upload quota</CardDescription>
          </CardHeader>
          <CardContent className="pt-6">
            {storageInfo ? (
              <StorageIndicator storageInfo={storageInfo} />
            ) : (
              <div className="text-center text-gray-500 py-4">Loading storage info...</div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
