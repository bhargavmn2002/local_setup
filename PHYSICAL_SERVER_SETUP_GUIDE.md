# SignoX Physical Server Setup Guide

## 🖥️ Server Requirements

### Minimum Hardware Specifications
- **CPU**: 4 cores (Intel i5 or AMD Ryzen 5 equivalent)
- **RAM**: 8GB (16GB recommended for production)
- **Storage**: 500GB SSD (1TB+ recommended)
- **Network**: Gigabit Ethernet
- **GPU**: Optional (for video processing acceleration)

### Recommended Production Specifications
- **CPU**: 8+ cores (Intel i7/Xeon or AMD Ryzen 7/EPYC)
- **RAM**: 32GB+ 
- **Storage**: 
  - **System**: 500GB NVMe SSD (OS and applications)
  - **Media**: 4TB+ HDD/SSD RAID 1 (media file storage)
  - **Backup**: 8TB+ external storage (backup drives)
- **Network**: Dual Gigabit NICs (for redundancy)
- **GPU**: NVIDIA GPU (for FFmpeg hardware acceleration)

## 🐧 Operating System Setup

### Ubuntu Server 22.04 LTS (Recommended)

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install essential packages
sudo apt install -y curl wget git vim htop tree unzip software-properties-common

# Install build tools
sudo apt install -y build-essential python3-pip
```

## 📦 Core Dependencies Installation

### 1. Node.js (v18+ LTS)
```bash
# Install Node.js via NodeSource
curl -fsSL https://deb.nodesource.com/setup_lts.x | sudo -E bash -
sudo apt install -y nodejs

# Verify installation
node --version  # Should be v18+
npm --version
```

### 2. MongoDB (Database)
```bash
# Import MongoDB GPG key
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Install MongoDB
sudo apt update
sudo apt install -y mongodb-org

# Start and enable MongoDB
sudo systemctl start mongod
sudo systemctl enable mongod

# Verify installation
sudo systemctl status mongod
```

### 3. Redis (Caching)
```bash
# Install Redis
sudo apt install -y redis-server

# Configure Redis for production
sudo nano /etc/redis/redis.conf
# Uncomment and set: maxmemory 2gb
# Uncomment and set: maxmemory-policy allkeys-lru

# Start and enable Redis
sudo systemctl start redis-server
sudo systemctl enable redis-server

# Test Redis
redis-cli ping  # Should return PONG
```

### 4. Nginx (Reverse Proxy & Load Balancer)
```bash
# Install Nginx
sudo apt install -y nginx

# Start and enable Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 5. PM2 (Process Manager)
```bash
# Install PM2 globally
sudo npm install -g pm2

# Setup PM2 startup script
pm2 startup
sudo env PATH=$PATH:/usr/bin /usr/lib/node_modules/pm2/bin/pm2 startup systemd -u $USER --hp $HOME
```

### 6. FFmpeg (Media Processing)
```bash
# Install FFmpeg with all codecs
sudo apt install -y ffmpeg

# Verify installation
ffmpeg -version
ffprobe -version
```

## 🔐 Security Setup

### 1. Firewall Configuration
```bash
# Enable UFW firewall
sudo ufw enable

# Allow SSH (change port if needed)
sudo ufw allow 22/tcp

# Allow HTTP and HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Allow custom ports for development
sudo ufw allow 3000/tcp  # Frontend dev
sudo ufw allow 5000/tcp  # Backend dev
sudo ufw allow 5443/tcp  # Backend HTTPS

# Check firewall status
sudo ufw status
```

### 2. SSL Certificate Setup
```bash
# Install Certbot for Let's Encrypt
sudo apt install -y certbot python3-certbot-nginx

# Generate SSL certificate (replace with your domain)
sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com

# Auto-renewal setup
sudo crontab -e
# Add: 0 12 * * * /usr/bin/certbot renew --quiet
```

### 3. User Setup
```bash
# Create signox user
sudo adduser signox
sudo usermod -aG sudo signox

# Switch to signox user
su - signox
```

## � Local Storage Setup

### 1. Disk Partitioning Strategy
```bash
# Check available disks
sudo fdisk -l
lsblk

# Example partition scheme:
# /dev/sda1 - 500GB - System (OS, applications)
# /dev/sdb1 - 4TB - Media storage (RAID 1)
# /dev/sdc1 - 4TB - Media storage (RAID 1 mirror)
# /dev/sdd1 - 8TB - Backup storage
```

