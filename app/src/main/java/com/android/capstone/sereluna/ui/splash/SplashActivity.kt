package com.android.capstone.sereluna.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityLoginBinding
import com.android.capstone.sereluna.databinding.ActivitySplashBinding
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.ui.auth.SignupActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

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