package com.android.capstone.sereluna.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.capstone.sereluna.MainActivity
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.ActivityOnboardingBinding
import com.android.capstone.sereluna.ui.auth.LoginActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = OnboardingAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()
    }

    private inner class OnboardingAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return OnboardingFragment.newInstance(
                when (position) {
                    0 -> "Welcome to Sereluna, your safe space."
                    1 -> "Track your mood and reflect on your day."
                    else -> "Start your journey towards a better you."
                },
                position == itemCount - 1
            )
        }
    }
}

class OnboardingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_page, container, false)
        val titleTextView = view.findViewById<TextView>(R.id.tvOnboardingTitle)
        val finishButton = view.findViewById<Button>(R.id.btnOnboardingFinish)

        titleTextView.text = requireArguments().getString(ARG_TITLE)

        if (requireArguments().getBoolean(ARG_IS_LAST)) {
            finishButton.visibility = View.VISIBLE
            finishButton.setOnClickListener {
                startActivity(Intent(requireActivity(), LoginActivity::class.java))
                requireActivity().finish()
            }
        } else {
            finishButton.visibility = View.GONE
        }

        return view
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_IS_LAST = "is_last"

        fun newInstance(title: String, isLastPage: Boolean): OnboardingFragment {
            val fragment = OnboardingFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_TITLE, title)
                putBoolean(ARG_IS_LAST, isLastPage)
            }
            return fragment
        }
    }
}
