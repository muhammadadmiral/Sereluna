package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentHomeBinding // FIX: Use existing binding from HomeFragment
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.diary.ScreeningActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso
import com.android.capstone.sereluna.ui.CalendarActivity
import com.android.capstone.sereluna.data.repository.ScreeningRepository
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    // FIX: Changed type to FragmentHomeBinding
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private val screeningRepository = ScreeningRepository()
    private var screeningDoneToday: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // FIX: Inflate the correct, existing layout
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupObservers()
        checkScreeningStatus()
        userViewModel.loadUserData()
    }

    override fun onResume() {
        super.onResume()
        checkScreeningStatus()
    }

    private fun setupListeners() {
        binding.cvDiary.setOnClickListener {
            startActivity(Intent(requireContext(), DiaryActivity::class.java))
        }
        binding.cvArticle.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_articleFragment)
        }
        binding.cvDoctor.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_doctorFragment)
        }
        binding.cvScreening.setOnClickListener {
            if (screeningDoneToday) {
                Toast.makeText(requireContext(), "Jatah skrining harian sudah digunakan.", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(requireContext(), ScreeningActivity::class.java))
            }
        }
        binding.cvCalendar.setOnClickListener {
            startActivity(Intent(requireContext(), CalendarActivity::class.java))
        }
        binding.cvSleeptracking.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_sleepTrackingFragment)
        }
    }

    private fun checkScreeningStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                screeningDoneToday = screeningRepository.hasTodayScreening()
                binding.cvScreening.isEnabled = !screeningDoneToday
                binding.cvScreening.alpha = if (screeningDoneToday) 0.5f else 1f
            } catch (_: Exception) {
                // jika gagal cek, biarkan tetap enabled
            }
        }
    }

    private fun setupObservers() {
        userViewModel.userData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val userData = state.data
                    // FIX: Use correct view ID from fragment_home.xml
                    binding.tvUserName.text = "Hello, ${userData["name"] as? String ?: "User"}!"
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        // FIX: Use correct view ID from fragment_home.xml
                        Picasso.get().load(photoUrl).into(binding.circleImageView)
                    }
                }
                else -> {
                    // Handle loading or error state
                    binding.tvUserName.text = "Loading..."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
