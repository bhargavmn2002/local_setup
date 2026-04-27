const prisma = require('../config/db');
const { getClientAdminId } = require('../utils/storage.utils');

/**
 * GET /api/display-groups
 * Get all display groups for the current user's organization
 */
exports.getDisplayGroups = async (req, res) => {
  try {
    const user = req.user;
    const clientAdminId = await getClientAdminId(user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    let whereClause = {};

    if (user.role === 'SUPER_ADMIN') {
      // Super Admin sees all groups
      whereClause = {};
    } else if (user.role === 'CLIENT_ADMIN') {
      // Client Admin sees groups in their organization
      whereClause = { clientAdminId: user.id };
    } else if (user.role === 'USER_ADMIN') {
      // User Admin sees groups they created or in their client organization
      whereClause = { clientAdminId };
    } else if (user.role === 'STAFF') {
      // Staff sees groups in their organization
      whereClause = { clientAdminId };
    } else {
      return res.status(403).json({ message: 'Access denied' });
    }

    const displayGroups = await prisma.displayGroup.findMany({
      where: whereClause,
      include: {
        displays: {
          select: {
            id: true,
            name: true,
            status: true,
            isPaired: true,
            lastHeartbeat: true,
          }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            name: true,
          }
        }
      },
      orderBy: { createdAt: 'desc' }
    });

    // Calculate real-time status for each group
    const groupsWithStatus = displayGroups.map(group => {
      const totalDisplays = group.displays.length;
      const onlineDisplays = group.displays.filter(display => {
        if (!display.isPaired || !display.lastHeartbeat) return false;
        const timeDiff = (new Date() - new Date(display.lastHeartbeat)) / 1000;
        return timeDiff < 60; // Online if heartbeat within 60 seconds
      }).length;

      return {
        ...group,
        displayCount: totalDisplays,
        onlineCount: onlineDisplays,
        offlineCount: totalDisplays - onlineDisplays,
      };
    });

    res.json({ displayGroups: groupsWithStatus });
  } catch (error) {
    console.error('Get Display Groups Error:', error);
    res.status(500).json({ message: 'Failed to fetch display groups', error: error.message });
  }
};

/**
 * POST /api/display-groups
 * Create a new display group
 */
exports.createDisplayGroup = async (req, res) => {
  try {
    const { name, description, color } = req.body;
    const user = req.user;

    if (!name || name.trim().length === 0) {
      return res.status(400).json({ message: 'Group name is required' });
    }

    // Check permissions
    if (!['SUPER_ADMIN', 'CLIENT_ADMIN', 'USER_ADMIN'].includes(user.role)) {
      return res.status(403).json({ message: 'Insufficient permissions to create display groups' });
    }

    const clientAdminId = await getClientAdminId(user?.id);
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Check if group name already exists in this organization
    const existingGroup = await prisma.displayGroup.findFirst({
      where: {
        name: name.trim(),
        clientAdminId,
      }
    });

    if (existingGroup) {
      return res.status(400).json({ message: 'A group with this name already exists' });
    }

    const displayGroup = await prisma.displayGroup.create({
      data: {
        name: name.trim(),
        description: description?.trim() || null,
        color: color || '#3B82F6', // Default blue color
        createdById: user.id,
        clientAdminId,
      },
      include: {
        displays: {
          select: {
            id: true,
            name: true,
            status: true,
            isPaired: true,
          }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            name: true,
          }
        }
      }
    });

    res.status(201).json({ 
      message: 'Display group created successfully',
      displayGroup: {
        ...displayGroup,
        displayCount: 0,
        onlineCount: 0,
        offlineCount: 0,
      }
    });
  } catch (error) {
    console.error('Create Display Group Error:', error);
    res.status(500).json({ message: 'Failed to create display group', error: error.message });
  }
};

/**
 * GET /api/display-groups/:id
 * Get a specific display group with its displays
 */