### 2. RAID Setup for Media Storage (Optional but Recommended)
```bash
# Install mdadm for software RAID
sudo apt install -y mdadm

# Create RAID 1 array for media storage
sudo mdadm --create --verbose /dev/md0 --level=1 --raid-devices=2 /dev/sdb1 /dev/sdc1

# Format the RAID array
sudo mkfs.ext4 /dev/md0

# Create mount point
sudo mkdir -p /media/signox-storage

# Mount the RAID array
sudo mount /dev/md0 /media/signox-storage

# Add to fstab for permanent mounting
echo '/dev/md0 /media/signox-storage ext4 defaults 0 2' | sudo tee -a /etc/fstab

# Set ownership
sudo chown -R signox:signox /media/signox-storage
```

### 3. Directory Structure Setup
```bash
# Create media storage directories
sudo mkdir -p /media/signox-storage/{uploads,backups,logs,temp}
sudo mkdir -p /media/signox-storage/uploads/{images,videos,thumbnails,optimized}

# Create symlinks to project directories
ln -s /media/signox-storage/uploads /home/signox/signox-project/signox_backend/public/uploads
ln -s /media/signox-storage/backups /home/signox/backups
ln -s /media/signox-storage/logs /home/signox/signox-project/logs

# Set proper permissions
sudo chown -R signox:signox /media/signox-storage
sudo chmod -R 755 /media/signox-storage
```

## 📁 Project Deployment
### 1. Clone Repository
cd /home/signox
git clone https://github.com/yourusername/signox-project.git
cd signox-project

# Set proper permissions
sudo chown -R signox:signox /home/signox/signox-project
```

### 2. Backend Setup
```bash
cd signox_backend

# Install dependencies
npm install

# Install Prisma CLI
npm install -g prisma

# Setup environment variables
cp .env.example .env
nano .env
```

### 3. Frontend Setup
```bash
cd ../signox_frontend

# Install dependencies
npm install

# Build for production
npm run build
```

## ⚙️ Environment Configuration

### Backend Environment (.env)
```bash
# Database
DATABASE_URL="mongodb://127.0.0.1:27017/signox_production"

# Security
JWT_SECRET="your-super-secure-jwt-secret-minimum-32-characters"
MAX_LOGIN_ATTEMPTS=5
LOCKOUT_TIME=900000

# Server
PORT=5000
HOST=0.0.0.0
NODE_ENV=production

# CORS (your domain)
CORS_ORIGIN=https://yourdomain.com,https://www.yourdomain.com

# HTTPS
ENABLE_HTTPS=true
SSL_KEY_PATH=/etc/letsencrypt/live/yourdomain.com/privkey.pem
SSL_CERT_PATH=/etc/letsencrypt/live/yourdomain.com/fullchain.pem
HTTPS_PORT=5443

# Redis
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_DB=0

# Local Media Storage (Physical Server Only)
USE_S3_FOR_MEDIA=false
USE_S3_FOR_PLAYER_APPS=false

# Local Storage Paths
MEDIA_STORAGE_PATH=/media/signox-storage/uploads
BACKUP_STORAGE_PATH=/media/signox-storage/backups
TEMP_STORAGE_PATH=/media/signox-storage/temp

# Media Processing
ENABLE_IMAGE_OPTIMIZATION=true
ENABLE_VIDEO_THUMBNAILS=true
MAX_FILE_SIZE=500MB
ALLOWED_IMAGE_TYPES=jpg,jpeg,png,gif,webp
ALLOWED_VIDEO_TYPES=mp4,webm,mov,avi

# Rate Limiting
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX=1000
AUTH_RATE_LIMIT_MAX=50
```

### Frontend Environment (.env.local)
```bash
NEXT_PUBLIC_API_URL=https://yourdomain.com/api
NODE_ENV=production
```

## � Local Media Processing Setup

### 1. FFmpeg Configuration for Hardware Acceleration
```bash
# Check for NVIDIA GPU support
nvidia-smi

# Install NVIDIA drivers and CUDA (if GPU available)
sudo apt install -y nvidia-driver-470 nvidia-cuda-toolkit

# Verify FFmpeg hardware acceleration support
ffmpeg -hwaccels

# Test hardware encoding
ffmpeg -f lavfi -i testsrc=duration=10:size=1920x1080:rate=30 -c:v h264_nvenc test_hw.mp4
```

### 2. Image Processing Optimization
```bash
# Install additional image processing tools
sudo apt install -y imagemagick webp

