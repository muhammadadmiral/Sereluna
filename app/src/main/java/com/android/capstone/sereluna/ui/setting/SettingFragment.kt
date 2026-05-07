package com.android.capstone.sereluna.ui.setting

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.capstone.sereluna.databinding.FragmentSettingBinding
import com.android.capstone.sereluna.ui.profile.ProfileActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

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

        setupClickListeners()
        setupObservers()

        userViewModel.loadUserData()
    }

    private fun setupClickListeners() {
        binding.cvAccount.setOnClickListener {
            val intent = Intent(requireActivity(), ProfileActivity::class.java)
            startActivity(intent)
        }

        // TODO: Implement other settings navigation (Logout, Dark Mode Switch, etc.)
    }

    private fun setupObservers() {
        userViewModel.userData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val userData = state.data
                    binding.tvUsername.text = userData["name"] as? String ?: "User"
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.profileImage)
                    }
                }
                is UiState.Loading -> {
                    binding.tvUsername.text = "Loading..."
                }
                is UiState.Error -> {
                    binding.tvUsername.text = "Error"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
