const prisma = require('../config/db');
const { getClientAdminId } = require('../utils/storage.utils');

exports.listPlaylists = async (req, res) => {
  try {
    // Get the current user's client admin ID to filter playlists
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as media controller)
    if (req.user.role === 'CLIENT_ADMIN') {
      // CLIENT_ADMIN sees all playlists from all USER_ADMINs under them (for management)
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      // Get all STAFF users created by these USER_ADMINs
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      // CLIENT_ADMIN sees playlists from: client admin + all user admins + all staff
      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      // USER_ADMIN only sees their own playlists + their staff's playlists (STRICT ISOLATION)
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      // USER_ADMIN sees playlists from: themselves + their staff only
      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      // STAFF sees playlists from their USER_ADMIN + other staff under same USER_ADMIN
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        // STAFF sees playlists from: their USER_ADMIN + all staff under same USER_ADMIN
        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id]; // Fallback to own playlists only
      }
      
    } else {
      // SUPER_ADMIN or other roles - see all playlists (no filtering)
      userIds = null; // Will skip the createdById filter
    }

    // Build where clause
    const where = {};
    
    // Add user-based filtering (skip for SUPER_ADMIN)
    if (userIds !== null) {
      where.createdById = {
        in: userIds // Only show playlists created by users in the same group
      };
    }

    // Filter playlists to only show those created by authorized users
    const playlists = await prisma.playlist.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      include: {
        _count: {
          select: { items: true },
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            role: true
          }
        }
      },
    });

    res.json({ playlists });
  } catch (error) {
    console.error('List Playlists CRASH:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.getPlaylist = async (req, res) => {
  try {
    const { id } = req.params;
    if (!id) return res.status(400).json({ message: 'Playlist ID is required' });

    // Get the current user's client admin ID
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as listPlaylists)
    if (req.user.role === 'CLIENT_ADMIN') {
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id];
      }
      
    } else {
      // SUPER_ADMIN can access any playlist
      userIds = null;
    }

    const playlist = await prisma.playlist.findUnique({
      where: { id },
      include: {
        items: {
          orderBy: { order: 'asc' },
          include: { media: true },
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            role: true
          }
        }
      },
    });

    if (!playlist) return res.status(404).json({ message: 'Playlist not found' });

    // Check if the playlist was created by someone in the authorized group (skip check for SUPER_ADMIN)
    if (userIds !== null && (!playlist.createdById || !userIds.includes(playlist.createdById))) {
      return res.status(403).json({ message: 'Access denied. You can only access playlists from your organization.' });
    }

    res.json({ playlist });
  } catch (error) {
    console.error('Get Playlist Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.createPlaylist = async (req, res) => {
  try {
    const { name } = req.body;
    if (!name) return res.status(400).json({ message: 'Playlist name required' });
    const playlist = await prisma.playlist.create({
      data: { name, createdById: req.user?.id || null },
    });
    res.status(201).json({ playlist });
  } catch (error) {
    console.error('Create Playlist Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.updatePlaylist = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, items } = req.body;
    if (!id) return res.status(400).json({ message: 'Playlist ID is required' });
    if (!name) return res.status(400).json({ message: 'Playlist name required' });
    if (!Array.isArray(items)) return res.status(400).json({ message: 'items must be an array' });

    // Get the current user's client admin ID
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as other playlist functions)
    if (req.user.role === 'CLIENT_ADMIN') {
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id];
      }
      
    } else {
      // SUPER_ADMIN can modify any playlist
      userIds = null;
    }

    // Check if playlist exists and belongs to the authorized group
    const exists = await prisma.playlist.findUnique({ 
      where: { id }, 
      select: { 
        id: true, 
        createdById: true 
      } 
    });
    
    if (!exists) return res.status(404).json({ message: 'Playlist not found' });

    // Check if the playlist was created by someone in the authorized group (skip check for SUPER_ADMIN)
    if (userIds !== null && (!exists.createdById || !userIds.includes(exists.createdById))) {
      return res.status(403).json({ message: 'Access denied. You can only modify playlists from your organization.' });
    }

    // Validate that all media items belong to the same client
    if (items.length > 0) {
      for (const it of items) {
        if (!it.mediaId) return res.status(400).json({ message: 'Each item must include mediaId' });
        if (typeof it.order !== 'number') return res.status(400).json({ message: 'Each item must include numeric order' });
      }

      // Check if all media items belong to the same client
      const mediaIds = items.map(item => item.mediaId);
      const mediaItems = await prisma.media.findMany({
        where: {
          id: { in: mediaIds }
        },
        select: {
          id: true,
          createdById: true
        }
      });

      // Verify all media items exist and belong to the same client
      for (const mediaItem of mediaItems) {
        if (!mediaItem.createdById || !userIds.includes(mediaItem.createdById)) {
          return res.status(403).json({ message: 'Access denied. You can only use media from your organization in playlists.' });
        }
      }

      if (mediaItems.length !== mediaIds.length) {
        return res.status(400).json({ message: 'Some media items were not found or are not accessible.' });
      }
    }

    const validResizeModes = ['FIT', 'FILL', 'STRETCH'];
    const validRotation = (r) => [0, 90, 180, 270].includes(Number(r));
    const itemData = items.map((it) => ({
      playlistId: id,
      mediaId: it.mediaId,
      duration: it.duration ?? null,
      order: it.order,
      loopVideo: it.loopVideo === true,
      orientation: it.orientation === 'LANDSCAPE' || it.orientation === 'PORTRAIT' ? it.orientation : null,
      resizeMode: it.resizeMode && validResizeModes.includes(String(it.resizeMode).toUpperCase()) ? String(it.resizeMode).toUpperCase() : 'FIT',
      rotation: validRotation(it.rotation) ? Number(it.rotation) : 0,
    }));

    try {
      await prisma.$transaction([
        prisma.playlist.update({ where: { id }, data: { name } }),
        prisma.playlistItem.deleteMany({ where: { playlistId: id } }),
        ...(items.length > 0 ? [
          prisma.playlistItem.createMany({
            data: itemData,
          })
        ] : []),
      ]);
    } catch (createError) {
      const msg = createError?.message || '';
      // Fallback for older DB schema without orientation/resizeMode
      if (items.length > 0 && (msg.includes('Unknown arg') || msg.includes('loopVideo') || msg.includes('orientation') || msg.includes('resizeMode') || msg.includes('rotation') || msg.includes('Invalid'))) {
        await prisma.$transaction([
          prisma.playlist.update({ where: { id }, data: { name } }),
          prisma.playlistItem.deleteMany({ where: { playlistId: id } }),
          prisma.playlistItem.createMany({
            data: items.map((it) => ({
              playlistId: id,
              mediaId: it.mediaId,
              duration: it.duration ?? null,
              order: it.order,
            })),
          }),
        ]);
      } else {
        throw createError;
      }
    }

    const playlist = await prisma.playlist.findUnique({
      where: { id },
      include: { items: { orderBy: { order: 'asc' }, include: { media: true } } },
    });
    res.json({ playlist });
  } catch (error) {
    console.error('Update Playlist Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

exports.deletePlaylist = async (req, res) => {
  try {
    const playlistId = req.params.id;

    // Get the current user's client admin ID
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let userIds = [];

    // Different filtering logic based on user role (same as other playlist functions)
    if (req.user.role === 'CLIENT_ADMIN') {
      const userAdmins = await prisma.user.findMany({
        where: {
          role: 'USER_ADMIN',
          managedByClientAdminId: clientAdminId
        },
        select: { id: true }
      });

      const userAdminIds = userAdmins.map(ua => ua.id);

      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: {
            in: userAdminIds
          }
        },
        select: { id: true }
      });

      userIds = [
        clientAdminId,
        ...userAdminIds,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'USER_ADMIN') {
      const staffUsers = await prisma.user.findMany({
        where: {
          role: 'STAFF',
          createdByUserAdminId: req.user.id
        },
        select: { id: true }
      });

      userIds = [
        req.user.id,
        ...staffUsers.map(s => s.id)
      ];
      
    } else if (req.user.role === 'STAFF') {
      const staffUser = await prisma.user.findUnique({
        where: { id: req.user.id },
        select: { createdByUserAdminId: true }
      });

      if (staffUser?.createdByUserAdminId) {
        const siblingStaff = await prisma.user.findMany({
          where: {
            role: 'STAFF',
            createdByUserAdminId: staffUser.createdByUserAdminId
          },
          select: { id: true }
        });

        userIds = [
          staffUser.createdByUserAdminId,
          ...siblingStaff.map(s => s.id)
        ];
      } else {
        userIds = [req.user.id];
      }
      
    } else {
      // SUPER_ADMIN can delete any playlist
      userIds = null;
    }

    // Check if playlist exists and belongs to the authorized group
    const playlist = await prisma.playlist.findUnique({
      where: { id: playlistId },
      select: {
        id: true,
        createdById: true
      }
    });

    if (!playlist) {
      return res.status(404).json({ message: 'Playlist not found' });
    }

    // Check if the playlist was created by someone in the authorized group (skip check for SUPER_ADMIN)
    if (userIds !== null && (!playlist.createdById || !userIds.includes(playlist.createdById))) {
      return res.status(403).json({ message: 'Access denied. You can only delete playlists from your organization.' });
    }

    // Check if playlist is assigned to any display
    const display = await prisma.display.findFirst({
      where: { playlistId: playlistId },
      select: { id: true },
    });
    
    if (display) {
      return res.status(400).json({ message: 'Cannot delete playlist that is assigned to a display.' });
    }

    await prisma.$transaction([
      prisma.playlistItem.deleteMany({ where: { playlistId } }),
      prisma.playlist.delete({ where: { id: playlistId } }),
    ]);
    
    res.json({ message: 'Playlist deleted successfully' });
  } catch (error) {
    console.error('Delete Playlist Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};
