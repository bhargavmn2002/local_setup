const express = require('express');
const router = express.Router();
const displayGroupController = require('../controllers/displayGroup.controller');
const { requireAuth } = require('../middleware/auth.middleware');

// GET /api/display-groups
// Get all display groups for the current user's organization
router.get('/', requireAuth, displayGroupController.getDisplayGroups);

// POST /api/display-groups
// Create a new display group
router.post('/', requireAuth, displayGroupController.createDisplayGroup);

// GET /api/display-groups/:id
// Get a specific display group with its displays
router.get('/:id', requireAuth, displayGroupController.getDisplayGroup);

// PATCH /api/display-groups/:id
// Update a display group
router.patch('/:id', requireAuth, displayGroupController.updateDisplayGroup);

// DELETE /api/display-groups/:id
// Delete a display group
router.delete('/:id', requireAuth, displayGroupController.deleteDisplayGroup);

// POST /api/display-groups/:id/displays
// Add displays to a group
router.post('/:id/displays', requireAuth, displayGroupController.addDisplaysToGroup);

// DELETE /api/display-groups/:id/displays
// Remove displays from a group
router.delete('/:id/displays', requireAuth, displayGroupController.removeDisplaysFromGroup);

// POST /api/display-groups/:id/assign-playlist
// Assign a playlist to all displays in a group
router.post('/:id/assign-playlist', requireAuth, displayGroupController.assignPlaylistToGroup);

// POST /api/display-groups/:id/assign-layout
// Assign a layout to all displays in a group
router.post('/:id/assign-layout', requireAuth, displayGroupController.assignLayoutToGroup);

module.exports = router;