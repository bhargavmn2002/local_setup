# SignoX User Limits Migration - Complete Implementation

## Overview
This document details all changes made to move resource limits from CLIENT_ADMIN level to USER_ADMIN level, allowing CLIENT_ADMINs to act as resellers with individual USER_ADMIN limits.

## 1. Database Schema Changes

### 1.1 Updated ClientProfile Model
**File**: `signox_backend/prisma/schema.prisma`

**BEFORE:**
```prisma
model ClientProfile {
  // ... other fields
  maxDisplays   Int       @default(10)
  maxUsers      Int       @default(5)
  maxStorageMB  Int       @default(25)
  maxMonthlyUsageMB    Int     @default(150)
  monthlyUploadedBytes BigInt  @default(0)
  usageQuotaResetDate  DateTime @default(now())
  billingDayOfMonth    Int     @default(1)
  // ... other fields
}
```

**AFTER:**
```prisma
model ClientProfile {
  // ... other fields
  // Removed all limit fields - limits now at USER_ADMIN level
  licenseExpiry DateTime?
  isActive      Boolean   @default(true)
  companyName  String?
  contactEmail String?
  contactPhone String?
  // ... other fields
}
```
### 1.2 New UserAdminProfile Model
**File**: `signox_backend/prisma/schema.prisma`

```prisma
model UserAdminProfile {
  id          String @id @default(auto()) @map("_id") @db.ObjectId
  userAdminId String @unique @db.ObjectId
  
  // License and limits (moved from ClientProfile)
  maxDisplays   Int       @default(10)
  maxUsers      Int       @default(5) // Max STAFF users this USER_ADMIN can create
  maxStorageMB  Int       @default(25)
  
  // Monthly usage limits (bandwidth/transfer quota)
  maxMonthlyUsageMB    Int     @default(150)
  monthlyUploadedBytes BigInt  @default(0)
  usageQuotaResetDate  DateTime @default(now())
  billingDayOfMonth    Int     @default(1)
  
  licenseExpiry DateTime?
  isActive      Boolean   @default(true)

  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  // Relations
  userAdmin User @relation(fields: [userAdminId], references: [id], onDelete: NoAction, onUpdate: NoAction)
}
```

### 1.3 Updated User Model
**File**: `signox_backend/prisma/schema.prisma`

**ADDED:**
```prisma
model User {
  // ... existing fields
  
  // User Admin profile with limits
  userAdminProfile UserAdminProfile? // One-to-one: User Admin has one profile with limits
  
  // ... rest of existing fields
}
```

## 2. Backend API Changes

### 2.1 User Controller Updates
**File**: `signox_backend/src/controllers/user.controller.js`

#### 2.1.1 Updated createUser Function
**Key Changes:**
- SUPER_ADMIN creates CLIENT_ADMIN without limits
- CLIENT_ADMIN creates USER_ADMIN with individual limits
- Added UserAdminProfile creation for USER_ADMIN users

```javascript
if (requester.role === 'CLIENT_ADMIN') {
  // Create USER_ADMIN + UserAdminProfile with limits
  const created = await prisma.$transaction(async (tx) => {
    const userAdmin = await tx.user.create({
      data: {
        email,
        password: hashedPassword,
        role: 'USER_ADMIN',
        isActive: true,
        managedByClientAdminId: requester.id,
      },
    });

    // Create UserAdminProfile with limits
    const profile = await tx.userAdminProfile.create({
      data: {
        userAdminId: userAdmin.id,
        maxDisplays: Number.isFinite(Number(maxDisplays)) ? Number(maxDisplays) : 10,
        maxUsers: Number.isFinite(Number(maxUsers)) ? Number(maxUsers) : 5,
        maxStorageMB: Number.isFinite(Number(maxStorageMB)) ? Number(maxStorageMB) : 25,
        maxMonthlyUsageMB: Number.isFinite(Number(maxMonthlyUsageMB)) ? Number(maxMonthlyUsageMB) : 150,
        licenseExpiry: licenseExpiry ? new Date(licenseExpiry) : null,
        isActive: true,
      },
    });

    return { userAdmin, profile };
  });
  // ... return response
}
```
#### 2.1.2 Updated listUsers Function
**Key Changes:**
- CLIENT_ADMIN now returns USER_ADMIN users with their userAdminProfile
- Added userAdminProfile to response data

