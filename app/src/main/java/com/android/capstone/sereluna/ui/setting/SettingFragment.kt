package com.android.capstone.sereluna.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentSettingBinding
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.android.capstone.sereluna.ui.profile.ProfileActivity
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.android.capstone.sereluna.util.DarkModePrefUtil
import com.android.capstone.sereluna.util.AuthSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class SettingFragment : Fragment() {

    private lateinit var binding: FragmentSettingBinding
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        // Mengamati data pengguna dari ViewModel
        userViewModel.userName.observe(viewLifecycleOwner, { name ->
            binding.tvUsername.text = name
        })

        userViewModel.userPhotoUrl.observe(viewLifecycleOwner, { photoUrl ->
            if (photoUrl.isNotEmpty()) {
                Picasso.get().load(photoUrl).into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.pp_suzy)
            }
        })

        // Dark Mode
        val switchDarkMode = binding.switchDarkMode
        val isDarkMode = DarkModePrefUtil.isDarkMode(requireContext())
        switchDarkMode.isChecked = isDarkMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            DarkModePrefUtil.setDarkMode(requireContext(), isChecked)
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            AuthSessionManager.clear(requireContext())
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        // Account (buka ProfileActivity)
        binding.cvAccount.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name").orEmpty()
                val photo = doc.getString("photoUrl").orEmpty()
                userViewModel.updateUserName(name.ifBlank { "Guest" })
                userViewModel.updateUserPhotoUrl(photo)
            }
    }
}
