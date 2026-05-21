package com.android.capstone.sereluna.ui.setting

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.FragmentSettingBinding
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.ui.profile.ProfileActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.android.capstone.sereluna.util.AuthSessionManager
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private val repository = SerelunaRepository()

    private val profileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            userViewModel.loadUserData(forceRefresh = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSettingRows()
        setupClickListeners()
        setupObservers()

        userViewModel.loadUserData()
    }

    private fun initSettingRows() {
        // Akun Section
        binding.itemEditProfile.apply {
            tvSettingIcon.text = "\ue3c9" // edit
            tvSettingTitle.text = "Edit Profil"
        }
        binding.itemChangePassword.apply {
            tvSettingIcon.text = "\ue897" // lock
            tvSettingTitle.text = "Ganti Password Manual"
        }
        binding.itemResetEmail.apply {
            tvSettingIcon.text = "\ue0be" // email
            tvSettingTitle.text = "Reset via Link Email"
        }
        binding.itemDeleteAccount.apply {
            tvSettingIcon.text = "\ue872" // delete
            tvSettingTitle.text = "Hapus Akun"
            tvSettingTitle.setTextColor(resources.getColor(R.color.red_error, null))
            tvSettingIcon.setTextColor(resources.getColor(R.color.red_error, null))
        }

        // Legal Section
        binding.itemTerms.apply {
            tvSettingIcon.text = "\uef42" // description
            tvSettingTitle.text = "Syarat & Ketentuan"
        }
        binding.itemPrivacy.apply {
            tvSettingIcon.text = "\ue898" // lock_person
            tvSettingTitle.text = "Kebijakan Privasi"
        }
    }

    private fun setupClickListeners() {
        binding.itemEditProfile.root.setOnClickListener {
            val intent = Intent(requireActivity(), ProfileActivity::class.java)
            profileLauncher.launch(intent)
        }

        binding.itemChangePassword.root.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.itemResetEmail.root.setOnClickListener {
            val email = binding.tvEmail.text.toString()
            if (email.isNotEmpty()) {
                showResetEmailConfirmation(email)
            }
        }

        binding.itemDeleteAccount.root.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        binding.itemTerms.root.setOnClickListener {
            openUrl("https://sereluna.app/terms")
        }

        binding.itemPrivacy.root.setOnClickListener {
            openUrl("https://sereluna.app/privacy")
        }

        // Dark Mode Toggle
        val isDarkModeEnabled = DarkModePrefUtil.isDarkMode(requireContext())
        binding.switchDarkMode.setOnCheckedChangeListener(null)
        binding.switchDarkMode.isChecked = isDarkModeEnabled
        
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != isDarkModeEnabled) {
                requireActivity().overridePendingTransition(0, 0)
                DarkModePrefUtil.setDarkMode(requireContext(), isChecked)
            }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etOld = dialogView.findViewById<EditText>(R.id.etOldPassword)
        val etNew = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirm = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ganti Password")
            .setView(dialogView)
            .setPositiveButton("Ganti") { dialog, _ ->
                val oldPass = etOld.text.toString()
                val newPass = etNew.text.toString()
                val confirmPass = etConfirm.text.toString()

                if (newPass.length < 8) {
                    Toast.makeText(requireContext(), "Password baru minimal 8 karakter", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPass != confirmPass) {
                    Toast.makeText(requireContext(), "Konfirmasi password tidak cocok", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    try {
                        repository.changePassword(oldPass, newPass)
                        Toast.makeText(requireContext(), "Password berhasil diganti", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus Akun Permanen?")
            .setMessage("Akun dan seluruh data kamu akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteAccount()
                        performLogout()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal menghapus akun: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Keluar dari Akun?")
            .setMessage("Apakah kamu yakin ingin keluar dari Sereluna?")
            .setPositiveButton("Keluar") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showResetEmailConfirmation(email: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Kirim Link Reset Password?")
            .setMessage("Kami akan mengirimkan link instruksi untuk mengganti password ke email Anda ($email).")
            .setPositiveButton("Kirim") { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.forgotPassword(email)
                        Toast.makeText(requireContext(), "Link berhasil dikirim. Silakan cek email Anda.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Gagal mengirim link: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun performLogout() {
        AuthSessionManager.clear(requireContext())
        lifecycleScope.launch {
            repository.signOut()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membuka link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        userViewModel.userData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val userData = state.data
                    binding.tvUsername.text = userData["name"] as? String ?: "User"
                    binding.tvEmail.text = userData["email"] as? String ?: ""
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get().load(photoUrl).placeholder(R.drawable.profile_image).into(binding.profileImage)
                    }
                }
                is UiState.Loading -> {
                    binding.tvUsername.text = "Memuat..."
                }
                is UiState.Error -> {
                    binding.tvUsername.text = "Gagal memuat profil"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