```javascript
if (requester.role === 'CLIENT_ADMIN') {
  const userAdmins = await prisma.user.findMany({
    where: {
      role: 'USER_ADMIN',
      managedByClientAdminId: requester.id,
    },
    include: {
      userAdminProfile: true, // Include the new profile
    },
    orderBy: { createdAt: 'desc' },
  });

  return res.json({
    role: 'CLIENT_ADMIN',
    users: userAdmins.map((u) => ({
      id: u.id,
      email: u.email,
      role: u.role,
      isActive: u.isActive,
      createdAt: u.createdAt,
      userAdminProfile: u.userAdminProfile ? {
        ...u.userAdminProfile,
        monthlyUploadedBytes: Number(u.userAdminProfile.monthlyUploadedBytes || 0)
      } : null,
    })),
  });
}
```

#### 2.1.3 Updated deleteUser Function
**Key Changes:**
- Added UserAdminProfile deletion when deleting USER_ADMIN
- Updated cleanup logic to handle new profile structure

```javascript
// 9) Delete UserAdminProfile if exists
if (target.userAdminProfile) {
  await tx.userAdminProfile.delete({ where: { id: target.userAdminProfile.id } });
}
```

#### 2.1.4 Updated getAccountInfo Function
**Key Changes:**
- Now works for USER_ADMIN instead of CLIENT_ADMIN
- Uses userAdminProfile for limits instead of clientProfile

```javascript
exports.getAccountInfo = async (req, res) => {
  // Only USER_ADMIN has account info with limits now
  if (user.role !== 'USER_ADMIN' || !user.userAdminProfile) {
    return res.status(403).json({ message: 'Account information not available for this user type' });
  }

  // Count current usage
  const [displayCount, userCount] = await Promise.all([
    prisma.display.count({
      where: { managedByUserId: userId } // USER_ADMIN manages displays
    }),
    prisma.user.count({
      where: { createdByUserAdminId: userId } // USER_ADMIN creates STAFF
    })
  ]);

  res.json({
    companyName: user.managedByClientAdmin?.clientProfile?.companyName || 'Unknown Company',
    maxDisplays: user.userAdminProfile.maxDisplays,
    maxUsers: user.userAdminProfile.maxUsers,
    maxStorageMB: user.userAdminProfile.maxStorageMB,
    maxMonthlyUsageMB: user.userAdminProfile.maxMonthlyUsageMB,
    currentDisplays: displayCount,
    currentUsers: userCount,
    // ... other fields
  });
};
```

### 2.2 Authentication Controller Updates
**File**: `signox_backend/src/controllers/auth.controller.js`

#### 2.2.1 Updated User Query with UserAdminProfile
```javascript
const user = await prisma.user.findUnique({
  where: { email: email.toLowerCase().trim() },
  include: {
    clientProfile: true,
    userAdminProfile: true, // Added this
    managedByClientAdmin: {
      include: {
        clientProfile: true,
      },
    },
    createdByUserAdmin: {
      include: {
        userAdminProfile: true, // Added this
        managedByClientAdmin: {
          include: {
            clientProfile: true,
          },
        },
      },
    },
  },
});
```
#### 2.2.2 Enhanced License Checking for USER_ADMIN
```javascript
// Check if parent Client Admin is active (for User Admins)
if (user.role === 'USER_ADMIN' && user.managedByClientAdmin) {
  // Check organization-level license
  if (user.managedByClientAdmin.clientProfile) {
    if (!user.managedByClientAdmin.clientProfile.isActive) {
      return res.status(401).json({ message: 'Your organization license is suspended.' });
    }
    
    if (user.managedByClientAdmin.clientProfile.licenseExpiry && 
        new Date(user.managedByClientAdmin.clientProfile.licenseExpiry) < new Date()) {
      return res.status(401).json({ message: 'Your organization license has expired.' });
    }
  }

  // Check USER_ADMIN's own profile and license
  if (user.userAdminProfile) {
    if (!user.userAdminProfile.isActive) {
      return res.status(401).json({ message: 'Your account is suspended.' });
    }
    
    if (user.userAdminProfile.licenseExpiry && 
        new Date(user.userAdminProfile.licenseExpiry) < new Date()) {
      return res.status(401).json({ message: 'Your license has expired.' });
    }
  }
}
```

