package com.android.capstone.sereluna

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.capstone.sereluna.databinding.ActivityMainBinding
import com.android.capstone.sereluna.service.ScreeningReminderScheduler
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.util.AuthSessionManager
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.google.firebase.FirebaseApp

import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.NotificationAdapter
import com.android.capstone.sereluna.data.repository.NotificationRepository
import com.android.capstone.sereluna.databinding.DropdownNotificationsBinding
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val notificationRepository = NotificationRepository()
    private val notificationAdapter = NotificationAdapter()
    private var notificationListener: ListenerRegistration? = null
    private var notificationPopup: PopupWindow? = null

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
        setupActionBarWithNavController(navController)

        // Setup BottomNavigationView with NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        setupNotificationDropdown()

        requestNotificationPermissionIfNeeded()
        ScreeningReminderScheduler.scheduleNext(this)
    }

    private fun setupNotificationDropdown() {
        binding.btnNotificationDropdown.setOnClickListener {
            showNotificationDropdown()
        }

        notificationListener = notificationRepository.observeNotifications(
            onChanged = { notifications ->
                // Filter out background notifications like diary summary
                val filteredNotifications = notifications.filter { 
                    it.notifStatus != "diary" && it.notifStatus != "diary_summary" 
                }
                notificationAdapter.submitList(filteredNotifications)
            },
            onError = { /* Handle error */ }
        )
    }

    private fun showNotificationDropdown() {
        val dropdownBinding = DropdownNotificationsBinding.inflate(LayoutInflater.from(this))
        dropdownBinding.rvDropdownNotifications.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notificationAdapter
        }

        // Check if list is empty
        if (notificationAdapter.itemCount == 0) {
            dropdownBinding.tvEmptyNotifications.visibility = android.view.View.VISIBLE
            dropdownBinding.rvDropdownNotifications.visibility = android.view.View.GONE
        } else {
            dropdownBinding.tvEmptyNotifications.visibility = android.view.View.GONE
            dropdownBinding.rvDropdownNotifications.visibility = android.view.View.VISIBLE
        }

        notificationPopup = PopupWindow(
            dropdownBinding.root,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            showAsDropDown(binding.btnNotificationDropdown, 0, 0)
        }
    }

    override fun onDestroy() {
        notificationListener?.remove()
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

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 2101
    }
}
