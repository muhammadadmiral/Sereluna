package com.android.capstone.sereluna.ui.diary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.adapter.ChatAdapter
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.ActivityChatbotBinding
import com.android.capstone.sereluna.ui.viewmodel.ChatViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private val repository = SerelunaRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        setupRecyclerView()
        setupListeners()
        setupObservers()
        loadProfileData()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun setupListeners() {
        binding.submitFab.setOnClickListener {
            val userMessage = binding.diaryEditText.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                viewModel.sendMessage(userMessage)
                binding.diaryEditText.text?.clear()
            } else {
                Toast.makeText(this, "Tulis sesuatu dulu ya...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.backButton.setOnClickListener {
            showExitConfirmationDialog()
        }

        binding.btnFinish.setOnClickListener {
            showExitConfirmationDialog()
        }
        binding.btnSummarize.visibility = View.GONE
    }

    private fun showExitConfirmationDialog() {
        val messages = viewModel.chatMessages.value
        if (messages.isNullOrEmpty()) {
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Akhiri Sesi Percakapan?")
            .setMessage("Apakah kamu yakin ingin keluar? Sesi ini akan ditutup secara permanen dan dirangkum ke dalam Jurnal. Sesi yang sudah ditutup tidak dapat dilanjutkan kembali.")
            .setPositiveButton("Akhiri & Simpan") { dialog, _ ->
                Toast.makeText(this, "Menyimpan dan meringkas sesi...", Toast.LENGTH_SHORT).show()
                viewModel.finishSession()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupObservers() {
        viewModel.chatMessages.observe(this) { messages ->
            chatAdapter.updateMessages(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerView.smoothScrollToPosition(messages.size - 1)
            }
        }

        viewModel.isThinking.observe(this) { isThinking ->
            binding.tvBotStatus.text = if (isThinking) "Typing..." else "Online"
            binding.tvBotStatus.setTextColor(
                if (isThinking) 
                    ContextCompat.getColor(this, R.color.brand_purple_primary) 
                else 
                    ContextCompat.getColor(this, R.color.calendar_green)
            )
            updateInputLockState()
        }

        viewModel.errorState.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.sessionClosed.observe(this) { closed ->
            if (closed) {
                Toast.makeText(this, "Sesi berhasil disimpan ke jurnal.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private var isRenderingInProgress = false

    private fun updateInputLockState() {
        val isThinking = viewModel.isThinking.value ?: false
        val isLocked = isThinking || isRenderingInProgress
        
        binding.submitFab.isEnabled = !isLocked
        binding.diaryEditText.isEnabled = !isLocked
        binding.submitFab.alpha = if (isLocked) 0.5f else 1.0f
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(emptyList()) { isRendering ->
            isRenderingInProgress = isRendering
            updateInputLockState()
            if (!isRendering) {
                binding.recyclerView.post {
                    if (chatAdapter.itemCount > 0) {
                        binding.recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                    }
                }
            }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            try {
                val photoUrl = repository.getProfile().photo_url
                if (photoUrl.isNotEmpty()) {
                    Picasso.get().load(photoUrl).into(binding.profileImageDiary)
                }
            } catch (_: Exception) {
            }
        }
    }
}
