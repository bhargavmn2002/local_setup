# SignoX Suspension System Changes

## Overview
This document details all changes made to the SignoX suspension system to implement the new requirements:
1. Remove SUPER_ADMIN ability to suspend CLIENT_ADMIN
2. Change CLIENT_ADMIN → USER_ADMIN relationship to have both suspend and delete options
3. Add automatic license expiry suspension for USER_ADMIN
4. Update frontend interfaces accordingly

---

## 1. Profile Page Updates

### File: `signox_frontend/src/app/profile/page.tsx`

#### Changes Made:
- **Updated USER_ADMIN profile display** to show their own company information instead of organization hierarchy
- **Added conditional rendering** based on user role
- **Updated TypeScript types** to include `userAdminProfile` data

#### Key Changes:
```typescript
// Added userAdminProfile to UserProfile type
type UserProfile = {
  // ... existing fields
  userAdminProfile?: {
    companyName: string | null;
    contactNumber: string | null;
    maxDisplays: number;
    maxUsers: number;
    maxStorageMB: number;
    maxMonthlyUsageMB: number;
    licenseExpiry: string | null;
    monthlyUploadedBytes: number;
    isActive: boolean;
  } | null;
};

// Conditional display logic
{profile?.role === 'USER_ADMIN' ? (
  // Show company info for USER_ADMIN from their own profile
  <>
    {profile.userAdminProfile?.companyName && (
      <div className="bg-gradient-to-br from-yellow-50 to-orange-50 p-4 rounded-xl border border-yellow-200">
        <Label className="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-2">Company Name</Label>
        <p className="text-lg font-bold text-gray-900">{profile.userAdminProfile.companyName}</p>
      </div>
    )}
    // ... contact number display
  </>
) : (
  // Show hierarchy for other roles
  // ... existing hierarchy display
)}
```

---

## 2. Password Change Restrictions

### File: `signox_backend/src/controllers/user.controller.js`

#### Changes Made:
- **Added role-based restrictions** to `updatePassword` function
- **Blocked USER_ADMIN and STAFF** from changing their own passwords
- **Only SUPER_ADMIN and CLIENT_ADMIN** can change passwords

#### Key Changes:
```javascript
exports.updatePassword = async (req, res) => {
  try {
    const userId = req.user.id;
    const { currentPassword, newPassword } = req.body;

    // Check if user role is allowed to change password
    if (req.user.role === 'USER_ADMIN' || req.user.role === 'STAFF') {
      return res.status(403).json({ 
        message: 'Password change is not allowed for your user role. Please contact your administrator.' 
      });
    }
    // ... rest of function
  }
};
```

### Frontend Changes:
- **Profile page**: Password change section only shows for SUPER_ADMIN and CLIENT_ADMIN
- **Sidebar**: Reset Password button only shows for admin roles

---

## 3. Suspension System Overhaul

### 3.1 Removed SUPER_ADMIN → CLIENT_ADMIN Suspension

#### File: `signox_backend/src/controllers/client.controller.js`

```javascript
exports.toggleClientAdminStatus = async (req, res) => {
  return res.status(403).json({ 
    message: 'Client Admin suspension has been disabled. Only Client Admins can manage their User Admins.' 
  });
};
```

#### File: `signox_backend/src/controllers/user.controller.js`

```javascript
if (requester.role === 'SUPER_ADMIN') {
  // SUPER_ADMIN can no longer suspend CLIENT_ADMIN
  return res.status(403).json({ 
    message: 'Super Admin can no longer suspend Client Admins. This functionality has been removed.' 
  });
}
```

### 3.2 Added Separate Suspend and Delete for CLIENT_ADMIN → USER_ADMIN