#### 2.2.3 Updated Response with UserAdminProfile
```javascript
return res.json({
  accessToken: token,
  user: {
    id: userResponse.id,
    email: userResponse.email,
    role: userResponse.role,
    staffRole: userResponse.staffRole,
    isActive: userResponse.isActive,
    clientProfile: clientProfileForResponse,
    userAdminProfile: userAdminProfileForResponse, // Added this
  },
});
```

### 2.3 Authentication Middleware Updates
**File**: `signox_backend/src/middleware/auth.middleware.js`

#### 2.3.1 Enhanced User Query
```javascript
const user = await prisma.user.findUnique({
  where: { id: decoded.id },
  include: {
    clientProfile: true,
    userAdminProfile: true, // Added this
    managedByClientAdmin: {
      include: { clientProfile: true },
    },
    createdByUserAdmin: {
      include: {
        userAdminProfile: true, // Added this
        managedByClientAdmin: {
          include: { clientProfile: true },
        },
      },
    },
  },
});
```

#### 2.3.2 Two-Level License Checking
```javascript
// Check organization-level license (CLIENT_ADMIN level)
if (clientProfileToCheck) {
  if (!clientProfileToCheck.isActive) {
    return res.status(403).json({
      message: 'Your organization license is suspended.',
    });
  }
  // ... expiry check
}

// Check user-level license (USER_ADMIN level)
if (userAdminProfileToCheck) {
  if (!userAdminProfileToCheck.isActive) {
    return res.status(403).json({
      message: 'Your account is suspended.',
    });
  }
  // ... expiry check
}
```

## 3. Frontend Changes

### 3.1 AuthContext Updates
**File**: `signox_frontend/src/contexts/AuthContext.tsx`

#### 3.1.1 Updated User Interface
```typescript
interface User {
  id: string;
  email: string;
  role: Role;
  staffRole?: StaffRole;
  isActive: boolean;
  managedByClientAdminId?: string | null;

  clientProfile?: {
    maxDisplays: number;
    maxUsers: number;
    licenseExpiry?: string;
    companyName?: string;
  };

  userAdminProfile?: { // Added this
    maxDisplays: number;
    maxUsers: number;
    maxStorageMB: number;
    maxMonthlyUsageMB: number;
    monthlyUploadedBytes: number;
    licenseExpiry?: string;
    isActive: boolean;
  };
}
```
### 3.2 CLIENT_ADMIN Users Page Updates
**File**: `signox_frontend/src/app/client/users/page.tsx`

#### 3.2.1 Enhanced Form State
```typescript
const [formData, setFormData] = useState({ 
  email: '', 
  password: '',
  maxDisplays: '10',
  maxUsers: '5',
  maxStorageMB: '25',
  maxMonthlyUsageMB: '150',
  licenseExpiry: ''
});
```

#### 3.2.2 Updated Create User Function
```typescript
const handleCreate = async (e: React.FormEvent) => {
  e.preventDefault();
  try {
    await api.post('/users', {
      email: formData.email,
      password: formData.password,
      maxDisplays: Number(formData.maxDisplays || 10),
      maxUsers: Number(formData.maxUsers || 5),
      maxStorageMB: Number(formData.maxStorageMB || 25),
      maxMonthlyUsageMB: Number(formData.maxMonthlyUsageMB || 150),
      licenseExpiry: formData.licenseExpiry || null,
    });
    // ... rest of function
  }
};
```

