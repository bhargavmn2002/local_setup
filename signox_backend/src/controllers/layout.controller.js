const prisma = require('../config/db');
const { getClientAdminId } = require('../utils/storage.utils');

/** Get authorized user IDs based on role - implements individual USER_ADMIN isolation */
async function getAuthorizedUserIds(user, clientAdminId) {
  let userIds = [];

  if (user.role === 'CLIENT_ADMIN') {
    // CLIENT_ADMIN sees all layouts from all USER_ADMINs under them (for management)
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
    
  } else if (user.role === 'USER_ADMIN') {
    // USER_ADMIN only sees their own layouts + their staff's layouts (STRICT ISOLATION)
    const staffUsers = await prisma.user.findMany({
      where: {
        role: 'STAFF',
        createdByUserAdminId: user.id
      },
      select: { id: true }
    });

    userIds = [
      user.id,
      ...staffUsers.map(s => s.id)
    ];
    
  } else if (user.role === 'STAFF') {
    // STAFF sees layouts from their USER_ADMIN + other staff under same USER_ADMIN
    const staffUser = await prisma.user.findUnique({
      where: { id: user.id },
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
      userIds = [user.id];
    }
    
  } else {
    // SUPER_ADMIN or other roles - see all layouts (no filtering)
    return null; // Will skip the createdById filter
  }

  return userIds;
}

/** Coerce API/JSON speed to a positive px/sec (matches player & CMS slider) */
function normalizeScrollSpeedPxPerSec(speed) {
  const n = Number(speed);
  return Number.isFinite(n) && n > 0 ? Math.max(n, 5) : 50;
}

/**
 * GET /api/layouts
 * List all layouts for the current user's client
 */
exports.listLayouts = async (req, res) => {
  try {
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    const userIds = await getAuthorizedUserIds(req.user, clientAdminId);

    // Build where clause
    const where = {};
    
    // Add user-based filtering (skip for SUPER_ADMIN)
    if (userIds !== null) {
      where.createdById = {
        in: userIds // Only show layouts created by users in the same group
      };
    }

    // Filter layouts to only show those created by authorized users
    const layouts = await prisma.layout.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      include: {
        _count: {
          select: { sections: true, displays: true }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            role: true
          }
        }
      }
    });

    res.json({ layouts });
  } catch (error) {
    console.error('List Layouts Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/layouts/:id/preview
 * Get layout data optimized for preview rendering
 */
exports.getLayoutPreview = async (req, res) => {
  try {
    const { id } = req.params;
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Get all USER_ADMINs under this client admin
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

    // Combine all user IDs: client admin + user admins + staff users
    const userIds = [
      clientAdminId,
      ...userAdminIds,
      ...staffUsers.map(s => s.id)
    ];

    const layout = await prisma.layout.findFirst({
      where: {
        id,
        createdById: {
          in: userIds
        }
      },
      include: {
        sections: {
          orderBy: { order: 'asc' },
          include: {
            items: {
              orderBy: { order: 'asc' },
              include: {
                media: {
                  select: {
                    id: true,
                    name: true,
                    type: true,
                    filename: true,
                    duration: true,
                    url: true
                  }
                }
              }
            },
            scrollTexts: {
              orderBy: { zIndex: 'asc' }
            }
          }
        },
        scrollTexts: {
          orderBy: { zIndex: 'asc' }
        }
      }
    });

    if (!layout) {
      return res.status(404).json({ message: 'Layout not found' });
    }

    // Transform for preview player
    const previewData = {
      id: layout.id,
      name: layout.name,
      width: layout.width,
      height: layout.height,
      orientation: layout.orientation,
      sections: layout.sections.map(section => ({
        id: section.id,
        name: section.name,
        order: section.order,
        x: section.x,
        y: section.y,
        width: section.width,
        height: section.height,
        loopEnabled: section.loopEnabled,
        frequency: section.frequency,
        type: section.sectionType === 'SCROLL_TEXT' ? 'text' : 'media',
        textConfig: section.textConfig ? JSON.parse(section.textConfig) : null,
        items: section.items.map(item => ({
          id: item.id,
          order: item.order,
          duration: item.duration,
          orientation: item.orientation,
          resizeMode: item.resizeMode,
          rotation: item.rotation,
          media: {
            id: item.media.id,
            name: item.media.name,
            type: item.media.type,
            // Always use the correct URL from database (HLS for videos, original for images)
            url: item.media.url || `/uploads/${item.media.filename}`,
            duration: item.media.duration
          }
        }))
      }))
    };

    res.json(previewData);
  } catch (error) {
    console.error('Get Layout Preview Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/layouts/:id
 * Get a single layout with all sections and media
 */
exports.getLayout = async (req, res) => {
  try {
    const { id } = req.params;
    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Get all USER_ADMINs under this client admin
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

    // Combine all user IDs: client admin + user admins + staff users
    const userIds = [
      clientAdminId,
      ...userAdminIds,
      ...staffUsers.map(s => s.id)
    ];

    const layout = await prisma.layout.findUnique({
      where: { id },
      include: {
        sections: {
          orderBy: { order: 'asc' },
          include: {
            items: {
              orderBy: { order: 'asc' },
              include: {
                media: {
                  select: {
                    id: true,
                    name: true,
                    url: true,
                    type: true,
                    duration: true,
                    fileSize: true,
                    width: true,
                    height: true
                  }
                }
              }
            }
          }
        },
        createdBy: {
          select: {
            id: true,
            email: true,
            role: true
          }
        },
        _count: {
          select: { displays: true, sections: true }
        }
      }
    });

    if (!layout) {
      return res.status(404).json({ message: 'Layout not found' });
    }

    // Check if layout belongs to the same client
    if (!layout.createdById || !userIds.includes(layout.createdById)) {
      return res.status(403).json({ message: 'Access denied. You can only view layouts from your organization.' });
    }

    // Parse textConfig for sections
    const layoutWithParsedConfig = {
      ...layout,
      sections: layout.sections.map(section => ({
        ...section,
        type: section.sectionType === 'SCROLL_TEXT' ? 'text' : 'media',
        textConfig: section.textConfig ? JSON.parse(section.textConfig) : null
      }))
    };

    res.json({ layout: layoutWithParsedConfig });
  } catch (error) {
    console.error('Get Layout Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * POST /api/layouts
 * Create a new layout with sections
 */
exports.createLayout = async (req, res) => {
  try {
    const { name, description, width, height, orientation, sections } = req.body;

    if (!name) {
      return res.status(400).json({ message: 'Layout name is required' });
    }

    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Determine orientation from dimensions if not provided
    let layoutOrientation = orientation || 'LANDSCAPE';
    if (!orientation && width && height) {
      layoutOrientation = height > width ? 'PORTRAIT' : 'LANDSCAPE';
    }

    // Create layout with sections in a transaction
    const layout = await prisma.$transaction(async (tx) => {
      const newLayout = await tx.layout.create({
        data: {
          name,
          description: description || null,
          width: width || 1920,
          height: height || 1080,
          orientation: layoutOrientation,
          isActive: true,
          createdById: req.user.id
        }
      });

      // Create sections if provided (from template)
      if (sections && Array.isArray(sections) && sections.length > 0) {
        for (const section of sections) {
          const newSection = await tx.layoutSection.create({
            data: {
              layoutId: newLayout.id,
              name: section.name || `Section ${section.order + 1}`,
              order: section.order || 0,
              x: section.x || 0,
              y: section.y || 0,
              width: section.width || 100,
              height: section.height || 100,
              frequency: section.frequency || null,
              loopEnabled: section.loopEnabled !== undefined ? section.loopEnabled : true,
              sectionType: section.sectionType || (section.type === 'text' ? 'SCROLL_TEXT' : 'MEDIA'),
              textConfig: section.textConfig ? JSON.stringify(section.textConfig) : null
            }
          });

          // Create section items if provided
          if (section.items && Array.isArray(section.items) && section.items.length > 0) {
            const validResize = (r) => ['FIT', 'FILL', 'STRETCH'].includes(String(r || '').toUpperCase());
            const validRotation = (r) => [0, 90, 180, 270].includes(Number(r));
            await tx.layoutSectionItem.createMany({
              data: section.items.map((item, index) => ({
                sectionId: newSection.id,
                mediaId: item.mediaId,
                order: item.order !== undefined ? item.order : index,
                duration: item.duration || null,
                orientation: item.orientation === 'LANDSCAPE' || item.orientation === 'PORTRAIT' ? item.orientation : null,
                resizeMode: validResize(item.resizeMode) ? String(item.resizeMode).toUpperCase() : 'FIT',
                rotation: validRotation(item.rotation) ? Number(item.rotation) : 0,
              }))
            });
          }
        }
      }

      return newLayout;
    });

    // Fetch the complete layout with sections
    const completeLayout = await prisma.layout.findUnique({
      where: { id: layout.id },
      include: {
        sections: {
          orderBy: { order: 'asc' },
          include: {
            items: {
              orderBy: { order: 'asc' },
              include: {
                media: {
                  select: {
                    id: true,
                    name: true,
                    url: true,
                    type: true,
                    duration: true,
                    fileSize: true
                  }
                }
              }
            }
          }
        }
      }
    });

    res.status(201).json({ layout: completeLayout });
  } catch (error) {
    console.error('Create Layout Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/layouts/:id
 * Update a layout
 */
exports.updateLayout = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, description, width, height, orientation, isActive } = req.body;

    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Get all USER_ADMINs under this client admin
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

    // Combine all user IDs: client admin + user admins + staff users
    const userIds = [
      clientAdminId,
      ...userAdminIds,
      ...staffUsers.map(s => s.id)
    ];

    // Check if layout exists and belongs to the same client
    const existingLayout = await prisma.layout.findUnique({
      where: { id },
      select: { createdById: true }
    });

    if (!existingLayout) {
      return res.status(404).json({ message: 'Layout not found' });
    }

    if (!existingLayout.createdById || !userIds.includes(existingLayout.createdById)) {
      return res.status(403).json({ message: 'Access denied. You can only modify layouts from your organization.' });
    }

    // Determine orientation from dimensions if not provided but dimensions changed
    let layoutOrientation = orientation;
    if (orientation === undefined && width !== undefined && height !== undefined) {
      layoutOrientation = height > width ? 'PORTRAIT' : 'LANDSCAPE';
    }

    // Build update data object (filter out null and undefined values)
    const updateData = {};
    if (name !== undefined && name !== null) updateData.name = name;
    if (description !== undefined && description !== null) updateData.description = description;
    if (width !== undefined && width !== null) updateData.width = width;
    if (height !== undefined && height !== null) updateData.height = height;
    if (layoutOrientation !== undefined && layoutOrientation !== null) updateData.orientation = layoutOrientation;
    if (isActive !== undefined && isActive !== null) updateData.isActive = isActive;

    const updatedLayout = await prisma.layout.update({
      where: { id },
      data: updateData,
      include: {
        sections: {
          orderBy: { order: 'asc' },
          include: {
            items: {
              orderBy: { order: 'asc' },
              include: {
                media: {
                  select: {
                    id: true,
                    name: true,
                    url: true,
                    type: true,
                    duration: true,
                    fileSize: true
                  }
                }
              }
            }
          }
        }
      }
    });

    res.json({ layout: updatedLayout });
  } catch (error) {
    console.error('Update Layout Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * DELETE /api/layouts/:id
 * Delete a layout
 */
exports.deleteLayout = async (req, res) => {
  try {
    const { id } = req.params;

    const clientAdminId = await getClientAdminId(req.user?.id);
    
    if (!clientAdminId) {
      return res.status(400).json({ message: 'Unable to determine client association' });
    }

    // Get all USER_ADMINs under this client admin
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

    // Combine all user IDs: client admin + user admins + staff users
    const userIds = [
      clientAdminId,
      ...userAdminIds,
      ...staffUsers.map(s => s.id)
    ];

    // Check if layout exists and belongs to the same client
    const existingLayout = await prisma.layout.findUnique({
      where: { id },
      include: {
        _count: {
          select: { displays: true, sections: true }
        }
      }
    });

    if (!existingLayout) {
      return res.status(404).json({ message: 'Layout not found' });
    }

    if (existingLayout.createdById && !userIds.includes(existingLayout.createdById)) {
      return res.status(403).json({ message: 'Access denied. You can only delete layouts from your organization.' });
    }

    // Unassign this layout from all displays first (so delete always succeeds; avoids stale-count glitches)
    await prisma.display.updateMany({
      where: { OR: [{ layoutId: id }, { activeLayoutId: id }] },
      data: { layoutId: null, activeLayoutId: null }
    });

    // Delete child records first (schema uses NoAction on Layout for widgets/sections, so no cascade)
    // 1. Widgets
    await prisma.widget.deleteMany({ where: { layoutId: id } });

    // 2. Layout section items -> then sections
    const sections = await prisma.layoutSection.findMany({
      where: { layoutId: id },
      select: { id: true }
    });
    const sectionIds = sections.map((s) => s.id);
    if (sectionIds.length > 0) {
      await prisma.layoutSectionItem.deleteMany({ where: { sectionId: { in: sectionIds } } });
    }
    await prisma.layoutSection.deleteMany({ where: { layoutId: id } });

    // 3. Zones and layout zones
    await prisma.zone.deleteMany({ where: { layoutId: id } });
    await prisma.layoutZone.deleteMany({ where: { layoutId: id } });

    // 4. Unassign this layout from any schedules (Schedule.layoutId has NoAction)
    await prisma.schedule.updateMany({
      where: { layoutId: id },
      data: { layoutId: null }
    });

    // 5. Delete the layout
    await prisma.layout.delete({
      where: { id }
    });

    res.json({ message: 'Layout deleted successfully' });
  } catch (error) {
    console.error('Delete Layout Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/layouts/:id/sections/:sectionId
 * Update a section (including media items)
 */
exports.updateSection = async (req, res) => {
  try {
    const { id, sectionId } = req.params;
    const { name, items, frequency, loopEnabled } = req.body;

    // Verify section belongs to layout
    const section = await prisma.layoutSection.findUnique({
      where: { id: sectionId },
      include: { layout: true }
    });

    if (!section || section.layoutId !== id) {
      return res.status(404).json({ message: 'Section not found' });
    }

    await prisma.$transaction(async (tx) => {
      // Update section properties (filter out null and undefined values)
      const sectionUpdates = {};
      if (name !== undefined && name !== null) sectionUpdates.name = name;
      if (frequency !== undefined && frequency !== null) sectionUpdates.frequency = frequency;
      if (loopEnabled !== undefined && loopEnabled !== null) sectionUpdates.loopEnabled = loopEnabled;
      
      if (Object.keys(sectionUpdates).length > 0) {
        await tx.layoutSection.update({
          where: { id: sectionId },
          data: sectionUpdates
        });
      }

      // Update section items if provided
      if (items !== undefined && Array.isArray(items)) {
        // Delete existing items
        await tx.layoutSectionItem.deleteMany({
          where: { sectionId }
        });

        // Create new items
        if (items.length > 0) {
          const validResize = (r) => ['FIT', 'FILL', 'STRETCH'].includes(String(r || '').toUpperCase());
          const validRotation = (r) => [0, 90, 180, 270].includes(Number(r));
          await tx.layoutSectionItem.createMany({
            data: items.map((item, index) => ({
              sectionId,
              mediaId: item.mediaId,
              order: item.order !== undefined ? item.order : index,
              duration: item.duration || null,
              orientation: item.orientation === 'LANDSCAPE' || item.orientation === 'PORTRAIT' ? item.orientation : null,
              resizeMode: validResize(item.resizeMode) ? String(item.resizeMode).toUpperCase() : 'FIT',
              rotation: validRotation(item.rotation) ? Number(item.rotation) : 0,
            }))
          });
        }
      }
    });

    // Fetch updated section
    const updatedSection = await prisma.layoutSection.findUnique({
      where: { id: sectionId },
      include: {
        items: {
          orderBy: { order: 'asc' },
          include: {
            media: {
              select: {
                id: true,
                name: true,
                url: true,
                type: true,
                duration: true,
                fileSize: true
              }
            }
          }
        }
      }
    });

    res.json({ section: updatedSection });
  } catch (error) {
    console.error('Update Section Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * POST /api/layouts/:id/widgets
 * Add a widget to a layout (legacy support)
 */
exports.addWidget = async (req, res) => {
  try {
    const { id } = req.params;
    const { type, name, x, y, width, height, zIndex, config, mediaId } = req.body;

    if (!type) {
      return res.status(400).json({ message: 'Widget type is required' });
    }

    // Get max zIndex if not provided
    let finalZIndex = zIndex;
    if (finalZIndex === undefined) {
      const maxWidget = await prisma.widget.findFirst({
        where: { layoutId: id },
        orderBy: { zIndex: 'desc' },
        select: { zIndex: true }
      });
      finalZIndex = maxWidget ? maxWidget.zIndex + 1 : 0;
    }

    const widget = await prisma.widget.create({
      data: {
        layoutId: id,
        type,
        name: name || null,
        x: x || 0,
        y: y || 0,
        width: width || 100,
        height: height || 100,
        zIndex: finalZIndex,
        config: config ? JSON.stringify(config) : null,
        mediaId: mediaId || null
      },
      include: {
        media: {
          select: {
            id: true,
            name: true,
            url: true,
            type: true
          }
        }
      }
    });

    res.status(201).json({ widget });
  } catch (error) {
    console.error('Add Widget Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/layouts/:layoutId/widgets/:widgetId
 * Update a widget (legacy support)
 */
exports.updateWidget = async (req, res) => {
  try {
    const { layoutId, widgetId } = req.params;
    const { type, name, x, y, width, height, zIndex, config, mediaId } = req.body;

    // Verify widget exists and belongs to layout
    const widget = await prisma.widget.findUnique({
      where: { id: widgetId },
      include: {
        layout: {
          select: { createdById: true }
        }
      }
    });

    if (!widget || widget.layoutId !== layoutId) {
      return res.status(404).json({ message: 'Widget not found' });
    }

    // Build update data object (filter out null and undefined values)
    const updateData = {};
    if (type !== undefined && type !== null) updateData.type = type;
    if (name !== undefined && name !== null) updateData.name = name;
    if (x !== undefined && x !== null) updateData.x = x;
    if (y !== undefined && y !== null) updateData.y = y;
    if (width !== undefined && width !== null) updateData.width = width;
    if (height !== undefined && height !== null) updateData.height = height;
    if (zIndex !== undefined && zIndex !== null) updateData.zIndex = zIndex;
    if (config !== undefined) updateData.config = config ? JSON.stringify(config) : null;
    if (mediaId !== undefined) updateData.mediaId = mediaId || null;

    const updatedWidget = await prisma.widget.update({
      where: { id: widgetId },
      data: updateData,
      include: {
        media: {
          select: {
            id: true,
            name: true,
            url: true,
            type: true
          }
        }
      }
    });

    res.json({ widget: updatedWidget });
  } catch (error) {
    console.error('Update Widget Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * DELETE /api/layouts/:layoutId/widgets/:widgetId
 * Delete a widget (legacy support)
 */
exports.deleteWidget = async (req, res) => {
  try {
    const { layoutId, widgetId } = req.params;

    // Verify widget exists and belongs to layout
    const widget = await prisma.widget.findUnique({
      where: { id: widgetId }
    });

    if (!widget || widget.layoutId !== layoutId) {
      return res.status(404).json({ message: 'Widget not found' });
    }

    await prisma.widget.delete({
      where: { id: widgetId }
    });

    res.json({ message: 'Widget deleted successfully' });
  } catch (error) {
    console.error('Delete Widget Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * POST /api/layouts/:id/scroll-text
 * Add scrolling text to a layout
 */
exports.addScrollText = async (req, res) => {
  try {
    const { id } = req.params;
    const { 
      text, 
      direction, 
      speed, 
      fontSize, 
      fontColor, 
      backgroundColor, 
      isOverlay, 
      x, 
      y, 
      width, 
      height, 
      zIndex,
      sectionId 
    } = req.body;

    if (!text) {
      return res.status(400).json({ message: 'Text is required' });
    }

    // Validate direction
    const validDirections = ['LEFT_TO_RIGHT', 'RIGHT_TO_LEFT', 'TOP_TO_BOTTOM', 'BOTTOM_TO_TOP'];
    const scrollDirection = validDirections.includes(direction) ? direction : 'LEFT_TO_RIGHT';

    const scrollText = await prisma.scrollText.create({
      data: {
        layoutId: id,
        text,
        direction: scrollDirection,
        speed: normalizeScrollSpeedPxPerSec(speed),
        fontSize: fontSize || 24,
        fontColor: fontColor || '#FFFFFF',
        backgroundColor: backgroundColor || null,
        isOverlay: isOverlay || false,
        x: x || 0,
        y: y || 0,
        width: width || 100,
        height: height || 10,
        zIndex: zIndex || 10,
        sectionId: sectionId || null
      }
    });

    res.status(201).json({ scrollText });
  } catch (error) {
    console.error('Add Scroll Text Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/layouts/:layoutId/scroll-text/:scrollTextId
 * Update scrolling text
 */
exports.updateScrollText = async (req, res) => {
  try {
    const { layoutId, scrollTextId } = req.params;
    const { 
      text, 
      direction, 
      speed, 
      fontSize, 
      fontColor, 
      backgroundColor, 
      isOverlay, 
      x, 
      y, 
      width, 
      height, 
      zIndex,
      sectionId 
    } = req.body;

    // Verify scroll text exists and belongs to layout
    const scrollText = await prisma.scrollText.findUnique({
      where: { id: scrollTextId }
    });

    if (!scrollText || scrollText.layoutId !== layoutId) {
      return res.status(404).json({ message: 'Scroll text not found' });
    }

    // Build update data object
    const updateData = {};
    if (text !== undefined) updateData.text = text;
    if (direction !== undefined) {
      const validDirections = ['LEFT_TO_RIGHT', 'RIGHT_TO_LEFT', 'TOP_TO_BOTTOM', 'BOTTOM_TO_TOP'];
      updateData.direction = validDirections.includes(direction) ? direction : 'LEFT_TO_RIGHT';
    }
    if (speed !== undefined) updateData.speed = normalizeScrollSpeedPxPerSec(speed);
    if (fontSize !== undefined) updateData.fontSize = fontSize;
    if (fontColor !== undefined) updateData.fontColor = fontColor;
    if (backgroundColor !== undefined) updateData.backgroundColor = backgroundColor;
    if (isOverlay !== undefined) updateData.isOverlay = isOverlay;
    if (x !== undefined) updateData.x = x;
    if (y !== undefined) updateData.y = y;
    if (width !== undefined) updateData.width = width;
    if (height !== undefined) updateData.height = height;
    if (zIndex !== undefined) updateData.zIndex = zIndex;
    if (sectionId !== undefined) updateData.sectionId = sectionId;

    const updatedScrollText = await prisma.scrollText.update({
      where: { id: scrollTextId },
      data: updateData
    });

    res.json({ scrollText: updatedScrollText });
  } catch (error) {
    console.error('Update Scroll Text Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * DELETE /api/layouts/:layoutId/scroll-text/:scrollTextId
 * Delete scrolling text
 */
exports.deleteScrollText = async (req, res) => {
  try {
    const { layoutId, scrollTextId } = req.params;

    // Verify scroll text exists and belongs to layout
    const scrollText = await prisma.scrollText.findUnique({
      where: { id: scrollTextId }
    });

    if (!scrollText || scrollText.layoutId !== layoutId) {
      return res.status(404).json({ message: 'Scroll text not found' });
    }

    await prisma.scrollText.delete({
      where: { id: scrollTextId }
    });

    res.json({ message: 'Scroll text deleted successfully' });
  } catch (error) {
    console.error('Delete Scroll Text Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * PUT /api/layouts/:id/sections/:sectionId/text-config
 * Update text configuration for a section
 */
exports.updateSectionTextConfig = async (req, res) => {
  try {
    const { id, sectionId } = req.params;
    const { text, direction, speed, fontSize, fontWeight, textColor, backgroundColor } = req.body;

    // Verify section belongs to layout
    const section = await prisma.layoutSection.findUnique({
      where: { id: sectionId },
      include: { layout: true }
    });

    if (!section || section.layoutId !== id) {
      return res.status(404).json({ message: 'Section not found' });
    }

    // Convert direction format from frontend to backend
    const directionMap = {
      'left-to-right': 'LEFT_TO_RIGHT',
      'right-to-left': 'RIGHT_TO_LEFT',
      'top-to-bottom': 'TOP_TO_BOTTOM',
      'bottom-to-top': 'BOTTOM_TO_TOP'
    };

    const scrollDirection = directionMap[direction] || 'LEFT_TO_RIGHT';

    // Store text configuration as JSON in the section
    const textConfig = {
      text,
      direction,
      speed: normalizeScrollSpeedPxPerSec(speed),
      fontSize: fontSize || 24,
      fontWeight: fontWeight || 'normal',
      textColor: textColor || '#000000',
      backgroundColor: backgroundColor || 'transparent'
    };

    const updatedSection = await prisma.layoutSection.update({
      where: { id: sectionId },
      data: {
        textConfig: JSON.stringify(textConfig),
        sectionType: 'SCROLL_TEXT'
      }
    });

    res.json({ section: updatedSection, textConfig });
  } catch (error) {
    console.error('Update Section Text Config Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/layouts/:id/scroll-texts
 * Get all scrolling texts for a layout
 */
exports.getScrollTexts = async (req, res) => {
  try {
    const { id } = req.params;

    const scrollTexts = await prisma.scrollText.findMany({
      where: { layoutId: id },
      orderBy: { zIndex: 'asc' }
    });

    res.json({ scrollTexts });
  } catch (error) {
    console.error('Get Scroll Texts Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};

/**
 * GET /api/layouts/:id/debug
 * Debug endpoint to check layout data
 */
exports.debugLayout = async (req, res) => {
  try {
    const { id } = req.params;
    
    const layout = await prisma.layout.findUnique({
      where: { id },
      include: {
        sections: {
          orderBy: { order: 'asc' },
          include: {
            items: {
              orderBy: { order: 'asc' },
              include: {
                media: {
                  select: {
                    id: true,
                    name: true,
                    type: true,
                    url: true,
                  }
                }
              }
            }
          }
        }
      }
    });

    if (!layout) {
      return res.status(404).json({ message: 'Layout not found' });
    }

    // Debug info
    const debugInfo = {
      layout: {
        id: layout.id,
        name: layout.name,
        sectionsCount: layout.sections.length
      },
      sections: layout.sections.map(section => ({
        id: section.id,
        name: section.name,
        sectionType: section.sectionType,
        hasTextConfig: !!section.textConfig,
        textConfig: section.textConfig ? JSON.parse(section.textConfig) : null,
        itemsCount: section.items.length
      }))
    };

    res.json(debugInfo);
  } catch (error) {
    console.error('Debug Layout Error:', error);
    res.status(500).json({ message: 'Server error', error: error.message });
  }
};