#### Backend: New Suspend Function
```javascript
exports.toggleUserAdminSuspension = async (req, res) => {
  try {
    const requester = req.user;
    const { id } = req.params;

    // Only CLIENT_ADMIN can suspend/reactivate USER_ADMIN
    if (requester.role !== 'CLIENT_ADMIN') {
      return res.status(403).json({ message: 'Only Client Admins can suspend/reactivate User Admins' });
    }

    const target = await prisma.user.findUnique({
      where: { id },
      include: { userAdminProfile: true },
    });

    if (target.role !== 'USER_ADMIN' || target.managedByClientAdminId !== requester.id) {
      return res.status(403).json({ message: 'You can only suspend/reactivate your own User Admins' });
    }

    const newStatus = !target.isActive;

    // Toggle suspension status with cascade to staff
    await prisma.$transaction(async (tx) => {
      // 1) Update the USER_ADMIN status
      await tx.user.update({
        where: { id: target.id },
        data: { isActive: newStatus },
      });

      // 2) Update UserAdminProfile status if exists
      if (target.userAdminProfile) {
        await tx.userAdminProfile.update({
          where: { id: target.userAdminProfile.id },
          data: { isActive: newStatus },
        });
      }

      // 3) Update all STAFF under this USER_ADMIN
      await tx.user.updateMany({
        where: { 
          role: 'STAFF', 
          createdByUserAdminId: target.id 
        },
        data: { isActive: newStatus },
      });

      // 4) If suspending, unassign displays
      if (!newStatus) {
        await tx.display.updateMany({
          where: { managedByUserId: target.id, clientAdminId: requester.id },
          data: {
            playlistId: null,
            layoutId: null,
            activeLayoutId: null,
            managedByUserId: null,
          },
        });
      }
    });

    const action = newStatus ? 'reactivated' : 'suspended';
    const staffMessage = newStatus 
      ? 'All staff under this User Admin have also been reactivated.'
      : 'All staff under this User Admin have also been suspended.';

    return res.json({ 
      message: `User Admin ${action} successfully. ${staffMessage}`,
      isActive: newStatus
    });

  } catch (error) {
    console.error('Toggle User Admin Suspension Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};
```

#### Backend: Restored Hard Delete Function
```javascript
// Restored full hard delete functionality for CLIENT_ADMIN → USER_ADMIN
// Includes complete data cleanup: UserAdminProfile, STAFF, content, schedules
```

#### New Route Added:
```javascript
// File: signox_backend/src/routes/user.routes.js
router.patch('/:id/suspend', requireAuth, requireAnyAdmin, toggleUserAdminSuspension);
```

---

## 4. Automatic License Expiry System

### File: `signox_backend/src/services/license-expiry.service.js`

#### New Service Created:
```javascript
/**
 * Check and suspend USER_ADMIN accounts with expired licenses
 * This function should be called periodically (e.g., daily via cron job)
 */
async function checkAndSuspendExpiredLicenses() {
  try {
    console.log('🔍 Checking for expired USER_ADMIN licenses...');
    
    const now = new Date();
    
    // Find all USER_ADMIN profiles with expired licenses that are still active
    const expiredProfiles = await prisma.userAdminProfile.findMany({
      where: {
        licenseExpiry: {
          lt: now // License expiry date is less than current date
        },
        isActive: true // Only check active profiles
      },
      include: {
        userAdmin: {
          select: {
            id: true,
            email: true,
            isActive: true
          }
        }
      }
    });

    if (expiredProfiles.length === 0) {
      console.log('✅ No expired licenses found');
      return { suspended: 0, message: 'No expired licenses found' };
    }

    console.log(`⚠️  Found ${expiredProfiles.length} expired licenses`);

    let suspendedCount = 0;

    // Process each expired profile
    for (const profile of expiredProfiles) {
      if (!profile.userAdmin || !profile.userAdmin.isActive) {
        console.log(`⏭️  Skipping already inactive user: ${profile.userAdmin?.email || 'Unknown'}`);
        continue;
      }

      try {
        await prisma.$transaction(async (tx) => {
          // 1. Suspend the USER_ADMIN
          await tx.user.update({
            where: { id: profile.userAdmin.id },
            data: { isActive: false }
          });

          // 2. Suspend the UserAdminProfile
          await tx.userAdminProfile.update({
            where: { id: profile.id },
            data: { isActive: false }
          });

          // 3. Suspend all STAFF under this USER_ADMIN
          const staffUpdateResult = await tx.user.updateMany({
            where: {
              role: 'STAFF',
              createdByUserAdminId: profile.userAdmin.id,
              isActive: true // Only suspend active staff
            },
            data: { isActive: false }
          });

          // 4. Unassign displays managed by this USER_ADMIN
          await tx.display.updateMany({
            where: { 
              managedByUserId: profile.userAdmin.id 
            },
            data: {
              playlistId: null,
              layoutId: null,
              activeLayoutId: null,
              managedByUserId: null,
            }
          });

          console.log(`🔒 Suspended USER_ADMIN: ${profile.userAdmin.email} (License expired: ${profile.licenseExpiry})`);
          console.log(`   └── Also suspended ${staffUpdateResult.count} staff members`);
          
          suspendedCount++;
        });
      } catch (error) {
        console.error(`❌ Failed to suspend USER_ADMIN ${profile.userAdmin.email}:`, error);
      }
    }

    const message = `Suspended ${suspendedCount} USER_ADMIN accounts with expired licenses`;
    console.log(`✅ ${message}`);
    
    return { 
      suspended: suspendedCount, 
      total: expiredProfiles.length,
      message 
    };

  } catch (error) {
    console.error('❌ Error checking expired licenses:', error);
    throw error;
  }
}

/**
 * Get summary of license expiry status
 */
async function getLicenseExpirySummary() {
  try {
    const now = new Date();
    const thirtyDaysFromNow = new Date(now.getTime() + (30 * 24 * 60 * 60 * 1000));

    const [expired, expiringSoon, active] = await Promise.all([
      // Expired licenses
      prisma.userAdminProfile.count({
        where: {
          licenseExpiry: { lt: now },
          isActive: true
        }
      }),
      
      // Expiring within 30 days
      prisma.userAdminProfile.count({
        where: {
          licenseExpiry: {
            gte: now,
            lte: thirtyDaysFromNow
          },
          isActive: true
        }
      }),
      
      // Active licenses (not expiring soon)
      prisma.userAdminProfile.count({
        where: {
          OR: [
            { licenseExpiry: { gt: thirtyDaysFromNow } },
            { licenseExpiry: null }
          ],
          isActive: true
        }
      })
    ]);

    return {
      expired,
      expiringSoon,
      active,
      total: expired + expiringSoon + active
    };
  } catch (error) {
    console.error('❌ Error getting license summary:', error);
    throw error;
  }
}

module.exports = {
  checkAndSuspendExpiredLicenses,
  getLicenseExpirySummary
};
```