#### 3.2.3 Enhanced Form UI
```jsx
<DialogContent className="bg-white max-w-2xl">
  <DialogHeader>
    <DialogTitle className="text-2xl">Create New User Admin</DialogTitle>
  </DialogHeader>
  <form onSubmit={handleCreate} className="space-y-4">
    {/* Basic Info */}
    <div className="grid grid-cols-2 gap-4">
      <div className="space-y-2">
        <Label>Email Address</Label>
        <Input 
          required 
          type="email" 
          value={formData.email}
          onChange={e => setFormData({...formData, email: e.target.value})}
        />
      </div>
      <div className="space-y-2">
        <Label>Password</Label>
        <Input 
          required 
          type="password" 
          value={formData.password}
          onChange={e => setFormData({...formData, password: e.target.value})}
        />
      </div>
    </div>

    {/* Resource Limits Section */}
    <div className="border-t pt-4">
      <h3 className="text-lg font-semibold mb-3">Resource Limits</h3>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Max Displays</Label>
          <Input 
            type="number"
            min="0"
            value={formData.maxDisplays}
            onChange={e => setFormData({...formData, maxDisplays: e.target.value})}
          />
        </div>
        <div className="space-y-2">
          <Label>Max Staff Users</Label>
          <Input 
            type="number"
            min="0"
            value={formData.maxUsers}
            onChange={e => setFormData({...formData, maxUsers: e.target.value})}
          />
        </div>
        <div className="space-y-2">
          <Label>Storage Limit (MB)</Label>
          <Input 
            type="number"
            min="0"
            value={formData.maxStorageMB}
            onChange={e => setFormData({...formData, maxStorageMB: e.target.value})}
          />
        </div>
        <div className="space-y-2">
          <Label>Monthly Upload Limit (MB)</Label>
          <Input 
            type="number"
            min="0"
            value={formData.maxMonthlyUsageMB}
            onChange={e => setFormData({...formData, maxMonthlyUsageMB: e.target.value})}
          />
        </div>
      </div>
    </div>

    <div className="space-y-2">
      <Label>License Expiry (Optional)</Label>
      <Input 
        type="date"
        value={formData.licenseExpiry}
        onChange={e => setFormData({...formData, licenseExpiry: e.target.value})}
      />
    </div>
  </form>
</DialogContent>
```

#### 3.2.4 Updated Table with Limits Display
```jsx
<TableHeader>
  <TableRow>
    <TableHead>User (Email)</TableHead>
    <TableHead>Role</TableHead>
    <TableHead>Limits</TableHead> {/* Added this column */}
    <TableHead>Status</TableHead>
    <TableHead className="text-right">Actions</TableHead>
  </TableRow>
</TableHeader>
<TableBody>
  {users.map((u) => (
    <TableRow key={u.id}>
      <TableCell className="font-medium">{u.email}</TableCell>
      <TableCell><Badge variant="outline">User Admin</Badge></TableCell>
      <TableCell>
        {u.userAdminProfile ? (
          <div className="text-sm text-gray-600">
            <div>Displays: {u.userAdminProfile.maxDisplays}</div>
            <div>Storage: {u.userAdminProfile.maxStorageMB}MB</div>
          </div>
        ) : (
          <span className="text-gray-400">No limits set</span>
        )}
      </TableCell>
      {/* ... rest of row */}
    </TableRow>
  ))}
</TableBody>
```
### 3.3 SUPER_ADMIN Clients Page Updates
**File**: `signox_frontend/src/app/super-admin/clients/page.tsx`

#### 3.3.1 Simplified Form State (Removed Limits)
```typescript
// BEFORE
const [form, setForm] = useState({
  name: '',
  email: '',
  password: '',
  companyName: '',
  maxDisplays: '10',
  maxUsers: '5',
  maxStorageMB: '25',
  maxMonthlyUsageMB: '150',
  licenseExpiry: '',
});

// AFTER
const [form, setForm] = useState({
  name: '',
  email: '',
  password: '',
  companyName: '',
  licenseExpiry: '',
});
```

#### 3.3.2 Simplified Create Client Function
```typescript
// BEFORE
await api.post('/users', {
  email: form.email,
  password: form.password,
  companyName: form.companyName,
  maxDisplays: Number(form.maxDisplays || 10),
  maxUsers: Number(form.maxUsers || 5),
  maxStorageMB: Number(form.maxStorageMB || 25),
  maxMonthlyUsageMB: Number(form.maxMonthlyUsageMB || 150),
  licenseExpiry: form.licenseExpiry || null,
});

// AFTER
await api.post('/users', {
  email: form.email,
  password: form.password,
  companyName: form.companyName,
  licenseExpiry: form.licenseExpiry || null,
});
```

