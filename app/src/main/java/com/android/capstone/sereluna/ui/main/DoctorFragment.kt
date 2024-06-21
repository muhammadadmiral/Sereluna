package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentDoctorBinding
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.squareup.picasso.Picasso

class DoctorFragment : Fragment() {

    private var _binding: FragmentDoctorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // You can load doctor information dynamically here
        loadDoctorData()

        // Example handling click for cvDoctor1
        binding.cvDoctor1.setOnClickListener {
            // Handle Doctor 1 click event
            // Example: Show DoctorDetailFragment or start DoctorActivity
            // For now, let's open DiaryActivity as an example
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }

        binding.cvDoctor2.setOnClickListener {
            // Handle Doctor 2 click event
            // Example: Show DoctorDetailFragment or start DoctorActivity
            // For now, let's open DiaryActivity as an example
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }

        // Similarly, handle click events for other doctors (cvDoctor3, cvDoctor4, etc.)
        // You can replace DiaryActivity with your specific doctor detail view or activity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadDoctorData() {
        // Example code to load doctor data (replace with your logic)
        // You can load doctor names, images, etc., dynamically here
        Picasso.get().load(R.drawable.doctor_icon).into(binding.ivDoctorIcon1)
        binding.tvDoctorName1.text = "Doctor 1"

        Picasso.get().load(R.drawable.doctor_icon).into(binding.ivDoctorIcon2)
        binding.tvDoctorName2.text = "Doctor 2"

        // Load data for other doctors similarly
    }
}