### Integration into Server Startup

#### File: `signox_backend/src/server.js`

```javascript
// Added service import
const licenseExpiryService = require('./services/license-expiry.service');

// Added license expiry check function
let licenseExpiryInterval;

function startLicenseExpiryCheck() {
  console.log('🔄 [LICENSE EXPIRY] Starting license expiry check service...');
  
  // Run immediately on startup
  licenseExpiryService.checkAndSuspendExpiredLicenses()
    .then(result => {
      console.log(`✅ [LICENSE EXPIRY] Initial check completed: ${result.message}`);
    })
    .catch(error => {
      console.error('❌ [LICENSE EXPIRY] Initial check failed:', error);
    });
  
  // Run every 24 hours (86400000 ms)
  licenseExpiryInterval = setInterval(async () => {
    try {
      const result = await licenseExpiryService.checkAndSuspendExpiredLicenses();
      console.log(`✅ [LICENSE EXPIRY] Daily check completed: ${result.message}`);
    } catch (error) {
      console.error('❌ [LICENSE EXPIRY] Daily check failed:', error);
    }
  }, 24 * 60 * 60 * 1000); // 24 hours
}

// Added to both HTTP and HTTPS server startup
startLicenseExpiryCheck();
```

---

## 5. Frontend Updates

### 5.1 CLIENT_ADMIN Users Page

#### File: `signox_frontend/src/app/client/users/page.tsx`

#### Major Changes:

1. **Added Suspend/Reactivate State Management:**
```typescript
// Suspend/Reactivate State
const [suspending, setSuspending] = useState(false);
```

2. **Added Suspend/Reactivate Function:**
```typescript
// Handle Suspend/Reactivate User Admin
const handleSuspendToggle = async (userId: string, currentStatus: boolean) => {
  const action = currentStatus ? 'suspend' : 'reactivate';
  const confirmMessage = currentStatus 
    ? 'Are you sure you want to suspend this User Admin? This will also suspend all their staff members and unassign their displays.'
    : 'Are you sure you want to reactivate this User Admin? This will also reactivate all their staff members.';

  if (confirm(confirmMessage)) {
    try {
      setSuspending(true);
      await api.patch(`/users/${userId}/suspend`);
      fetchUsers(); // Refresh the list
    } catch (err: any) {
      alert(err.response?.data?.message || `Failed to ${action} user`);
    } finally {
      setSuspending(false);
    }
  }
};
```

