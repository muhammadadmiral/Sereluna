package com.android.capstone.sereluna.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.databinding.ActivityForgotPasswordBinding
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val repository = SerelunaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        lifecycleScope.launch {
            try {
                repository.forgotPassword(email)
                binding.btnSendResetLink.isEnabled = true
                binding.btnSendResetLink.text = "Kirim Link Reset"
                Toast.makeText(this@ForgotPasswordActivity, "Jika email terdaftar, link reset password telah dikirim.", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                binding.btnSendResetLink.isEnabled = true
                binding.btnSendResetLink.text = "Kirim Link Reset"
                Toast.makeText(this@ForgotPasswordActivity, "Berhasil mengirim instruksi ke email.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
