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
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val serelunaRepository = SerelunaRepository()
    private val refreshHandler = Handler(Looper.getMainLooper())
    
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateNotificationBadge()
            updateGamificationData()
            refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }
    
    private val internalBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNotificationBadge()
            updateGamificationData()
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

        DarkModePrefUtil.applySavedMode(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.bottomNavigationView.setupWithNavController(navController)

        binding.cvGamification.setOnClickListener {
            // Future: Navigate to Aura Detail page
        }

        requestNotificationPermissionIfNeeded()
        ScreeningReminderScheduler.scheduleNext(this)
        submitPendingDeviceToken()
        
        // Initial Fetch
        updateNotificationBadge()
        updateGamificationData()
        startPolling()
        
        val filter = IntentFilter(MyFirebaseMessagingService.ACTION_NOTIFICATION_REFRESH)
        ContextCompat.registerReceiver(this, internalBroadcastReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            updateNotificationBadge()
            updateGamificationData()
        }
        startPolling()
    }

    override fun onPause() {
        stopPolling()
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(internalBroadcastReceiver) }
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2101)
    }

    private fun submitPendingDeviceToken() {
        val prefs = getSharedPreferences("fcm_token", MODE_PRIVATE)
        val token = prefs.getString("pending_token", null) ?: return
        lifecycleScope.launch {
            try {
                serelunaRepository.submitDeviceToken(token)
                prefs.edit().remove("pending_token").apply()
            } catch (_: Exception) {}
        }
    }

    private fun updateNotificationBadge() {
        lifecycleScope.launch {
            try {
                val unreadCount = serelunaRepository.getNotificationUnreadCount().unread_count
                if (unreadCount > 0) {
                    binding.bottomNavigationView.getOrCreateBadge(R.id.NotificationFragment).apply {
                        isVisible = true
                        number = unreadCount
                        backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_error)
                    }
                } else {
                    binding.bottomNavigationView.removeBadge(R.id.NotificationFragment)
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateGamificationData() {
        lifecycleScope.launch {
            try {
                // Technical Guide: Backend team needs to provide GET /user/gamification
                // Data shown below is the hyper-complex state designed to care for the user
                val level = 14 
                val streak = 8
                val progress = 72
                val rank = "Crescent Voyager"

                binding.tvUserLevel.text = level.toString()
                binding.tvStreakCount.text = "$streak Days"
                binding.pbAuraProgress.progress = progress
                binding.tvRankTitle.text = rank

                // Visual care: Glow effect if streak is high
                if (streak >= 7) {
                    binding.cvGamification.strokeColor = ContextCompat.getColor(this@MainActivity, R.color.brand_rose_gold)
                }

            } catch (_: Exception) {}
        }
    }

    private fun startPolling() {
        refreshHandler.removeCallbacks(refreshRunnable)
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
    }

    private fun stopPolling() {
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 60_000L
    }
}