exports.getDisplayGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const user = req.user;

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id },
      include: {
        displays: {
          include: {
            playlist: {
              select: { id: true, name: true }
            },
            layout: {
              select: { id: true, name: true }
            }
          }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            name: true,
          }
        }
      }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check access permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    res.json({ displayGroup });
  } catch (error) {
    console.error('Get Display Group Error:', error);
    res.status(500).json({ message: 'Failed to fetch display group', error: error.message });
  }
};

/**
 * PATCH /api/display-groups/:id
 * Update a display group
 */
exports.updateDisplayGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, description, color } = req.body;
    const user = req.user;

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    if (!['SUPER_ADMIN', 'CLIENT_ADMIN', 'USER_ADMIN'].includes(user.role)) {
      return res.status(403).json({ message: 'Insufficient permissions to update display groups' });
    }

    // Check if new name conflicts with existing groups
    if (name && name.trim() !== displayGroup.name) {
      const existingGroup = await prisma.displayGroup.findFirst({
        where: {
          name: name.trim(),
          clientAdminId,
          id: { not: id }
        }
      });

      if (existingGroup) {
        return res.status(400).json({ message: 'A group with this name already exists' });
      }
    }

    const updatedGroup = await prisma.displayGroup.update({
      where: { id },
      data: {
        ...(name && { name: name.trim() }),
        ...(description !== undefined && { description: description?.trim() || null }),
        ...(color && { color }),
      },
      include: {
        displays: {
          select: {
            id: true,
            name: true,
            status: true,
            isPaired: true,
          }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            name: true,
          }
        }
      }
    });

    res.json({ 
      message: 'Display group updated successfully',
      displayGroup: updatedGroup
    });
  } catch (error) {
    console.error('Update Display Group Error:', error);
    res.status(500).json({ message: 'Failed to update display group', error: error.message });
  }
};

/**
 * DELETE /api/display-groups/:id
 * Delete a display group (displays will be unassigned from group)
 */
exports.deleteDisplayGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const user = req.user;

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id },
      include: {
        displays: { select: { id: true } }
      }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    if (!['SUPER_ADMIN', 'CLIENT_ADMIN', 'USER_ADMIN'].includes(user.role)) {
      return res.status(403).json({ message: 'Insufficient permissions to delete display groups' });
    }

    // Remove displays from group (set displayGroupId to null)
    if (displayGroup.displays.length > 0) {
      await prisma.display.updateMany({
        where: { displayGroupId: id },
        data: { displayGroupId: null }
      });
    }

    // Delete the group
    await prisma.displayGroup.delete({
      where: { id }
    });

    res.json({ message: 'Display group deleted successfully' });
  } catch (error) {
    console.error('Delete Display Group Error:', error);
    res.status(500).json({ message: 'Failed to delete display group', error: error.message });
  }
};

/**
 * POST /api/display-groups/:id/displays
 * Add displays to a group
 */
exports.addDisplaysToGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const { displayIds } = req.body;
    const user = req.user;

    if (!Array.isArray(displayIds) || displayIds.length === 0) {
      return res.status(400).json({ message: 'Display IDs array is required' });
    }

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    // Verify all displays exist and belong to the same organization
    const displays = await prisma.display.findMany({
      where: {
        id: { in: displayIds },
        clientAdminId,
        isPaired: true
      }
    });

    if (displays.length !== displayIds.length) {
      return res.status(400).json({ message: 'Some displays not found or not accessible' });
    }

    // Add displays to group
    await prisma.display.updateMany({
      where: { id: { in: displayIds } },
      data: { displayGroupId: id }
    });

    res.json({ 
      message: `${displays.length} displays added to group successfully`,
      addedCount: displays.length
    });
  } catch (error) {
    console.error('Add Displays to Group Error:', error);
    res.status(500).json({ message: 'Failed to add displays to group', error: error.message });
  }
};

/**
 * DELETE /api/display-groups/:id/displays
 * Remove displays from a group
 */
