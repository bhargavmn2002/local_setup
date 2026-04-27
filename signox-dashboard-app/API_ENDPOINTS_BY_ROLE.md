# API Endpoints by Role - Android Dashboard App

## Overview
Different user roles use different API endpoints for user management operations.

---

## SUPER_ADMIN

### Create Client Admin
**Endpoint:** `POST /api/users/client-admin`

**Request Body:**
```json
{
  "email": "client@example.com",
  "password": "password123",
  "companyName": "Company Name",
  "maxDisplays": 10,
  "maxUsers": 5,
  "maxStorageMB": 25,
  "licenseExpiry": "2025-12-31" // optional
}
```

### List Client Admins
**Endpoint:** `GET /api/users/client-admins`

**Response:**
```json
{
  "clientAdmins": [
    {
      "id": "uuid",
      "email": "client@example.com",
      "isActive": true,
      "clientProfile": {
        "clientId": "CL-12345",
        "companyName": "Company Name",
        "maxDisplays": 10,
        "maxUsers": 5,
        "maxStorageMB": 25,
        "licenseExpiry": "2025-12-31"
      },
      "displaysUsed": 5
    }
  ]
}
```

### Update Client Admin
**Endpoint:** `PUT /api/users/client-admins/{id}`

**Request Body:**
```json
{
  "companyName": "Updated Company Name",
  "maxDisplays": 20,
  "maxUsers": 10,
  "maxStorageMB": 50,
  "licenseExpiry": "2026-12-31",
  "contactEmail": "contact@example.com",
  "contactPhone": "+1234567890"
}
```

### Suspend/Activate Client Admin
**Endpoint:** `PATCH /api/users/client-admins/{id}/status`

**Response:**
```json
{
  "message": "Client admin status updated",
  "isActive": false
}
```

### Delete Client Admin
**Endpoint:** `DELETE /api/users/client-admins/{id}`

---

## CLIENT_ADMIN

### Create User Admin
**Endpoint:** `POST /api/users`

**Request Body:**
```json
{
  "email": "useradmin@example.com",
  "password": "password123"
}
```

### List User Admins
**Endpoint:** `GET /api/users`

**Response:**
```json
{
  "role": "CLIENT_ADMIN",
  "users": [
    {
      "id": "uuid",
      "email": "useradmin@example.com",
      "role": "USER_ADMIN",
      "isActive": true,
      "managedByClientAdminId": "client-admin-id",
      "createdAt": "2024-01-01T00:00:00.000Z"
    }
  ]
}
```

### Delete User Admin
**Endpoint:** `DELETE /api/users/{id}`

### Reset Password
**Endpoint:** `PUT /api/users/{id}/reset-password`

**Request Body:**
```json
{
  "newPassword": "newpassword123"
}
```

---

## USER_ADMIN

### Create Staff User
**Endpoint:** `POST /api/users`

**Request Body:**
```json
{
  "email": "staff@example.com",
  "password": "password123",
  "staffRole": "CONTENT_MANAGER"
}
```

**Valid Staff Roles:**
- `DISPLAY_MANAGER`
- `BROADCAST_MANAGER`
- `CONTENT_MANAGER`
- `CMS_VIEWER`
- `POP_MANAGER`

### List Staff Users
**Endpoint:** `GET /api/users`

**Response:**
```json
{
  "role": "USER_ADMIN",
  "users": [
    {
      "id": "uuid",
      "email": "staff@example.com",
      "role": "STAFF",
      "staffRole": "CONTENT_MANAGER",
      "isActive": true,
      "createdAt": "2024-01-01T00:00:00.000Z"
    }
  ]
}
```

### Delete Staff User
**Endpoint:** `DELETE /api/users/{id}`

### Reset Password
**Endpoint:** `PUT /api/users/{id}/reset-password`

**Request Body:**
```json
{
  "newPassword": "newpassword123"
}
```

### Bulk Delete Staff Users
**Endpoint:** `POST /api/users/bulk-delete`

**Request Body:**
```json
{
  "userIds": ["uuid1", "uuid2", "uuid3"]
}
```

---

## Implementation Notes

### Android App Changes Required

1. **UserRepository** - Add role-aware methods:
   - `createClientAdmin()` for SUPER_ADMIN
   - `getClientAdmins()` for SUPER_ADMIN
   - `updateClientAdmin()` for SUPER_ADMIN
   - `toggleClientAdminStatus()` for SUPER_ADMIN

2. **UserListFragment** - Show different UI based on role:
   - SUPER_ADMIN: Show Client Admin list with company info, suspend/activate buttons
   - CLIENT_ADMIN: Show User Admin list with basic info
   - USER_ADMIN: Show Staff list with staff roles

3. **UserCreateFragment** - Different forms based on role:
   - SUPER_ADMIN: Form with company details, limits, license expiry
   - CLIENT_ADMIN: Simple form with email/password
   - USER_ADMIN: Form with email/password/staffRole dropdown

4. **UserAdapter** - Display different information:
   - SUPER_ADMIN view: Company name, Client ID, displays used/max, license status
   - CLIENT_ADMIN view: Email, status
   - USER_ADMIN view: Email, staff role, status

---

## Error Handling

### Common Errors

**403 Forbidden:**
```json
{
  "message": "Insufficient permissions to create this user"
}
```
- User is trying to create a user type they don't have permission for

**409 Conflict:**
```json
{
  "message": "User already exists"
}
```
- Email is already registered

**400 Bad Request:**
```json
{
  "message": "Company name is required"
}
```
- Missing required fields for the operation

---

## Testing Checklist

- [ ] SUPER_ADMIN can create CLIENT_ADMIN with company details
- [ ] SUPER_ADMIN can list all CLIENT_ADMIN users
- [ ] SUPER_ADMIN can suspend/activate CLIENT_ADMIN
- [ ] SUPER_ADMIN can edit CLIENT_ADMIN limits
- [ ] SUPER_ADMIN can delete CLIENT_ADMIN
- [ ] CLIENT_ADMIN can create USER_ADMIN
- [ ] CLIENT_ADMIN can list their USER_ADMIN users
- [ ] CLIENT_ADMIN can delete USER_ADMIN
- [ ] CLIENT_ADMIN can reset USER_ADMIN password
- [ ] USER_ADMIN can create STAFF with staff roles
- [ ] USER_ADMIN can list their STAFF users
- [ ] USER_ADMIN can delete STAFF
- [ ] USER_ADMIN can reset STAFF password
- [ ] USER_ADMIN can bulk delete STAFF users