# Configure ImageMagick for large files
sudo nano /etc/ImageMagick-6/policy.xml

# Increase limits:
# <policy domain="resource" name="memory" value="2GiB"/>
# <policy domain="resource" name="map" value="4GiB"/>
# <policy domain="resource" name="disk" value="8GiB"/>
```

### 3. Storage Monitoring Setup
```bash
# Install storage monitoring tools
sudo apt install -y smartmontools ncdu

# Setup disk health monitoring
sudo smartctl -a /dev/sda  # Check disk health
sudo smartctl -t short /dev/sda  # Run short test

# Create disk monitoring script
sudo nano /home/signox/scripts/monitor-storage.sh
```

Create storage monitoring script:
```bash
#!/bin/bash
# Storage monitoring script

STORAGE_PATH="/media/signox-storage"
ALERT_THRESHOLD=85  # Alert when 85% full
EMAIL="admin@yourdomain.com"

# Check disk usage
USAGE=$(df "$STORAGE_PATH" | awk 'NR==2 {print $5}' | sed 's/%//')

if [ "$USAGE" -gt "$ALERT_THRESHOLD" ]; then
    echo "WARNING: Storage usage is ${USAGE}% on $STORAGE_PATH" | mail -s "Storage Alert" "$EMAIL"
fi

# Check RAID status (if using RAID)
if [ -f /proc/mdstat ]; then
    RAID_STATUS=$(cat /proc/mdstat | grep -E "(clean|active)")
    if [ -z "$RAID_STATUS" ]; then
        echo "WARNING: RAID array may have issues" | mail -s "RAID Alert" "$EMAIL"
    fi
fi

# Log storage stats
echo "$(date): Storage usage: ${USAGE}%" >> /var/log/signox-storage.log
```

## 🗄️ Database Setup
### 1. MongoDB Configuration
mongosh
use admin
db.createUser({
  user: "admin",
  pwd: "secure_password_here",
  roles: ["userAdminAnyDatabase", "dbAdminAnyDatabase", "readWriteAnyDatabase"]
})

# Create SignoX database and user
use signox_production
db.createUser({
  user: "signox_user",
  pwd: "signox_secure_password",
  roles: ["readWrite"]
})
exit
```

### 2. Enable MongoDB Authentication
```bash
# Edit MongoDB config
sudo nano /etc/mongod.conf

# Add security section:
security:
  authorization: enabled

# Restart MongoDB
sudo systemctl restart mongod
```

### 3. Initialize Database Schema
```bash
cd /home/signox/signox-project/signox_backend

# Generate Prisma client
npx prisma generate

# Deploy database schema
npx prisma db push

# Seed initial data (optional)
node scripts/seedAdmin.js
```

## 🚀 Application Deployment

### 1. PM2 Configuration
Create `/home/signox/signox-project/ecosystem.config.js`:

```javascript
module.exports = {
  apps: [
    {
      name: 'signox-backend',
      script: './signox_backend/src/server.js',
      cwd: '/home/signox/signox-project',
      instances: 2,
      exec_mode: 'cluster',
      env: {
        NODE_ENV: 'production',
        PORT: 5000
      },
      error_file: './logs/backend-error.log',
      out_file: './logs/backend-out.log',
      log_file: './logs/backend-combined.log',
      time: true,
      max_memory_restart: '1G',
      node_args: '--max-old-space-size=2048'
    },
    {
      name: 'signox-frontend',
      script: 'npm',
      args: 'start',
      cwd: '/home/signox/signox-project/signox_frontend',
      instances: 1,
      env: {
        NODE_ENV: 'production',
        PORT: 3000
      },
      error_file: './logs/frontend-error.log',
      out_file: './logs/frontend-out.log',
      log_file: './logs/frontend-combined.log',
      time: true
    }
  ]
};
```

### 2. Start Applications
```bash
# Create logs directory
mkdir -p /home/signox/signox-project/logs

# Start applications with PM2
cd /home/signox/signox-project
pm2 start ecosystem.config.js

# Save PM2 configuration
pm2 save

# Check status
pm2 status
pm2 logs
```

## 🌐 Nginx Configuration

Create `/etc/nginx/sites-available/signox`:

