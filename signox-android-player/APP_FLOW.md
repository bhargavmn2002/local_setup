# SignoX Android Player - Application Flow

## 🔄 Complete Application Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│         Auto-connect to: https://signoxcms.com/player       │
│                    (No user input required)                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────┐
                    │ Has Device Token?│
                    └─────────────────┘
                       ↓           ↓
                     YES          NO
                       ↓           ↓
        ┌──────────────────┐   ┌──────────────────┐
        │  Already Paired  │   │ Generate Pairing │
        │  Skip to Config  │   │      Code        │
        └──────────────────┘   └──────────────────┘
                ↓                       ↓
                │              ┌─────────────────────┐
                │              │  SHOW PAIRING CODE  │
                │              │    (e.g., ABC123)   │
                │              └─────────────────────┘
                │                       ↓
                │              ┌─────────────────────┐
                │              │  Poll Server Every  │
                │              │     5 seconds       │
                │              │  (Wait for Admin)   │
                │              └─────────────────────┘
                │                       ↓
                │              ┌─────────────────────┐
                │              │   Admin Enters Code │
                │              │   in Web Dashboard  │
                │              └─────────────────────┘
                │                       ↓
                └──────────────┬────────┘
                               ↓
                    ┌─────────────────────┐
                    │   DEVICE PAIRED ✓   │
                    │  Save Device Token  │
                    └─────────────────────┘
                               ↓
                    ┌─────────────────────┐
                    │  Start Config Poll  │
                    │   (Every 5 seconds) │
                    └─────────────────────┘
                               ↓
                    ┌─────────────────────┐
                    │  Start Heartbeat    │
                    │  (Every 30 seconds) │
                    └─────────────────────┘
                               ↓
                    ┌─────────────────────┐
                    │  Fetch Config from  │
                    │       Server        │
                    └─────────────────────┘
                               ↓
        ┌──────────────────────┴──────────────────────┐
        ↓                      ↓                       ↓
┌──────────────┐      ┌──────────────┐       ┌──────────────┐
│ Has Layout?  │      │Has Playlist? │       │  No Content  │
│   Priority 1 │      │  Priority 2  │       │   Assigned   │
└──────────────┘      └──────────────┘       └──────────────┘
        ↓                      ↓                       ↓
┌──────────────┐      ┌──────────────┐       ┌──────────────┐
│ PLAY LAYOUT  │      │PLAY PLAYLIST │       │SHOW STANDBY  │
│  (Fullscreen)│      │ (Fullscreen) │       │    SCREEN    │
└──────────────┘      └──────────────┘       └──────────────┘
        ↓                      ↓                       ↓
        └──────────────────────┴───────────────────────┘
                               ↓
                    ┌─────────────────────┐
                    │   KIOSK MODE ON     │
                    │  • Fullscreen       │
                    │  • Hide System UI   │
                    │  • Keep Screen On   │
                    │  • Auto-restart     │
                    └─────────────────────┘
                               ↓
                    ┌─────────────────────┐
                    │  Continuous Loop:   │
                    │  • Poll config      │
                    │  • Send heartbeat   │
                    │  • Update content   │
                    │  • Cache media      │
                    └─────────────────────┘
```

## 🎬 User Experience Flow

### First Time Setup
```
1. Install APK → 2. Open App → 3. See Pairing Code → 4. Wait for Admin → 5. Content Plays
```

### Subsequent Launches
```
1. Open App → 2. Auto-connect → 3. Content Plays Immediately
```

### Offline Mode
```
1. No Internet → 2. Use Cached Config → 3. Play Cached Media
```

## 🔐 Exit Flow

```
┌─────────────────────────────────────────────────────────────────┐
│              User wants to exit kiosk mode                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │  5 Taps in Top-Right│
                    │       Corner        │
                    └─────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │   Show PIN Dialog   │
                    │   (Enter 0000)      │
                    └─────────────────────┘
                              ↓
                    ┌─────────────────────┐
                    │   Correct PIN?      │
                    └─────────────────────┘
                       ↓           ↓
                     YES          NO
                       ↓           ↓
            ┌──────────────┐  ┌──────────────┐
            │  Exit App    │  │ Show Error & │
            │  Completely  │  │ Re-enable    │
            │              │  │ Kiosk Mode   │
            └──────────────┘  └──────────────┘
```

## 📡 Network Communication

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android Player Device                         │
└─────────────────────────────────────────────────────────────────┘
                              ↕
                    (HTTPS Connection)
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│              https://signoxcms.com/player                    │
│                                                                  │
│  Endpoints:                                                      │
│  • POST /generate-pairing-code                                   │
│  • GET  /check-pairing-status                                    │
│  • GET  /config                                                  │
│  • POST /heartbeat                                               │
│  • POST /proof-of-play                                           │
│  • POST /location                                                │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄 Polling Intervals

| Service | Interval | Purpose |
|---------|----------|---------|
| Pairing Check | 5 seconds | Check if admin has paired device |
| Config Poll | 5 seconds | Fetch latest content assignments |
| Heartbeat | 30 seconds | Keep device status updated |
| Location Update | On change | Send GPS coordinates |

## 💾 Data Storage

```
SharedPreferences:
├── device_id          (Unique device identifier)
├── device_token       (Authentication token)
├── pairing_code       (Current pairing code)
├── cached_config      (Last known config - for offline)
└── location           (Last GPS coordinates)

File Cache:
├── /cache/media/      (Downloaded media files)
├── /cache/hls/        (HLS video segments)
└── /cache/images/     (Optimized images)
```

## 🎯 Content Priority

```
Priority 1: LAYOUT
    ↓ (if no layout)
Priority 2: PLAYLIST
    ↓ (if no playlist)
Priority 3: STANDBY SCREEN
```

## 🛡️ Kiosk Mode Features

```
✓ Fullscreen display (no status bar)
✓ Hide navigation buttons
✓ Prevent app switching
✓ Auto-start on boot
✓ Watchdog service (auto-restart)
✓ Keep screen always on
✓ PIN-protected exit
✓ Immersive mode (hide system UI)
```

---

**This flow ensures zero-configuration deployment for end users!** 🎉
