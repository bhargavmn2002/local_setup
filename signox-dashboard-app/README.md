# SignoX Dashboard Android App

**Production-Quality Digital Signage Management Dashboard**

## 🎉 Status: Phase 4 Complete (100%)

All authentication, dashboard, display, media, and playlist features are fully implemented and ready to build!

---

## 📱 Quick Start

### Build the App
```bash
cd project(signoX)/signox-dashboard-app
chmod +x gradlew
./gradlew clean assembleDebug
```

### Install
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Test Login
```
Email: admin@signox.com
Password: admin123
```

---

## ✨ Features

### Phase 1: Authentication & Dashboards ✅
- ✅ Login with email/password
- ✅ Token-based authentication
- ✅ Auto-login on app restart
- ✅ Role-based dashboard routing
- ✅ 4 different dashboard types
- ✅ Pull-to-refresh
- ✅ Logout functionality
- ✅ Error handling

### Phase 3: Media Management ✅
- ✅ Media library with grid/list view
- ✅ Upload images and videos
- ✅ Media preview (full screen)
- ✅ Media details and metadata
- ✅ Delete media with confirmation
- ✅ Storage usage indicator
- ✅ Search and filter media

### Phase 4: Playlist Management ✅
- ✅ Playlist list with search
- ✅ Create new playlists
- ✅ Edit playlists (name and items)
- ✅ Add media items to playlists
- ✅ Reorder items (drag & drop)
- ✅ Edit item duration
- ✅ Delete playlists
- ✅ Total duration calculation

### Dashboard Types
1. **Super Admin** - Platform-wide statistics
2. **Client Admin** - Client management view
3. **User Admin** - Content management view
4. **Staff** - Role-based limited view

---

## 🏗️ Architecture

### Design Pattern
- **MVVM** (Model-View-ViewModel)
- **Repository Pattern**
- **Dependency Injection** (Hilt)

### Tech Stack
- **Language**: Kotlin
- **UI**: Material Design 3, ViewBinding
- **Networking**: Retrofit, OkHttp
- **Async**: Coroutines, Flow
- **Storage**: DataStore (token management)
- **DI**: Hilt

### Project Structure
```
app/src/main/java/com/signox/dashboard/
├── SignoXApplication.kt          # App entry point
├── data/
│   ├── api/                      # API services
│   ├── local/                    # Local storage
│   ├── model/                    # Data models
│   └── repository/               # Data repositories
├── di/                           # Dependency injection
└── ui/
    ├── auth/                     # Login screens
    ├── dashboard/                # Dashboard screens
    ├── main/                     # Main container
    └── splash/                   # Splash screen
```

---

## 🎨 Design

### Theme
- **Primary Color**: Yellow/Gold (#FCD34D)
- **Style**: Material Design 3
- **Layout**: Card-based, responsive

### Screens
- Splash Screen (auto-login check)
- Login Screen (email/password)
- 4 Dashboard Screens (role-based)

---

## 🔧 Configuration

### Backend URL
Edit `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BASE_URL", "\"http://YOUR_IP:5000/api/\"")
```

Current: `http://192.168.0.118:5000/api/`

### App Settings
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Package**: com.signox.dashboard

---

## 📚 Documentation

- **BUILD_AND_SETUP_GUIDE.md** - Detailed setup instructions
- **PHASE1_COMPLETE.md** - Phase 1 completion summary
- **PHASE1_PROGRESS.md** - Full file list and progress

---

## 🚀 Next Phases

### Phase 5: Layout Builder
- Layout templates
- Visual layout editor
- Zone configuration
- Preview layouts
- Assign to displays

### Phase 6: Schedule Management
- Time-based scheduling
- Calendar view
- Schedule assignment

### Phase 7: Advanced Features
- Analytics dashboard
- Proof of Play reports
- User management
- Settings and preferences

---

## 🧪 Testing

### Test Scenarios
1. **Login Flow**: Login → Dashboard → Logout
2. **Auto-Login**: Login → Close → Reopen (should auto-login)
3. **Pull-to-Refresh**: Swipe down to refresh data
4. **Role-Based**: Test with different user roles

### Test Users
- Super Admin: `admin@signox.com` / `admin123`
- (Add more test users in your backend)

---

## 📦 Dependencies

### Core
- Kotlin 1.9.0
- Android Gradle Plugin 8.1.1

### UI
- Material Design 3
- SwipeRefreshLayout
- CardView

### Networking
- Retrofit 2.9.0
- OkHttp 4.11.0
- Gson 2.10.1

### DI
- Hilt 2.48

### Storage
- DataStore 1.0.0

### Async
- Coroutines 1.7.3

---

## 🐛 Troubleshooting

### Build Issues
```bash
# Clean build
./gradlew clean

# Check Java version (need Java 17)
java -version

# Sync Gradle
./gradlew --refresh-dependencies
```

### Connection Issues
- Ensure backend is running
- Check device can reach backend IP
- Verify BASE_URL in build.gradle.kts
- Check network permissions in AndroidManifest.xml

### Login Issues
- Verify test credentials
- Check backend API is responding
- Look at Logcat for error messages

---

## 📝 File Count

- **Total Files**: 60+
- **Kotlin Files**: 31
- **XML Files**: 24
- **Gradle Files**: 5
- **Documentation**: 5

---

## 👥 Roles & Permissions

### Super Admin
- View all clients
- View all displays
- Platform-wide statistics

### Client Admin
- Manage user admins
- View client displays
- License management

### User Admin
- Manage displays
- Upload media
- Create playlists
- Manage schedules

### Staff
- Limited access based on staff role
- View assigned content
- No management features

---

## 🎯 Success Metrics

✅ Clean architecture  
✅ Type-safe code  
✅ Error handling  
✅ Loading states  
✅ Professional UI  
✅ Secure authentication  
✅ Role-based access  
✅ Production-ready  

---

## 📞 Support

For issues or questions:
1. Check documentation files
2. Review build logs
3. Verify backend connectivity
4. Check Android Studio errors

---

## 🎉 Credits

Built with modern Android development best practices:
- Clean Architecture
- SOLID principles
- Material Design guidelines
- Kotlin best practices

---

**Ready to build? Run:**
```bash
./gradlew clean assembleDebug
```

**Happy coding! 🚀**
