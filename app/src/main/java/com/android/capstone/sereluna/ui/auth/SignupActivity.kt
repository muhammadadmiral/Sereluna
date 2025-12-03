package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.data.model.UserProfile
import com.android.capstone.sereluna.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.apply {
            btnRegister.setOnClickListener {
                val name = usernameEditText.text.toString()
                val email = emailEditText.text.toString()
                val password = emailEditText2.text.toString()

                if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    registerUser(name, email, password)
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

    private fun registerUser(name: String, email: String, password: String) {
        if (password.length < 6) {
            Toast.makeText(this@SignupActivity, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid
                    if (userId != null) {
                        createUserDocument(userId, name, email)
                    } else {
                        Toast.makeText(this@SignupActivity, "Registration succeeded but user is unavailable", Toast.LENGTH_SHORT).show()
                    }
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

    private fun createUserDocument(userId: String, name: String, email: String) {
        val userProfile = UserProfile(
            name = name,
            email = email,
            photoUrl = "",
            provider = "password"
        )

        firestore.collection("users").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this@SignupActivity, "Failed to save profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
