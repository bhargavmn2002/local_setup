const prisma = require('../config/db');
const bcrypt = require('bcryptjs');

/**
 * POST /api/users
 * Role-aware user creation based on requester role.
 *
 * SUPER_ADMIN -> creates CLIENT_ADMIN + ClientProfile (no limits)
 * CLIENT_ADMIN -> creates USER_ADMIN + UserAdminProfile (with limits)
 * USER_ADMIN -> creates STAFF (linked via createdByUserAdminId, requires staffRole)
 */
exports.createUser = async (req, res) => {
  try {
    const requester = req.user;
    const { 
      email, 
      password, 
      companyName, 
      maxDisplays, 
      maxUsers, 
      maxStorageMB,
      maxMonthlyUsageMB,
      licenseExpiry, 
      staffRole 
    } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password required' });
    }

    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      return res.status(409).json({ message: 'User already exists' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    if (requester.role === 'SUPER_ADMIN') {
      // Create CLIENT_ADMIN + ClientProfile (no limits at this level)
      if (!companyName) {
        return res.status(400).json({ message: 'Company name is required' });
      }

      const created = await prisma.$transaction(async (tx) => {
        const clientAdmin = await tx.user.create({
          data: {
            email,
            password: hashedPassword,
            role: 'CLIENT_ADMIN',
            isActive: true,
          },
        });

        // Generate unique client ID
        const clientCount = await tx.clientProfile.count();
        const clientId = `CL-${String(clientCount + 1).padStart(5, '0')}`;

        const profile = await tx.clientProfile.create({
          data: {
            clientAdminId: clientAdmin.id,
            clientId,
            companyName,
            licenseExpiry: licenseExpiry ? new Date(licenseExpiry) : null,
            isActive: true,
            contactEmail: email,
          },
        });

        return { clientAdmin, profile };
      });

      return res.status(201).json({
        message: 'Client Admin created successfully',
        user: {
          id: created.clientAdmin.id,
          email: created.clientAdmin.email,
          role: created.clientAdmin.role,
          isActive: created.clientAdmin.isActive,
          clientProfile: created.profile,
        },
      });
    }

    if (requester.role === 'CLIENT_ADMIN') {
      // Create USER_ADMIN + UserAdminProfile with limits
      const { 
        companyName, 
        contactNumber,
        ...otherFields 
      } = req.body;

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

        // Create UserAdminProfile with limits and company info
        const profile = await tx.userAdminProfile.create({
          data: {
            userAdminId: userAdmin.id,
            companyName: companyName || null,
            contactNumber: contactNumber || null,
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

      return res.status(201).json({
        message: 'User Admin created successfully',
        user: {
          id: created.userAdmin.id,
          email: created.userAdmin.email,
          role: created.userAdmin.role,
          isActive: created.userAdmin.isActive,
          managedByClientAdminId: created.userAdmin.managedByClientAdminId,
          userAdminProfile: {
            ...created.profile,
            monthlyUploadedBytes: Number(created.profile.monthlyUploadedBytes || 0)
          },
        },
      });
    }

    if (requester.role === 'USER_ADMIN') {
      // Create STAFF member under this User Admin
      if (!staffRole) {
        return res.status(400).json({ message: 'staffRole is required for staff users' });
      }

      const allowedStaffRoles = [
        'DISPLAY_MANAGER',
        'BROADCAST_MANAGER',
        'CONTENT_MANAGER',
        'CMS_VIEWER',
        'POP_MANAGER',
      ];

      if (!allowedStaffRoles.includes(staffRole)) {
        return res
          .status(400)
          .json({ message: `Invalid staffRole. Allowed: ${allowedStaffRoles.join(', ')}` });
      }

      const staffUser = await prisma.user.create({
        data: {
          email,
          password: hashedPassword,
          role: 'STAFF',
          staffRole,
          isActive: true,
          createdByUserAdminId: requester.id,
        },
        select: {
          id: true,
          email: true,
          role: true,
          staffRole: true,
          isActive: true,
          createdAt: true,
        },
      });

      return res.status(201).json({
        message: 'Staff user created successfully',
        user: staffUser,
      });
    }

    return res.status(403).json({ message: 'Insufficient permissions to create this user' });
  } catch (error) {
    console.error('Create User Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/users
 * Returns only users managed by the requester, based on hierarchy.
 * Supports query parameters for filtering.
 */
exports.listUsers = async (req, res) => {
  try {
    const requester = req.user;
    const { managedByClientAdminId, createdByUserAdminId } = req.query;

    if (requester.role === 'SUPER_ADMIN') {
      // If managedByClientAdminId is provided, return USER_ADMIN users under that CLIENT_ADMIN
      if (managedByClientAdminId) {
        const userAdmins = await prisma.user.findMany({
          where: {
            role: 'USER_ADMIN',
            managedByClientAdminId: managedByClientAdminId,
          },
          include: {
            userAdminProfile: true,
          },
          orderBy: { createdAt: 'desc' },
        });

        return res.json({
          role: 'SUPER_ADMIN',
          users: userAdmins.map((u) => ({
            id: u.id,
            email: u.email,
            role: u.role,
            isActive: u.isActive,
            createdAt: u.createdAt,
            managedByClientAdminId: u.managedByClientAdminId,
            userAdminProfile: u.userAdminProfile ? {
              ...u.userAdminProfile,
              monthlyUploadedBytes: Number(u.userAdminProfile.monthlyUploadedBytes || 0)
            } : null,
          })),
        });
      }

      // Default: return CLIENT_ADMIN users
      const clientAdmins = await prisma.user.findMany({
        where: { role: 'CLIENT_ADMIN' },
        include: {
          clientProfile: true,
        },
        orderBy: { createdAt: 'desc' },
      });

      return res.json({
        role: 'SUPER_ADMIN',
        users: clientAdmins.map((u) => ({
          id: u.id,
          email: u.email,
          role: u.role,
          isActive: u.isActive,
          createdAt: u.createdAt,
          clientProfile: u.clientProfile,
        })),
      });
    }

    if (requester.role === 'CLIENT_ADMIN') {
      // If createdByUserAdminId is provided, return STAFF users under that USER_ADMIN
      if (createdByUserAdminId) {
        const staff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: createdByUserAdminId,
          },
          orderBy: { createdAt: 'desc' },
          select: {
            id: true,
            email: true,
            role: true,
            staffRole: true,
            isActive: true,
            createdAt: true,
          },
        });

        return res.json({
          role: 'CLIENT_ADMIN',
          users: staff,
        });
      }

      // Default: return USER_ADMIN users
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: requester.id,
        },
        include: {
          userAdminProfile: true,
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

    if (requester.role === 'USER_ADMIN') {
      const staff = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: requester.id,
        },
        orderBy: { createdAt: 'desc' },
        select: {
          id: true,
          email: true,
          role: true,
          staffRole: true,
          isActive: true,
          createdAt: true,
        },
      });

      return res.json({
        role: 'USER_ADMIN',
        users: staff,
      });
    }

    return res.status(403).json({ message: 'User listing not available for this role' });
  } catch (error) {
    console.error('List Users Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/users/:id
 * Role-aware user update based on requester role.
 * 
 * CLIENT_ADMIN -> updates USER_ADMIN + UserAdminProfile (with limits)
 * USER_ADMIN -> updates STAFF (staffRole only)
 */
exports.updateUser = async (req, res) => {
  try {
    const requester = req.user;
    const { id } = req.params;
    const { 
      email, 
      companyName,
      contactNumber,
      maxDisplays, 
      maxUsers, 
      maxStorageMB,
      maxMonthlyUsageMB,
      licenseExpiry, 
      staffRole,
      isActive
    } = req.body;

    // Find the user to update
    const userToUpdate = await prisma.user.findUnique({
      where: { id },
      include: {
        userAdminProfile: true,
        clientProfile: true,
      },
    });

    if (!userToUpdate) {
      return res.status(404).json({ message: 'User not found' });
    }

    if (requester.role === 'CLIENT_ADMIN') {
      // CLIENT_ADMIN can only update USER_ADMIN users they manage
      if (userToUpdate.role !== 'USER_ADMIN' || userToUpdate.managedByClientAdminId !== requester.id) {
        return res.status(403).json({ message: 'You can only update User Admins you manage' });
      }

      // Check if email is being changed and if it's already taken
      if (email && email !== userToUpdate.email) {
        const existing = await prisma.user.findUnique({ where: { email } });
        if (existing) {
          return res.status(409).json({ message: 'Email already exists' });
        }
      }

      const updated = await prisma.$transaction(async (tx) => {
        // Update user basic info
        const user = await tx.user.update({
          where: { id },
          data: {
            ...(email && { email }),
            ...(typeof isActive === 'boolean' && { isActive }),
          },
        });

        // Update UserAdminProfile with limits
        let profile = null;
        if (userToUpdate.userAdminProfile) {
          profile = await tx.userAdminProfile.update({
            where: { userAdminId: id },
            data: {
              ...(companyName !== undefined && { companyName }),
              ...(contactNumber !== undefined && { contactNumber }),
              ...(maxDisplays !== undefined && { maxDisplays: Number(maxDisplays) }),
              ...(maxUsers !== undefined && { maxUsers: Number(maxUsers) }),
              ...(maxStorageMB !== undefined && { maxStorageMB: Number(maxStorageMB) }),
              ...(maxMonthlyUsageMB !== undefined && { maxMonthlyUsageMB: Number(maxMonthlyUsageMB) }),
              ...(licenseExpiry !== undefined && { 
                licenseExpiry: licenseExpiry ? new Date(licenseExpiry) : null 
              }),
              ...(typeof isActive === 'boolean' && { isActive }),
            },
          });
        }

        return { user, profile };
      });

      return res.json({
        message: 'User Admin updated successfully',
        user: {
          id: updated.user.id,
          email: updated.user.email,
          role: updated.user.role,
          isActive: updated.user.isActive,
          managedByClientAdminId: updated.user.managedByClientAdminId,
          userAdminProfile: updated.profile ? {
            ...updated.profile,
            monthlyUploadedBytes: Number(updated.profile.monthlyUploadedBytes || 0)
          } : null,
        },
      });
    }

    if (requester.role === 'USER_ADMIN') {
      // USER_ADMIN can only update STAFF users they created
      if (userToUpdate.role !== 'STAFF' || userToUpdate.createdByUserAdminId !== requester.id) {
        return res.status(403).json({ message: 'You can only update Staff users you created' });
      }

      // Check if email is being changed and if it's already taken
      if (email && email !== userToUpdate.email) {
        const existing = await prisma.user.findUnique({ where: { email } });
        if (existing) {
          return res.status(409).json({ message: 'Email already exists' });
        }
      }

      const allowedStaffRoles = [
        'DISPLAY_MANAGER',
        'BROADCAST_MANAGER',
        'CONTENT_MANAGER',
        'CMS_VIEWER',
        'POP_MANAGER',
      ];

      if (staffRole && !allowedStaffRoles.includes(staffRole)) {
        return res.status(400).json({ 
          message: `Invalid staffRole. Allowed: ${allowedStaffRoles.join(', ')}` 
        });
      }

      const updatedUser = await prisma.user.update({
        where: { id },
        data: {
          ...(email && { email }),
          ...(staffRole && { staffRole }),
          ...(typeof isActive === 'boolean' && { isActive }),
        },
        select: {
          id: true,
          email: true,
          role: true,
          staffRole: true,
          isActive: true,
          createdAt: true,
        },
      });

      return res.json({
        message: 'Staff user updated successfully',
        user: updatedUser,
      });
    }

    return res.status(403).json({ message: 'Insufficient permissions to update this user' });
  } catch (error) {
    console.error('Update User Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * DELETE /api/users/:id
 * Role-aware delete:
 * - SUPER_ADMIN -> NO LONGER CAN SUSPEND CLIENT_ADMIN (removed functionality)
 * - CLIENT_ADMIN -> hard deletes USER_ADMIN (permanent deletion with data cleanup)
 * - USER_ADMIN -> hard deletes STAFF
 */
exports.deleteUser = async (req, res) => {
  try {
    const requester = req.user;
    const { id } = req.params;

    if (!id) return res.status(400).json({ message: 'User ID is required' });

    const target = await prisma.user.findUnique({
      where: { id },
      include: { 
        clientProfile: true,
        userAdminProfile: true,
      },
    });

    if (!target) {
      return res.status(404).json({ message: 'User not found' });
    }

    if (requester.role === 'SUPER_ADMIN') {
      // SUPER_ADMIN can no longer suspend CLIENT_ADMIN
      return res.status(403).json({ 
        message: 'Super Admin can no longer suspend Client Admins. This functionality has been removed.' 
      });
    }

    if (requester.role === 'CLIENT_ADMIN') {
      if (target.role !== 'USER_ADMIN' || target.managedByClientAdminId !== requester.id) {
        return res
          .status(403)
          .json({ message: 'CLIENT_ADMIN can only delete their own USER_ADMIN users' });
      }

      // Hard delete USER_ADMIN and related records including UserAdminProfile.
      // Important: Displays belong to CLIENT_ADMIN; we only unassign them from the deleted USER_ADMIN.
      await prisma.$transaction(async (tx) => {
        // 1) Staff under this User Admin
        const staff = await tx.user.findMany({
          where: { role: 'STAFF', createdByUserAdminId: target.id },
          select: { id: true },
        });
        const staffIds = staff.map((s) => s.id);

        // 2) All user IDs that may own content (USER_ADMIN + its STAFF)
        const allUserIds = [target.id, ...staffIds];

        // 3) Collect playlist/layout IDs for deep cleanup
        const playlists = await tx.playlist.findMany({
          where: { createdById: { in: allUserIds } },
          select: { id: true },
        });
        const playlistIds = playlists.map((p) => p.id);

        const layouts = await tx.layout.findMany({
          where: { createdById: { in: allUserIds } },
          select: { id: true },
        });
        const layoutIds = layouts.map((l) => l.id);

        // 4) Playlist items
        if (playlistIds.length > 0) {
          await tx.playlistItem.deleteMany({
            where: { playlistId: { in: playlistIds } },
          });
        }

        // 5) Deep layout children
        if (layoutIds.length > 0) {
          await tx.widget.deleteMany({ where: { layoutId: { in: layoutIds } } });

          const sections = await tx.layoutSection.findMany({
            where: { layoutId: { in: layoutIds } },
            select: { id: true },
          });
          const sectionIds = sections.map((s) => s.id);

          if (sectionIds.length > 0) {
            await tx.layoutSectionItem.deleteMany({
              where: { sectionId: { in: sectionIds } },
            });
          }

          await tx.layoutSection.deleteMany({ where: { layoutId: { in: layoutIds } } });
          await tx.zone.deleteMany({ where: { layoutId: { in: layoutIds } } });
          await tx.layoutZone.deleteMany({ where: { layoutId: { in: layoutIds } } });
        }

        // 6) Schedules created by these users + join table
        const schedules = await tx.schedule.findMany({
          where: { createdById: { in: allUserIds } },
          select: { id: true },
        });
        const scheduleIds = schedules.map((s) => s.id);

        if (scheduleIds.length > 0) {
          await tx.scheduleDisplay.deleteMany({
            where: { scheduleId: { in: scheduleIds } },
          });
        }

        await tx.schedule.deleteMany({
          where: { createdById: { in: allUserIds } },
        });

        // 7) Unassign displays from this USER_ADMIN (stop/manage reset)
        await tx.display.updateMany({
          where: { managedByUserId: target.id, clientAdminId: requester.id },
          data: {
            playlistId: null,
            layoutId: null,
            activeLayoutId: null,
            managedByUserId: null,
          },
        });

        // 8) Delete content created by these users
        await tx.playlist.deleteMany({ where: { createdById: { in: allUserIds } } });
        await tx.layout.deleteMany({ where: { createdById: { in: allUserIds } } });
        await tx.media.deleteMany({ where: { createdById: { in: allUserIds } } });

        // 9) Delete UserAdminProfile if exists
        if (target.userAdminProfile) {
          await tx.userAdminProfile.delete({ where: { id: target.userAdminProfile.id } });
        }

        // 10) Delete staff users, then the user admin
        if (staffIds.length > 0) {
          await tx.user.deleteMany({ where: { id: { in: staffIds } } });
        }

        await tx.user.delete({ where: { id: target.id } });
      });

      return res.json({ message: 'User Admin deleted successfully' });
    }

    if (requester.role === 'USER_ADMIN') {
      if (target.role !== 'STAFF' || target.createdByUserAdminId !== requester.id) {
        return res
          .status(403)
          .json({ message: 'USER_ADMIN can only delete their own STAFF users' });
      }

      // Hard delete staff user (permanent deletion)
      await prisma.user.delete({
        where: { id: target.id },
      });

      return res.json({ message: 'Staff user deleted successfully' });
    }

    return res.status(403).json({ message: 'Insufficient permissions to delete this user' });
  } catch (error) {
    console.error('Delete User Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PATCH /api/users/:id/suspend
 * Toggle suspension status for USER_ADMIN (CLIENT_ADMIN only)
 */
exports.toggleUserAdminSuspension = async (req, res) => {
  try {
    const requester = req.user;
    const { id } = req.params;

    if (!id) return res.status(400).json({ message: 'User ID is required' });

    // Only CLIENT_ADMIN can suspend/reactivate USER_ADMIN
    if (requester.role !== 'CLIENT_ADMIN') {
      return res.status(403).json({ message: 'Only Client Admins can suspend/reactivate User Admins' });
    }

    const target = await prisma.user.findUnique({
      where: { id },
      include: { 
        userAdminProfile: true,
      },
    });

    if (!target) {
      return res.status(404).json({ message: 'User not found' });
    }

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

      // 4) If suspending, unassign displays; if reactivating, leave displays unassigned (manual reassignment)
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

/**
 * DELETE /api/users/bulk
 * Bulk delete multiple staff users (USER_ADMIN only)
 */
exports.bulkDeleteUsers = async (req, res) => {
  try {
    const requester = req.user;
    const { userIds } = req.body;

    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return res.status(400).json({ message: 'User IDs array is required' });
    }

    if (requester.role !== 'USER_ADMIN') {
      return res.status(403).json({ message: 'Only USER_ADMIN can bulk delete staff users' });
    }

    // Verify all users are STAFF and belong to this USER_ADMIN
    const staffUsers = await prisma.user.findMany({
      where: {
        id: { in: userIds },
        role: 'STAFF',
        createdByUserAdminId: requester.id,
      },
    });

    if (staffUsers.length !== userIds.length) {
      return res.status(403).json({ 
        message: 'Some users cannot be deleted. You can only delete your own STAFF users.' 
      });
    }

    // Delete all staff users
    await prisma.user.deleteMany({
      where: {
        id: { in: userIds },
      },
    });

    return res.json({ 
      message: `${userIds.length} staff user(s) deleted successfully`,
      deletedCount: userIds.length
    });
  } catch (error) {
    console.error('Bulk Delete Users Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/users/profile/settings
 * Get current user's profile for settings screen (flat structure for backward compatibility)
 */
exports.getUserProfileSettings = async (req, res) => {
  try {
    const userId = req.user.id;

    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        clientProfile: true,
        userAdminProfile: true,
        managedByClientAdmin: {
          include: {
            clientProfile: true
          }
        },
        createdByUserAdmin: {
          include: {
            userAdminProfile: true,
            managedByClientAdmin: {
              include: {
                clientProfile: true
              }
            }
          }
        }
      }
    });

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Determine company name based on role
    let companyName = null;
    if (user.role === 'CLIENT_ADMIN') {
      companyName = user.clientProfile?.companyName || null;
    } else if (user.role === 'USER_ADMIN' && user.managedByClientAdmin) {
      companyName = user.managedByClientAdmin.clientProfile?.companyName || null;
    } else if (user.role === 'STAFF' && user.createdByUserAdmin?.managedByClientAdmin) {
      companyName = user.createdByUserAdmin.managedByClientAdmin.clientProfile?.companyName || null;
    }

    // Return flat profile structure for settings screen
    res.json({
      id: user.id,
      name: user.name || null,
      email: user.email,
      role: user.role,
      staffRole: user.staffRole,
      companyName: companyName,
      profilePicture: null,
      createdAt: user.createdAt
    });
  } catch (error) {
    console.error('Get User Profile Settings Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/users/profile
 * Get current user's profile with hierarchical information
 */
exports.getUserProfile = async (req, res) => {
  try {
    const userId = req.user.id;

    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        clientProfile: true,
        userAdminProfile: true,
        managedByClientAdmin: {
          include: {
            clientProfile: true
          }
        },
        createdByUserAdmin: {
          include: {
            userAdminProfile: true,
            managedByClientAdmin: {
              include: {
                clientProfile: true
              }
            }
          }
        }
      }
    });

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Build hierarchical information
    let companyName = null;
    let clientAdmin = null;
    let userAdmin = null;

    if (user.role === 'CLIENT_ADMIN') {
      // CLIENT_ADMIN sees their own company
      companyName = user.clientProfile?.companyName || null;
    } else if (user.role === 'USER_ADMIN' && user.managedByClientAdmin) {
      // USER_ADMIN sees company and client admin info
      companyName = user.managedByClientAdmin.clientProfile?.companyName || null;
      clientAdmin = {
        id: user.managedByClientAdmin.id,
        email: user.managedByClientAdmin.email,
        name: user.managedByClientAdmin.name || null
      };
    } else if (user.role === 'STAFF' && user.createdByUserAdmin) {
      // STAFF sees full hierarchy: company -> client admin -> user admin -> self
      const userAdminData = user.createdByUserAdmin;
      const clientAdminData = userAdminData.managedByClientAdmin;
      
      if (clientAdminData) {
        companyName = clientAdminData.clientProfile?.companyName || null;
        clientAdmin = {
          id: clientAdminData.id,
          email: clientAdminData.email,
          name: clientAdminData.name || null
        };
      }
      
      userAdmin = {
        id: userAdminData.id,
        email: userAdminData.email,
        name: userAdminData.name || null
      };
    }

    // Return profile in format expected by Android app (ProfileResponse structure)
    // Convert BigInt to number for JSON serialization
    const clientProfileForResponse = user.clientProfile ? {
      ...user.clientProfile,
      // monthlyUploadedBytes removed - now in userAdminProfile
    } : null;

    const userAdminProfileForResponse = user.userAdminProfile ? {
      ...user.userAdminProfile,
      monthlyUploadedBytes: Number(user.userAdminProfile.monthlyUploadedBytes || 0)
    } : null;

    res.json({
      user: {
        id: user.id,
        name: user.name || null,
        email: user.email,
        role: user.role,
        staffRole: user.staffRole,
        isActive: user.isActive,
        managedByClientAdminId: user.managedByClientAdminId,
        clientProfile: clientProfileForResponse,
        userAdminProfile: userAdminProfileForResponse
      },
      hierarchy: {
        companyName: companyName,
        clientAdmin: clientAdmin,
        userAdmin: userAdmin
      }
    });
  } catch (error) {
    console.error('Get User Profile Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/users/profile/password
 * Update current user's password
 * Restricted to SUPER_ADMIN and CLIENT_ADMIN roles only
 */
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

    if (!currentPassword || !newPassword) {
      return res.status(400).json({ message: 'Current password and new password are required' });
    }

    // Get current user
    const user = await prisma.user.findUnique({
      where: { id: userId }
    });

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Verify current password
    const isCurrentPasswordValid = await bcrypt.compare(currentPassword, user.password);
    if (!isCurrentPasswordValid) {
      return res.status(400).json({ message: 'Current password is incorrect' });
    }

    // Hash new password
    const hashedNewPassword = await bcrypt.hash(newPassword, 10);

    // Update password
    await prisma.user.update({
      where: { id: userId },
      data: { password: hashedNewPassword }
    });

    res.json({ message: 'Password updated successfully' });
  } catch (error) {
    console.error('Update Password Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/users/profile
 * Update current user's profile (name only)
 */
exports.updateProfile = async (req, res) => {
  try {
    const userId = req.user.id;
    const { name } = req.body;

    if (!name || name.trim() === '') {
      return res.status(400).json({ message: 'Name is required' });
    }

    // Update user name
    const updatedUser = await prisma.user.update({
      where: { id: userId },
      data: { name: name.trim() },
      include: {
        clientProfile: true
      }
    });

    // Return updated profile
    const { password: _, ...userProfile } = updatedUser;
    
    res.json({
      id: userProfile.id,
      name: userProfile.name,
      email: userProfile.email,
      role: userProfile.role,
      staffRole: userProfile.staffRole,
      companyName: userProfile.clientProfile?.companyName || null,
      profilePicture: null,
      createdAt: userProfile.createdAt
    });
  } catch (error) {
    console.error('Update Profile Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/users/me/account
 * Get account information (for USER_ADMIN - limits are now at USER_ADMIN level)
 */
exports.getAccountInfo = async (req, res) => {
  try {
    const userId = req.user.id;
    const user = await prisma.user.findUnique({
      where: { id: userId },
      include: {
        userAdminProfile: true,
        managedByClientAdmin: {
          include: {
            clientProfile: true
          }
        }
      }
    });

    if (!user) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Only USER_ADMIN has account info with limits now
    if (user.role !== 'USER_ADMIN' || !user.userAdminProfile) {
      return res.status(403).json({ message: 'Account information not available for this user type' });
    }

    // Count current usage
    const [displayCount, userCount] = await Promise.all([
      prisma.display.count({
        where: { managedByUserId: userId }
      }),
      prisma.user.count({
        where: { createdByUserAdminId: userId }
      })
    ]);

    // Calculate storage usage (simplified - you may want to implement actual storage calculation)
    const storageUsed = 0; // TODO: Implement actual storage calculation

    // Get company name from parent CLIENT_ADMIN
    const companyName = user.managedByClientAdmin?.clientProfile?.companyName || 'Unknown Company';

    res.json({
      companyName: companyName,
      maxDisplays: user.userAdminProfile.maxDisplays,
      maxUsers: user.userAdminProfile.maxUsers,
      maxStorageMB: user.userAdminProfile.maxStorageMB,
      maxMonthlyUsageMB: user.userAdminProfile.maxMonthlyUsageMB,
      currentDisplays: displayCount,
      currentUsers: userCount,
      currentStorageMB: storageUsed,
      monthlyUploadedBytes: Number(user.userAdminProfile.monthlyUploadedBytes || 0),
      licenseExpiry: user.userAdminProfile.licenseExpiry,
      subscriptionStatus: user.userAdminProfile.isActive ? 'Active' : 'Inactive'
    });
  } catch (error) {
    console.error('Get Account Info Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};


/**
 * PUT /api/users/:id/reset-password
 * Reset password for a user (admin function)
 */
exports.resetUserPassword = async (req, res) => {
  try {
    const requester = req.user;
    const { id } = req.params;
    const { newPassword } = req.body;

    if (!id) return res.status(400).json({ message: 'User ID is required' });
    if (!newPassword) return res.status(400).json({ message: 'New password is required' });

    const target = await prisma.user.findUnique({
      where: { id },
      include: { clientProfile: true }
    });

    if (!target) {
      return res.status(404).json({ message: 'User not found' });
    }

    // Check permissions based on hierarchy
    let canReset = false;

    if (requester.role === 'SUPER_ADMIN') {
      canReset = true; // Super admin can reset anyone's password
    } else if (requester.role === 'CLIENT_ADMIN') {
      // Client admin can reset USER_ADMIN and STAFF passwords in their organization
      if (target.role === 'USER_ADMIN' && target.managedByClientAdminId === requester.id) {
        canReset = true;
      } else if (target.role === 'STAFF') {
        // Check if staff belongs to this client admin's organization
        const staffUserAdmin = await prisma.user.findUnique({
          where: { id: target.createdByUserAdminId },
          select: { managedByClientAdminId: true }
        });
        if (staffUserAdmin?.managedByClientAdminId === requester.id) {
          canReset = true;
        }
      }
    } else if (requester.role === 'USER_ADMIN') {
      // User admin can reset STAFF passwords they created
      if (target.role === 'STAFF' && target.createdByUserAdminId === requester.id) {
        canReset = true;
      }
    }

    if (!canReset) {
      return res.status(403).json({ message: 'Insufficient permissions to reset this user\'s password' });
    }

    // Hash new password
    const hashedPassword = await bcrypt.hash(newPassword, 10);

    // Update password
    await prisma.user.update({
      where: { id },
      data: { password: hashedPassword }
    });

    res.json({ 
      message: 'Password reset successfully',
      targetUser: {
        id: target.id,
        email: target.email,
        role: target.role
      }
    });
  } catch (error) {
    console.error('Reset User Password Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};