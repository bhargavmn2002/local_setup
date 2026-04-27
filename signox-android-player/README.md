# SignoX Android Player

A professional digital signage player for Android devices with enterprise kiosk mode features.

## 🌐 Server Configuration

**Permanently configured to connect to:**
```
https://signoxcms.com/player
```

No server configuration required by end users. The app automatically connects on launch.

---

## 🚀 Quick Start

### Build & Install
```bash
./build-and-install.sh
```

### Build Release APK
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

---

## 📚 Documentation

### Getting Started
- **[DEPLOYMENT_READY.md](DEPLOYMENT_READY.md)** - Complete deployment guide
- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Quick command reference
- **[SETUP_COMPLETE.md](SETUP_COMPLETE.md)** - Setup completion summary

### Technical Details
- **[CONFIGURATION.md](CONFIGURATION.md)** - Detailed configuration guide
- **[APP_FLOW.md](APP_FLOW.md)** - Visual application flow diagrams
- **[README_CONFIGURATION.md](README_CONFIGURATION.md)** - Configuration overview

---

## ✨ Features

### Zero-Configuration Deployment
- ✅ Hardcoded server URL
- ✅ Automatic connection
- ✅ No user setup required
- ✅ Instant pairing

### Enterprise Kiosk Mode
- ✅ Fullscreen playback
- ✅ Hidden system UI
- ✅ Auto-start on boot
- ✅ Watchdog service
- ✅ PIN-protected exit

### Content Support
- ✅ Images (JPG, PNG, WebP)
- ✅ Videos (MP4, HLS)
- ✅ Playlists
- ✅ Layouts (multi-zone)
- ✅ Schedules
- ✅ Offline mode

### Monitoring
- ✅ Heartbeat
- ✅ Proof of play
- ✅ Location tracking
- ✅ Error reporting

---

## 🎯 How It Works

```
1. Install APK → 2. Open App → 3. Auto-connect → 4. Show Pairing Code → 5. Admin Pairs → 6. Play Content
```

### Detailed Flow
1. **App Launch**: Automatically connects to `https://signoxcms.com/player`
2. **Pairing**: Displays a pairing code (e.g., "ABC123")
3. **Admin Action**: Admin enters code in web dashboard
4. **Content Delivery**: Device receives and plays assigned content
5. **Continuous Operation**: App stays in kiosk mode, auto-updates content

---

## 🏗️ Build Commands

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Install to Device
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Quick Build & Install
```bash
./build-and-install.sh
```

---

## 📱 Requirements

### Device Requirements
- Android 7.0 (API 24) or higher
- 2GB RAM minimum (4GB recommended)
- 16GB storage minimum
- Internet connection (WiFi or cellular)
- GPS (optional, for location tracking)

### Server Requirements
- SignoX CMS server running at `https://signoxcms.com/player`
- HTTPS enabled
- Valid SSL certificate

---

## 🔧 Configuration

### Server URL
The server URL is hardcoded in:
```
app/src/main/java/com/signox/player/data/api/ApiClient.kt
```

To change it, modify:
```kotlin
private const val FIXED_BASE_URL = "https://signoxcms.com/api"
```

### Exit PIN
Default PIN: `0000`

To change it, modify `MainActivity.kt` line ~380:
```kotlin
if (enteredPin == "0000") { // Change this
```

### Polling Intervals
Configured in `ConfigService.kt`:
- Config Poll: 5 seconds
- Heartbeat: 30 seconds
- Pairing Check: 5 seconds

---

## 🔍 Testing

### View Logs
```bash
adb logcat | grep -E "MainActivity|ConfigService|ApiClient"
```

### Check Connection
```bash
adb logcat -s OkHttp:D
```

### Monitor Pairing
```bash
adb logcat -s ConfigService:D
```

---

## 🆘 Troubleshooting

### Connection Issues
- Check device internet connection
- Verify server is accessible
- Review server logs

### Pairing Issues
- Ensure server is running
- Try resetting pairing (5 taps → PIN → Reset)
- Check pairing endpoint

### Content Issues
- Verify content is assigned in dashboard
- Check media file URLs
- Review app logs

---

## 📊 Project Structure

```
signox-android-player/
├── app/
│   ├── src/main/java/com/signox/player/
│   │   ├── MainActivity.kt              # Main entry point
│   │   ├── data/
│   │   │   ├── api/ApiClient.kt         # Server URL configuration
│   │   │   ├── repository/              # Data management
│   │   │   └── dto/                     # Data models
│   │   ├── service/
│   │   │   ├── ConfigService.kt         # Pairing & config
│   │   │   ├── KioskModeManager.kt      # Kiosk mode
│   │   │   └── WatchdogService.kt       # Auto-restart
│   │   ├── ui/
│   │   │   ├── screens/                 # UI screens
│   │   │   └── player/                  # Media players
│   │   ├── cache/                       # Offline caching
│   │   └── utils/                       # Utilities
│   └── build.gradle.kts                 # Build configuration
├── build-and-install.sh                 # Quick build script
├── DEPLOYMENT_READY.md                  # Deployment guide
├── CONFIGURATION.md                     # Configuration details
└── README.md                            # This file
```

---

## 🤝 Contributing

This is a production application. For changes:
1. Test thoroughly on development devices
2. Update documentation
3. Build and test release APK
4. Deploy to pilot devices
5. Roll out to production

---

## 📄 License

Proprietary - SignoX CMS

---

## 📞 Support

For technical support:
- Review documentation files
- Check application logs
- Contact development team

---

## ✅ Status

- **Configuration**: ✅ Complete
- **Server URL**: `https://signoxcms.com/player`
- **Production Ready**: ✅ Yes
- **Last Updated**: February 2026

---

**Ready to deploy! 🚀**