```nginx
# Rate limiting
limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/s;

# Upstream servers
upstream backend {
    server 127.0.0.1:5000;
    keepalive 32;
}

upstream frontend {
    server 127.0.0.1:3000;
    keepalive 32;
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

# Main HTTPS server
server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    # SSL Configuration
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/javascript application/xml+rss application/json;

    # Client max body size (for file uploads)
    client_max_body_size 500M;

    # API routes (Backend)
    location /api/ {
        limit_req zone=api burst=20 nodelay;
        
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }

    # Auth routes (stricter rate limiting)
    location /api/auth/ {
        limit_req zone=auth burst=10 nodelay;
        
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Media uploads (longer timeout for large files)
    location /uploads/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 600s;
        proxy_send_timeout 600s;
        client_max_body_size 500M;
        
        # Cache media files
        expires 1h;
        add_header Cache-Control "public, immutable";
        
        # Enable range requests for video streaming
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }

    # Downloads (APK, WGT files)
    location /downloads/ {
        proxy_pass http://backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Cache downloads
        expires 24h;
        add_header Cache-Control "public";
    }

    # Frontend (Next.js)
    location / {
        proxy_pass http://frontend;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }
}
```

Enable the site:
```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/signox /etc/nginx/sites-enabled/

# Remove default site
sudo rm /etc/nginx/sites-enabled/default

# Test configuration
sudo nginx -t

# Restart Nginx
sudo systemctl restart nginx
```

## 📊 Local Monitoring & Analytics

### 1. Storage Usage Dashboard
Create `/home/signox/scripts/storage-dashboard.sh`:

```bash
#!/bin/bash
# Storage usage dashboard

echo "=== SignoX Storage Dashboard ==="
echo "Generated: $(date)"
echo ""

# System storage
echo "=== System Storage ==="
df -h / | tail -1 | awk '{print "Used: " $3 " / " $2 " (" $5 ")"}'

# Media storage
echo ""
echo "=== Media Storage ==="
df -h /media/signox-storage | tail -1 | awk '{print "Used: " $3 " / " $2 " (" $5 ")"}'

# Media file counts
echo ""
echo "=== Media File Statistics ==="
echo "Images: $(find /media/signox-storage/uploads/images -type f 2>/dev/null | wc -l)"
echo "Videos: $(find /media/signox-storage/uploads/videos -type f 2>/dev/null | wc -l)"
echo "Thumbnails: $(find /media/signox-storage/uploads/thumbnails -type f 2>/dev/null | wc -l)"

# Database size
echo ""
echo "=== Database Statistics ==="
mongo_size=$(du -sh /var/lib/mongodb 2>/dev/null | cut -f1)
echo "MongoDB size: ${mongo_size:-N/A}"

# Backup status
echo ""
echo "=== Backup Status ==="
latest_db_backup=$(ls -t /media/signox-storage/backups/mongodb_*.tar.gz 2>/dev/null | head -1)
latest_media_backup=$(ls -t /media/signox-storage/backups/daily/media_*.tar.gz 2>/dev/null | head -1)

if [ -n "$latest_db_backup" ]; then
    echo "Latest DB backup: $(basename $latest_db_backup)"
else
    echo "Latest DB backup: None found"
fi

if [ -n "$latest_media_backup" ]; then
    echo "Latest media backup: $(basename $latest_media_backup)"
else
    echo "Latest media backup: None found"
fi

# RAID status (if applicable)
if [ -f /proc/mdstat ]; then
    echo ""
    echo "=== RAID Status ==="
    cat /proc/mdstat | grep -E "(md[0-9]+|active|clean)"
fi

echo ""
echo "=== System Load ==="
uptime
```

### 2. Performance Monitoring
```bash
# Install system monitoring tools
sudo apt install -y htop iotop nethogs vnstat

# Setup network monitoring
sudo vnstat -u -i eth0  # Replace eth0 with your interface

# Create performance monitoring script
sudo nano /home/signox/scripts/performance-monitor.sh
```

Performance monitoring script:
```bash
#!/bin/bash
LOG_FILE="/media/signox-storage/logs/performance.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

# CPU usage
CPU_USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)

# Memory usage
MEM_USAGE=$(free | grep Mem | awk '{printf "%.1f", ($3/$2) * 100.0}')

# Disk I/O
DISK_IO=$(iostat -d 1 2 | tail -1 | awk '{print $4}')

# Network usage
NET_RX=$(cat /sys/class/net/eth0/statistics/rx_bytes)
NET_TX=$(cat /sys/class/net/eth0/statistics/tx_bytes)

# Log performance metrics
echo "$DATE,CPU:${CPU_USAGE}%,MEM:${MEM_USAGE}%,DISK_IO:${DISK_IO},NET_RX:${NET_RX},NET_TX:${NET_TX}" >> $LOG_FILE

# Alert if CPU or memory usage is high
if (( $(echo "$CPU_USAGE > 80" | bc -l) )) || (( $(echo "$MEM_USAGE > 85" | bc -l) )); then
    echo "High resource usage detected - CPU: ${CPU_USAGE}%, Memory: ${MEM_USAGE}%" | mail -s "Performance Alert" admin@yourdomain.com
fi
```

