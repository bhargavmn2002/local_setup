const express = require('express');
const router = express.Router();
const {
  listLayouts,
  getLayout,
  getLayoutPreview,
  createLayout,
  updateLayout,
  deleteLayout,
  updateSection,
  updateSectionTextConfig,
  addWidget,
  updateWidget,
  deleteWidget,
  addScrollText,
  updateScrollText,
  deleteScrollText,
  getScrollTexts,
  debugLayout
} = require('../controllers/layout.controller');
const { requireAuth, requireContentManagement } = require('../middleware/auth.middleware');

// All routes require authentication
router.use(requireAuth);

// Layout CRUD operations (require content management access)
router.get('/', requireContentManagement, listLayouts);
router.get('/:id', requireContentManagement, getLayout);
router.get('/:id/debug', requireContentManagement, debugLayout);
router.get('/:id/preview', requireContentManagement, getLayoutPreview);
router.post('/', requireContentManagement, createLayout);
router.put('/:id', requireContentManagement, updateLayout);
router.delete('/:id', requireContentManagement, deleteLayout);

// Section operations
router.put('/:id/sections/:sectionId', requireContentManagement, updateSection);
router.put('/:id/sections/:sectionId/text-config', requireContentManagement, updateSectionTextConfig);

// Widget operations (legacy support)
router.post('/:id/widgets', requireContentManagement, addWidget);
router.put('/:layoutId/widgets/:widgetId', requireContentManagement, updateWidget);
router.delete('/:layoutId/widgets/:widgetId', requireContentManagement, deleteWidget);

module.exports = router;