#### 3.3.3 Simplified Form UI (Removed Limit Fields)
```jsx
{/* BEFORE: Had complex grid with limit fields */}
<div className="grid gap-4 md:grid-cols-4">
  <div className="space-y-2">
    <Label>Max Displays</Label>
    <Input type="number" value={form.maxDisplays} />
  </div>
  {/* ... more limit fields */}
</div>

{/* AFTER: Simple license expiry field */}
<div className="space-y-2">
  <Label>License Expiry</Label>
  <Input
    type="date"
    value={form.licenseExpiry}
    onChange={(e) => setForm({ ...form, licenseExpiry: e.target.value })}
  />
  <p className="text-xs text-gray-500">Optional: Set organization license expiry</p>
</div>
```

### 3.4 Display Management Updates
**File**: `signox_frontend/src/app/user/displays/page.tsx`

#### 3.4.1 Updated Limit Checking Logic
```typescript
// BEFORE
const fetchDisplayLimits = async () => {
  try {
    // For USER_ADMIN, get their client admin's profile
    if (user?.role === 'USER_ADMIN' && user?.managedByClientAdminId) {
      const response = await api.get(`/users/client-admins`);
      const clientAdmins = response.data.clientAdmins || [];
      const clientAdmin = clientAdmins.find((ca: any) => ca.id === user.managedByClientAdminId);
      
      if (clientAdmin?.clientProfile) {
        const pairedDisplays = displays.filter(d => d.isPaired).length;
        setDisplayLimits({
          maxDisplays: clientAdmin.clientProfile.maxDisplays || 10,
          currentCount: pairedDisplays
        });
      }
    }
  } catch (error) {
    console.error('Failed to fetch display limits:', error);
  }
};

// AFTER
const fetchDisplayLimits = async () => {
  try {
    // For USER_ADMIN, check their own userAdminProfile limits
    if (user?.role === 'USER_ADMIN' && user?.userAdminProfile) {
      const pairedDisplays = displays.filter(d => d.isPaired).length;
      setDisplayLimits({
        maxDisplays: user.userAdminProfile.maxDisplays || 10,
        currentCount: pairedDisplays
      });
    }
    // For CLIENT_ADMIN, check their clientProfile limits (if they still have limits)
    else if (user?.role === 'CLIENT_ADMIN' && user?.clientProfile) {
      const pairedDisplays = displays.filter(d => d.isPaired).length;
      setDisplayLimits({
        maxDisplays: user.clientProfile.maxDisplays || 10,
        currentCount: pairedDisplays
      });
    }
    // For STAFF, we need to get their parent USER_ADMIN's limits
    else if (user?.role === 'STAFF') {
      const pairedDisplays = displays.filter(d => d.isPaired).length;
      setDisplayLimits({
        maxDisplays: 10, // Default limit for STAFF
        currentCount: pairedDisplays
      });
    }
  } catch (error) {
    console.error('Failed to fetch display limits:', error);
  }
};
```

## 4. Migration Scripts

### 4.1 Data Migration Script
**File**: `signox_backend/scripts/migrateToUserAdminLimits.js`

```javascript
const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {
  console.log('🚀 Starting migration: Moving limits from CLIENT_ADMIN to USER_ADMIN level...');

  // Find all USER_ADMIN users that don't have a UserAdminProfile yet
  const userAdmins = await prisma.user.findMany({
    where: {
      role: 'USER_ADMIN',
      userAdminProfile: null,
    },
    include: {
      managedByClientAdmin: {
        include: {
          clientProfile: true,
        },
      },
    },
  });

  console.log(`📊 Found ${userAdmins.length} USER_ADMIN users to migrate`);

  for (const userAdmin of userAdmins) {
    try {
      const clientProfile = userAdmin.managedByClientAdmin?.clientProfile;
      
      if (!clientProfile) {
        console.warn(`⚠️  USER_ADMIN ${userAdmin.email} has no parent CLIENT_ADMIN profile. Skipping.`);
        continue;
      }

      // Create UserAdminProfile with limits from parent CLIENT_ADMIN
      await prisma.userAdminProfile.create({
        data: {
          userAdminId: userAdmin.id,
          maxDisplays: clientProfile.maxDisplays || 10,
          maxUsers: clientProfile.maxUsers || 5,
          maxStorageMB: clientProfile.maxStorageMB || 25,
          maxMonthlyUsageMB: clientProfile.maxMonthlyUsageMB || 150,
          monthlyUploadedBytes: clientProfile.monthlyUploadedBytes || BigInt(0),
          usageQuotaResetDate: clientProfile.usageQuotaResetDate || new Date(),
          billingDayOfMonth: clientProfile.billingDayOfMonth || 1,
          licenseExpiry: clientProfile.licenseExpiry,
          isActive: clientProfile.isActive,
        },
      });

      console.log(`✅ Migrated USER_ADMIN: ${userAdmin.email}`);
    } catch (error) {
      console.error(`❌ Error migrating USER_ADMIN ${userAdmin.email}:`, error.message);
    }
  }
}
```
### 4.2 Updated Seed Scripts
**File**: `signox_backend/scripts/seedDemoUsers.js`