## 📊 Monitoring & Logging
### 1. Log Rotation
```bash
# Create logrotate config

sudo nano /etc/logrotate.d/signox

# Add content:
/media/signox-storage/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    create 644 signox signox
    postrotate
        pm2 reloadLogs
    endscript
}
```

### 2. System Monitoring
```bash
# Install monitoring tools
sudo apt install -y htop iotop nethogs bc mailutils

# Setup automated monitoring
chmod +x /home/signox/scripts/performance-monitor.sh
chmod +x /home/signox/scripts/storage-dashboard.sh

# Add to cron for regular monitoring
crontab -e

# Add monitoring schedules:
*/5 * * * * /home/signox/scripts/performance-monitor.sh
0 8 * * * /home/signox/scripts/storage-dashboard.sh | mail -s "Daily Storage Report" admin@yourdomain.com
```

### 3. Local Web Dashboard (Optional)
Create a simple web dashboard for monitoring:

```bash
# Create dashboard directory
mkdir -p /home/signox/dashboard
cd /home/signox/dashboard

# Create simple HTML dashboard
cat > index.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>SignoX Server Dashboard</title>
    <meta http-equiv="refresh" content="30">
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .metric { background: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 5px; }
        .alert { background: #ffebee; border-left: 4px solid #f44336; }
        .ok { background: #e8f5e8; border-left: 4px solid #4caf50; }
    </style>
</head>
<body>
    <h1>SignoX Server Dashboard</h1>
    <div id="metrics"></div>
    
    <script>
        function updateMetrics() {
            fetch('/api/metrics')
                .then(response => response.json())
                .then(data => {
                    document.getElementById('metrics').innerHTML = data.html;
                });
        }
        
        updateMetrics();
        setInterval(updateMetrics, 30000);
    </script>
</body>
</html>
EOF

# Create simple metrics API
cat > server.js << 'EOF'
const express = require('express');
const { exec } = require('child_process');
const path = require('path');

const app = express();
app.use(express.static('.'));

app.get('/api/metrics', (req, res) => {
    exec('/home/signox/scripts/storage-dashboard.sh', (error, stdout) => {
        if (error) {
            return res.json({ html: '<div class="metric alert">Error loading metrics</div>' });
        }
        
        const html = stdout.split('\n')
            .filter(line => line.trim())
            .map(line => `<div class="metric ok">${line}</div>`)
            .join('');
            
        res.json({ html });
    });
});

app.listen(8080, () => {
    console.log('Dashboard running on http://localhost:8080');
});
EOF

# Install dependencies and start dashboard
npm init -y
npm install express
pm2 start server.js --name signox-dashboard
```

## 🔄 Backup Strategy (Physical Server)

### 1. Database Backup Script (Local Storage)
Create `/home/signox/scripts/backup-db.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/media/signox-storage/backups"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="signox_production"

# Create backup directory
mkdir -p $BACKUP_DIR

# MongoDB backup
mongodump --db $DB_NAME --out $BACKUP_DIR/mongodb_$DATE

# Compress backup
tar -czf $BACKUP_DIR/mongodb_$DATE.tar.gz -C $BACKUP_DIR mongodb_$DATE
rm -rf $BACKUP_DIR/mongodb_$DATE

# Keep only last 14 days of backups (local storage)
find $BACKUP_DIR -name "mongodb_*.tar.gz" -mtime +14 -delete

echo "Database backup completed: mongodb_$DATE.tar.gz"
```

