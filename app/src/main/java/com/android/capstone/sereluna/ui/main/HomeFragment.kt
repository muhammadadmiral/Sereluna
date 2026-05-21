package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.diary.ScreeningActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private var nextScreeningDate: String? = null

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
        userViewModel.loadScreeningStatus()
    }

    private fun setupListeners() {
        binding.cvDiary.setOnClickListener {
            val intent = Intent(requireActivity(), DiaryActivity::class.java)
            startActivity(intent)
        }
        binding.cvScreening.setOnClickListener {
            startActivity(Intent(requireActivity(), ScreeningActivity::class.java))
        }
        // TODO: Add OnClickListeners for cvArticle, cvDoctor, cvCalendar, etc.
    }

    private var isUserLoaded = false
    private var isScreeningLoaded = false

    private fun updateVisibility() {
        if (isUserLoaded && isScreeningLoaded) {
            binding.homeProgressBar.visibility = View.GONE
            binding.homeContent.visibility = View.VISIBLE
        } else {
            binding.homeProgressBar.visibility = View.VISIBLE
            binding.homeContent.visibility = View.INVISIBLE
        }
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
                    isUserLoaded = true
                    updateVisibility()
                }
                is UiState.Loading -> {
                    isUserLoaded = false
                    updateVisibility()
                }
                is UiState.Error -> {
                    isUserLoaded = true 
                    updateVisibility()
                    binding.tvUserName.text = "Hi!"
                }
            }
        }

        userViewModel.screeningStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val status = state.data
                    nextScreeningDate = status.next_recommended_date
                    binding.cvScreening.visibility = View.VISIBLE 
                    binding.textViewScreening.text = if (status.is_due) {
                        "Skrining DASS-21"
                    } else {
                        nextScreeningDate?.let { "Skrining tersedia lagi: $it" }
                            ?: "Skrining tersedia lagi nanti"
                    }
                    isScreeningLoaded = true
                    updateVisibility()
                }
                is UiState.Loading -> {
                    isScreeningLoaded = false
                    updateVisibility()
                }
                is UiState.Error -> {
                    isScreeningLoaded = true
                    updateVisibility()
                    // Fallback to default state
                    binding.cvScreening.visibility = View.VISIBLE
                    binding.textViewScreening.text = "Skrining DASS-21"
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
