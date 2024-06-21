package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.calendar.CalendarActivity
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class MainFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userViewModel.userName.observe(viewLifecycleOwner, { name ->
            binding.tvUserName.text = "👋🏻 Hi $name!"
        })

        userViewModel.userPhotoUrl.observe(viewLifecycleOwner, { photoUrl ->
            if (photoUrl.isNotEmpty()) {
                Picasso.get().load(photoUrl).into(binding.circleImageView)
            } else {
                binding.circleImageView.setImageResource(R.drawable.profile_image)
            }
        })

        binding.cvArticle.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_articleFragment)
        }

        binding.cvDiary.setOnClickListener {
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }

        binding.cvCalendar.setOnClickListener {
            val intent = Intent(requireContext(), CalendarActivity::class.java)
            startActivity(intent)
        }

        binding.cvSleeptracking.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_sleepTrackingFragment)
        }

        binding.cvDoctor.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_doctorFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