#### 4.2.1 CLIENT_ADMIN Creation (No Limits)
```javascript
// BEFORE
await prisma.clientProfile.upsert({
  where: { clientAdminId: clientAdmin.id },
  update: {
    clientId: 'CL-DEMO-001',
    maxDisplays: 5,
    maxUsers: 10,
    licenseExpiry: new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
    companyName: 'Demo Client Co',
    contactEmail: 'client@signox.com',
    isActive: true,
  },
  create: {
    // ... same data
  },
});

// AFTER
await prisma.clientProfile.upsert({
  where: { clientAdminId: clientAdmin.id },
  update: {
    clientId: 'CL-DEMO-001',
    licenseExpiry: new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
    companyName: 'Demo Client Co',
    contactEmail: 'client@signox.com',
    isActive: true,
  },
  create: {
    // ... same data (no limits)
  },
});
```

#### 4.2.2 USER_ADMIN Creation (With Limits)
```javascript
// Create USER_ADMIN
const userAdmin = await upsertUser({
  email: 'useradmin@signox.com',
  password: 'useradmin123',
  role: 'USER_ADMIN',
  managedByClientAdminId: clientAdmin.id,
});

// Create UserAdminProfile with limits
await prisma.userAdminProfile.upsert({
  where: { userAdminId: userAdmin.id },
  update: {
    maxDisplays: 5,
    maxUsers: 10,
    maxStorageMB: 25,
    maxMonthlyUsageMB: 150,
    licenseExpiry: new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
    isActive: true,
  },
  create: {
    userAdminId: userAdmin.id,
    maxDisplays: 5,
    maxUsers: 10,
    maxStorageMB: 25,
    maxMonthlyUsageMB: 150,
    licenseExpiry: new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
    isActive: true,
  },
});
```

## 5. API Endpoint Changes Summary

### 5.1 User Creation Endpoints

#### 5.1.1 SUPER_ADMIN → CLIENT_ADMIN
**Endpoint**: `POST /api/users`
**Requester**: SUPER_ADMIN

**BEFORE:**
```json
{
  "email": "client@company.com",
  "password": "password123",
  "companyName": "Company Inc",
  "maxDisplays": 10,
  "maxUsers": 5,
  "maxStorageMB": 25,
  "licenseExpiry": "2024-12-31"
}
```

**AFTER:**
```json
{
  "email": "client@company.com",
  "password": "password123",
  "companyName": "Company Inc",
  "licenseExpiry": "2024-12-31"
}
```

#### 5.1.2 CLIENT_ADMIN → USER_ADMIN
**Endpoint**: `POST /api/users`
**Requester**: CLIENT_ADMIN

**BEFORE:**
```json
{
  "email": "manager@company.com",
  "password": "password123"
}
```

**AFTER:**
```json
{
  "email": "manager@company.com",
  "password": "password123",
  "maxDisplays": 10,
  "maxUsers": 5,
  "maxStorageMB": 25,
  "maxMonthlyUsageMB": 150,
  "licenseExpiry": "2024-12-31"
}
```

### 5.2 Authentication Response Changes

#### 5.2.1 USER_ADMIN Login Response
**BEFORE:**
```json
{
  "user": {
    "id": "...",
    "email": "...",
    "role": "USER_ADMIN",
    "clientProfile": null
  }
}
```

**AFTER:**
```json
{
  "user": {
    "id": "...",
    "email": "...",
    "role": "USER_ADMIN",
    "clientProfile": null,
    "userAdminProfile": {
      "maxDisplays": 10,
      "maxUsers": 5,
      "maxStorageMB": 25,
      "maxMonthlyUsageMB": 150,
      "monthlyUploadedBytes": 0,
      "licenseExpiry": "2024-12-31",
      "isActive": true
    }
  }
}
```

