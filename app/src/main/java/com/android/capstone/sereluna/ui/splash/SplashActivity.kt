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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.view.View
import androidx.activity.viewModels
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.android.capstone.sereluna.ui.viewmodel.UiState

@SuppressLint("CustomSplashScreen")
class SplashActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply dark mode based on saved preference
        DarkModePrefUtil.applySavedMode(this)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp == null) {
            Toast.makeText(
                this,
                "Firebase config belum tersedia. Tambahkan app/google-services.json lokal.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val auth = FirebaseAuth.getInstance(firebaseApp)
        if (auth.currentUser != null && AuthSessionManager.isSessionValid(this)) {
            fetchInitialData()
        } else if (auth.currentUser != null && !AuthSessionManager.isSessionValid(this)) {
            AuthSessionManager.clear(this)
            showAuthButtons()
        } else {
            showAuthButtons()
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

            btnRetry.setOnClickListener {
                fetchInitialData()
            }
        }
    }

    private fun fetchInitialData() {
        hideAuthButtons()
        binding.errorLayout.visibility = View.GONE
        binding.progressBarSplash.visibility = View.VISIBLE
        userViewModel.loadUserData(forceRefresh = true)
    }

    private fun setupObservers() {
        userViewModel.userData.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    binding.progressBarSplash.visibility = View.GONE
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is UiState.Loading -> {
                    binding.progressBarSplash.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                    hideAuthButtons()
                }
                is UiState.Error -> {
                    binding.progressBarSplash.visibility = View.GONE
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvErrorMessage.text = state.message
                    hideAuthButtons()
                }
            }
        }
    }

    private fun showAuthButtons() {
        binding.btnLoginSplash.visibility = View.VISIBLE
        binding.btnSignupSplash.visibility = View.VISIBLE
        binding.progressBarSplash.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
    }

    private fun hideAuthButtons() {
        binding.btnLoginSplash.visibility = View.GONE
        binding.btnSignupSplash.visibility = View.GONE
    }
}
