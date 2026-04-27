'use client';

import { useEffect, useState } from 'react';
import { Download, Smartphone, Tv, Package } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { DashboardLayout } from '@/components/layout/DashboardLayout';
import AOS from 'aos';
import 'aos/dist/aos.css';

export default function PlayerAppsPage() {
  const [downloading, setDownloading] = useState<string | null>(null);

  useEffect(() => {
    // Initialize AOS
    AOS.init({
      duration: 800,
      once: true,
      easing: 'ease-out-cubic',
    });
  }, []);

  const handleDownload = async (appType: 'android' | 'tizen') => {
    setDownloading(appType);
    
    try {
      const fileName = appType === 'android' ? 'signox-player.apk' : 'signox-player.wgt';
      
      // Option 1: Try to get download URL from backend API (for S3/CloudFront)
      // This allows for better tracking and signed URLs
      try {
        const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:5000/api'}/player-apps/download-url?type=${appType}`);
        
        if (response.ok) {
          const data = await response.json();
          // Use the URL from backend (could be S3 signed URL)
          const link = document.createElement('a');
          link.href = data.downloadUrl;
          link.download = fileName;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          return;
        }
      } catch (apiError) {
        console.log('API endpoint not available, falling back to public folder');
      }
      
      // Option 2: Fallback to public folder (works for local and basic AWS deployments)
      const filePath = appType === 'android' 
        ? '/downloads/signox-player.apk' 
        : '/downloads/signox-player.wgt';
      
      const link = document.createElement('a');
      link.href = filePath;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    } catch (error) {
      console.error('Download failed:', error);
      alert('Failed to download the file. Please try again.');
    } finally {
      setDownloading(null);
    }
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
                  <Package className="h-10 w-10 text-yellow-400" />
                  <h1 className="text-4xl font-black text-white">Player Applications</h1>
                </div>
                <p className="text-gray-300 text-lg">Download the Signox Player app for your display devices</p>
              </div>
            </div>
          </div>
        </div>

        {/* Player Cards Grid */}
        <div className="grid gap-6 md:grid-cols-2" data-aos="fade-up" data-aos-delay="100">
          {/* Android Player Card */}
          <Card className="overflow-hidden hover:shadow-2xl transition-all duration-300 hover:scale-105 border-gray-200 group">
            <CardHeader className="bg-gradient-to-r from-green-50 to-emerald-50">
              <div className="flex items-center justify-center gap-3 py-4">
                <div className="p-4 bg-gradient-to-br from-green-400 to-green-600 rounded-xl shadow-lg">
                  <Smartphone className="h-10 w-10 text-white" />
                </div>
                <CardTitle className="text-3xl font-black">Android Player</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="py-8 flex items-center justify-center">
              <Button
                onClick={() => handleDownload('android')}
                disabled={downloading === 'android'}
                className="h-14 px-8 gap-3 bg-gradient-to-r from-green-400 to-green-600 hover:from-green-500 hover:to-green-700 text-white font-bold shadow-lg hover:shadow-green-500/50 transition-all duration-300 hover:scale-105 text-lg"
              >
                <Download className="h-6 w-6" />
                {downloading === 'android' ? 'Downloading...' : 'Download APK'}
              </Button>
            </CardContent>
          </Card>

          {/* Tizen Player Card */}
          <Card className="overflow-hidden hover:shadow-2xl transition-all duration-300 hover:scale-105 border-gray-200 group">
            <CardHeader className="bg-gradient-to-r from-blue-50 to-cyan-50">
              <div className="flex items-center justify-center gap-3 py-4">
                <div className="p-4 bg-gradient-to-br from-blue-400 to-blue-600 rounded-xl shadow-lg">
                  <Tv className="h-10 w-10 text-white" />
                </div>
                <CardTitle className="text-3xl font-black">Tizen Player</CardTitle>
              </div>
            </CardHeader>
            <CardContent className="py-8 flex items-center justify-center">
              <Button
                onClick={() => handleDownload('tizen')}
                disabled={downloading === 'tizen'}
                className="h-14 px-8 gap-3 bg-gradient-to-r from-blue-400 to-blue-600 hover:from-blue-500 hover:to-blue-700 text-white font-bold shadow-lg hover:shadow-blue-500/50 transition-all duration-300 hover:scale-105 text-lg"
              >
                <Download className="h-6 w-6" />
                {downloading === 'tizen' ? 'Downloading...' : 'Download WGT'}
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
}
