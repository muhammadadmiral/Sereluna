package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.MainActivity
import com.android.capstone.sereluna.databinding.ActivityLoginBinding
import com.android.capstone.sereluna.ui.onboarding.OnboardingActivity

class LoginActivity:AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {

            btnLogin.setOnClickListener {
                val intent = Intent(this@LoginActivity, OnboardingActivity::class.java)
                startActivity(intent)
            }
            tvToRegister.setOnClickListener {
                val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                startActivity(intent)
            }

        }

    }

}