package com.android.capstone.sereluna.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.apply {
            btnBack.setOnClickListener {
                finish()
            }

            btnSendResetLink.setOnClickListener {
                val email = etEmail.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this@ForgotPasswordActivity, "Mohon masukkan email Anda.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                sendResetLink(email)
            }
        }
    }

    private fun sendResetLink(email: String) {
        binding.btnSendResetLink.isEnabled = false
        binding.btnSendResetLink.text = "Mengirim..."

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.btnSendResetLink.isEnabled = true
                binding.btnSendResetLink.text = "Kirim Link Reset"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Link reset password telah dikirim ke email.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Terjadi kesalahan yang tidak diketahui."
                    Toast.makeText(this, "Gagal mengirim link: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }
}