3. **Updated Button Layout:**
```typescript
<div className="flex gap-2 justify-end">
  <Button // View Details
    variant="ghost" 
    size="sm" 
    className="text-blue-500 hover:text-blue-700"
    onClick={() => openDetailsDialog(u)}
    title="View Details"
  >
    <Eye className="h-4 w-4" />
  </Button>
  <Button // Edit
    variant="ghost" 
    size="sm" 
    className="text-green-500 hover:text-green-700"
    onClick={() => openEditDialog(u)}
    title="Edit User Admin"
  >
    <Edit className="h-4 w-4" />
  </Button>
  <Button // Suspend/Reactivate
    variant="ghost" 
    size="sm" 
    className={u.isActive ? "text-orange-500 hover:text-orange-700" : "text-green-500 hover:text-green-700"}
    onClick={() => handleSuspendToggle(u.id, u.isActive)}
    title={u.isActive ? "Suspend User Admin" : "Reactivate User Admin"}
    disabled={suspending}
  >
    {suspending ? (
      <Loader2 className="h-4 w-4 animate-spin" />
    ) : u.isActive ? (
      <UserX className="h-4 w-4" />
    ) : (
      <Power className="h-4 w-4" />
    )}
  </Button>
  <Button // Reset Password
    variant="ghost" 
    size="sm" 
    className="text-blue-500 hover:text-blue-700"
    onClick={() => openResetDialog(u.id, u.email)}
    title="Reset Password"
  >
    <Key className="h-4 w-4" />
  </Button>
  <Button // Delete
    variant="ghost" 
    size="sm" 
    className="text-red-500 hover:text-red-700"
    onClick={() => handleDelete(u.id)}
    title="Delete User Admin"
  >
    <Trash2 className="h-4 w-4" />
  </Button>
</div>
```

4. **Added New Icons:**
```typescript
import { Plus, Trash2, ShieldAlert, Key, Loader2, Users, Edit, Eye, Building2, Phone, Calendar, HardDrive, Upload, UserCheck, Power, UserX } from 'lucide-react';
```

### 5.2 Super Admin Clients Page

#### File: `signox_frontend/src/app/super-admin/clients/page.tsx`

#### Changes Made:

1. **Removed Suspend/Activate Button:**
```typescript
// Removed the suspend/activate button from the actions
// Only kept Edit and Delete buttons
```

2. **Updated Messaging:**
```typescript
<p className="text-gray-300 text-lg">
  Create and manage tenant (Client Admin) accounts. Suspension is now handled at the User Admin level by Client Admins.
</p>
```

3. **Disabled Toggle Function:**
```typescript
async function toggleStatus(id: string) {
  // This functionality has been removed
  setError('Client Admin suspension has been disabled. Only Client Admins can manage their User Admins.');
  return;
}
```

4. **Simplified Status Display:**
```typescript
<TableCell>
  <Badge variant={c.isActive ? "default" : "destructive"}>
    {c.isActive ? 'Active' : 'Inactive'}
  </Badge>
</TableCell>
```

---

## 6. User Details Dialog Enhancement

### File: `signox_frontend/src/app/client/users/page.tsx`

#### Added Comprehensive User Details Modal:

1. **Basic Information Section:**
   - Email address
   - Account status (Active/Inactive)
   - Account creation date

2. **Company Information Section:**
   - Company name
   - Contact phone number
   - License expiry date

3. **Resource Limits Section:**
   - Maximum displays allowed
   - Maximum staff users allowed
   - Storage limit (MB)
   - Monthly upload limit (MB)

4. **Current Usage Section:**
   - Active displays count vs limit
   - Staff users count vs limit
   - Storage used vs limit
   - Monthly uploads vs limit

5. **Usage Overview Section:**
   - Visual progress bars showing usage percentages
   - Color-coded progress indicators

#### Key Features:
- **Real-time data fetching** when dialog opens
- **Loading states** while fetching statistics
- **Responsive design** with card-based layout
- **Professional styling** consistent with existing UI

---

## 7. Summary of New Suspension Model

### Current Hierarchy and Permissions:

#### **SUPER_ADMIN:**
- ❌ **Cannot suspend CLIENT_ADMIN** (removed)
- ✅ **Can create/edit/delete CLIENT_ADMIN**
- ✅ **Can change own password**

#### **CLIENT_ADMIN:**
- ✅ **Can suspend/reactivate USER_ADMIN** (new - preserves data)
- ✅ **Can permanently delete USER_ADMIN** (restored - removes all data)
- ✅ **Can create/edit USER_ADMIN**
- ✅ **Can change own password**

#### **USER_ADMIN:**
- ✅ **Can permanently delete STAFF** (unchanged)
- ✅ **Can create/edit STAFF**
- ❌ **Cannot change own password** (new restriction)
- 🤖 **Automatically suspended when license expires**

#### **STAFF:**
- ❌ **Cannot manage other users**
- ❌ **Cannot change own password** (new restriction)

