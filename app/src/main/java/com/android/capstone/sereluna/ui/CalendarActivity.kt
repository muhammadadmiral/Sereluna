package com.android.capstone.sereluna.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
    private val readableDateFormat = SimpleDateFormat("dd MMMM yyyy", localeId)
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
        binding.addMoodButton.setOnClickListener { showMoodSheet() }
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
        binding.selectedDateText.text = "Dipilih: ${readableDateFormat.format(selectedDate.time)}"
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
                binding.errorText.text = "Gagal memuat kalender. Coba cek koneksi atau login ulang."
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
        if (dateKey.isNullOrBlank()) return
        lifecycleScope.launch {
            try {
                val detail = repository.getCalendarDetail(dateKey)
                showDayDetail(detail)
            } catch (e: Exception) {
                Toast.makeText(
                    this@CalendarActivity,
                    "Detail tanggal belum bisa dimuat.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showMoodSheet() {
        val dialog = BottomSheetDialog(this)
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
                    Toast.makeText(
                        this@CalendarActivity,
                        "Gagal menyimpan mood.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialog.setContentView(moodBinding.root)
        dialog.show()
    }

    private fun showDayDetail(detail: CalendarDetailDto) {
        val dialog = BottomSheetDialog(this)
        val detailBinding = DialogCalendarDetailBinding.inflate(layoutInflater)
        val dateText = detail.date.takeIf { it.isNotBlank() } ?: selectedDateKey()
        val summary = summaryByDate[dateText]
        val indicator = summary?.indicator ?: indicatorFromLevel(detail.wellbeing.level)
        val indicatorColor = indicatorColor(indicator)

        detailBinding.detailDateText.text = formatDateKey(dateText)
        detailBinding.wellbeingScoreChip.text = detail.wellbeing.score?.let { "Score $it" } ?: "Score --"
        detailBinding.wellbeingScoreChip.chipStrokeColor = ColorStateList.valueOf(indicatorColor)
        detailBinding.wellbeingScoreChip.chipBackgroundColor = ColorStateList.valueOf(
            withAlpha(indicatorColor, 26)
        )
        detailBinding.wellbeingScoreChip.setTextColor(indicatorColor)
        detailBinding.wellbeingLevelText.text = detail.wellbeing.level?.toDisplayLabel()
            ?: summary?.wellbeing_level?.toDisplayLabel()
            ?: "Belum ada level wellbeing"

        detailBinding.detailMoodText.text = detail.mood?.let {
            "${moodEmoji(it)} ${it.toDisplayLabel()}"
        } ?: "Belum ada mood"

        detailBinding.detailBedtimeText.text = "Tidur: ${formatIsoTime(detail.sleep.bedtime)}"
        detailBinding.detailWakeupText.text = "Bangun: ${formatIsoTime(detail.sleep.wakeup)}"
        detailBinding.detailSleepTotalText.text = "Total: ${formatHours(detail.sleep.total_sleep_hours)}"
        detailBinding.detailSleepQualityText.text = "Kualitas: ${
            detail.sleep.sleep_quality?.toDisplayLabel() ?: "Belum ada data"
        }"

        detailBinding.detailDiaryText.text = detail.diary_snippet?.takeIf { it.isNotBlank() }
            ?: "Belum ada diary untuk tanggal ini."

        detailBinding.detailRecommendationText.text = detail.wellbeing.recommendation
            ?: "Belum ada rekomendasi khusus untuk tanggal ini."
        bindSignals(detailBinding, detail.wellbeing.signals)

        dialog.setContentView(detailBinding.root)
        dialog.show()
    }

    private fun bindSignals(binding: DialogCalendarDetailBinding, signals: List<String>) {
        binding.signalsContainer.removeAllViews()
        val signalItems = signals.ifEmpty { listOf("Belum ada sinyal spesifik.") }
        signalItems.forEach { signal ->
            val textView = TextView(this).apply {
                text = "- $signal"
                setTextColor(ContextCompat.getColor(this@CalendarActivity, R.color.text_secondary_dark))
                textSize = 14f
                typeface = ResourcesCompat.getFont(this@CalendarActivity, R.font.raleway_medium)
                setLineSpacing(2f, 1f)
                setPadding(0, 4.dp(), 0, 0)
            }
            binding.signalsContainer.addView(textView)
        }
    }

    private fun indicatorColor(indicator: String?): Int {
        return when (indicator?.lowercase(Locale.ROOT)) {
            "green", "stable" -> ContextCompat.getColor(this, R.color.calendar_green)
            "yellow", "watch", "monitor" -> ContextCompat.getColor(this, R.color.calendar_yellow)
            "orange", "attention" -> ContextCompat.getColor(this, R.color.calendar_orange)
            "red", "high" -> ContextCompat.getColor(this, R.color.calendar_red)
            else -> ContextCompat.getColor(this, R.color.gray_200)
        }
    }

    private fun indicatorFromLevel(level: String?): String? {
        return when (level?.lowercase(Locale.ROOT)) {
            "stable" -> "green"
            "monitor", "watch", "perlu dipantau" -> "yellow"
            "attention", "perhatian" -> "orange"
            "high", "critical", "butuh perhatian tinggi" -> "red"
            else -> null
        }
    }

    private fun formatIsoTime(value: String?): String {
        if (value.isNullOrBlank()) return "--"
        isoParsers.forEach { parser ->
            val parsed = runCatching { parser.parse(value) }.getOrNull()
            if (parsed != null) return displayTimeFormat.format(parsed)
        }
        return value
    }

    private fun formatDateKey(value: String): String {
        val parsed = runCatching { dateKeyFormat.parse(value) }.getOrNull()
        return parsed?.let { readableDateFormat.format(it) } ?: value
    }

    private fun formatHours(value: Double?): String {
        if (value == null || value <= 0.0) return "--"
        val formatted = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
        return "$formatted jam"
    }

    private fun moodEmoji(mood: String): String {
        return when (mood.lowercase(Locale.ROOT)) {
            "happy" -> "\uD83D\uDE0A"
            "calm" -> "\uD83D\uDE0C"
            "sad" -> "\uD83D\uDE14"
            "anxious" -> "\uD83D\uDE1F"
            "angry" -> "\uD83D\uDE20"
            "tired" -> "\uD83D\uDE34"
            else -> "\uD83D\uDE10"
        }
    }

    private fun String.toDisplayLabel(): String {
        return split("_", "-", " ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                val lower = word.lowercase(Locale.ROOT)
                lower.replaceFirstChar { it.uppercase(Locale.ROOT) }
            }
    }

    private fun Calendar.cloneCalendar(): Calendar = clone() as Calendar

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private data class CalendarDayCell(
        val date: Calendar?,
        val dateKey: String?,
        val day: Int?,
        val isToday: Boolean,
        val isSelected: Boolean,
        val summary: CalendarSummaryItemDto?
    ) {
        companion object {
            fun blank() = CalendarDayCell(
                date = null,
                dateKey = null,
                day = null,
                isToday = false,
                isSelected = false,
                summary = null
            )
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
            val binding = ItemCalendarDayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return DayViewHolder(binding, colorForIndicator, onClick)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class DayViewHolder(
            private val binding: ItemCalendarDayBinding,
            private val colorForIndicator: (String?) -> Int,
            private val onClick: (CalendarDayCell) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(cell: CalendarDayCell) {
                if (cell.day == null) {
                    binding.root.visibility = View.INVISIBLE
                    binding.dayCard.setOnClickListener(null)
                    return
                }

                binding.root.visibility = View.VISIBLE
                binding.dayNumberText.text = cell.day.toString()
                binding.dayCard.setOnClickListener { onClick(cell) }
                binding.dayCard.contentDescription = "Tanggal ${cell.day}"

                val indicator = cell.summary?.indicator
                val indicatorColor = colorForIndicator(indicator)
                val hasIndicator = !indicator.isNullOrBlank()

                binding.indicatorDot.isVisible = hasIndicator
                if (hasIndicator) {
                    binding.indicatorDot.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(indicatorColor)
                    }
                }

                binding.moodIconText.isVisible = !cell.summary?.mood.isNullOrBlank()
                binding.sleepIconText.isVisible = cell.summary?.has_sleep_data == true
                binding.diaryIconText.isVisible = cell.summary?.has_diary == true

                val strokeColor = when {
                    hasIndicator -> indicatorColor
                    cell.isToday -> colorForIndicator("stable")
                    else -> Color.TRANSPARENT
                }
                binding.dayCard.strokeColor = strokeColor
                binding.dayCard.strokeWidth = if (cell.isSelected || hasIndicator) 2.dp(itemView) else 1.dp(itemView)
                binding.dayCard.setCardBackgroundColor(
                    if (cell.isSelected) withAlpha(strokeColor, 22) else Color.TRANSPARENT
                )
                binding.dayNumberText.setTypeface(null, if (cell.isSelected || cell.isToday) Typeface.BOLD else Typeface.NORMAL)
            }

            private fun Int.dp(view: View): Int = (this * view.resources.displayMetrics.density).toInt()

            private fun withAlpha(color: Int, alpha: Int): Int {
                return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
            }
        }
    }
}
