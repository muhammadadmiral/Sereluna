package com.android.capstone.sereluna

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.capstone.sereluna.databinding.ActivityMainBinding
import com.android.capstone.sereluna.service.ScreeningReminderScheduler
import com.android.capstone.sereluna.service.MyFirebaseMessagingService
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.util.AuthSessionManager
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.google.firebase.FirebaseApp
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val serelunaRepository = SerelunaRepository()
    private val badgeRefreshHandler = Handler(Looper.getMainLooper())
    private val badgeRefreshRunnable = object : Runnable {
        override fun run() {
            updateNotificationBadge()
            badgeRefreshHandler.postDelayed(this, BADGE_REFRESH_INTERVAL_MS)
        }
    }
    private val notificationRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNotificationBadge()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        if (!AuthSessionManager.isSessionValid(this)) {
            AuthSessionManager.clear(this)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Apply dark mode based on saved preference
        DarkModePrefUtil.applySavedMode(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Navigation Component
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup Toolbar with NavController
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Setup BottomNavigationView with NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        binding.btnNotification.setOnClickListener {
            navController.navigate(R.id.NotificationFragment)
        }

        requestNotificationPermissionIfNeeded()
        ScreeningReminderScheduler.scheduleNext(this)
        submitPendingDeviceToken()
        updateNotificationBadge()
        startBadgeRefreshPolling()
        val refreshFilter = IntentFilter(MyFirebaseMessagingService.ACTION_NOTIFICATION_REFRESH)
        ContextCompat.registerReceiver(
            this,
            notificationRefreshReceiver,
            refreshFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) updateNotificationBadge()
        startBadgeRefreshPolling()
    }

    override fun onPause() {
        stopBadgeRefreshPolling()
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(notificationRefreshReceiver) }
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (
            requestCode == REQUEST_POST_NOTIFICATIONS &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            ScreeningReminderScheduler.scheduleNext(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_POST_NOTIFICATIONS
        )
    }

    private fun submitPendingDeviceToken() {
        val prefs = getSharedPreferences("fcm_token", MODE_PRIVATE)
        val token = prefs.getString("pending_token", null) ?: return
        lifecycleScope.launch {
            try {
                serelunaRepository.submitDeviceToken(token)
                prefs.edit().remove("pending_token").apply()
            } catch (_: Exception) {
            }
        }
    }

    private fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                val unreadCount = serelunaRepository.getNotificationUnreadCount().unread_count
                
                // Update header badge
                if (unreadCount > 0) {
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                    binding.tvNotificationBadge.text = if (unreadCount > 9) "9+" else unreadCount.toString()
                } else {
                    binding.tvNotificationBadge.visibility = View.GONE
                }

                // Update bottom nav badge
                if (unreadCount > 0) {
                    binding.bottomNavigationView.getOrCreateBadge(R.id.NotificationFragment).apply {
                        isVisible = true
                        number = unreadCount
                        backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_error)
                        badgeTextColor = ContextCompat.getColor(this@MainActivity, R.color.white)
                    }
                } else {
                    binding.bottomNavigationView.removeBadge(R.id.NotificationFragment)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun startBadgeRefreshPolling() {
        badgeRefreshHandler.removeCallbacks(badgeRefreshRunnable)
        badgeRefreshHandler.postDelayed(badgeRefreshRunnable, BADGE_REFRESH_INTERVAL_MS)
    }

    private fun stopBadgeRefreshPolling() {
        badgeRefreshHandler.removeCallbacks(badgeRefreshRunnable)
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 2101
        private const val BADGE_REFRESH_INTERVAL_MS = 60_000L
    }
}
