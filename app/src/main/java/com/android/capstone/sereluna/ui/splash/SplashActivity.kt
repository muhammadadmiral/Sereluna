package com.android.capstone.sereluna.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivitySplashBinding
import com.android.capstone.sereluna.MainActivity
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.ui.auth.SignupActivity
import com.android.capstone.sereluna.util.AuthSessionManager
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply dark mode based on saved preference
        DarkModePrefUtil.applySavedMode(this)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null && AuthSessionManager.isSessionValid(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        } else if (auth.currentUser != null && !AuthSessionManager.isSessionValid(this)) {
            AuthSessionManager.clear(this)
        }

        binding.apply {

            btnLoginSplash.setOnClickListener {
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
            }

            btnSignupSplash.setOnClickListener {
                val intent = Intent(this@SplashActivity, SignupActivity::class.java)
                startActivity(intent)
            }

        }

    }

}
