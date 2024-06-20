package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityLoginBinding
import com.android.capstone.sereluna.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.apply {
            btnLogin.setOnClickListener {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    loginUser(email, password)
                } else {
                    Toast.makeText(this@LoginActivity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }

            tvToRegister.setOnClickListener {
                val intent = Intent(this@LoginActivity, SignupActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login berhasil
                    val intent = Intent(this@LoginActivity, OnboardingActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Handle login gagal
                    val errorMessage = "Authentication failed: ${task.exception?.message}"
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
