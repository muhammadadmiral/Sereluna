package com.android.capstone.sereluna.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.R

class CalendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Setup your CalendarView and other UI elements here
    }

    fun onBackButtonClicked(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}
