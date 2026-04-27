# Custom Layout Feature

## Overview
The Custom Layout feature allows users to create completely custom layouts by drawing sections directly on a canvas, providing unlimited creative freedom for digital signage layouts.

## User Workflow

### 1. Access Custom Layout Creator
- Navigate to **Layouts** page
- Click **"Create Custom Layout"** button (purple outline button next to "Create Layout")

### 2. Layout Configuration
- **Layout Name**: Enter a descriptive name for your custom layout
- **Resolution**: Choose from preset resolutions (HD 720p, HD 1080p, 2K QHD, 4K UHD)
- **Orientation**: Select Landscape or Portrait mode

### 3. Drawing Sections
- Click **"Draw Custom Area"** to enable drawing mode
- **Click and drag** on the canvas to create custom sections
- Each drawn area opens a naming dialog:
  - **Section Name**: Give your section a descriptive name
  - **Content Type**: Choose between Media (images/videos) or Text (scrolling text)
- **Overlay System**: Sections created later will appear on top of earlier sections
- **Layer Indicators**: Each section shows its layer number (L1, L2, etc.)

### 4. Section Management
- **View Sections**: All created sections appear in the left panel
- **Select Sections**: Click on sections in the canvas or list to select them
- **Edit Properties**: Modify selected section's name, type, position, and size
- **Delete Sections**: Remove unwanted sections with the X button

### 5. Save Layout
- Click **"Save Layout"** when satisfied with your design
- System creates the layout and redirects to the layout builder
- Assign media content to each section in the layout builder

## Key Features

### Canvas Drawing
- **Visual Drawing**: Drag to create sections of any size and position
- **Grid Background**: Helpful grid overlay for precise positioning
- **Real-time Preview**: See sections as you draw them
- **Drawing Feedback**: Visual indicators for drawing mode and instructions

### Section Types
- **Media Sections**: For images and videos
- **Text Sections**: For scrolling text content
- **Mixed Layouts**: Combine different section types in one layout

### Overlay System
- **Layer Order**: Sections are layered based on creation order
- **Z-Index Management**: Later sections automatically appear on top
- **Visual Feedback**: Layer numbers (L1, L2, L3) shown in editor and section list
- **Perfect Overlays**: Sections can overlap completely or partially for rich compositions

### Professional Tools
- **Multiple Resolutions**: Support for various display sizes
- **Orientation Support**: Both landscape and portrait layouts
- **Section Management**: Easy selection, editing, and deletion
- **Visual Feedback**: Clear indicators for selected sections and drawing mode

## Use Cases

### Creative Layouts
- **Picture-in-Picture**: Large background with smaller overlay sections
- **Asymmetric Designs**: Non-grid layouts with custom positioning
- **Layered Content**: Multiple overlapping sections for rich compositions

### Specialized Applications
- **News Tickers**: Custom text areas for scrolling information
- **Branding Overlays**: Logo sections positioned over main content
- **Multi-zone Displays**: Complex layouts with many different content areas

## Technical Implementation

### Frontend Components
- **CustomLayoutEditor**: Main canvas-based editor component
- **Drawing System**: Mouse-based section creation with real-time feedback
- **Section Management**: List view and property editing for created sections

### Backend Integration
- **Layout Creation**: Saves custom sections with position and type data
- **Section Types**: Supports MEDIA and SCROLL_TEXT section types
- **Responsive Storage**: Percentage-based positioning for device compatibility

### Data Flow
1. User draws sections on canvas
2. Each section gets named and typed via dialog
3. Layout data (name, dimensions, sections) sent to backend
4. Backend creates layout with custom sections
5. User redirected to layout builder for media assignment

## Benefits

### For Users
- **Complete Creative Control**: No template limitations
- **Intuitive Interface**: Visual drawing instead of complex configuration
- **Professional Results**: Grid-assisted precision drawing
- **Flexible Content**: Mix media and text in custom arrangements

### For System
- **Extensible Architecture**: Builds on existing layout system
- **Consistent Data Model**: Uses same backend structures as template layouts
- **Responsive Design**: Percentage-based positioning works across devices
- **Future-Proof**: Easy to add new section types and features

## Future Enhancements

### Planned Features
- **Section Templates**: Save and reuse common section arrangements
- **Snap-to-Grid**: Automatic alignment assistance
- **Copy/Paste Sections**: Duplicate sections quickly
- **Undo/Redo**: Action history for complex layouts
- **Import/Export**: Share custom layouts between systems

### Advanced Capabilities
- **Animation Zones**: Sections with transition effects
- **Interactive Areas**: Touch-responsive sections
- **Dynamic Sizing**: Sections that adapt to content
- **Multi-layer Support**: Z-index management for complex overlays