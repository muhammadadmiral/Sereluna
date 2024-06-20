package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.apply {
            btnRegister.setOnClickListener {
                val email = emailEditText.text.toString()
                val password = emailEditText2.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    registerUser(email, password)
                } else {
                    Toast.makeText(this@SignupActivity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }

            tvToLogin.setOnClickListener {
                val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registrasi berhasil, arahkan ke LoginActivity atau halaman login lainnya
                    val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Registrasi gagal, tampilkan pesan error yang spesifik
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." -> "Email already registered. Please use a different email."
                        else -> "Registration failed: ${task.exception?.message}"
                    }
                    Toast.makeText(this@SignupActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}
