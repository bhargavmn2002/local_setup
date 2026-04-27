# Phases 2-9: Complete Feature Breakdown

## Overview of Remaining Phases

Here's what needs to be built to complete the SignoX Dashboard Android app:

---

## 📱 PHASE 2: Display Management
**Priority**: HIGH | **Time**: 2-3 hours | **Files**: ~15-20

### Features to Build:

#### 1. Display List Screen
- **What**: View all displays in a list/grid
- **Includes**:
  - RecyclerView with display cards
  - Display name, status (online/offline), last seen
  - Filter by status (all/online/offline)
  - Search by display name
  - Pull to refresh
  - Empty state when no displays
  - Click to view details

#### 2. Display Pairing (QR Code)
- **What**: Pair new Android player devices
- **Includes**:
  - Generate QR code with pairing token
  - Display QR code on screen
  - Pairing instructions
  - Manual pairing code option
  - Success/failure feedback
  - Auto-refresh display list after pairing

#### 3. Display Details Screen
- **What**: View detailed info about a display
- **Includes**:
  - Device name, ID, status
  - Last seen timestamp
  - Device specs (Android version, screen size)
  - Assigned content (playlist/layout)
  - Location information
  - Online/offline indicator
  - Edit display button
  - Delete display button

#### 4. Display Assignment
- **What**: Assign displays to users
- **Includes**:
  - Select user from list
  - Assign/unassign display
  - Transfer ownership
  - Bulk assignment (select multiple)
  - Confirmation dialogs

#### 5. Display Monitoring
- **What**: Real-time status monitoring
- **Includes**:
  - Connection status indicator
  - Last heartbeat time
  - Device health status
  - Screenshot preview (if available)
  - Restart display option
  - Send command to display

### Files to Create:
```
ui/display/
├── DisplayListFragment.kt
├── DisplayDetailsFragment.kt
├── DisplayPairingFragment.kt
├── DisplayViewModel.kt
├── DisplayAdapter.kt
└── DisplayRepository.kt

res/layout/
├── fragment_display_list.xml
├── fragment_display_details.xml
├── fragment_display_pairing.xml
├── item_display_card.xml
└── dialog_assign_display.xml

data/model/
└── Display.kt
```

---

## 📁 PHASE 3: Media Management
**Priority**: HIGH | **Time**: 3-4 hours | **Files**: ~20-25

### Features to Build:

#### 1. Media Library Screen
- **What**: View all uploaded media files
- **Includes**:
  - Grid view with thumbnails
  - List view option
  - Filter by type (images/videos/all)
  - Search by filename
  - Sort by (name, date, size)
  - Select multiple files
  - Storage usage indicator
  - Upload button (FAB)

#### 2. Media Upload
- **What**: Upload images and videos
- **Includes**:
  - Image picker (gallery)
  - Video picker (gallery)
  - Camera capture option
  - Multiple file selection
  - Upload progress bar
  - File size validation
  - File type validation
  - Success/error messages
  - Auto-refresh after upload

#### 3. Media Preview
- **What**: View media in full screen
- **Includes**:
  - Image viewer with zoom/pan
  - Video player with controls
  - Media information (name, size, duration)
  - Share option
  - Delete option
  - Edit metadata option

#### 4. Media Details & Edit
- **What**: View and edit media metadata
- **Includes**:
  - File name (editable)
  - File size
  - Upload date
  - Dimensions (for images)
  - Duration (for videos)
  - Tags/categories
  - Used in playlists (list)
  - Delete confirmation

#### 5. Storage Management
- **What**: Monitor and manage storage
- **Includes**:
  - Total storage used
  - Storage limit
  - Storage breakdown (images/videos)
  - Cleanup suggestions
  - Delete unused media
  - Storage analytics chart

### Files to Create:
```
ui/media/
├── MediaListFragment.kt
├── MediaUploadFragment.kt
├── MediaPreviewFragment.kt
├── MediaDetailsFragment.kt
├── MediaViewModel.kt
├── MediaAdapter.kt
└── MediaRepository.kt

res/layout/
├── fragment_media_list.xml
├── fragment_media_upload.xml
├── fragment_media_preview.xml
├── fragment_media_details.xml
├── item_media_grid.xml
├── item_media_list.xml
└── dialog_media_options.xml

data/model/
└── Media.kt

utils/
├── ImagePicker.kt
├── VideoPicker.kt
└── FileUploader.kt
```

---

## 🎵 PHASE 4: Playlist Management
**Priority**: HIGH | **Time**: 4-5 hours | **Files**: ~25-30

### Features to Build:

