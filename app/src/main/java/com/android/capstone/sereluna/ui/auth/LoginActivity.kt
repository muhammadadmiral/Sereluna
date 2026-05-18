package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityLoginBinding
import com.android.capstone.sereluna.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val serelunaRepository = SerelunaRepository()
    private var rememberMe: Boolean = false
    private var isPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.apply {
            cbRememberMe.setOnCheckedChangeListener { _, isChecked ->
                rememberMe = isChecked
            }

            btnLogin.setOnClickListener {
                val email = emailEditText.text.toString().trim()
                val password = passwordEditText.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    loginUser(email, password)
                } else {
                    Toast.makeText(this@LoginActivity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }

            val openSignup = {
                val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                startActivity(intent)
            }
            btnOpenSignup.setOnClickListener { openSignup() }
            createAccountContainer.setOnClickListener { openSignup() }
            tvToRegister.setOnClickListener { openSignup() }
            tvForgotPassword.setOnClickListener { openForgotPassword() }
            btnTogglePassword.setOnClickListener { togglePasswordVisibility() }
        }
    }

    private fun loginUser(email: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Signing in..."
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        ensureUserDocument(user) {
                            com.android.capstone.sereluna.util.AuthSessionManager.startSession(this, rememberMe)
                            navigateToOnboarding()
                        }
                    } else {
                        binding.btnLogin.isEnabled = true
                        binding.btnLogin.text = "Login"
                        Toast.makeText(this@LoginActivity, "Login succeeded but user is unavailable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Email atau password salah."
                        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan di Firebase project ini."
                        else -> "Login gagal: ${task.exception?.message ?: "Unknown error"}"
                    }
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun ensureUserDocument(user: FirebaseUser, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                serelunaRepository.getProfile()
                onComplete()
            } catch (_: Exception) {
                createUserDocument(user, onComplete)
            }
        }
    }

    private fun createUserDocument(user: FirebaseUser, onComplete: () -> Unit) {
        val safeName = when {
            !user.displayName.isNullOrBlank() -> user.displayName!!
            !user.email.isNullOrBlank() -> user.email!!.substringBefore("@")
            else -> "New User"
        }

        lifecycleScope.launch {
            try {
                serelunaRepository.updateProfile(safeName, null)
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Failed to save account: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                onComplete()
            }
        }
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this@LoginActivity, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openForgotPassword() {
        val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        val start = binding.passwordEditText.selectionStart
        val end = binding.passwordEditText.selectionEnd
        if (isPasswordVisible) {
            binding.passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(com.android.capstone.sereluna.R.drawable.ic_hide_password)
        } else {
            binding.passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnTogglePassword.setImageResource(com.android.capstone.sereluna.R.drawable.ic_show_password)
        }
        binding.passwordEditText.setSelection(start.coerceAtLeast(0), end.coerceAtLeast(0))
    }
}
