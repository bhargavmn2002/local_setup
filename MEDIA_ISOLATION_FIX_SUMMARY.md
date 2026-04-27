# Media Isolation Fix - USER_ADMIN Level Isolation

## ✅ ISSUE FIXED: Media isolation between USER_ADMINs under the same CLIENT_ADMIN

### The Problem
When you created two USER_ADMIN accounts under the same CLIENT_ADMIN (representing different organizations/companies), the media uploaded by one USER_ADMIN was being shown to the other USER_ADMIN.

**Before Fix:**
```
Client Admin A
├── User Admin 1 (Company A) uploads media X
└── User Admin 2 (Company B) can see media X ← WRONG!
```

### Root Cause
The media controller was using **organization-level sharing** logic where all USER_ADMINs under the same CLIENT_ADMIN shared media:

```javascript
// OLD LOGIC - Shared media between USER_ADMINs
const userAdmins = await prisma.user.findMany({
  where: {
    role: 'USER_ADMIN',
    managedByClientAdminId: clientAdminId  // Same client = shared media
  }
});

// All USER_ADMINs under same CLIENT_ADMIN shared media
const userIds = [clientAdminId, ...userAdminIds, ...staffIds];
```

### The Fix Applied
Updated the media controller to provide **strict USER_ADMIN isolation** with role-based filtering:

#### New Logic by Role:

**CLIENT_ADMIN** (Management View):
- Can see ALL media from all USER_ADMINs under them
- Needed for management and oversight purposes

**USER_ADMIN** (Individual Company View):
- Can ONLY see their own media + their staff's media
- Complete isolation from other USER_ADMINs
- Each USER_ADMIN has their own private media library

**STAFF** (Team View):
- Can see media from their USER_ADMIN + sibling staff
- Inherits access from their USER_ADMIN manager

**SUPER_ADMIN** (System View):
- Can see all media (no filtering)
- System administration purposes

### Code Changes Made

#### 1. Updated `listMedia()` Function
```javascript
// NEW LOGIC - Role-based isolation
if (req.user.role === 'USER_ADMIN') {
  // USER_ADMIN only sees their own media + their staff's media
  const staffUsers = await prisma.user.findMany({
    where: {
      role: 'STAFF',
      createdByUserAdminId: req.user.id  // Only THIS user admin's staff
    }
  });

  userIds = [req.user.id, ...staffUsers.map(s => s.id)];
}
```

#### 2. Updated `deleteMedia()` Function
- Same role-based filtering logic
- USER_ADMIN can only delete their own media + their staff's media
- CLIENT_ADMIN can delete media from all their USER_ADMINs

#### 3. Updated `updateMedia()` Function
- Same role-based filtering logic
- USER_ADMIN can only update their own media + their staff's media
- CLIENT_ADMIN can update media from all their USER_ADMINs

### Result After Fix

**✅ Strict USER_ADMIN Isolation:**
```
Client Admin A
├── User Admin 1 (Company A) uploads media X → Only User Admin 1 sees media X
└── User Admin 2 (Company B) uploads media Y → Only User Admin 2 sees media Y
```

**✅ Management Oversight:**
```
Client Admin A → Can see both media X and media Y (for management)
```

**✅ Staff Inheritance:**
```
User Admin 1 (Company A)
├── Staff 1A → Can see media X (from User Admin 1)
└── Staff 1B → Can see media X (from User Admin 1)

User Admin 2 (Company B)  
├── Staff 2A → Can see media Y (from User Admin 2)
└── Staff 2B → Can see media Y (from User Admin 2)
```

### Files Modified
- `signox_backend/src/controllers/media.controller.js`
  - `listMedia()` function - Updated filtering logic
  - `deleteMedia()` function - Updated access control
  - `updateMedia()` function - Updated access control

### Testing Recommendations

1. **Create two USER_ADMINs under same CLIENT_ADMIN**
2. **Upload media as USER_ADMIN 1**
3. **Login as USER_ADMIN 2** → Should NOT see USER_ADMIN 1's media
4. **Login as CLIENT_ADMIN** → Should see media from both USER_ADMINs
5. **Test STAFF users** → Should only see their USER_ADMIN's media

### Benefits Achieved

✅ **Individual Company Privacy**: Each USER_ADMIN has their own private media library
✅ **Matches Migration Model**: Aligns with individual company identity from the user limits migration
✅ **Management Oversight**: CLIENT_ADMIN retains visibility for management purposes
✅ **Staff Inheritance**: STAFF users properly inherit from their USER_ADMIN
✅ **Security**: Prevents accidental cross-company media access
✅ **Scalability**: Works as organizations grow with multiple USER_ADMINs

### Consistency with System Architecture

This fix completes the individual USER_ADMIN model established in the user limits migration:

- ✅ Individual company identity (`companyName`, `contactNumber`)
- ✅ Individual limits (`maxDisplays`, `maxUsers`, `maxStorageMB`)  
- ✅ Individual licenses (`licenseExpiry`)
- ✅ **Individual media libraries** (NOW FIXED)

Each USER_ADMIN now truly represents their own independent company with complete isolation from other USER_ADMINs under the same CLIENT_ADMIN.

---

**Status**: ✅ **MEDIA ISOLATION ISSUE RESOLVED**

The media uploaded by one USER_ADMIN will no longer appear in another USER_ADMIN's account, even when they belong to the same CLIENT_ADMIN.