# SignoX Local Setup Guide (MongoDB via LAN)

This guide will help you set up the SignoX project on your local network with a local MongoDB database.

## Prerequisites

1. **MongoDB Community Edition** installed on your server/local machine
2. **Node.js** (v18 or higher) and **npm** installed
3. All devices connected to the same LAN network

---

## Step 1: Install and Configure MongoDB

### Install MongoDB Community Edition

**For Ubuntu/Debian:**
```bash
# Import MongoDB public GPG key
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Create list file for MongoDB
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Update package database
sudo apt-get update

# Install MongoDB
sudo apt-get install -y mongodb-org

# Start MongoDB service
sudo systemctl start mongod
sudo systemctl enable mongod
```

**For Windows:**
- Download MongoDB Community Server from: https://www.mongodb.com/try/download/community
- Run the installer and follow the setup wizard
- Install as a Windows Service

**For macOS:**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

### Configure MongoDB for LAN Access

1. **Edit MongoDB configuration file:**

**Linux/macOS:** `/etc/mongod.conf`
**Windows:** `C:\Program Files\MongoDB\Server\7.0\bin\mongod.cfg`

```yaml
# network interfaces
net:
  port: 27017
  bindIp: 0.0.0.0  # Listen on all network interfaces (LAN access)

# security (optional - for production, enable authentication)
# security:
#   authorization: enabled
```

2. **Restart MongoDB service:**

**Linux:**
```bash
sudo systemctl restart mongod
```

**Windows:**
```powershell
net stop MongoDB
net start MongoDB
```

**macOS:**
```bash
brew services restart mongodb-community
```

3. **Verify MongoDB is running:**
```bash
mongosh
# You should see MongoDB shell prompt
```

4. **Find your server's LAN IP address:**

**Linux/macOS:**
```bash
ip addr show  # or ifconfig
# Look for inet address like 192.168.1.x
```

**Windows:**
```powershell
ipconfig
# Look for IPv4 Address like 192.168.1.x
```

---

## Step 2: Configure Backend

### 1. Navigate to backend directory:
```bash
cd signoxc/signox_backend
```

### 2. Install dependencies:
```bash
npm install
```

### 3. Update `.env` file:

Replace the MongoDB Atlas connection string with your local MongoDB:

```env
# Local MongoDB Connection
# Replace 192.168.1.232 with your server's actual LAN IP
DATABASE_URL="mongodb://192.168.1.232:27017/signox_db"

# Or if MongoDB is on the same machine as backend:
# DATABASE_URL="mongodb://localhost:27017/signox_db"

# Or with authentication (if you enabled it):
# DATABASE_URL="mongodb://username:password@192.168.1.232:27017/signox_db?authSource=admin"

JWT_SECRET=35a0dbc5be739b9f7e3525383c05e943e8084a1cf2a602c7cf1ed3a552b48b6e
PORT=5000
HOST=0.0.0.0
NODE_ENV=development

# Update CORS to include your LAN IPs
CORS_ORIGIN=http://localhost:3000,http://192.168.1.232:3000,http://192.168.1.231:3000,http://192.168.1.233:3000

TIMEZONE=Asia/Kolkata
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX=200
AUTH_RATE_LIMIT_MAX=200
MAX_LOGIN_ATTEMPTS=50
LOCKOUT_TIME=900000

# S3 Configuration (set to false for local storage)
USE_S3_FOR_PLAYER_APPS=false
USE_S3_FOR_MEDIA=false
```

### 4. Generate Prisma Client:
```bash
npx prisma generate
```

### 5. Push database schema to MongoDB:
```bash
npx prisma db push
```

### 6. Seed initial admin user:
```bash
node scripts/seedAdmin.js
```

This will create a super admin user:
- Email: `admin@signox.com`
- Password: `Admin@123`

### 7. Start the backend server:
```bash
npm run dev
```

The backend should now be running on `http://192.168.1.232:5000` (replace with your IP)

---

## Step 3: Configure Frontend

### 1. Navigate to frontend directory:
```bash
cd signoxc/signox_frontend
```

### 2. Install dependencies:
```bash
npm install
```

### 3. Update `.env` file:

```env
# Replace with your backend server's LAN IP
NEXT_PUBLIC_API_URL=http://192.168.1.232:5000/api
```

### 4. Start the frontend development server:
```bash
npm run dev
```

The frontend should now be running on `http://localhost:3000`

---

## Step 4: Configure Android Player App

### Update API endpoint in Android Player:

1. Open `signoxc/signox-android-player/app/src/main/java/com/signox/player/data/ApiService.kt`

2. Update the `BASE_URL`:
```kotlin
companion object {
    // Replace with your backend server's LAN IP
    private const val BASE_URL = "http://192.168.1.232:5000/api/"
    
    // ... rest of the code
}
```

3. Rebuild the Android app

---

## Step 5: Configure Tizen Player

### Update API endpoint in Tizen Player:

1. Open `signoxc/tizen-player/js/config.js`

2. Update the `API_BASE_URL`:
```javascript
const CONFIG = {
    // Replace with your backend server's LAN IP
    API_BASE_URL: 'http://192.168.1.232:5000/api',
    
    // ... rest of the config
};
```

3. Rebuild the Tizen app

---

## Step 6: Configure Offline Manager App

