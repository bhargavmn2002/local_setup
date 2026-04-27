# Storage Isolation Fix - Individual USER_ADMIN Storage Tracking

## ✅ ISSUE FIXED: Storage limits being shared between USER_ADMINs under the same CLIENT_ADMIN

### The Problem
When you added media in one USER_ADMIN's account, it was reflecting in the storage usage of another USER_ADMIN's account under the same CLIENT_ADMIN. This was because the storage tracking was still using **organization-level aggregation** instead of **individual USER_ADMIN storage tracking**.

**Before Fix:**
```
Client Admin A
├── User Admin 1 (Company A) uploads 10MB → Shows in both User Admin storage
└── User Admin 2 (Company B) sees 10MB used ← WRONG!
```

### Root Cause
The media controller was using **client-wide storage functions** that aggregated storage across all USER_ADMINs under the same CLIENT_ADMIN:

```javascript
// OLD LOGIC - Shared storage tracking
await incrementMonthlyUpload(clientAdminId, fileSize); // ❌ Client-wide increment
const storageInfo = await getClientStorageInfo(clientAdminId); // ❌ Client-wide info
```

This caused:
1. **Upload tracking** to be shared across all USER_ADMINs
2. **Storage usage calculations** to show aggregated usage
3. **Storage limits** to be shared instead of individual

### The Fix Applied

Updated the media controller to use **individual USER_ADMIN storage tracking** with role-based storage functions:

#### 1. Updated Media Upload Tracking
```javascript
// NEW LOGIC - Individual user tracking
await incrementUserMonthlyUpload(req.user.id, fileSize); // ✅ Individual user increment
```

#### 2. Updated Storage Info Retrieval
```javascript
// NEW LOGIC - Role-based storage info
let storageInfo;
if (req.user.role === 'USER_ADMIN') {
  storageInfo = await getUserStorageInfo(req.user.id); // ✅ Individual USER_ADMIN storage
} else {
  storageInfo = await getClientStorageInfo(clientAdminId); // ✅ CLIENT_ADMIN sees aggregate
}
```

#### 3. Functions Updated
- ✅ **`createMedia()`** - Uses individual user upload tracking and storage info
- ✅ **`listMedia()`** - Shows individual user storage info
- ✅ **`deleteMedia()`** - Updates individual user storage info
- ✅ **`getStorageInfo()`** - Already correctly implemented with role-based logic

### How Individual Storage Tracking Works

#### **USER_ADMIN Storage Calculation**
```javascript
// Calculate storage used by this USER_ADMIN and their STAFF only
const staffUsers = await prisma.user.findMany({
  where: {
    role: 'STAFF',
    createdByUserAdminId: userId // Only THIS user admin's staff
  }
});

const userIds = [userId, ...staffUsers.map(s => s.id)];

const result = await prisma.media.aggregate({
  where: {
    createdById: {
      in: userIds // Only media from this USER_ADMIN + their staff
    }
  },
  _sum: { fileSize: true }
});
```

#### **Individual Upload Tracking**
```javascript
// Increment monthly upload for the specific USER_ADMIN
if (user.role === 'USER_ADMIN') {
  userAdminId = userId; // Direct USER_ADMIN
} else if (user.role === 'STAFF' && user.createdByUserAdminId) {
  userAdminId = user.createdByUserAdminId; // STAFF uploads count toward their USER_ADMIN
}

await prisma.userAdminProfile.update({
  where: { userAdminId },
  data: {
    monthlyUploadedBytes: { increment: bytes }
  }
});
```

### Result After Fix

**✅ Individual USER_ADMIN Storage:**
```
Client Admin A
├── User Admin 1 (Company A) 
│   ├── Storage Used: 10MB / 25MB limit
│   └── Monthly Usage: 10MB / 150MB limit
└── User Admin 2 (Company B)
    ├── Storage Used: 5MB / 25MB limit  
    └── Monthly Usage: 5MB / 150MB limit
```

**✅ CLIENT_ADMIN Management View:**
```
Client Admin A → Can see aggregate: 15MB total used across all USER_ADMINs
```

**✅ STAFF Inheritance:**
```
User Admin 1 (Company A)
├── Staff 1A uploads 2MB → Counts toward User Admin 1's storage (12MB total)
└── Staff 1B uploads 1MB → Counts toward User Admin 1's storage (13MB total)

User Admin 2 (Company B) → Still shows 5MB (unaffected)
```

### Storage Limits and Quotas

Each USER_ADMIN now has **completely independent**:

1. **Storage Limits** (`maxStorageMB` in UserAdminProfile)
   - Default: 25MB per USER_ADMIN
   - Tracks actual disk space used by their media files

2. **Monthly Upload Quotas** (`maxMonthlyUsageMB` in UserAdminProfile)  
   - Default: 150MB per USER_ADMIN per month
   - Tracks bandwidth/transfer usage for uploads

3. **Usage Tracking** (`monthlyUploadedBytes` in UserAdminProfile)
   - Individual counter per USER_ADMIN
   - Resets monthly based on `billingDayOfMonth`

### Files Modified
- `signox_backend/src/controllers/media.controller.js`
  - Updated import to include `incrementUserMonthlyUpload`
  - Changed `createMedia()` to use individual user upload tracking
  - Updated `listMedia()`, `deleteMedia()` to use role-based storage info
  - All functions now respect individual USER_ADMIN storage boundaries

### Database Schema
- ✅ **No schema changes needed** - UserAdminProfile already contains individual limits
- ✅ **Existing relationships support individual tracking** - createdById, userAdminProfile, etc.

### Testing Recommendations

1. **Create two USER_ADMINs under same CLIENT_ADMIN**
2. **Upload media as USER_ADMIN 1** → Check storage shows only in USER_ADMIN 1
3. **Upload media as USER_ADMIN 2** → Check storage shows only in USER_ADMIN 2  
4. **Login as CLIENT_ADMIN** → Should see aggregate storage from both
5. **Test STAFF uploads** → Should count toward their USER_ADMIN's storage
6. **Test storage limits** → Each USER_ADMIN should hit their own 25MB limit independently

### Benefits Achieved

✅ **Individual Storage Privacy**: Each USER_ADMIN has their own storage usage tracking
✅ **Independent Limits**: USER_ADMIN 1 can use full 25MB without affecting USER_ADMIN 2
✅ **Proper STAFF Inheritance**: STAFF uploads count toward their USER_ADMIN's storage
✅ **Management Oversight**: CLIENT_ADMIN can still see aggregate usage for management
✅ **Accurate Quotas**: Monthly upload quotas are tracked individually per USER_ADMIN
✅ **Scalability**: Each USER_ADMIN can grow independently without affecting others

### Consistency with System Architecture

This fix completes the individual USER_ADMIN model:

- ✅ Individual company identity (`companyName`, `contactNumber`)
- ✅ Individual limits (`maxDisplays`, `maxUsers`, `maxStorageMB`, `maxMonthlyUsageMB`)  
- ✅ Individual licenses (`licenseExpiry`)
- ✅ Individual media libraries (previously fixed)
- ✅ **Individual storage tracking** (NOW FIXED)

Each USER_ADMIN now truly represents their own independent company with complete storage isolation from other USER_ADMINs under the same CLIENT_ADMIN.

---

**Status**: ✅ **STORAGE ISOLATION ISSUE RESOLVED**

The storage usage from one USER_ADMIN will no longer appear in another USER_ADMIN's account. Each USER_ADMIN now has their own independent storage tracking, limits, and quotas.