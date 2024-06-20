package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.diary.DiaryActivity

class MainFragment: Fragment() {

    private lateinit var binding : FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.cvDiary.setOnClickListener{
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }

    }


}