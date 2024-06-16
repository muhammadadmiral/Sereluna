package com.android.capstone.sereluna.data.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.capstone.sereluna.ui.onboarding.OnboardingFragment1
import com.android.capstone.sereluna.ui.onboarding.OnboardingFragment2
import com.android.capstone.sereluna.ui.onboarding.OnboardingFragment3

class OnboardingAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragmentList = listOf(
        OnboardingFragment1(),
        OnboardingFragment2(),
        OnboardingFragment3()

    )

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

}