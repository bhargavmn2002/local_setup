# SignoX Offline Content Manager - Development Phases

## Project Overview
A comprehensive offline Android application that combines content management and playback functionality for digital signage displays. The app operates completely offline, allowing users to manage media libraries, create layouts, build playlists, set schedules, and play content directly on the display device.

## Core Features
- **Media Library Management** - Import and organize media files
- **Playlist Builder** - Create and manage content playlists
- **Layout Designer** - Design custom layouts with zones
- **Schedule Manager** - Set up automated content scheduling
- **Content Player** - Full-screen playback of playlists and layouts
- **Complete Offline Operation** - No internet or remote CMS dependency

---

## Phase 1: Project Setup & Core Infrastructure ✅ COMPLETED
**Duration:** 1 week

### Tasks:
- ✅ Create new Android project structure
- ✅ Set up database (Room/SQLite) for local data storage
- ✅ Configure file storage system for media files
- ✅ Set up basic navigation architecture (Activities/Fragments)
- ✅ Import and adapt UI components from existing dashboard app
- ✅ Create base models (Media, Playlist, Layout, Schedule)

### Deliverables:
- ✅ Project skeleton with proper architecture
- ✅ Database schema and entities
- ✅ Basic navigation framework
- ✅ Core data models

### Implementation Details:
- **Project Structure**: Created `signox-offline-manager` with proper Kotlin/Android setup
- **Database**: Room database with SQLite backend, entities for Media, Playlist, Layout, Schedule
- **Architecture**: MVVM pattern with Repository layer, proper DAOs for data access
- **Dependencies**: Material Design 3, ExoPlayer, Glide, Navigation components
- **Configuration**: Landscape orientation, file permissions, application class setup
- **Models**: Complete data models with relationships and type converters

### Files Created:
- Project configuration files (build.gradle.kts, settings.gradle.kts, etc.)
- Database entities and DAOs (Media, Playlist, Layout, Schedule)
- Repository classes for data management
- Application class and manifest configuration
- Resource files (strings, colors, themes)
- Core package structure with proper organization

---

## Phase 2: Dashboard & Navigation 🔄 IN PROGRESS
**Duration:** 3-4 days

### Tasks:
- 🔄 Build main dashboard with navigation buttons
- ⏳ Create basic screen layouts for each section
- ⏳ Implement navigation between dashboard and sections
- ⏳ Set up toolbar/action bar with back navigation
- ⏳ Add basic styling and theming

### Deliverables:
- ⏳ Functional main dashboard
- ⏳ Navigation to all major sections
- ⏳ Consistent UI theme and styling

---

## Phase 3: Media Library Management
**Duration:** 1 week

### Tasks:
- File picker for importing media from device storage
- USB/external storage access and import
- Media file validation (supported formats)
- Media preview functionality (images/videos)
- Media organization and metadata storage
- Delete/rename media files

### Deliverables:
- Complete media import system
- Media library with preview capabilities
- File management operations

---

## Phase 4: Playlist Management
**Duration:** 1 week

### Tasks:
- Create playlist interface
- Add media to playlists with drag-and-drop
- Set duration for each media item
- Playlist preview functionality
- Save/load playlists from database
- Edit and delete playlists

### Deliverables:
- Functional playlist creation and editing
- Playlist management system
- Duration and ordering controls

---

## Phase 5: Layout Designer
**Duration:** 1.5 weeks

### Tasks:
- Basic layout creation interface
- Zone-based layout system (reuse from existing code)
- Drag-and-drop media to zones
- Layout preview functionality
- Save/load layouts from database
- Layout templates

### Deliverables:
- Layout design interface
- Zone-based content placement
- Layout preview and templates

---

## Phase 6: Schedule Management
**Duration:** 1 week

### Tasks:
- Time-based scheduling interface
- Daily/weekly schedule creation
- Assign playlists/layouts to time slots
- Schedule conflict detection
- Schedule preview and validation

### Deliverables:
- Complete scheduling system
- Time-based content assignment
- Schedule validation and conflict resolution

---

## Phase 7: Content Player
**Duration:** 1.5 weeks

### Tasks:
- Play button implementation with selection dialog
- Full-screen player activity
- Playlist playback engine
- Layout rendering and playback
- Media transition handling
- Player controls (pause/resume/stop)

### Deliverables:
- Full-screen content player
- Playlist and layout playback
- Player control system

---

## Phase 8: Advanced Features & Polish
**Duration:** 1 week

### Tasks:
- Schedule automation (auto-play based on time)
- Settings and preferences
- Import/export configurations
- Error handling and validation
- Performance optimization
- UI/UX improvements

### Deliverables:
- Automated scheduling
- Settings and configuration options
- Enhanced user experience

---

## Phase 9: Testing & Deployment
**Duration:** 3-4 days

### Tasks:
- Comprehensive testing on different devices
- USB compatibility testing
- Performance testing with large media files
- Bug fixes and stability improvements
- APK generation and deployment

### Deliverables:
- Fully tested application
- Deployment-ready APK
- Documentation and user guide

---

## Technical Stack
- **Platform:** Android (Kotlin)
- **Database:** Room (SQLite)
- **UI Framework:** Material Design 3
- **Media Handling:** ExoPlayer
- **File Management:** Android Storage Access Framework
- **Architecture:** MVVM with Repository pattern

## Estimated Timeline
**Total Duration:** 6-8 weeks

## Success Criteria
- ✅ Complete offline functionality
- ✅ Intuitive user interface
- ✅ Reliable media playback
- ✅ Efficient file management
- ✅ Stable performance on target devices
- ✅ No external dependencies

---

*This document serves as the master plan for developing the SignoX Offline Content Manager application.*