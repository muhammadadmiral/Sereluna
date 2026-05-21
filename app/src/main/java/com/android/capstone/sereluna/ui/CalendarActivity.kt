package com.android.capstone.sereluna.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.api.CalendarDetailDto
import com.android.capstone.sereluna.data.api.CalendarSummaryItemDto
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityCalendarBinding
import com.android.capstone.sereluna.databinding.DialogCalendarDetailBinding
import com.android.capstone.sereluna.databinding.DialogMoodBinding
import com.android.capstone.sereluna.databinding.ItemCalendarDayBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var calendarAdapter: CalendarDayAdapter
    private val repository = SerelunaRepository()
    private val localeId = Locale("id", "ID")
    private val monthFormat = SimpleDateFormat("MMMM yyyy", localeId)
    private val readableDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", localeId)
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayTimeFormat = SimpleDateFormat("HH:mm", localeId)
    private val isoParsers = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    )

    private var currentMonth: Calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private var selectedDate: Calendar = Calendar.getInstance()
    private var summaryByDate: Map<String, CalendarSummaryItemDto> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar()
        setupListeners()
        renderMonth()
        loadMonthSummary()
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter(
            colorForIndicator = ::indicatorColor,
            onClick = { cell ->
                selectDate(cell.date)
                loadDayDetail(cell.dateKey)
            }
        )
        binding.calendarDaysRecyclerView.apply {
            layoutManager = GridLayoutManager(this@CalendarActivity, 7)
            adapter = calendarAdapter
        }
    }

    private fun setupListeners() {
        binding.calendarToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.previousMonthButton.setOnClickListener { moveMonth(-1) }
        binding.nextMonthButton.setOnClickListener { moveMonth(1) }
        binding.retryButton.setOnClickListener { loadMonthSummary() }
        binding.addMoodButton.setOnClickListener {
            if (selectedDate.after(Calendar.getInstance())) {
                Toast.makeText(this, "Tidak bisa mencatat mood untuk masa depan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showMoodSheet()
        }
    }

    private fun moveMonth(delta: Int) {
        val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)
        currentMonth.add(Calendar.MONTH, delta)
        currentMonth.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate = currentMonth.cloneCalendar().apply {
            set(Calendar.DAY_OF_MONTH, min(selectedDay, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        renderMonth()
        loadMonthSummary()
    }

    private fun renderMonth() {
        binding.monthTitleText.text = monthFormat.format(currentMonth.time)
        binding.selectedDateText.text = "Pilih tanggal untuk melihat detail"
        binding.addMoodButton.text = "Catat mood ${selectedDate.get(Calendar.DAY_OF_MONTH)}"
        calendarAdapter.submit(buildCalendarCells())
    }

    private fun buildCalendarCells(): List<CalendarDayCell> {
        val firstDay = currentMonth.cloneCalendar().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val leadingBlanks = (firstDay.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val cells = mutableListOf<CalendarDayCell>()

        repeat(leadingBlanks) { cells.add(CalendarDayCell.blank()) }

        val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayKey = dateKeyFormat.format(Calendar.getInstance().time)
        val selectedKey = selectedDateKey()

        for (day in 1..daysInMonth) {
            val date = currentMonth.cloneCalendar().apply {
                set(Calendar.DAY_OF_MONTH, day)
            }
            val dateKey = dateKeyFormat.format(date.time)
            cells.add(
                CalendarDayCell(
                    date = date,
                    dateKey = dateKey,
                    day = day,
                    isToday = dateKey == todayKey,
                    isSelected = dateKey == selectedKey,
                    summary = summaryByDate[dateKey]
                )
            )
        }

        while (cells.size % 7 != 0) {
            cells.add(CalendarDayCell.blank())
        }
        return cells
    }

    private fun loadMonthSummary() {
        val year = currentMonth.get(Calendar.YEAR)
        val month = currentMonth.get(Calendar.MONTH) + 1
        binding.loadingIndicator.isVisible = true
        binding.errorText.isVisible = false
        binding.retryButton.isVisible = false

        lifecycleScope.launch {
            try {
                val items = repository.getCalendarSummary(year, month)
                summaryByDate = items.associateBy { it.date }
                binding.loadingIndicator.isVisible = false
                renderMonth()
            } catch (e: Exception) {
                binding.loadingIndicator.isVisible = false
                binding.errorText.text = "Gagal memuat kalender."
                binding.errorText.isVisible = true
                binding.retryButton.isVisible = true
                renderMonth()
            }
        }
    }

    private fun selectDate(date: Calendar?) {
        if (date == null) return
        selectedDate = date.cloneCalendar()
        renderMonth()
    }

    private fun selectedDateKey(): String = dateKeyFormat.format(selectedDate.time)

    private fun loadDayDetail(dateKey: String?) {
        if (dateKey == null) return
        lifecycleScope.launch {
            try {
                val detail = repository.getCalendarDetail(dateKey)
                showDayDetail(detail)
            } catch (e: Exception) {
                Toast.makeText(this@CalendarActivity, "Gagal memuat detail hari ini", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDayDetail(detail: CalendarDetailDto) {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val detailBinding = DialogCalendarDetailBinding.inflate(layoutInflater)
        
        detailBinding.tvDetailDate.text = readableDateFormat.format(selectedDate.time)
        
        // Mood setup
        val mood = detail.mood
        if (mood != null) {
            detailBinding.tvMoodIcon.text = moodEmoji(mood)
            detailBinding.tvMoodLabel.text = mood.toDisplayLabel()
        } else {
            detailBinding.tvMoodIcon.text = "\ue10c" // neutral icon
            detailBinding.tvMoodLabel.text = "Belum ada mood"
        }

        // Sleep setup
        if (detail.has_sleep_data) {
            detailBinding.cvSleepDetail.visibility = View.VISIBLE
            val sleepInfo = "${formatHours(detail.sleep.total_sleep_hours)} (${detail.sleep.sleep_quality?.toDisplayLabel() ?: "Cukup"})"
            detailBinding.tvSleepDuration.text = sleepInfo
        } else {
            detailBinding.cvSleepDetail.visibility = View.GONE
        }

        // Diary setup
        if (detail.has_diary) {
            detailBinding.tvDiaryTitle.visibility = View.VISIBLE
            detailBinding.tvDiarySnippet.visibility = View.VISIBLE
            detailBinding.tvDiarySnippet.text = detail.diary_snippet
        } else {
            detailBinding.tvDiaryTitle.visibility = View.GONE
            detailBinding.tvDiarySnippet.visibility = View.GONE
        }

        // AI Insight setup
        if (!detail.wellbeing.recommendation.isNullOrBlank()) {
            detailBinding.cvAiInsight.visibility = View.VISIBLE
            detailBinding.tvAiInsightText.text = detail.wellbeing.recommendation
        } else {
            detailBinding.cvAiInsight.visibility = View.GONE
        }

        detailBinding.btnCloseSheet.setOnClickListener { dialog.dismiss() }
        
        dialog.setContentView(detailBinding.root)
        dialog.show()
    }

    private fun showMoodSheet() {
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val moodBinding = DialogMoodBinding.inflate(layoutInflater)
        val dateKey = selectedDateKey()
        
        moodBinding.moodDateText.text = readableDateFormat.format(selectedDate.time)

        moodBinding.saveMoodButton.setOnClickListener {
            val checkedId = moodBinding.moodChipGroup.checkedChipId
            val chip = moodBinding.root.findViewById<Chip>(checkedId)
            val mood = chip?.tag?.toString() ?: "happy"
            moodBinding.saveMoodButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    repository.submitMood(dateKey, mood)
                    Toast.makeText(this@CalendarActivity, "Mood tersimpan.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadMonthSummary()
                } catch (e: Exception) {
                    moodBinding.saveMoodButton.isEnabled = true
                    Toast.makeText(this@CalendarActivity, "Gagal menyimpan mood.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.setContentView(moodBinding.root)
        dialog.show()
    }

    private fun indicatorColor(indicator: String?): Int {
        return when (indicator?.lowercase(Locale.ROOT)) {
            "green", "stable" -> ContextCompat.getColor(this, R.color.calendar_green)
            "yellow", "watch" -> ContextCompat.getColor(this, R.color.calendar_yellow)
            "orange", "attention" -> ContextCompat.getColor(this, R.color.calendar_orange)
            "red", "high" -> ContextCompat.getColor(this, R.color.calendar_red)
            else -> ContextCompat.getColor(this, R.color.gray_200)
        }
    }

    private fun formatHours(value: Double?): String {
        if (value == null || value <= 0.0) return "0 jam"
        return "${value.toInt()} jam"
    }

    private fun moodEmoji(mood: String): String {
        return when (mood.lowercase(Locale.ROOT)) {
            "happy" -> "\ue7f2" // mood
            "neutral" -> "\ue811" // sentiment_neutral
            "sad" -> "\ue814" // sentiment_very_dissatisfied
            "anxious" -> "\ue812" // sentiment_dissatisfied
            "angry" -> "\ue813" // sentiment_very_dissatisfied (angry variation)
            else -> "\ue811"
        }
    }

    private fun getMoodUnicode(mood: String): String {
        return when (mood.lowercase(Locale.ROOT)) {
            "happy" -> "\uE7F2"
            "neutral" -> "\uE811"
            "sad" -> "\uE814"
            "anxious" -> "\uE812"
            "angry" -> "\uE813"
            else -> "\uE811"
        }
    }

    private fun String.toDisplayLabel(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    private fun Calendar.cloneCalendar(): Calendar = clone() as Calendar

    private data class CalendarDayCell(
        val date: Calendar?,
        val dateKey: String?,
        val day: Int?,
        val isToday: Boolean,
        val isSelected: Boolean,
        val summary: CalendarSummaryItemDto?
    ) {
        companion object {
            fun blank() = CalendarDayCell(null, null, null, false, false, null)
        }
    }

    private class CalendarDayAdapter(
        private val colorForIndicator: (String?) -> Int,
        private val onClick: (CalendarDayCell) -> Unit
    ) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

        private val items = mutableListOf<CalendarDayCell>()

        fun submit(newItems: List<CalendarDayCell>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DayViewHolder(binding, colorForIndicator, onClick)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) = holder.bind(items[position])
        override fun getItemCount(): Int = items.size

        class DayViewHolder(
            private val binding: ItemCalendarDayBinding,
            private val colorForIndicator: (String?) -> Int,
            private val onClick: (CalendarDayCell) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cell: CalendarDayCell) {
                if (cell.day == null) {
                    binding.root.visibility = View.INVISIBLE
                    return
                }

                binding.root.visibility = View.VISIBLE
                binding.dayNumberText.text = cell.day.toString()
                binding.dayCard.setOnClickListener { onClick(cell) }

                val indicatorColor = colorForIndicator(cell.summary?.indicator)
                binding.indicatorDot.isVisible = !cell.summary?.indicator.isNullOrBlank()
                if (binding.indicatorDot.isVisible) {
                    binding.indicatorDot.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(indicatorColor)
                    }
                }

                binding.moodIconText.isVisible = !cell.summary?.mood.isNullOrBlank()
                binding.sleepIconText.isVisible = cell.summary?.has_sleep_data == true
                binding.diaryIconText.isVisible = cell.summary?.has_diary == true

                binding.dayCard.strokeColor = if (cell.isSelected) indicatorColor else Color.TRANSPARENT
                binding.dayCard.strokeWidth = if (cell.isSelected) 4 else 0
                binding.dayNumberText.setTypeface(null, if (cell.isToday) Typeface.BOLD else Typeface.NORMAL)
            }
        }
    }
}
