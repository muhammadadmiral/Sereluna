package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.article.ArticleFragment
import com.android.capstone.sereluna.ui.diary.ChatbotActivity
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {

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
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, ArticleFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cvDiary.setOnClickListener {
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }
        binding.cvButton1.setOnClickListener {
            val intent = Intent(requireContext(), ChatbotActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