### Suspension Effects:

#### **CLIENT_ADMIN → USER_ADMIN Suspension:**
- ✅ **Preserves all data** (playlists, media, layouts, schedules)
- ✅ **Reversible** (can be reactivated)
- ✅ **Cascades to STAFF** (all staff also suspended)
- ✅ **Unassigns displays** (for security)

#### **CLIENT_ADMIN → USER_ADMIN Deletion:**
- ❌ **Permanently removes all data**
- ❌ **Cannot be undone**
- ❌ **Deletes all STAFF** under the USER_ADMIN
- ❌ **Removes all content** (playlists, media, layouts, schedules)

#### **License Expiry Suspension:**
- 🤖 **Automatic daily checks**
- ✅ **Preserves all data**
- ✅ **Cascades to STAFF**
- ✅ **Unassigns displays**
- ✅ **Can be reactivated** by updating license expiry

---

## 8. Technical Implementation Details

### Database Changes:
- **No schema changes required** - uses existing `isActive` fields
- **Leverages existing relationships** between User, UserAdminProfile, and STAFF

### API Endpoints:
- **New**: `PATCH /api/users/:id/suspend` - Toggle USER_ADMIN suspension
- **Modified**: `DELETE /api/users/:id` - Restored hard delete for USER_ADMIN
- **Disabled**: `PATCH /api/users/client-admins/:id/status` - CLIENT_ADMIN suspension

### Security:
- **Role-based access control** enforced at API level
- **Hierarchical permissions** maintained
- **Cascade effects** properly implemented
- **Data integrity** preserved during operations

### Monitoring:
- **Daily license expiry checks** with logging
- **Automatic suspension** of expired accounts
- **Detailed logging** of all suspension/deletion operations
- **Error handling** and recovery mechanisms

---

## 9. Testing Recommendations

### Backend Testing:
1. **Test CLIENT_ADMIN suspend/reactivate USER_ADMIN**
2. **Test CLIENT_ADMIN delete USER_ADMIN** (permanent)
3. **Test license expiry automatic suspension**
4. **Test cascade effects** to STAFF users
5. **Test display unassignment** during suspension
6. **Test permission restrictions** for different roles

### Frontend Testing:
1. **Test button functionality** in CLIENT_ADMIN users page
2. **Test user details dialog** with real data
3. **Test suspend/reactivate confirmation dialogs**
4. **Test delete confirmation dialogs**
5. **Test loading states** and error handling
6. **Test responsive design** on different screen sizes

### Integration Testing:
1. **Test end-to-end suspension workflow**
2. **Test end-to-end deletion workflow**
3. **Test license expiry automation**
4. **Test user login restrictions** when suspended
5. **Test display management** after suspension/deletion

---

## 10. Future Enhancements

### Potential Improvements:
1. **Bulk suspension/reactivation** of multiple USER_ADMIN accounts
2. **Scheduled suspension** (suspend at specific date/time)
3. **Suspension reasons** and audit trail
4. **Email notifications** for suspensions and reactivations
5. **Grace period** before license expiry suspension
6. **Dashboard widgets** showing suspension statistics
7. **Export/import** of suspension data
8. **Advanced filtering** by suspension status

### Monitoring Enhancements:
1. **Real-time license expiry alerts**
2. **Suspension activity dashboard**
3. **Usage analytics** before/after suspensions
4. **Performance impact** monitoring
5. **Automated reports** for administrators

---

## Files Modified

### Backend Files:
- `signox_backend/src/controllers/user.controller.js` - Added suspend function, restored delete, added password restrictions
- `signox_backend/src/controllers/client.controller.js` - Disabled CLIENT_ADMIN suspension
- `signox_backend/src/routes/user.routes.js` - Added suspend route
- `signox_backend/src/services/license-expiry.service.js` - New service for automatic license expiry
- `signox_backend/src/server.js` - Integrated license expiry service

### Frontend Files:
- `signox_frontend/src/app/profile/page.tsx` - Updated USER_ADMIN profile display
- `signox_frontend/src/app/client/users/page.tsx` - Added suspend/reactivate buttons and user details dialog
- `signox_frontend/src/app/super-admin/clients/page.tsx` - Removed CLIENT_ADMIN suspension functionality

### New Files Created:
- `signox_backend/src/services/license-expiry.service.js` - License expiry automation service
- `SUSPENSION_SYSTEM_CHANGES.md` - This documentation file

---

*Last Updated: [Current Date]*
*Version: 1.0*
*Author: Kiro AI Assistant*