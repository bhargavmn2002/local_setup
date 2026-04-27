# Display Limits Isolation Fix - Individual USER_ADMIN Display Limits

## ✅ ISSUE FIXED: Display limits being shared at CLIENT_ADMIN level instead of individual USER_ADMIN level

### The Problem
Display limits were being enforced at the **CLIENT_ADMIN organization level** instead of **individual USER_ADMIN level**. This meant that all USER_ADMINs under the same CLIENT_ADMIN shared the same display limit pool, which contradicted the individual company model.

**Before Fix:**
```
Client Admin A (10 display limit total)
├── User Admin 1 (Company A) creates 8 displays → Uses 8/10 of shared pool
└── User Admin 2 (Company B) can only create 2 more displays ← WRONG!
```

### Root Cause
The display controller was checking **CLIENT_ADMIN level limits** from `ClientProfile` instead of **individual USER_ADMIN limits** from `UserAdminProfile`:

```javascript
// OLD LOGIC - Shared display limits
const profile = await prisma.clientProfile.findUnique({
  where: { clientAdminId },
});

const maxDisplays = profile?.maxDisplays ?? 10; // ❌ Shared across all USER_ADMINs

const activeDisplays = await prisma.display.count({
  where: {
    clientAdminId, // ❌ Counts displays from ALL USER_ADMINs
    isPaired: true,
    status: { not: 'PAIRING' },
  },
});
```

### The Fix Applied

Updated the display controller to use **individual USER_ADMIN display limits** with role-based enforcement:

#### **For USER_ADMIN Role** (Individual Limits):
```javascript
// NEW LOGIC - Individual USER_ADMIN limits
const userAdminProfile = await prisma.userAdminProfile.findUnique({
  where: { userAdminId: req.user.id },
});

const maxDisplays = userAdminProfile?.maxDisplays ?? 10; // ✅ Individual limit

// Count displays for THIS USER_ADMIN only
const activeDisplays = await prisma.display.count({
  where: {
    managedByUserId: req.user.id, // ✅ Only this USER_ADMIN's displays
    isPaired: true,
    status: { not: 'PAIRING' },
  },
});
```

#### **For CLIENT_ADMIN Role** (Management Oversight):
```javascript
// NEW LOGIC - Aggregate limits for CLIENT_ADMIN management
const userAdmins = await prisma.user.findMany({
  where: {
    role: 'USER_ADMIN',
    managedByClientAdminId: clientAdminId
  },
  include: { userAdminProfile: true }
});

// Calculate total allowed displays across all USER_ADMINs
const totalMaxDisplays = userAdmins.reduce((sum, ua) => 
  sum + (ua.userAdminProfile?.maxDisplays || 10), 0
);
```

### Result After Fix

**✅ Individual USER_ADMIN Display Limits:**
```
Client Admin A
├── User Admin 1 (Company A) 
│   ├── Display Limit: 10 displays
│   └── Can create 10 displays independently
└── User Admin 2 (Company B)
    ├── Display Limit: 10 displays  
    └── Can create 10 displays independently (total: 20 displays under Client Admin A)
```

**✅ CLIENT_ADMIN Management View:**
```
Client Admin A → Can create displays up to aggregate limit (20 displays total)
```

**✅ Proper Error Messages:**
```javascript
// USER_ADMIN gets specific error message
error: `Display limit reached. You can have maximum ${maxDisplays} displays. Currently active: ${activeDisplays}. Contact your administrator to increase your limit.`

// CLIENT_ADMIN gets aggregate error message  
error: `Total display limit reached across all user admins. Maximum allowed: ${totalMaxDisplays}. Currently active: ${activeDisplays}.`
```

### How Individual Display Limits Work

#### **USER_ADMIN Display Creation**:
1. Check `userAdminProfile.maxDisplays` for this specific USER_ADMIN
2. Count displays where `managedByUserId = req.user.id` (only their displays)
3. Allow creation if `activeDisplays < maxDisplays` for this USER_ADMIN
4. Each USER_ADMIN has their own independent display quota

#### **CLIENT_ADMIN Display Creation**:
1. Calculate aggregate `totalMaxDisplays` across all USER_ADMINs under them
2. Count displays where `clientAdminId = req.user.id` (all displays under this client)
3. Allow creation if total displays < total aggregate limit
4. CLIENT_ADMIN can create displays up to the sum of all USER_ADMIN limits

#### **Display Ownership**:
- Displays created by USER_ADMIN: `managedByUserId = userAdminId`
- Displays created by CLIENT_ADMIN: `clientAdminId = clientAdminId` (for management)
- Each display counts toward the creator's individual limit

### Database Schema Support

The existing schema already supported individual display limits:

```prisma
model UserAdminProfile {
  maxDisplays   Int @default(10) // ✅ Individual display limit per USER_ADMIN
}

model Display {
  managedByUserId String? @db.ObjectId // ✅ Tracks which USER_ADMIN manages this display
  clientAdminId   String? @db.ObjectId // ✅ Tracks which CLIENT_ADMIN for management
}
```

### Files Modified
- `signox_backend/src/controllers/display.controller.js`
  - Updated display pairing logic to use individual USER_ADMIN limits
  - Added role-based limit checking (USER_ADMIN vs CLIENT_ADMIN)
  - Improved error messages with specific limit information

### Analytics Already Correct
The analytics controller was already correctly implemented:
- ✅ `analytics.controller.js` uses `userAdminProfile?.maxDisplays` for USER_ADMIN
- ✅ Shows individual display counts and limits per USER_ADMIN
- ✅ CLIENT_ADMIN sees aggregate display counts without shared limits

### Testing Recommendations

1. **Create two USER_ADMINs under same CLIENT_ADMIN**
2. **Set different display limits** (e.g., USER_ADMIN 1: 5 displays, USER_ADMIN 2: 3 displays)
3. **Test USER_ADMIN 1** → Should be able to create 5 displays independently
4. **Test USER_ADMIN 2** → Should be able to create 3 displays independently  
5. **Test CLIENT_ADMIN** → Should be able to create up to 8 displays total (5+3)
6. **Test limit enforcement** → Each USER_ADMIN should hit their own limit independently

### Benefits Achieved

✅ **Individual Display Quotas**: Each USER_ADMIN has their own display limit (default: 10)
✅ **Independent Growth**: USER_ADMIN 1 can use all 10 displays without affecting USER_ADMIN 2
✅ **Proper Error Messages**: Clear feedback about individual vs aggregate limits
✅ **Management Oversight**: CLIENT_ADMIN can still create displays with aggregate limit awareness
✅ **Scalability**: Each USER_ADMIN can be assigned different display limits as needed
✅ **Consistency**: Aligns with individual company model (storage, media, licenses)

### Consistency with System Architecture

This fix completes the individual USER_ADMIN model:

- ✅ Individual company identity (`companyName`, `contactNumber`)
- ✅ Individual limits (`maxUsers`, `maxStorageMB`, `maxMonthlyUsageMB`)  
- ✅ Individual licenses (`licenseExpiry`)
- ✅ Individual media libraries (previously fixed)
- ✅ Individual storage tracking (previously fixed)
- ✅ **Individual display limits** (NOW FIXED)

Each USER_ADMIN now truly represents their own independent company with complete display limit isolation from other USER_ADMINs under the same CLIENT_ADMIN.

---

**Status**: ✅ **DISPLAY LIMITS ISOLATION ISSUE RESOLVED**

Display limits are now enforced individually per USER_ADMIN. Each USER_ADMIN can create displays up to their own `maxDisplays` limit without being affected by other USER_ADMINs' display usage under the same CLIENT_ADMIN.