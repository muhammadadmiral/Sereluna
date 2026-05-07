package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()

        userViewModel.loadUserData()
    }

    private fun setupListeners() {
        binding.cvDiary.setOnClickListener {
            val intent = Intent(requireActivity(), DiaryActivity::class.java)
            startActivity(intent)
        }
        // TODO: Add OnClickListeners for cvArticle, cvDoctor, cvCalendar, etc.
    }

    private fun setupObservers() {
        userViewModel.userData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val userData = state.data
                    binding.tvUserName.text = "👋🏻 Hi, ${userData["name"] as? String ?: "User"}!"
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.circleImageView)
                    }
                }
                is UiState.Loading -> {
                    // You can show a placeholder/shimmer here
                    binding.tvUserName.text = "Loading..."
                }
                is UiState.Error -> {
                    // Handle error state
                    binding.tvUserName.text = "Error loading data"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
