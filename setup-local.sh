#!/bin/bash

# SignoX Local Setup Script
# This script helps configure the project for local MongoDB setup

set -e

echo "🚀 SignoX Local Setup Script"
echo "=============================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Get server IP address
echo "📡 Detecting network configuration..."
SERVER_IP=$(hostname -I | awk '{print $1}')
if [ -z "$SERVER_IP" ]; then
    SERVER_IP=$(ip route get 1 | awk '{print $7;exit}')
fi

if [ -z "$SERVER_IP" ]; then
    print_warning "Could not auto-detect IP address"
    read -p "Enter your server's LAN IP address (e.g., 192.168.1.232): " SERVER_IP
else
    print_success "Detected IP address: $SERVER_IP"
    read -p "Is this correct? (y/n) [y]: " CONFIRM_IP
    CONFIRM_IP=${CONFIRM_IP:-y}
    if [ "$CONFIRM_IP" != "y" ]; then
        read -p "Enter your server's LAN IP address: " SERVER_IP
    fi
fi

echo ""
echo "🔧 Configuration Summary:"
echo "   Server IP: $SERVER_IP"
echo "   MongoDB: mongodb://$SERVER_IP:27017/signox_db"
echo "   Backend: http://$SERVER_IP:5000"
echo "   Frontend: http://$SERVER_IP:3000"
echo ""

read -p "Proceed with setup? (y/n) [y]: " PROCEED
PROCEED=${PROCEED:-y}
if [ "$PROCEED" != "y" ]; then
    echo "Setup cancelled."
    exit 0
fi

echo ""
echo "📝 Step 1: Configuring Backend..."

# Backup existing .env
if [ -f "signox_backend/.env" ]; then
    cp signox_backend/.env signox_backend/.env.backup
    print_success "Backed up existing .env to .env.backup"
fi

# Update backend .env
cat > signox_backend/.env << EOF
# Local MongoDB Connection
DATABASE_URL="mongodb://$SERVER_IP:27017/signox_db"

JWT_SECRET=35a0dbc5be739b9f7e3525383c05e943e8084a1cf2a602c7cf1ed3a552b48b6e
PORT=5000
HOST=0.0.0.0
NODE_ENV=development

# CORS Configuration - Add your LAN IPs here
CORS_ORIGIN=http://localhost:3000,http://$SERVER_IP:3000,http://127.0.0.1:3000

TIMEZONE=Asia/Kolkata
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX=200
AUTH_RATE_LIMIT_MAX=200
MAX_LOGIN_ATTEMPTS=50
LOCKOUT_TIME=900000

# Local Storage (not using S3)
USE_S3_FOR_PLAYER_APPS=false
USE_S3_FOR_MEDIA=false
EOF

print_success "Backend .env configured"

echo ""
echo "📝 Step 2: Configuring Frontend..."

# Backup existing frontend .env
if [ -f "signox_frontend/.env" ]; then
    cp signox_frontend/.env signox_frontend/.env.backup
    print_success "Backed up existing frontend .env to .env.backup"
fi

# Update frontend .env
cat > signox_frontend/.env << EOF
NEXT_PUBLIC_API_URL=http://$SERVER_IP:5000/api
EOF

print_success "Frontend .env configured"

echo ""
echo "📝 Step 3: Installing Backend Dependencies..."
cd signox_backend
if npm install; then
    print_success "Backend dependencies installed"
else
    print_error "Failed to install backend dependencies"
    exit 1
fi

echo ""
echo "📝 Step 4: Generating Prisma Client..."
if npx prisma generate; then
    print_success "Prisma client generated"
else
    print_error "Failed to generate Prisma client"
    exit 1
fi

echo ""
echo "📝 Step 5: Checking MongoDB Connection..."
print_info "Attempting to connect to MongoDB at $SERVER_IP:27017..."

# Try to push schema to MongoDB
if npx prisma db push --skip-generate; then
    print_success "Successfully connected to MongoDB and pushed schema"
else
    print_error "Failed to connect to MongoDB"
    print_warning "Please ensure:"
    echo "   1. MongoDB is installed and running"
    echo "   2. MongoDB is configured to accept connections from $SERVER_IP"
    echo "   3. Firewall allows port 27017"
    echo ""
    echo "   To configure MongoDB for LAN access, edit /etc/mongod.conf:"
    echo "   net:"
    echo "     bindIp: 0.0.0.0"
    echo ""
    echo "   Then restart MongoDB: sudo systemctl restart mongod"
    exit 1
fi

echo ""
echo "📝 Step 6: Seeding Admin User..."
if node scripts/seedAdmin.js; then
    print_success "Admin user created"
    echo ""
    echo "   📧 Email: admin@signox.com"
    echo "   🔑 Password: Admin@123"
else
    print_warning "Admin user may already exist or seeding failed"
fi

cd ..

echo ""
echo "📝 Step 7: Installing Frontend Dependencies..."
cd signox_frontend
if npm install; then
    print_success "Frontend dependencies installed"
else
    print_error "Failed to install frontend dependencies"
    exit 1
fi

cd ..

echo ""
echo "✅ Setup Complete!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🎉 Your SignoX system is configured for local network access!"
echo ""
echo "📋 Next Steps:"
echo ""
echo "1. Start the Backend Server:"
echo "   cd signox_backend"
echo "   npm run dev"
echo ""
echo "2. Start the Frontend (in a new terminal):"
echo "   cd signox_frontend"
echo "   npm run dev"
echo ""
echo "3. Access the application:"
echo "   • From this machine: http://localhost:3000"
echo "   • From other devices: http://$SERVER_IP:3000"
echo ""
echo "4. Login with:"
echo "   • Email: admin@signox.com"
echo "   • Password: Admin@123"
echo ""
echo "📱 For Android/Tizen Players:"
echo "   Update the API endpoint to: http://$SERVER_IP:5000/api"
echo ""
echo "🔥 Firewall Configuration:"
echo "   sudo ufw allow 27017/tcp  # MongoDB"
echo "   sudo ufw allow 5000/tcp   # Backend API"
echo "   sudo ufw allow 3000/tcp   # Frontend (optional)"
echo ""
echo "📖 For detailed instructions, see: LOCAL_SETUP_GUIDE.md"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