#### 1. Playlist List Screen
- **What**: View all playlists
- **Includes**:
  - List of playlists with thumbnails
  - Playlist name, item count, duration
  - Search playlists
  - Filter by status (active/inactive)
  - Create new playlist button
  - Click to edit playlist
  - Delete playlist option

#### 2. Create Playlist
- **What**: Create new playlist
- **Includes**:
  - Playlist name input
  - Description input
  - Add media items (from library)
  - Set duration per item
  - Reorder items (drag & drop)
  - Preview playlist
  - Save playlist
  - Cancel option

#### 3. Edit Playlist
- **What**: Modify existing playlist
- **Includes**:
  - Update name/description
  - Add more media items
  - Remove items
  - Reorder items
  - Change item durations
  - Preview changes
  - Save changes
  - Delete playlist

#### 4. Playlist Preview
- **What**: Preview playlist playback
- **Includes**:
  - Timeline view of items
  - Play preview (simulate playback)
  - Item thumbnails
  - Duration per item
  - Total playlist duration
  - Loop indicator
  - Transition effects

#### 5. Playlist Assignment
- **What**: Assign playlists to displays
- **Includes**:
  - Select displays (single/multiple)
  - Assign playlist
  - Set priority
  - Schedule playlist (optional)
  - Unassign playlist
  - View assigned displays
  - Bulk operations

### Files to Create:
```
ui/playlist/
├── PlaylistListFragment.kt
├── PlaylistCreateFragment.kt
├── PlaylistEditFragment.kt
├── PlaylistPreviewFragment.kt
├── PlaylistAssignFragment.kt
├── PlaylistViewModel.kt
├── PlaylistAdapter.kt
├── PlaylistItemAdapter.kt
└── PlaylistRepository.kt

res/layout/
├── fragment_playlist_list.xml
├── fragment_playlist_create.xml
├── fragment_playlist_edit.xml
├── fragment_playlist_preview.xml
├── fragment_playlist_assign.xml
├── item_playlist_card.xml
├── item_playlist_item.xml
└── dialog_playlist_options.xml

data/model/
├── Playlist.kt
└── PlaylistItem.kt
```

---

## 🎨 PHASE 5: Layout Builder
**Priority**: MEDIUM | **Time**: 5-6 hours | **Files**: ~30-35

### Features to Build:

#### 1. Layout Templates
- **What**: Pre-built layout templates
- **Includes**:
  - Template gallery (grid view)
  - Template preview
  - Template categories (full screen, split, grid)
  - Select template
  - Create custom layout
  - Template descriptions

#### 2. Layout Editor
- **What**: Visual layout editor
- **Includes**:
  - Canvas with zones
  - Add zone button
  - Resize zones (drag handles)
  - Move zones (drag & drop)
  - Delete zone
  - Zone properties (size, position)
  - Grid snapping
  - Preview mode

#### 3. Zone Configuration
- **What**: Configure each zone
- **Includes**:
  - Zone name
  - Content type (media/playlist/text/clock/weather)
  - Assign content
  - Background color
  - Border settings
  - Padding/margin
  - Z-index (layer order)

#### 4. Layout Preview
- **What**: Preview layout on device
- **Includes**:
  - Full-screen preview
  - Simulate content playback
  - Rotate preview (portrait/landscape)
  - Zoom in/out
  - Exit preview

#### 5. Layout Assignment
- **What**: Assign layouts to displays
- **Includes**:
  - Select displays
  - Assign layout
  - Set priority
  - Schedule layout
  - Override existing layout
  - Bulk assignment

### Files to Create:
```
ui/layout/
├── LayoutListFragment.kt
├── LayoutTemplatesFragment.kt
├── LayoutEditorFragment.kt
├── LayoutPreviewFragment.kt
├── LayoutAssignFragment.kt
├── LayoutViewModel.kt
├── LayoutAdapter.kt
├── ZoneAdapter.kt
└── LayoutRepository.kt

res/layout/
├── fragment_layout_list.xml
├── fragment_layout_templates.xml
├── fragment_layout_editor.xml
├── fragment_layout_preview.xml
├── fragment_layout_assign.xml
├── item_layout_card.xml
├── item_template_card.xml
└── dialog_zone_config.xml

data/model/
├── Layout.kt
├── Zone.kt
└── Template.kt

ui/custom/
├── LayoutCanvas.kt (custom view)
├── ZoneView.kt (custom view)
└── DragDropHelper.kt
```

---

## 📅 PHASE 6: Schedule Management
**Priority**: MEDIUM | **Time**: 3-4 hours | **Files**: ~20-25

### Features to Build:

#### 1. Schedule List
- **What**: View all schedules
- **Includes**:
  - List of schedules
  - Schedule name, date range, status
  - Active/inactive indicator
  - Search schedules
  - Filter by status
  - Create schedule button
  - Edit/delete options

#### 2. Create Schedule
- **What**: Create time-based schedule
- **Includes**:
  - Schedule name
  - Start date/time picker
  - End date/time picker
  - Repeat options (daily, weekly, custom)
  - Select content (playlist/layout)
  - Select displays
  - Priority setting
  - Save schedule

#### 3. Calendar View
- **What**: Visual calendar of schedules
- **Includes**:
  - Monthly calendar view
  - Schedule markers on dates
  - Click date to see schedules
  - Color-coded by priority
  - Conflict detection
  - Quick edit from calendar
  - Navigate months

#### 4. Schedule Details
- **What**: View schedule information
- **Includes**:
  - Schedule name, dates, times
  - Assigned content
  - Assigned displays
  - Repeat pattern
  - Priority level
  - Status (active/inactive)
  - Edit button
  - Delete button

#### 5. Schedule Assignment
- **What**: Assign schedules to displays
- **Includes**:
  - Select displays
  - Assign schedule
  - Priority management
  - Conflict resolution
  - Override options
  - Bulk assignment

### Files to Create:
```
ui/schedule/
├── ScheduleListFragment.kt
├── ScheduleCreateFragment.kt
├── ScheduleEditFragment.kt
├── ScheduleCalendarFragment.kt
├── ScheduleDetailsFragment.kt
├── ScheduleViewModel.kt
├── ScheduleAdapter.kt
└── ScheduleRepository.kt

res/layout/
├── fragment_schedule_list.xml
├── fragment_schedule_create.xml
├── fragment_schedule_edit.xml
├── fragment_schedule_calendar.xml
├── fragment_schedule_details.xml
├── item_schedule_card.xml
└── dialog_schedule_conflict.xml

data/model/
└── Schedule.kt

ui/custom/
└── CalendarView.kt (custom calendar)
```

---

## 📊 PHASE 7: Analytics & Reports
**Priority**: MEDIUM | **Time**: 2-3 hours | **Files**: ~15-20

### Features to Build:

#### 1. Analytics Dashboard
- **What**: Visual analytics and charts
- **Includes**:
  - Display uptime chart
  - Content playback stats
  - Most played content
  - Display activity timeline
  - User activity stats
  - System health metrics
  - Date range filter
  - Export data option

#### 2. Proof of Play
- **What**: Playback verification logs
- **Includes**:
  - Playback log list
  - Filter by display
  - Filter by date range
  - Filter by content
  - Playback details (time, duration)
  - Screenshot evidence
  - Export report
  - Verification status

#### 3. Reports
- **What**: Generate and view reports
- **Includes**:
  - Report templates
  - Custom report builder
  - Date range selection
  - Display selection
  - Content selection
  - Generate report
  - View report (PDF/HTML)
  - Export options (PDF, CSV, Excel)
  - Email report
  - Schedule automatic reports

#### 4. Display Analytics
- **What**: Per-display analytics
- **Includes**:
  - Uptime percentage
  - Online/offline history
  - Content played
  - Playback duration
  - Error logs
  - Performance metrics
  - Charts and graphs

### Files to Create:
```
ui/analytics/
├── AnalyticsDashboardFragment.kt
├── ProofOfPlayFragment.kt
├── ReportsFragment.kt
├── ReportDetailsFragment.kt
├── AnalyticsViewModel.kt
└── AnalyticsRepository.kt

res/layout/
├── fragment_analytics_dashboard.xml
├── fragment_proof_of_play.xml
├── fragment_reports.xml
├── fragment_report_details.xml
├── item_playback_log.xml
└── item_report_card.xml

data/model/
├── Analytics.kt
├── PlaybackLog.kt
└── Report.kt

utils/
├── ChartHelper.kt
├── ReportGenerator.kt
└── PdfExporter.kt
```

---

## 👥 PHASE 8: User Management
**Priority**: LOW | **Time**: 3-4 hours | **Files**: ~20-25

### Features to Build:

#### 1. User List
- **What**: View all users
- **Includes**:
  - List of users with avatars
  - User name, email, role
  - Status (active/inactive)
  - Search users
  - Filter by role
  - Create user button
  - Click to view details

#### 2. Create User
- **What**: Add new user
- **Includes**:
  - Name input
  - Email input
  - Password input
  - Role selection (dropdown)
  - Staff role selection (if staff)
  - Company assignment (if client admin)
  - Permissions checkboxes
  - Send invitation email
  - Save user

#### 3. Edit User
- **What**: Modify user details
- **Includes**:
  - Update name, email
  - Change role
  - Update permissions
  - Reset password
  - Activate/deactivate user
  - Delete user
  - Save changes