### 2. Media Files Backup (Local Storage)
```bash
#!/bin/bash
BACKUP_DIR="/media/signox-storage/backups"
DATE=$(date +%Y%m%d_%H%M%S)
MEDIA_DIR="/media/signox-storage/uploads"
EXTERNAL_BACKUP="/mnt/external-backup"  # External drive mount point

# Create backup directory
mkdir -p $BACKUP_DIR/daily

# Backup media files locally
tar -czf $BACKUP_DIR/daily/media_$DATE.tar.gz -C $MEDIA_DIR .

# Copy to external drive (if mounted)
if mountpoint -q $EXTERNAL_BACKUP; then
    cp $BACKUP_DIR/daily/media_$DATE.tar.gz $EXTERNAL_BACKUP/
    echo "Media backup copied to external drive: media_$DATE.tar.gz"
fi

# Keep only last 7 days of local backups
find $BACKUP_DIR/daily -name "media_*.tar.gz" -mtime +7 -delete

# Keep only last 30 days on external drive
if mountpoint -q $EXTERNAL_BACKUP; then
    find $EXTERNAL_BACKUP -name "media_*.tar.gz" -mtime +30 -delete
fi

echo "Media backup completed: media_$DATE.tar.gz"
```

### 3. System Configuration Backup
```bash
#!/bin/bash
BACKUP_DIR="/media/signox-storage/backups"
DATE=$(date +%Y%m%d_%H%M%S)

# Backup system configurations
mkdir -p $BACKUP_DIR/config_$DATE

# Backup important config files
cp -r /etc/nginx $BACKUP_DIR/config_$DATE/
cp -r /etc/mongodb $BACKUP_DIR/config_$DATE/
cp /etc/redis/redis.conf $BACKUP_DIR/config_$DATE/
cp /home/signox/signox-project/.env* $BACKUP_DIR/config_$DATE/
cp /home/signox/signox-project/ecosystem.config.js $BACKUP_DIR/config_$DATE/

# Compress config backup
tar -czf $BACKUP_DIR/config_$DATE.tar.gz -C $BACKUP_DIR config_$DATE
rm -rf $BACKUP_DIR/config_$DATE

# Keep only last 30 days of config backups
find $BACKUP_DIR -name "config_*.tar.gz" -mtime +30 -delete

echo "Configuration backup completed: config_$DATE.tar.gz"
```

### 3. Automated Backups
```bash
# Make scripts executable
chmod +x /home/signox/scripts/*.sh

# Setup cron jobs for automated backups
crontab -e

# Add backup schedules:
0 2 * * * /home/signox/scripts/backup-db.sh
0 3 * * * /home/signox/scripts/backup-media.sh
0 4 * * * /home/signox/scripts/backup-config.sh
*/15 * * * * /home/signox/scripts/monitor-storage.sh

# Weekly full system backup (external drive)
0 1 * * 0 /home/signox/scripts/full-backup.sh
```

### 4. External Drive Setup for Offsite Backups
```bash
# Format external drive (replace /dev/sdX with your drive)
sudo mkfs.ext4 /dev/sdX1

# Create mount point
sudo mkdir -p /mnt/external-backup

# Add to fstab for manual mounting
echo '/dev/sdX1 /mnt/external-backup ext4 noauto,user 0 0' | sudo tee -a /etc/fstab

# Mount external drive
sudo mount /mnt/external-backup

# Create full backup script
sudo nano /home/signox/scripts/full-backup.sh
```

Full backup script:
```bash
#!/bin/bash
EXTERNAL_BACKUP="/mnt/external-backup"
DATE=$(date +%Y%m%d_%H%M%S)

# Check if external drive is mounted
if ! mountpoint -q $EXTERNAL_BACKUP; then
    echo "External backup drive not mounted. Attempting to mount..."
    sudo mount /mnt/external-backup
    
    if ! mountpoint -q $EXTERNAL_BACKUP; then
        echo "Failed to mount external backup drive. Backup aborted."
        exit 1
    fi
fi

# Create full system backup
mkdir -p $EXTERNAL_BACKUP/full-backups

# Backup database
/home/signox/scripts/backup-db.sh

# Backup media files
/home/signox/scripts/backup-media.sh

# Backup configurations
/home/signox/scripts/backup-config.sh

# Copy all backups to external drive
rsync -av /media/signox-storage/backups/ $EXTERNAL_BACKUP/full-backups/backup_$DATE/

echo "Full backup completed to external drive: backup_$DATE"

# Unmount external drive
sudo umount /mnt/external-backup
```

## 🚀 Local CDN & Media Optimization

### 1. Nginx Media Caching Setup
```bash
# Create cache directories
sudo mkdir -p /var/cache/nginx/media
sudo chown -R www-data:www-data /var/cache/nginx

# Add to Nginx configuration
sudo nano /etc/nginx/sites-available/signox
```

