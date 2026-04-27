# SignoX Digital Signage Platform

A comprehensive digital signage solution with Android player, Tizen player, dashboard app, and web management system.

## 🏗️ Architecture

- **Backend**: Node.js/Express API server
- **Frontend**: Next.js web dashboard  
- **Android Player**: Kotlin Android app for displays
- **Tizen Player**: HTML5/JS app for Samsung displays
- **Dashboard App**: Android management app

## 🚀 Quick Start

### Prerequisites
- Node.js 18+
- MongoDB
- Android Studio (for mobile apps)
- Tizen Studio (for Samsung displays)

### Backend Setup
```bash
cd signox_backend
npm install
cp .env.example .env
# Edit .env with your configuration
npm start
```

### Frontend Setup  
```bash
cd signox_frontend
npm install
cp .env.example .env
# Edit .env with your API URL
npm run build
npm start
```

## 📱 Player Apps

### Android Player
- Auto-connects to server
- Offline playback support
- Kiosk mode with PIN exit
- Location: `signox-android-player/`

### Tizen Player (Samsung Displays)
- Samsung SSSP compatible
- Auto-launch on boot
- Location: `tizen-player/`

## 🔧 Deployment

See `DEPLOYMENT_SUMMARY.txt` for complete deployment guide.

## 📄 License

Proprietary - SignoX Platform