#### 4. User Details
- **What**: View user information
- **Includes**:
  - User profile info
  - Role and permissions
  - Assigned displays
  - Activity history
  - Last login
  - Created date
  - Edit button
  - Delete button

#### 5. Staff Management
- **What**: Manage staff members
- **Includes**:
  - Staff list
  - Assign staff roles
  - Set permissions per role
  - Manage access levels
  - Activity logs
  - Performance metrics

### Files to Create:
```
ui/user/
├── UserListFragment.kt
├── UserCreateFragment.kt
├── UserEditFragment.kt
├── UserDetailsFragment.kt
├── StaffManagementFragment.kt
├── UserViewModel.kt
├── UserAdapter.kt
└── UserRepository.kt

res/layout/
├── fragment_user_list.xml
├── fragment_user_create.xml
├── fragment_user_edit.xml
├── fragment_user_details.xml
├── fragment_staff_management.xml
├── item_user_card.xml
└── dialog_user_permissions.xml

data/model/
└── UserManagement.kt
```

---

## ⚙️ PHASE 9: Settings & Profile
**Priority**: LOW | **Time**: 2-3 hours | **Files**: ~10-15

### Features to Build:

#### 1. Profile Screen
- **What**: User profile management
- **Includes**:
  - Profile picture (upload/change)
  - Name (editable)
  - Email (display only)
  - Role (display only)
  - Company info
  - Change password button
  - Logout button

#### 2. Change Password
- **What**: Update user password
- **Includes**:
  - Current password input
  - New password input
  - Confirm password input
  - Password strength indicator
  - Validation
  - Save button

#### 3. App Settings
- **What**: Application preferences
- **Includes**:
  - Notification settings (on/off)
  - Theme selection (light/dark/auto)
  - Language selection
  - Auto-refresh interval
  - Cache management (clear cache)
  - Data usage settings
  - About app (version, build)

#### 4. Account Settings
- **What**: Account and company info
- **Includes**:
  - Company name
  - Company logo
  - License details
  - Subscription status
  - Billing information
  - Storage usage
  - User limits
  - Upgrade options

#### 5. Notifications
- **What**: Notification preferences
- **Includes**:
  - Enable/disable notifications
  - Display offline alerts
  - Content upload notifications
  - Schedule reminders
  - System alerts
  - Email notifications
  - Push notification settings

### Files to Create:
```
ui/settings/
├── ProfileFragment.kt
├── ChangePasswordFragment.kt
├── AppSettingsFragment.kt
├── AccountSettingsFragment.kt
├── NotificationSettingsFragment.kt
├── SettingsViewModel.kt
└── SettingsRepository.kt

res/layout/
├── fragment_profile.xml
├── fragment_change_password.xml
├── fragment_app_settings.xml
├── fragment_account_settings.xml
└── fragment_notification_settings.xml

data/model/
└── Settings.kt
```

---

## 📊 Summary Table

| Phase | Feature | Priority | Time | Files | Complexity |
|-------|---------|----------|------|-------|------------|
| 2 | Display Management | HIGH | 2-3h | 15-20 | Medium |
| 3 | Media Management | HIGH | 3-4h | 20-25 | Medium |
| 4 | Playlist Management | HIGH | 4-5h | 25-30 | High |
| 5 | Layout Builder | MEDIUM | 5-6h | 30-35 | High |
| 6 | Schedule Management | MEDIUM | 3-4h | 20-25 | Medium |
| 7 | Analytics & Reports | MEDIUM | 2-3h | 15-20 | Medium |
| 8 | User Management | LOW | 3-4h | 20-25 | Medium |
| 9 | Settings & Profile | LOW | 2-3h | 10-15 | Low |

**Total Estimated**: 25-35 hours, 155-195 files

---

## 🎯 Recommended Order

### For Maximum Value:
1. **Phase 2** - Display Management (can pair devices)
2. **Phase 3** - Media Management (can upload content)
3. **Phase 4** - Playlist Management (can create playlists)
4. **Phase 6** - Schedule Management (can schedule content)
5. **Phase 5** - Layout Builder (advanced layouts)
6. **Phase 7** - Analytics (monitoring)
7. **Phase 9** - Settings (user preferences)
8. **Phase 8** - User Management (admin features)

### For Quick Wins:
1. **Phase 9** - Settings (easiest, 2-3h)
2. **Phase 2** - Display Management (essential)
3. **Phase 3** - Media Management (essential)
4. **Phase 4** - Playlist Management (essential)

---

Would you like to start with Phase 2 (Display Management) or another phase?
