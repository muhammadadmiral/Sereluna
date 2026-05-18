package com.android.capstone.sereluna.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.ActivityResetPasswordBinding
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var auth: FirebaseAuth
    private var oobCode: String? = null
    
    private var isNewPasswordVisible: Boolean = false
    private var isConfirmPasswordVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Get the Intent data (the deep link)
        val data = intent.data
        if (data != null) {
            val mode = data.getQueryParameter("mode")
            oobCode = data.getQueryParameter("oobCode")

            if (mode != "resetPassword" || oobCode.isNullOrEmpty()) {
                Toast.makeText(this, "Link tidak valid atau kadaluarsa.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        } else {
            Toast.makeText(this, "Akses langsung tidak diizinkan.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.apply {
            btnToggleNewPassword.setOnClickListener { toggleNewPasswordVisibility() }
            btnToggleConfirmPassword.setOnClickListener { toggleConfirmPasswordVisibility() }

            btnSavePassword.setOnClickListener {
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this@ResetPasswordActivity, "Password tidak boleh kosong.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this@ResetPasswordActivity, "Password tidak cocok.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (newPassword.length < 6) {
                    Toast.makeText(this@ResetPasswordActivity, "Password minimal 6 karakter.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                saveNewPassword(newPassword)
            }
        }
    }

    private fun saveNewPassword(newPassword: String) {
        val code = oobCode ?: return
        
        binding.btnSavePassword.isEnabled = false
        binding.btnSavePassword.text = "Menyimpan..."

        auth.confirmPasswordReset(code, newPassword)
            .addOnCompleteListener { task ->
                binding.btnSavePassword.isEnabled = true
                binding.btnSavePassword.text = "Simpan Password"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password berhasil diubah. Silakan login.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = task.exception?.message ?: "Gagal mengubah password."
                    Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun toggleNewPasswordVisibility() {
        isNewPasswordVisible = !isNewPasswordVisible
        val start = binding.etNewPassword.selectionStart
        val end = binding.etNewPassword.selectionEnd
        if (isNewPasswordVisible) {
            binding.etNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnToggleNewPassword.setImageResource(R.drawable.ic_hide_password)
        } else {
            binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnToggleNewPassword.setImageResource(R.drawable.ic_show_password)
        }
        binding.etNewPassword.setSelection(start.coerceAtLeast(0), end.coerceAtLeast(0))
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        val start = binding.etConfirmPassword.selectionStart
        val end = binding.etConfirmPassword.selectionEnd
        if (isConfirmPasswordVisible) {
            binding.etConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_hide_password)
        } else {
            binding.etConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.btnToggleConfirmPassword.setImageResource(R.drawable.ic_show_password)
        }
        binding.etConfirmPassword.setSelection(start.coerceAtLeast(0), end.coerceAtLeast(0))
    }
}
