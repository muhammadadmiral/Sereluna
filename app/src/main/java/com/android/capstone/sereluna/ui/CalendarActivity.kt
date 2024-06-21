package com.android.capstone.sereluna.ui.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.databinding.ActivityCalendarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Setup calendar interaction logic
        binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            binding.selectedDateText.text = "Selected Date: $selectedDate"
            loadMoodAndDiaryForDate(selectedDate)
        }

        binding.addMoodButton.setOnClickListener {
            if (selectedDate.isNotEmpty()) {
                showAddMoodDialog()
            } else {
                Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddMoodDialog() {
        val moodDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_mood, null)
        val moodEditText = moodDialogView.findViewById<EditText>(R.id.moodEditText)

        AlertDialog.Builder(this)
            .setTitle("Add Mood for $selectedDate")
            .setView(moodDialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val mood = moodEditText.text.toString()
                saveMoodForDate(selectedDate, mood)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveMoodForDate(date: String, mood: String) {
        val userId = auth.currentUser?.uid ?: return
        val moodData = mapOf(
            "date" to date,
            "mood" to mood,
            "userId" to userId
        )

        firestore.collection("moods")
            .document("$userId-$date")
            .set(moodData)
            .addOnSuccessListener {
                Toast.makeText(this, "Mood saved for $date", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save mood: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMoodAndDiaryForDate(date: String) {
        val userId = auth.currentUser?.uid ?: return
        val moodRef = firestore.collection("moods").document("$userId-$date")
        val diaryRef = firestore.collection("diaries").whereEqualTo("date", date).whereEqualTo("userId", userId)

        moodRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val mood = document.getString("mood") ?: "No mood recorded"
                    binding.moodText.text = "Mood: $mood"
                } else {
                    binding.moodText.text = "Mood: No mood recorded"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load mood: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        diaryRef.get()
            .addOnSuccessListener { documents ->
                val diaryContent = StringBuilder("Diary Entries:\n")
                for (document in documents) {
                    diaryContent.append(document.getString("content")).append("\n")
                }
                Toast.makeText(this, diaryContent.toString(), Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load diary: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