exports.removeDisplaysFromGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const { displayIds } = req.body;
    const user = req.user;

    if (!Array.isArray(displayIds) || displayIds.length === 0) {
      return res.status(400).json({ message: 'Display IDs array is required' });
    }

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    // Remove displays from group
    const result = await prisma.display.updateMany({
      where: { 
        id: { in: displayIds },
        displayGroupId: id
      },
      data: { displayGroupId: null }
    });

    res.json({ 
      message: `${result.count} displays removed from group successfully`,
      removedCount: result.count
    });
  } catch (error) {
    console.error('Remove Displays from Group Error:', error);
    res.status(500).json({ message: 'Failed to remove displays from group', error: error.message });
  }
};

/**
 * POST /api/display-groups/:id/assign-playlist
 * Assign a playlist to all displays in a group
 */
exports.assignPlaylistToGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const { playlistId } = req.body;
    const user = req.user;

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id },
      include: {
        displays: { select: { id: true, name: true } }
      }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    if (displayGroup.displays.length === 0) {
      return res.status(400).json({ message: 'No displays in this group' });
    }

    // Verify playlist exists and is accessible
    if (playlistId) {
      const playlist = await prisma.playlist.findFirst({
        where: {
          id: playlistId,
          createdById: user.role === 'SUPER_ADMIN' ? undefined : user.id
        }
      });

      if (!playlist) {
        return res.status(404).json({ message: 'Playlist not found or not accessible' });
      }
    }

    // Assign playlist to all displays in group
    const displayIds = displayGroup.displays.map(d => d.id);
    await prisma.display.updateMany({
      where: { id: { in: displayIds } },
      data: { 
        playlistId: playlistId || null,
        layoutId: null, // Clear layout when assigning playlist
        activeLayoutId: null
      }
    });

    res.json({ 
      message: `Playlist ${playlistId ? 'assigned to' : 'removed from'} ${displayIds.length} displays in group`,
      affectedDisplays: displayIds.length
    });
  } catch (error) {
    console.error('Assign Playlist to Group Error:', error);
    res.status(500).json({ message: 'Failed to assign playlist to group', error: error.message });
  }
};

/**
 * POST /api/display-groups/:id/assign-layout
 * Assign a layout to all displays in a group
 */
exports.assignLayoutToGroup = async (req, res) => {
  try {
    const { id } = req.params;
    const { layoutId } = req.body;
    const user = req.user;

    const displayGroup = await prisma.displayGroup.findUnique({
      where: { id },
      include: {
        displays: { select: { id: true, name: true } }
      }
    });

    if (!displayGroup) {
      return res.status(404).json({ message: 'Display group not found' });
    }

    // Check permissions
    const clientAdminId = await getClientAdminId(user?.id);
    if (user.role !== 'SUPER_ADMIN' && displayGroup.clientAdminId !== clientAdminId) {
      return res.status(403).json({ message: 'Access denied' });
    }

    if (displayGroup.displays.length === 0) {
      return res.status(400).json({ message: 'No displays in this group' });
    }

    // Verify layout exists and is accessible
    if (layoutId) {
      const layout = await prisma.layout.findFirst({
        where: {
          id: layoutId,
          createdById: user.role === 'SUPER_ADMIN' ? undefined : user.id
        }
      });

      if (!layout) {
        return res.status(404).json({ message: 'Layout not found or not accessible' });
      }
    }

    // Assign layout to all displays in group
    const displayIds = displayGroup.displays.map(d => d.id);
    await prisma.display.updateMany({
      where: { id: { in: displayIds } },
      data: { 
        layoutId: layoutId || null,
        playlistId: null, // Clear playlist when assigning layout
        activeLayoutId: layoutId || null
      }
    });

    res.json({ 
      message: `Layout ${layoutId ? 'assigned to' : 'removed from'} ${displayIds.length} displays in group`,
      affectedDisplays: displayIds.length
    });
  } catch (error) {
    console.error('Assign Layout to Group Error:', error);
    res.status(500).json({ message: 'Failed to assign layout to group', error: error.message });
  }
};