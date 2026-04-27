const prisma = require('../config/db');
const bcrypt = require('bcryptjs');

/**
 * Generate a unique client ID
 */
async function generateClientId() {
  let clientId;
  let isUnique = false;
  let attempts = 0;
  const maxAttempts = 100;

  while (!isUnique && attempts < maxAttempts) {
    // Generate format: CL-XXXXXX (6 random alphanumeric characters)
    const randomPart = Math.random().toString(36).substring(2, 8).toUpperCase();
    clientId = `CL-${randomPart}`;
    
    // Check if this ID already exists
    const existing = await prisma.clientProfile.findUnique({
      where: { clientId }
    });
    
    if (!existing) {
      isUnique = true;
    }
    attempts++;
  }

  if (!isUnique) {
    throw new Error('Unable to generate unique client ID after maximum attempts');
  }

  return clientId;
}

/**
 * POST /api/users/client-admin
 * Create a new Client Admin + linked ClientProfile (no limits, no license expiry - managed at USER_ADMIN level)
 */
exports.createClientAdmin = async (req, res) => {
  try {
    const {
      name, // optional display name / company contact name (not stored yet)
      email,
      password,
      companyName,
    } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: 'Email and password required' });
    }
    if (!companyName) {
      return res.status(400).json({ message: 'Company name required' });
    }

    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      return res.status(409).json({ message: 'User already exists' });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const created = await prisma.$transaction(async (tx) => {
      const user = await tx.user.create({
        data: {
          email,
          password: passwordHash,
          role: 'CLIENT_ADMIN',
          isActive: true,
        },
      });

      // Generate unique client ID
      const clientId = await generateClientId();

      const profile = await tx.clientProfile.create({
        data: {
          clientAdminId: user.id,
          clientId,
          companyName,
          isActive: true,
          contactEmail: email,
        },
      });

      return { user, profile };
    });

    res.status(201).json({
      message: 'Client Admin created',
      clientAdmin: {
        id: created.user.id,
        email: created.user.email,
        role: created.user.role,
        isActive: created.user.isActive,
        clientProfile: created.profile,
      },
      // keep name in response for UI even though not stored
      name: name || null,
    });
  } catch (error) {
    console.error('Create Client Admin Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/users/client-admins
 * List all Client Admin users + their ClientProfile and display usage.
 */
exports.listClientAdmins = async (req, res) => {
  try {
    const users = await prisma.user.findMany({
      where: { role: 'CLIENT_ADMIN' },
      orderBy: { createdAt: 'desc' },
      include: { clientProfile: true },
    });

    const now = new Date();

    const withUsage = await Promise.all(
      users.map(async (u) => {
        const displaysUsed = await prisma.display.count({
          where: { clientAdminId: u.id },
        });

        // Calculate license status
        let licenseStatus = 'active';
        let daysUntilExpiry = null;
        let isExpired = false;

        if (u.clientProfile) {
          // Check if profile is inactive
          if (!u.clientProfile.isActive) {
            licenseStatus = 'suspended';
          }
        }

        // Convert BigInt fields to numbers for JSON serialization (if any exist)
        const clientProfile = u.clientProfile ? {
          ...u.clientProfile
        } : null;

        return {
          id: u.id,
          email: u.email,
          role: u.role,
          isActive: u.isActive,
          createdAt: u.createdAt,
          clientProfile,
          displaysUsed,
          licenseStatus,
          daysUntilExpiry,
          isExpired,
        };
      })
    );

    res.json({
      clientAdmins: withUsage,
    });
  } catch (error) {
    console.error('List Client Admins Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PATCH /api/users/client-admins/:id/status
 * REMOVED: Super Admin can no longer suspend Client Admins
 * This functionality has been removed as per new requirements
 */
exports.toggleClientAdminStatus = async (req, res) => {
  return res.status(403).json({ 
    message: 'Client Admin suspension has been disabled. Only Client Admins can manage their User Admins.' 
  });
};

/**
 * PUT /api/users/client-admins/:id
 * Update client admin settings (no license expiry - managed at USER_ADMIN level)
 */
exports.updateClientAdmin = async (req, res) => {
  try {
    const { id } = req.params;
    const {
      companyName,
      contactEmail,
      contactPhone,
    } = req.body;

    if (!id) return res.status(400).json({ message: 'Client Admin ID required' });

    // Verify the user exists and is a CLIENT_ADMIN
    const user = await prisma.user.findUnique({ 
      where: { id },
      include: { clientProfile: true }
    });
    
    if (!user || user.role !== 'CLIENT_ADMIN') {
      return res.status(404).json({ message: 'Client Admin not found' });
    }

    if (!user.clientProfile) {
      return res.status(404).json({ message: 'Client profile not found' });
    }

    // Update the client profile
    const updatedProfile = await prisma.clientProfile.update({
      where: { clientAdminId: id },
      data: {
        ...(companyName !== undefined && { companyName }),
        ...(contactEmail !== undefined && { contactEmail }),
        ...(contactPhone !== undefined && { contactPhone }),
      },
    });

    // Return updated client admin with profile
    const updatedClientAdmin = await prisma.user.findUnique({
      where: { id },
      include: { clientProfile: true }
    });

    res.json({
      message: 'Client Admin updated successfully',
      clientAdmin: {
        id: updatedClientAdmin.id,
        email: updatedClientAdmin.email,
        role: updatedClientAdmin.role,
        isActive: updatedClientAdmin.isActive,
        createdAt: updatedClientAdmin.createdAt,
        clientProfile: updatedClientAdmin.clientProfile,
      },
    });
  } catch (error) {
    console.error('Update Client Admin Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * DELETE /api/users/client-admins/:id
 * Delete a client admin and all associated data
 */
exports.deleteClientAdmin = async (req, res) => {
  try {
    const { id } = req.params;
    if (!id) return res.status(400).json({ message: 'Client Admin ID required' });

    // Verify the user exists and is a CLIENT_ADMIN
    const user = await prisma.user.findUnique({ 
      where: { id },
      include: { 
        clientProfile: true
      }
    });
    
    if (!user || user.role !== 'CLIENT_ADMIN') {
      return res.status(404).json({ message: 'Client Admin not found' });
    }

    // Use transaction to ensure data consistency
    await prisma.$transaction(async (tx) => {
      // 1. Get all user admins under this client
      const userAdmins = await tx.user.findMany({
        where: { managedByClientAdminId: id },
        select: { id: true }
      });
      const userAdminIds = userAdmins.map(u => u.id);

      // 2. Get all staff users created by these user admins
      const staffUsers = userAdminIds.length > 0 ? await tx.user.findMany({
        where: { createdByUserAdminId: { in: userAdminIds } },
        select: { id: true }
      }) : [];
      const staffUserIds = staffUsers.map(u => u.id);

      // 3. All user IDs to delete (user admins + staff)
      const allUserIds = [...userAdminIds, ...staffUserIds];

      // 4. Collect playlist/layout IDs created by these users (used for deep cleanup)
      const playlists = allUserIds.length > 0 ? await tx.playlist.findMany({
        where: { createdById: { in: allUserIds } },
        select: { id: true }
      }) : [];
      const playlistIds = playlists.map(p => p.id);

      const layouts = allUserIds.length > 0 ? await tx.layout.findMany({
        where: { createdById: { in: allUserIds } },
        select: { id: true }
      }) : [];
      const layoutIds = layouts.map(l => l.id);

      // 5. Delete playlist items for playlists created by these users
      if (allUserIds.length > 0) {
        if (playlistIds.length > 0) {
          await tx.playlistItem.deleteMany({
            where: { playlistId: { in: playlistIds } }
          });
        }
      }

      // 6. Delete deep layout children BEFORE deleting layouts/media
      // Layouts contain: widgets, sections->sectionItems, zones, layoutZones (which may reference playlists)
      if (layoutIds.length > 0) {
        // 6a. Delete widgets
        await tx.widget.deleteMany({
          where: { layoutId: { in: layoutIds } }
        });

        // 6b. Delete layout section items -> then sections
        const sections = await tx.layoutSection.findMany({
          where: { layoutId: { in: layoutIds } },
          select: { id: true }
        });
        const sectionIds = sections.map(s => s.id);

        if (sectionIds.length > 0) {
          await tx.layoutSectionItem.deleteMany({
            where: { sectionId: { in: sectionIds } }
          });
        }

        await tx.layoutSection.deleteMany({
          where: { layoutId: { in: layoutIds } }
        });

        // 6c. Delete zones and template layout zones
        await tx.zone.deleteMany({
          where: { layoutId: { in: layoutIds } }
        });

        await tx.layoutZone.deleteMany({
          where: { layoutId: { in: layoutIds } }
        });
      }

      // 7. Delete schedule-display relationships and schedules created by these users
      if (allUserIds.length > 0) {
        // First get all schedules created by these users
        const schedules = await tx.schedule.findMany({
          where: { createdById: { in: allUserIds } },
          select: { id: true }
        });
        const scheduleIds = schedules.map(s => s.id);

        // Delete schedule-display relationships
        if (scheduleIds.length > 0) {
          await tx.scheduleDisplay.deleteMany({
            where: { scheduleId: { in: scheduleIds } }
          });
        }

        // Delete schedules
        await tx.schedule.deleteMany({
          where: { createdById: { in: allUserIds } }
        });
      }

      // 8. Unassign playlists/layouts from displays before deletion (including activeLayoutId)
      await tx.display.updateMany({
        where: { clientAdminId: id },
        data: { 
          playlistId: null,
          layoutId: null,
          activeLayoutId: null,
          managedByUserId: null
        }
      });

      // Also unassign displays managed by user admins being deleted
      if (allUserIds.length > 0) {
        await tx.display.updateMany({
          where: { managedByUserId: { in: allUserIds } },
          data: { 
            playlistId: null,
            layoutId: null,
            activeLayoutId: null,
            managedByUserId: null
          }
        });
      }

      // 9. Delete playlists created by users in this client
      if (allUserIds.length > 0) {
        await tx.playlist.deleteMany({
          where: { createdById: { in: allUserIds } }
        });
      }

      // 10. Delete layouts created by users in this client
      if (allUserIds.length > 0) {
        await tx.layout.deleteMany({
          where: { createdById: { in: allUserIds } }
        });
      }

      // 11. Delete media created by users in this client
      if (allUserIds.length > 0) {
        await tx.media.deleteMany({
          where: { createdById: { in: allUserIds } }
        });
      }

      // 12. Delete schedule-display rows tied to displays we are about to delete (safety)
      // (Schedules can be created by various users; the join table can block display deletion.)
      await tx.scheduleDisplay.deleteMany({
        where: {
          displayId: {
            in: (await tx.display.findMany({
              where: { clientAdminId: id },
              select: { id: true }
            })).map(d => d.id)
          }
        }
      });

      // 13. Delete all displays owned by this client
      await tx.display.deleteMany({
        where: { clientAdminId: id }
      });

      // 14. Delete UserAdminProfile for USER_ADMIN users
      if (userAdminIds.length > 0) {
        await tx.userAdminProfile.deleteMany({
          where: { userAdminId: { in: userAdminIds } }
        });
      }

      // 15. Delete STAFF users first (to avoid relation constraint violation)
      if (staffUserIds.length > 0) {
        await tx.user.deleteMany({
          where: { id: { in: staffUserIds } }
        });
      }

      // 16. Delete USER_ADMIN users (after staff are deleted and profiles are deleted)
      if (userAdminIds.length > 0) {
        await tx.user.deleteMany({
          where: { id: { in: userAdminIds } }
        });
      }

      // 17. Delete the client profile
      if (user.clientProfile) {
        await tx.clientProfile.delete({
          where: { clientAdminId: id }
        });
      }

      // 18. Finally, delete the client admin user
      await tx.user.delete({
        where: { id }
      });
    });

    res.json({
      message: 'Client Admin and all associated data deleted successfully',
      deletedClientId: user.clientProfile?.clientId,
      deletedCompanyName: user.clientProfile?.companyName,
    });
  } catch (error) {
    console.error('Delete Client Admin Error:', error);
    
    // More detailed error logging
    if (error.code) {
      console.error('Prisma Error Code:', error.code);
    }
    if (error.meta) {
      console.error('Prisma Error Meta:', error.meta);
    }
    
    res.status(500).json({ 
      message: 'Server error', 
      error: error.message,
      details: process.env.NODE_ENV === 'development' ? error.stack : undefined
    });
  }
};

