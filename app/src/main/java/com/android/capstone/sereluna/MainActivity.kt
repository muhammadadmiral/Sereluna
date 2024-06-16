package com.android.capstone.sereluna

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.capstone.sereluna.databinding.ActivityMainBinding
import com.android.capstone.sereluna.ui.main.MainFragment
import com.android.capstone.sereluna.ui.notification.NotificationFragment
import com.android.capstone.sereluna.ui.setting.SettingFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigationView.background = null

        // Set the initial fragment
        if (savedInstanceState == null) {
            replaceFragment(MainFragment())
        }

        @Suppress("DEPRECATION")
        binding.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(MainFragment())
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

   //     binding.fab.setOnClickListener {
            // Handle FAB click, e.g., open camera or navigate to a specific fragment
            // For example, open a new fragment or activity
    //    }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment)
            .commit()
    }

}