Add to the server block:
```nginx
# Media caching configuration
proxy_cache_path /var/cache/nginx/media levels=1:2 keys_zone=media_cache:100m max_size=10g inactive=60d use_temp_path=off;

server {
    # ... existing configuration ...
    
    # Static media files with aggressive caching
    location ~* \.(jpg|jpeg|png|gif|webp|mp4|webm|mov)$ {
        proxy_pass http://backend;
        proxy_cache media_cache;
        proxy_cache_valid 200 30d;
        proxy_cache_valid 404 1m;
        proxy_cache_use_stale error timeout updating http_500 http_502 http_503 http_504;
        proxy_cache_lock on;
        
        # Add cache headers
        add_header X-Cache-Status $upstream_cache_status;
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # Enable range requests for video streaming
        proxy_set_header Range $http_range;
        proxy_set_header If-Range $http_if_range;
    }
    
    # Thumbnails with shorter cache
    location /uploads/thumbnails/ {
        proxy_pass http://backend;
        proxy_cache media_cache;
        proxy_cache_valid 200 7d;
        expires 7d;
        add_header Cache-Control "public";
    }
}
```

### 2. Media Processing Pipeline
Create `/home/signox/scripts/process-media.sh`:

```bash
#!/bin/bash
# Media processing pipeline for uploaded files

MEDIA_DIR="/media/signox-storage/uploads"
TEMP_DIR="/media/signox-storage/temp"

process_image() {
    local input_file="$1"
    local output_dir="$2"
    local filename=$(basename "$input_file" | cut -d. -f1)
    
    # Create optimized versions
    convert "$input_file" -quality 85 -strip "$output_dir/optimized/${filename}_optimized.jpg"
    convert "$input_file" -quality 70 -resize 800x600 "$output_dir/optimized/${filename}_preview.jpg"
    convert "$input_file" -quality 60 -resize 200x150 "$output_dir/thumbnails/${filename}_thumb.jpg"
    
    # Create WebP version
    cwebp -q 80 "$input_file" -o "$output_dir/optimized/${filename}_optimized.webp"
    
    echo "Processed image: $filename"
}

process_video() {
    local input_file="$1"
    local output_dir="$2"
    local filename=$(basename "$input_file" | cut -d. -f1)
    
    # Generate thumbnail at 5 seconds
    ffmpeg -i "$input_file" -ss 00:00:05 -vframes 1 -q:v 2 "$output_dir/thumbnails/${filename}_thumb.jpg" -y
    
    # Create lower quality version for faster streaming (optional)
    ffmpeg -i "$input_file" -c:v libx264 -crf 28 -preset fast -c:a aac -b:a 128k "$output_dir/optimized/${filename}_compressed.mp4" -y
    
    echo "Processed video: $filename"
}

# Watch for new files and process them
inotifywait -m -r -e create --format '%w%f' "$MEDIA_DIR" | while read file; do
    if [[ "$file" =~ \.(jpg|jpeg|png|gif)$ ]]; then
        process_image "$file" "$MEDIA_DIR"
    elif [[ "$file" =~ \.(mp4|webm|mov|avi)$ ]]; then
        process_video "$file" "$MEDIA_DIR"
    fi
done
```

### 3. HLS Streaming Setup (for large videos)
```bash
# Install additional FFmpeg tools
sudo apt install -y ffmpeg

# Create HLS processing script
sudo nano /home/signox/scripts/create-hls.sh
```

