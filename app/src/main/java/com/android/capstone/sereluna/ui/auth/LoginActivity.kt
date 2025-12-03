package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.data.model.UserProfile
import com.android.capstone.sereluna.databinding.ActivityLoginBinding
import com.android.capstone.sereluna.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

            btnCreateAccount.setOnClickListener {
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    createAccount(email, password)
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

    private fun createAccount(email: String, password: String) {
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        createUserDocument(user) {
                            navigateToOnboarding()
                        }
                    } else {
                        Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = "Registration failed: ${task.exception?.message}"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        ensureUserDocument(user) {
                            navigateToOnboarding()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Login succeeded but user is unavailable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle login gagal
                    val errorMessage = "Authentication failed: ${task.exception?.message}"
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun ensureUserDocument(user: FirebaseUser, onComplete: () -> Unit) {
        val userRef = firestore.collection("users").document(user.uid)
        userRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    onComplete()
                } else {
                    createUserDocument(user, onComplete)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load account data: ${exception.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun createUserDocument(user: FirebaseUser, onComplete: () -> Unit) {
        val safeName = when {
            !user.displayName.isNullOrBlank() -> user.displayName!!
            !user.email.isNullOrBlank() -> user.email!!.substringBefore("@")
            else -> "New User"
        }

        val userProfile = UserProfile(
            name = safeName,
            email = user.email.orEmpty(),
            photoUrl = "",
            provider = "password"
        )

        firestore.collection("users").document(user.uid)
            .set(userProfile)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to save account: ${exception.message}", Toast.LENGTH_SHORT).show()
                onComplete()
            }
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this@LoginActivity, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }
}
