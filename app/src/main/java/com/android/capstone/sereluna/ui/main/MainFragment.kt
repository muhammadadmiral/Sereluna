package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.api.ScreeningStatusDto
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.diary.ScreeningActivity
import com.android.capstone.sereluna.ui.viewmodel.UiState
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso
import com.android.capstone.sereluna.ui.CalendarActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val userViewModel: UserViewModel by activityViewModels()
    private var isScreeningDue: Boolean = true
    private var screeningStatus: ScreeningStatusDto? = null
    private var isUserLoaded = false
    private var isScreeningLoaded = false
    private val countdownHandler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
            startActivity(Intent(requireContext(), DiaryActivity::class.java))
        }
        binding.cvArticle.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_articleFragment)
        }
        binding.cvDoctor.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_doctorFragment)
        }
        binding.cvScreening.setOnClickListener {
            if (!isScreeningDue) {
                Toast.makeText(requireContext(), screeningAvailableText(), Toast.LENGTH_SHORT).show()
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

    private fun updateHomeVisibility() {
        val ready = isUserLoaded && isScreeningLoaded
        binding.homeProgressBar.visibility = if (ready) View.GONE else View.VISIBLE
        binding.homeContent.visibility = if (ready) View.VISIBLE else View.INVISIBLE
    }

    private fun renderScreeningCard() {
        binding.cvScreening.isEnabled = true
        binding.cvScreening.alpha = if (isScreeningDue) 1f else 0.72f
        binding.textViewScreening.text = if (isScreeningDue) {
            stopScreeningCountdown()
            "Skrining DASS-21"
        } else {
            startScreeningCountdown()
            screeningAvailableText()
        }
    }

    private fun screeningAvailableText(): String {
        val millis = screeningTargetMillis()?.minus(System.currentTimeMillis())
        if (millis != null && millis > 0) {
            val totalSeconds = millis / 1000
            val days = totalSeconds / 86_400
            val hours = (totalSeconds % 86_400) / 3_600
            val minutes = (totalSeconds % 3_600) / 60
            val seconds = totalSeconds % 60
            return if (days > 0) {
                "Skrining tersedia lagi dalam ${days} hari %02d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "Skrining tersedia lagi dalam %02d:%02d:%02d".format(hours, minutes, seconds)
            }
        }

        val days = screeningStatus?.next_recommended_in_days
        if (days != null) {
            return when {
                days <= 0 -> "Skrining tersedia lagi hari ini"
                days == 1 -> "Skrining tersedia lagi besok"
                else -> "Skrining tersedia lagi dalam $days hari"
            }
        }
        val nextDate = screeningStatus?.next_recommended_date
        return if (nextDate.isNullOrBlank()) {
            "Skrining tersedia lagi nanti"
        } else {
            "Skrining tersedia lagi: $nextDate"
        }
    }

    private fun screeningTargetMillis(): Long? {
        val dateText = screeningStatus?.next_recommended_date
        if (!dateText.isNullOrBlank()) {
            val parsed = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateText)?.time
            }.getOrNull()
            if (parsed != null && parsed > System.currentTimeMillis()) return parsed
        }

        val days = screeningStatus?.next_recommended_in_days ?: return null
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, days.coerceAtLeast(0))
        }.timeInMillis
    }

    private fun startScreeningCountdown() {
        stopScreeningCountdown()
        countdownRunnable = object : Runnable {
            override fun run() {
                if (!isScreeningDue && _binding != null) {
                    binding.textViewScreening.text = screeningAvailableText()
                    countdownHandler.postDelayed(this, 1000L)
                }
            }
        }
        countdownHandler.post(countdownRunnable!!)
    }

    private fun stopScreeningCountdown() {
        countdownRunnable?.let { countdownHandler.removeCallbacks(it) }
        countdownRunnable = null
    }

    private fun setupObservers() {
        userViewModel.userData.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    val userData = state.data
                    binding.tvUserName.text = "Hello, ${userData["name"] as? String ?: "User"}!"
                    val photoUrl = userData["photoUrl"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        Picasso.get().load(photoUrl).into(binding.circleImageView)
                    }
                    isUserLoaded = true
                    updateHomeVisibility()
                }
                is UiState.Loading -> {
                    isUserLoaded = false
                    updateHomeVisibility()
                }
                is UiState.Error -> {
                    isUserLoaded = true
                    binding.tvUserName.text = "Loading..."
                    updateHomeVisibility()
                }
            }
        }

        userViewModel.screeningStatus.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Success -> {
                    screeningStatus = state.data
                    isScreeningDue = state.data.is_due
                    isScreeningLoaded = true
                    renderScreeningCard()
                    updateHomeVisibility()
                }
                is UiState.Loading -> {
                    isScreeningLoaded = false
                    updateHomeVisibility()
                }
                is UiState.Error -> {
                    isScreeningDue = true
                    isScreeningLoaded = true
                    renderScreeningCard()
                    updateHomeVisibility()
                }
            }
        }
    }

    override fun onDestroyView() {
        stopScreeningCountdown()
        super.onDestroyView()
        _binding = null
    }
}
