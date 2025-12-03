package com.android.capstone.sereluna.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.FragmentHomeBinding
import com.android.capstone.sereluna.ui.article.ArticleFragment
import com.android.capstone.sereluna.ui.calendar.CalendarActivity
import com.android.capstone.sereluna.ui.diary.DiaryActivity
import com.android.capstone.sereluna.ui.sleeptracking.SleepTrackingFragment
import com.android.capstone.sereluna.ui.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

        loadProfile()

        userViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = "Hi, ${name.ifBlank { "there" }}!"
        }

        userViewModel.userPhotoUrl.observe(viewLifecycleOwner) { photoUrl ->
            if (photoUrl.isNotEmpty()) {
                Picasso.get().load(photoUrl).into(binding.circleImageView)
            } else {
                binding.circleImageView.setImageResource(R.drawable.profile_image)
            }
        }

        binding.cvDiary.setOnClickListener {
            val intent = Intent(requireContext(), DiaryActivity::class.java)
            startActivity(intent)
        }

        binding.cvArticle.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.nav_host_fragment, ArticleFragment())
                addToBackStack(null)
            }
        }

        binding.cvDoctor.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.nav_host_fragment, DoctorFragment())
                addToBackStack(null)
            }
        }

        binding.cvCalendar.setOnClickListener {
            val intent = Intent(requireContext(), CalendarActivity::class.java)
            startActivity(intent)
        }

        binding.cvSleeptracking.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.nav_host_fragment, SleepTrackingFragment())
                addToBackStack(null)
            }
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
                userViewModel.updateUserName(name.ifBlank { "there" })
                userViewModel.updateUserPhotoUrl(photo)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
