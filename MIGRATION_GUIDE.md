# SignoX User Limits Migration Guide

## Overview

This migration moves resource limits (displays, storage, users) from the **CLIENT_ADMIN** level to the **USER_ADMIN** level. This allows CLIENT_ADMINs to act as resellers, giving different USER_ADMINs different resource allocations.

## Changes Made

### 1. Database Schema Changes
- **New Model**: `UserAdminProfile` - Contains limits for USER_ADMIN users
- **Updated Model**: `ClientProfile` - Removed limit fields, kept organization info
- **Updated Model**: `User` - Added relation to `UserAdminProfile`

### 2. Backend API Changes
- **User Creation**: CLIENT_ADMIN can now set limits when creating USER_ADMIN
- **Authentication**: License checking now validates both organization and user-level licenses
- **User Management**: Updated to handle UserAdminProfile creation/deletion

### 3. Frontend Changes
- **CLIENT_ADMIN User Creation**: Now includes limit fields for USER_ADMIN creation
- **SUPER_ADMIN Client Creation**: Removed limit fields (limits now set per USER_ADMIN)
- **Display Management**: Updated to check USER_ADMIN limits instead of CLIENT_ADMIN limits
- **AuthContext**: Added `userAdminProfile` to user interface

## Migration Steps

### 1. Database Migration
```bash
# Run the migration script to move existing limits
cd signox_backend
node scripts/migrateToUserAdminLimits.js
```

### 2. Update Database Schema
```bash
# Generate and apply Prisma migration
npx prisma db push
# or
npx prisma migrate dev --name move-limits-to-user-admin
```

### 3. Update Seed Data
```bash
# Re-run seed scripts with new structure
node scripts/seedDemoUsers.js
```

## New User Hierarchy

### Before (OLD):
```
SUPER_ADMIN
└── CLIENT_ADMIN (has limits: maxDisplays, maxStorage, etc.)
    └── USER_ADMIN (inherits CLIENT_ADMIN limits)
        └── STAFF (inherits CLIENT_ADMIN limits)
```

### After (NEW):
```
SUPER_ADMIN
└── CLIENT_ADMIN (organization info only)
    └── USER_ADMIN (has individual limits: maxDisplays, maxStorage, etc.)
        └── STAFF (inherits USER_ADMIN limits)
```

## API Changes

### User Creation
**CLIENT_ADMIN creating USER_ADMIN:**
```javascript
POST /api/users
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

**SUPER_ADMIN creating CLIENT_ADMIN:**
```javascript
POST /api/users
{
  "email": "client@company.com",
  "password": "password123",
  "companyName": "Company Inc",
  "licenseExpiry": "2024-12-31"
  // No limits - they're set per USER_ADMIN
}
```

### Authentication Response
```javascript
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

## License Checking

The system now validates licenses at two levels:

1. **Organization Level** (CLIENT_ADMIN): Basic organization license
2. **User Level** (USER_ADMIN): Individual user license and limits

Both must be active and not expired for USER_ADMIN and STAFF users to access the system.

## Frontend Updates

### Display Limits
```typescript
// OLD: Check CLIENT_ADMIN limits
const limits = user.clientProfile?.maxDisplays

// NEW: Check USER_ADMIN limits
const limits = user.userAdminProfile?.maxDisplays
```

### User Creation Forms
- **CLIENT_ADMIN**: Now includes limit fields when creating USER_ADMIN
- **SUPER_ADMIN**: Removed limit fields when creating CLIENT_ADMIN

## Testing

1. **Create CLIENT_ADMIN** (SUPER_ADMIN)
2. **Create USER_ADMIN with limits** (CLIENT_ADMIN)
3. **Verify limits are enforced** (USER_ADMIN)
4. **Create STAFF users** (USER_ADMIN)
5. **Test license expiry** at both levels

## Rollback Plan

If issues occur, you can rollback by:

1. Reverting the database schema changes
2. Restoring the previous API endpoints
3. Reverting frontend changes
4. Running a reverse migration script (if needed)

## Benefits

1. **Flexible Reseller Model**: CLIENT_ADMINs can offer different packages to USER_ADMINs
2. **Granular Control**: Each USER_ADMIN has independent limits
3. **Better Scalability**: Limits are enforced at the actual user level
4. **Cleaner Architecture**: Separation of organization vs user concerns