HLS processing script:
```bash
#!/bin/bash
# Convert videos to HLS format for adaptive streaming

create_hls() {
    local input_file="$1"
    local output_dir="$2"
    local filename=$(basename "$input_file" | cut -d. -f1)
    
    # Create HLS directory
    mkdir -p "$output_dir/hls/$filename"
    
    # Generate HLS playlist with multiple quality levels
    ffmpeg -i "$input_file" \
        -filter_complex \
        "[0:v]split=3[v1][v2][v3]; \
         [v1]copy[v1out]; \
         [v2]scale=w=1280:h=720[v2out]; \
         [v3]scale=w=854:h=480[v3out]" \
        -map "[v1out]" -c:v:0 libx264 -x264-params "nal-hrd=cbr:force-cfr=1" -b:v:0 5M -maxrate:v:0 5M -minrate:v:0 5M -bufsize:v:0 10M -preset slow -g 48 -sc_threshold 0 -keyint_min 48 \
        -map "[v2out]" -c:v:1 libx264 -x264-params "nal-hrd=cbr:force-cfr=1" -b:v:1 3M -maxrate:v:1 3M -minrate:v:1 3M -bufsize:v:1 6M -preset slow -g 48 -sc_threshold 0 -keyint_min 48 \
        -map "[v3out]" -c:v:2 libx264 -x264-params "nal-hrd=cbr:force-cfr=1" -b:v:2 1M -maxrate:v:2 1M -minrate:v:2 1M -bufsize:v:2 2M -preset slow -g 48 -sc_threshold 0 -keyint_min 48 \
        -map a:0 -c:a:0 aac -b:a:0 96k -ac 2 \
        -map a:0 -c:a:1 aac -b:a:1 96k -ac 2 \
        -map a:0 -c:a:2 aac -b:a:2 48k -ac 2 \
        -f hls \
        -hls_time 2 \
        -hls_playlist_type vod \
        -hls_flags independent_segments \
        -hls_segment_type mpegts \
        -hls_segment_filename "$output_dir/hls/$filename/data%02d.ts" \
        -master_pl_name "$filename.m3u8" \
        -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2" "$output_dir/hls/$filename/stream_%v.m3u8"
    
    echo "Created HLS streams for: $filename"
}

# Usage: ./create-hls.sh input_video.mp4 /media/signox-storage/uploads
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <input_file> <output_directory>"
    exit 1
fi

create_hls "$1" "$2"
```

## 🚀 Deployment Script (Physical Server)

Create `/home/signox/scripts/deploy.sh`:

```bash
#!/bin/bash
set -e

echo "🚀 Starting SignoX deployment..."

# Navigate to project directory
cd /home/signox/signox-project

# Pull latest changes
echo "📥 Pulling latest changes..."
git pull origin main

# Backend deployment
echo "🔧 Updating backend..."
cd signox_backend
npm install --production
npx prisma generate
npx prisma db push

# Frontend deployment
echo "🎨 Building frontend..."
cd ../signox_frontend
npm install --production
npm run build

# Restart applications
echo "🔄 Restarting applications..."
cd ..
pm2 restart ecosystem.config.js

# Wait for applications to start
sleep 10

# Check application status
echo "✅ Checking application status..."
pm2 status

echo "🎉 Deployment completed successfully!"
```

## 📋 Maintenance Tasks

### Daily Tasks
```bash
# Check system resources
htop
df -h
free -h

# Check application logs
pm2 logs --lines 50

# Check Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### Weekly Tasks
```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Clean up old logs
sudo journalctl --vacuum-time=7d

# Check SSL certificate expiry
sudo certbot certificates
```

### Monthly Tasks
```bash
# Analyze database performance
mongosh --eval "db.runCommand({serverStatus: 1})"

# Clean up old backups
find /home/signox/backups -mtime +30 -delete

# Update Node.js dependencies
cd /home/signox/signox-project/signox_backend && npm audit
cd /home/signox/signox-project/signox_frontend && npm audit
```

## 🆘 Troubleshooting

### Common Issues

1. **Application won't start**
   ```bash
   # Check logs
   pm2 logs
   
   # Check ports
   sudo netstat -tlnp | grep :5000
   sudo netstat -tlnp | grep :3000
   ```

2. **Database connection issues**
   ```bash
   # Check MongoDB status
   sudo systemctl status mongod
   
   # Check MongoDB logs
   sudo tail -f /var/log/mongodb/mongod.log
   ```

3. **High memory usage**
   ```bash
   # Check memory usage
   free -h
   
   # Restart applications
   pm2 restart all
   ```

4. **SSL certificate issues**
   ```bash
   # Renew certificate
   sudo certbot renew
   
   # Test certificate
   sudo certbot certificates
   ```

## 🔧 Performance Optimization

### 1. MongoDB Optimization
```bash
# Edit MongoDB config
sudo nano /etc/mongod.conf

# Add performance settings:
storage:
  wiredTiger:
    engineConfig:
      cacheSizeGB: 4  # Adjust based on available RAM
```

### 2. Node.js Optimization
```bash
# Increase Node.js memory limit
export NODE_OPTIONS="--max-old-space-size=4096"
```

### 3. Nginx Optimization
```bash
# Edit Nginx config
sudo nano /etc/nginx/nginx.conf

# Optimize worker processes
worker_processes auto;
worker_connections 1024;

# Enable gzip compression
gzip on;
gzip_comp_level 6;
```

This comprehensive setup guide will help you deploy SignoX on a physical server with production-ready configuration, monitoring, and maintenance procedures.