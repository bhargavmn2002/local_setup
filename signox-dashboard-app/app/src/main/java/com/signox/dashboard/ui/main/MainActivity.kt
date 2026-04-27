package com.signox.dashboard.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.signox.dashboard.R
import com.signox.dashboard.data.model.UserRole
import com.signox.dashboard.databinding.ActivityMainBinding
import com.signox.dashboard.ui.auth.LoginActivity
import com.signox.dashboard.ui.dashboard.DashboardViewModel
import com.signox.dashboard.ui.dashboard.SuperAdminDashboardFragment
import com.signox.dashboard.ui.dashboard.ClientAdminDashboardFragment
import com.signox.dashboard.ui.dashboard.UserAdminDashboardFragment
import com.signox.dashboard.ui.dashboard.StaffDashboardFragment
import com.signox.dashboard.ui.display.DisplayListFragment
import com.signox.dashboard.ui.media.MediaListFragment
import com.signox.dashboard.ui.playlist.PlaylistListFragment
import com.signox.dashboard.ui.layout.LayoutListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: DashboardViewModel by viewModels()
    private var currentUserRole: UserRole? = null
    private var currentStaffRole: String? = null
    
    companion object {
        const val EXTRA_USER_ROLE = "extra_user_role"
        const val EXTRA_STAFF_ROLE = "extra_staff_role"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        
        // Add SignoX logo to toolbar
        val logoView = layoutInflater.inflate(R.layout.toolbar_logo, null)
        binding.toolbar.addView(logoView)
        
        // Setup navigation drawer
        setupNavigationDrawer()
        
        // Get user role from intent
        val roleString = intent.getStringExtra(EXTRA_USER_ROLE)
        currentUserRole = roleString?.let { UserRole.valueOf(it) }
        currentStaffRole = intent.getStringExtra(EXTRA_STAFF_ROLE)
        
        // Save user role to SharedPreferences for fragments to access
        val sharedPrefs = getSharedPreferences("signox_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putString("user_role", currentUserRole?.name).apply()
        
        // Update navigation header with user info
        updateNavigationHeader()
        
        // Configure menu based on role
        configureMenuForRole(currentUserRole, currentStaffRole)
        
        loadDashboardForRole(currentUserRole)
        observeViewModel()
    }
    
    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        
        binding.navigationView.setNavigationItemSelectedListener(this)
    }
    
    private fun updateNavigationHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val tvUserEmail = headerView.findViewById<android.widget.TextView>(R.id.tvUserEmail)
        val tvUserRole = headerView.findViewById<android.widget.TextView>(R.id.tvUserRole)
        
        val sharedPrefs = getSharedPreferences("signox_prefs", MODE_PRIVATE)
        val email = sharedPrefs.getString("user_email", "user@example.com")
        val role = currentUserRole?.name?.replace("_", " ") ?: "User"
        
        tvUserEmail.text = email
        tvUserRole.text = role
    }
    
    private fun configureMenuForRole(role: UserRole?, staffRole: String? = null) {
        val menu = binding.navigationView.menu
        
        when (role) {
            UserRole.SUPER_ADMIN -> {
                // Super Admin: Only Manage Clients (Users) and Global Analytics
                menu.findItem(R.id.nav_displays).isVisible = false
                menu.findItem(R.id.nav_media).isVisible = false
                menu.findItem(R.id.nav_playlists).isVisible = false
                menu.findItem(R.id.nav_layouts).isVisible = false
                menu.findItem(R.id.nav_schedules).isVisible = false
                menu.findItem(R.id.nav_analytics).isVisible = true
                menu.findItem(R.id.nav_users).isVisible = true
            }
            UserRole.CLIENT_ADMIN -> {
                // Client Admin: User Admins, Analytics, Displays (view only)
                menu.findItem(R.id.nav_displays).isVisible = true
                menu.findItem(R.id.nav_media).isVisible = false
                menu.findItem(R.id.nav_playlists).isVisible = false
                menu.findItem(R.id.nav_layouts).isVisible = false
                menu.findItem(R.id.nav_schedules).isVisible = false
                menu.findItem(R.id.nav_analytics).isVisible = true
                menu.findItem(R.id.nav_users).isVisible = true
            }
            UserRole.USER_ADMIN -> {
                // User Admin: Full operations - Displays, Layouts, Playlists, Media, Schedules, Staff Users
                menu.findItem(R.id.nav_displays).isVisible = true
                menu.findItem(R.id.nav_media).isVisible = true
                menu.findItem(R.id.nav_playlists).isVisible = true
                menu.findItem(R.id.nav_layouts).isVisible = true
                menu.findItem(R.id.nav_schedules).isVisible = true
                menu.findItem(R.id.nav_analytics).isVisible = false
                menu.findItem(R.id.nav_users).isVisible = true  // USER_ADMIN can manage staff members
            }
            UserRole.STAFF -> {
                // Staff: Role-specific menu options based on staffRole
                configureStaffMenu(menu, staffRole)
            }
            else -> {
                // Default: Hide all menu items
                menu.findItem(R.id.nav_displays).isVisible = false
                menu.findItem(R.id.nav_media).isVisible = false
                menu.findItem(R.id.nav_playlists).isVisible = false
                menu.findItem(R.id.nav_layouts).isVisible = false
                menu.findItem(R.id.nav_schedules).isVisible = false
                menu.findItem(R.id.nav_analytics).isVisible = false
                menu.findItem(R.id.nav_users).isVisible = false
            }
        }
    }
    
    private fun configureStaffMenu(menu: android.view.Menu, staffRole: String?) {
        // Default: hide all
        menu.findItem(R.id.nav_displays).isVisible = false
        menu.findItem(R.id.nav_media).isVisible = false
        menu.findItem(R.id.nav_playlists).isVisible = false
        menu.findItem(R.id.nav_layouts).isVisible = false
        menu.findItem(R.id.nav_schedules).isVisible = false
        menu.findItem(R.id.nav_analytics).isVisible = false
        menu.findItem(R.id.nav_users).isVisible = false
        
        when (staffRole) {
            "DISPLAY_MANAGER" -> {
                // Display Manager: Can manage displays only
                menu.findItem(R.id.nav_displays).isVisible = true
            }
            "BROADCAST_MANAGER" -> {
                // Broadcast Manager: Can manage playlists and schedules
                menu.findItem(R.id.nav_playlists).isVisible = true
                menu.findItem(R.id.nav_schedules).isVisible = true
                menu.findItem(R.id.nav_displays).isVisible = true // View displays to assign content
            }
            "CONTENT_MANAGER" -> {
                // Content Manager: Can manage media, playlists, and layouts
                menu.findItem(R.id.nav_media).isVisible = true
                menu.findItem(R.id.nav_playlists).isVisible = true
                menu.findItem(R.id.nav_layouts).isVisible = true
            }
            "CMS_VIEWER" -> {
                // CMS Viewer: Read-only access to displays and media
                menu.findItem(R.id.nav_displays).isVisible = true
                menu.findItem(R.id.nav_media).isVisible = true
            }
            "POP_MANAGER" -> {
                // Proof of Play Manager: Can view analytics and displays
                menu.findItem(R.id.nav_analytics).isVisible = true
                menu.findItem(R.id.nav_displays).isVisible = true
            }
            else -> {
                // Unknown staff role: show basic options
                menu.findItem(R.id.nav_displays).isVisible = true
            }
        }
    }
    
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> loadDashboardForRole(currentUserRole)
            R.id.nav_displays -> navigateToDisplays()
            R.id.nav_media -> navigateToMedia()
            R.id.nav_playlists -> navigateToPlaylists()
            R.id.nav_layouts -> navigateToLayouts()
            R.id.nav_schedules -> navigateToSchedules()
            R.id.nav_analytics -> navigateToAnalytics()
            R.id.nav_users -> navigateToUsers()
            R.id.nav_profile -> navigateToProfile()
            R.id.nav_logout -> viewModel.logout()
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    private fun loadDashboardForRole(role: UserRole?) {
        val fragment: Fragment = when (role) {
            UserRole.SUPER_ADMIN -> SuperAdminDashboardFragment()
            UserRole.CLIENT_ADMIN -> ClientAdminDashboardFragment()
            UserRole.USER_ADMIN -> UserAdminDashboardFragment()
            UserRole.STAFF -> StaffDashboardFragment()
            null -> {
                logout()
                return
            }
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
            
        supportActionBar?.title = when (role) {
            UserRole.SUPER_ADMIN -> "Super Admin Dashboard"
            UserRole.CLIENT_ADMIN -> "Client Dashboard"
            UserRole.USER_ADMIN -> "Dashboard"
            UserRole.STAFF -> "Staff Dashboard"
            else -> "SignoX Dashboard"
        }
    }
    
    private fun observeViewModel() {
        viewModel.logoutEvent.observe(this) { shouldLogout ->
            if (shouldLogout) {
                logout()
            }
        }
    }
    
    private fun navigateToDisplays() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, DisplayListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Displays"
    }
    
    private fun navigateToMedia() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, MediaListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Media"
    }
    
    private fun navigateToPlaylists() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, PlaylistListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Playlists"
    }
    
    private fun navigateToLayouts() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, LayoutListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Layouts"
    }
    
    private fun navigateToSchedules() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, com.signox.dashboard.ui.schedule.ScheduleListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Schedules"
    }
    
    private fun navigateToAnalytics() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, com.signox.dashboard.ui.analytics.AnalyticsDashboardFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Analytics"
    }
    
    private fun navigateToUsers() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, com.signox.dashboard.ui.user.UserListFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Users"
    }
    
    private fun navigateToProfile() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, com.signox.dashboard.ui.settings.ProfileFragment())
            .addToBackStack(null)
            .commit()
        supportActionBar?.title = "Profile"
    }
    
    private fun logout() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