### 5.3 Account Info Endpoint Changes
**Endpoint**: `GET /api/users/me/account`

**BEFORE:**
- Only available for CLIENT_ADMIN
- Returns CLIENT_ADMIN limits

**AFTER:**
- Only available for USER_ADMIN
- Returns USER_ADMIN limits
- Includes company name from parent CLIENT_ADMIN

## 6. License Validation Changes

### 6.1 Two-Level License Checking
The system now validates licenses at two levels:

1. **Organization Level** (CLIENT_ADMIN): 
   - Basic organization license status
   - Organization license expiry

2. **User Level** (USER_ADMIN):
   - Individual user license status
   - Individual user license expiry
   - Resource limits enforcement

### 6.2 License Check Flow
```
USER_ADMIN Login:
├── Check CLIENT_ADMIN.clientProfile.isActive
├── Check CLIENT_ADMIN.clientProfile.licenseExpiry
├── Check USER_ADMIN.userAdminProfile.isActive
└── Check USER_ADMIN.userAdminProfile.licenseExpiry

STAFF Login:
├── Check CLIENT_ADMIN.clientProfile.isActive
├── Check CLIENT_ADMIN.clientProfile.licenseExpiry
├── Check USER_ADMIN.userAdminProfile.isActive
└── Check USER_ADMIN.userAdminProfile.licenseExpiry
```

## 7. Database Migration Steps

### 7.1 Pre-Migration
1. Backup existing database
2. Test migration script on development environment
3. Verify all USER_ADMIN users have parent CLIENT_ADMIN

### 7.2 Migration Execution
```bash
# 1. Run data migration script
cd signox_backend
node scripts/migrateToUserAdminLimits.js

# 2. Apply schema changes
npx prisma db push

# 3. Verify migration
node scripts/verifyMigration.js

# 4. Update seed data
node scripts/seedDemoUsers.js
```

### 7.3 Post-Migration Verification
1. Test CLIENT_ADMIN user creation (should not have limit fields)
2. Test USER_ADMIN creation with limits
3. Test display limit enforcement
4. Test license validation at both levels
5. Verify existing USER_ADMIN users have proper limits

## 8. Benefits of New Architecture

### 8.1 Business Benefits
- **Reseller Model**: CLIENT_ADMINs can offer different packages to USER_ADMINs
- **Flexible Pricing**: Different USER_ADMINs can have different resource allocations
- **Better Scalability**: Limits enforced at actual user level

### 8.2 Technical Benefits
- **Cleaner Architecture**: Separation of organization vs user concerns
- **Granular Control**: Each USER_ADMIN has independent limits
- **Better Data Isolation**: Resources properly attributed to actual users
- **Simplified CLIENT_ADMIN Management**: Focus on organization-level concerns

### 8.3 User Experience Benefits
- **Clear Hierarchy**: Obvious distinction between organization and user limits
- **Individual Accountability**: Each USER_ADMIN responsible for their own resources
- **Transparent Limits**: Users see their own limits, not shared organization limits

## 9. Testing Checklist

### 9.1 Backend Testing
- [ ] SUPER_ADMIN can create CLIENT_ADMIN without limits
- [ ] CLIENT_ADMIN can create USER_ADMIN with limits
- [ ] USER_ADMIN can create STAFF users
- [ ] License validation works at both levels
- [ ] Display limits enforced for USER_ADMIN
- [ ] Storage limits enforced for USER_ADMIN
- [ ] User deletion cleans up UserAdminProfile

### 9.2 Frontend Testing
- [ ] CLIENT_ADMIN user creation form shows limit fields
- [ ] SUPER_ADMIN client creation form doesn't show limit fields
- [ ] Display page shows correct limits for USER_ADMIN
- [ ] User list shows USER_ADMIN limits correctly
- [ ] Authentication context includes userAdminProfile

### 9.3 Integration Testing
- [ ] End-to-end user creation flow
- [ ] License expiry blocks access correctly
- [ ] Limit enforcement prevents over-allocation
- [ ] Migration script works correctly
- [ ] Seed scripts create proper data structure

This completes the comprehensive implementation of moving user limits from CLIENT_ADMIN to USER_ADMIN level, enabling a flexible reseller model for the SignoX platform.