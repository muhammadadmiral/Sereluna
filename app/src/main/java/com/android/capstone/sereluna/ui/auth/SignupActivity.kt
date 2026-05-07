package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.data.model.UserProfile
import com.android.capstone.sereluna.databinding.ActivitySignupBinding
import com.android.capstone.sereluna.ui.onboarding.OnboardingActivity
import com.android.capstone.sereluna.util.AuthSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
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
                val name = usernameEditText.text.toString().trim()
                val email = emailEditText.text.toString().trim()
                val password = emailEditText2.text.toString()

                if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                    registerUser(name, email, password)
                } else {
                    Toast.makeText(this@SignupActivity, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }

            val openLogin = {
                val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            loginLinkContainer.setOnClickListener { openLogin() }
            tvToLogin.setOnClickListener { openLogin() }
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        if (password.length < 6) {
            Toast.makeText(this@SignupActivity, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener {
                                createUserDocument(user.uid, name, email)
                            }
                    } else {
                        setLoading(false)
                        Toast.makeText(this@SignupActivity, "Registration succeeded but user is unavailable", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    setLoading(false)
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
                AuthSessionManager.startSession(this, rememberMe = false)
                val intent = Intent(this@SignupActivity, OnboardingActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { exception ->
                setLoading(false)
                Toast.makeText(this@SignupActivity, "Failed to save profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "Creating Account..." else "Create Account"
    }
}
