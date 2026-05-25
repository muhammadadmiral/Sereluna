package com.android.capstone.sereluna.ui.gamification

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityGamificationBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GamificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGamificationBinding
    private val repository = SerelunaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGamificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        startAuraAnimation()
        loadGamificationData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish() // Return to MainActivity
        }

        binding.btnOracle.setOnClickListener {
            binding.btnOracle.isEnabled = false
            Snackbar.make(binding.root, "Membuka koneksi ke Moon Oracle...", Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#FFD700"))
                .setTextColor(Color.parseColor("#150926"))
                .show()

            lifecycleScope.launch {
                try {
                    val response = repository.getOracleReading()
                    MaterialAlertDialogBuilder(this@GamificationActivity)
                        .setTitle("Pesan dari Moon Oracle")
                        .setMessage(response.reading)
                        .setPositiveButton("Tutup", null)
                        .show()
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Gagal terhubung ke Moon Oracle", Snackbar.LENGTH_LONG).show()
                } finally {
                    binding.btnOracle.isEnabled = true
                }
            }
        }
    }

    private fun startAuraAnimation() {
        // Create a pulsing effect for the aura glow
        val scaleDownX = ObjectAnimator.ofFloat(binding.vAuraGlow, "scaleX", 1.2f, 1.4f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.vAuraGlow, "scaleY", 1.2f, 1.4f)
        val alpha = ObjectAnimator.ofFloat(binding.vAuraGlow, "alpha", 0.3f, 0.1f)

        scaleDownX.repeatCount = ObjectAnimator.INFINITE
        scaleDownX.repeatMode = ObjectAnimator.REVERSE
        scaleDownX.duration = 2000
        scaleDownX.interpolator = AccelerateDecelerateInterpolator()

        scaleDownY.repeatCount = ObjectAnimator.INFINITE
        scaleDownY.repeatMode = ObjectAnimator.REVERSE
        scaleDownY.duration = 2000
        scaleDownY.interpolator = AccelerateDecelerateInterpolator()

        alpha.repeatCount = ObjectAnimator.INFINITE
        alpha.repeatMode = ObjectAnimator.REVERSE
        alpha.duration = 2000
        alpha.interpolator = AccelerateDecelerateInterpolator()

        scaleDownX.start()
        scaleDownY.start()
        alpha.start()
    }

    private fun loadGamificationData() {
        lifecycleScope.launch {
            try {
                val dto = repository.getGamificationPlayerCard()

                binding.tvRankTitle.text = dto.tier_name.uppercase()
                binding.tvEquippedTitle.text = dto.equipped_title ?: "Novice Explorer"
                binding.tvLevel.text = (dto.current_xp / 100).toString() // Simplified level calculation
                
                val nextXp = dto.next_tier_xp
                binding.tvXp.text = "${dto.current_xp} / $nextXp XP"
                
                val progress = if (nextXp > 0) ((dto.current_xp.toFloat() / nextXp.toFloat()) * 100).toInt() else 100
                binding.pbLevelProgress.progress = progress
                
                binding.tvStreak.text = dto.streak.toString()
                binding.tvStardust.text = dto.stardust.toString()
                binding.tvShields.text = dto.eclipse_shields_active.toString()

                // Apply dynamic color mapping
                try {
                    val color = Color.parseColor(dto.tier_color)
                    binding.tvRankTitle.setTextColor(color)
                    binding.tvLevel.setTextColor(color)
                    binding.pbLevelProgress.progressTintList = ColorStateList.valueOf(color)
                    binding.vAuraGlow.backgroundTintList = ColorStateList.valueOf(color)
                    binding.vAuraInnerGlow.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {}

                // Fetch and render quests
                val questsDto = repository.getGamificationQuests()
                binding.questsContainer.removeAllViews()

                if (questsDto.daily.isEmpty()) {
                    val emptyText = android.widget.TextView(this@GamificationActivity).apply {
                        text = "Kamu sudah menyelesaikan semua misi hari ini! 🎉"
                        setTextColor(Color.WHITE)
                        alpha = 0.7f
                        gravity = android.view.Gravity.CENTER
                        setPadding(0, 16, 0, 16)
                    }
                    binding.questsContainer.addView(emptyText)
                } else {
                    questsDto.daily.forEach { quest ->
                        val view = layoutInflater.inflate(R.layout.item_quest, binding.questsContainer, false)
                        val tvDesc = view.findViewById<android.widget.TextView>(R.id.tvQuestDesc)
                        val tvReward = view.findViewById<android.widget.TextView>(R.id.tvQuestReward)
                        val pbProgress = view.findViewById<android.widget.ProgressBar>(R.id.pbQuestProgress)

                        tvDesc.text = quest.desc
                        tvReward.text = "+${quest.reward_stardust}"
                        
                        pbProgress.max = quest.target
                        pbProgress.progress = quest.progress

                        binding.questsContainer.addView(view)
                    }
                }

            } catch (e: Exception) {
                // Fallback to "Gacor" UI state since API is still being built
                val tierName = "CELESTIAL GUARDIAN"
                val tierColorHex = "#FFD700" // Gold
                val level = 45
                val currentXp = 4500
                val nextXp = 5000
                val stardust = "1,250"
                val streak = "14"
                val shields = "1"

                binding.tvRankTitle.text = tierName
                binding.tvEquippedTitle.text = "Master of the Moon"
                binding.tvLevel.text = level.toString()
                binding.tvXp.text = "$currentXp / $nextXp XP"
                binding.tvStardust.text = stardust
                binding.tvStreak.text = streak
                binding.tvShields.text = shields

                val progress = ((currentXp.toFloat() / nextXp.toFloat()) * 100).toInt()
                binding.pbLevelProgress.progress = progress

                try {
                    val color = Color.parseColor(tierColorHex)
                    binding.tvRankTitle.setTextColor(color)
                    binding.tvLevel.setTextColor(color)
                    binding.pbLevelProgress.progressTintList = ColorStateList.valueOf(color)
                    binding.vAuraGlow.backgroundTintList = ColorStateList.valueOf(color)
                    binding.vAuraInnerGlow.backgroundTintList = ColorStateList.valueOf(color)
                } catch (_: Exception) {}

                // Fallback quests
                binding.questsContainer.removeAllViews()
                val dummyQuests = listOf(
                    com.android.capstone.sereluna.data.api.GamificationQuestDto("1", "Tulis 200 kata di Diary", 100, 200, 50),
                    com.android.capstone.sereluna.data.api.GamificationQuestDto("2", "Lakukan Skrining DASS-21", 0, 1, 100)
                )
                dummyQuests.forEach { quest ->
                    val view = layoutInflater.inflate(R.layout.item_quest, binding.questsContainer, false)
                    val tvDesc = view.findViewById<android.widget.TextView>(R.id.tvQuestDesc)
                    val tvReward = view.findViewById<android.widget.TextView>(R.id.tvQuestReward)
                    val pbProgress = view.findViewById<android.widget.ProgressBar>(R.id.pbQuestProgress)

                    tvDesc.text = quest.desc
                    tvReward.text = "+${quest.reward_stardust}"
                    
                    pbProgress.max = quest.target
                    pbProgress.progress = quest.progress

                    binding.questsContainer.addView(view)
                }
            }
        }
    }
}
