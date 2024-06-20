package com.android.capstone.sereluna

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.android.capstone.sereluna.databinding.ActivityMainBinding
import com.android.capstone.sereluna.ui.main.HomeFragment
import com.android.capstone.sereluna.ui.notification.NotificationFragment
import com.android.capstone.sereluna.ui.setting.SettingFragment
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Apply dark mode based on saved preference
        val isDarkMode = DarkModePrefUtil.isDarkMode(this)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.background = null

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.notification -> {
                    replaceFragment(NotificationFragment())
                    true
                }
                R.id.setting -> {
                    replaceFragment(SettingFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }
}