### Update API endpoint in Offline Manager:

1. Find the API configuration file in the offline manager app

2. Update the base URL to point to your backend server's LAN IP

---

## Network Configuration Checklist

### Firewall Rules

Make sure the following ports are open on your server:

**MongoDB:**
```bash
# Linux (UFW)
sudo ufw allow 27017/tcp

# Or for specific IP range
sudo ufw allow from 192.168.1.0/24 to any port 27017
```

**Backend API:**
```bash
# Linux (UFW)
sudo ufw allow 5000/tcp
```

**Windows Firewall:**
- Open Windows Defender Firewall
- Add inbound rules for ports 27017 and 5000

### Test Connectivity

From another device on the LAN:

**Test MongoDB connection:**
```bash
mongosh mongodb://192.168.1.232:27017/signox_db
```

**Test Backend API:**
```bash
curl http://192.168.1.232:5000/api/health
```

---

## Accessing the Application

### From the same machine:
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:5000`

### From other devices on LAN:
- Frontend: `http://192.168.1.232:3000` (replace with your server IP)
- Backend: `http://192.168.1.232:5000` (replace with your server IP)

### Default Login Credentials:
- Email: `admin@signox.com`
- Password: `Admin@123`

---

## Troubleshooting

### MongoDB Connection Issues

1. **Check MongoDB is running:**
```bash
sudo systemctl status mongod  # Linux
# or
mongosh  # Try connecting locally
```

2. **Check MongoDB logs:**
```bash
# Linux
sudo tail -f /var/log/mongodb/mongod.log

# Windows
# Check: C:\Program Files\MongoDB\Server\7.0\log\mongod.log
```

3. **Verify bindIp setting:**
```bash
# Should show 0.0.0.0 or your LAN IP
grep bindIp /etc/mongod.conf
```

### Backend Connection Issues

1. **Check backend logs:**
```bash
# In backend directory
npm run dev
# Look for connection errors
```

2. **Verify environment variables:**
```bash
# In backend directory
cat .env | grep DATABASE_URL
```

3. **Test Prisma connection:**
```bash
npx prisma db push
# Should connect successfully
```

### Frontend Connection Issues

1. **Check API URL in browser console:**
- Open browser DevTools (F12)
- Look for API_URL in console logs
- Check network tab for failed requests

2. **Verify CORS settings:**
- Make sure your frontend URL is in `CORS_ORIGIN` in backend `.env`

### Network Issues

1. **Ping test:**
```bash
ping 192.168.1.232  # Replace with your server IP
```

2. **Port test:**
```bash
# Linux/macOS
nc -zv 192.168.1.232 5000
nc -zv 192.168.1.232 27017

# Windows
Test-NetConnection -ComputerName 192.168.1.232 -Port 5000
```

3. **Check firewall:**
```bash
# Linux
sudo ufw status

# Windows
# Check Windows Defender Firewall settings
```

---

## Production Considerations

### Enable MongoDB Authentication

1. **Create admin user:**
```bash
mongosh
use admin
db.createUser({
  user: "admin",
  pwd: "your_secure_password",
  roles: [ { role: "userAdminAnyDatabase", db: "admin" } ]
})
```

2. **Create application user:**
```bash
use signox_db
db.createUser({
  user: "signox_user",
  pwd: "your_secure_password",
  roles: [ { role: "readWrite", db: "signox_db" } ]
})
```

3. **Enable authentication in mongod.conf:**
```yaml
security:
  authorization: enabled
```

4. **Update DATABASE_URL:**
```env
DATABASE_URL="mongodb://signox_user:your_secure_password@192.168.1.232:27017/signox_db?authSource=signox_db"
```

### Use Static IP

Configure your server with a static IP address to avoid connection issues when DHCP lease expires.

### Backup Strategy

Set up regular MongoDB backups:
```bash
# Create backup script
mongodump --uri="mongodb://localhost:27017/signox_db" --out=/path/to/backup/$(date +%Y%m%d)
```

---

## Quick Reference

### Important Files to Update

1. **Backend:** `signoxc/signox_backend/.env`
   - `DATABASE_URL` → Local MongoDB connection string
   - `CORS_ORIGIN` → Add your LAN IPs

2. **Frontend:** `signoxc/signox_frontend/.env`
   - `NEXT_PUBLIC_API_URL` → Backend LAN IP

3. **Android Player:** `signoxc/signox-android-player/app/src/main/java/com/signox/player/data/ApiService.kt`
   - `BASE_URL` → Backend LAN IP

4. **Tizen Player:** `signoxc/tizen-player/js/config.js`
   - `API_BASE_URL` → Backend LAN IP

### Common Commands

```bash
# Start MongoDB
sudo systemctl start mongod

# Start Backend
cd signoxc/signox_backend && npm run dev

# Start Frontend
cd signoxc/signox_frontend && npm run dev

# Check MongoDB status
sudo systemctl status mongod

# View MongoDB logs
sudo tail -f /var/log/mongodb/mongod.log

# Connect to MongoDB shell
mongosh mongodb://localhost:27017/signox_db
```

---

## Need Help?

If you encounter any issues:
1. Check the troubleshooting section above
2. Review the logs (MongoDB, Backend, Frontend)
3. Verify all IP addresses are correct
4. Ensure firewall rules are properly configured
5. Test connectivity between devices
