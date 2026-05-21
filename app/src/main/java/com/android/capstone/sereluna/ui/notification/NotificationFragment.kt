package com.android.capstone.sereluna.ui.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.NotificationAdapter
import com.android.capstone.sereluna.data.model.Notification
import com.android.capstone.sereluna.data.repository.SerelunaRepository
import com.android.capstone.sereluna.databinding.FragmentNotificationBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class NotificationFragment: Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationAdapter: NotificationAdapter
    private val repository = SerelunaRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            onNotificationClick(notification)
        }
        binding.rvNotification.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = notificationAdapter
        }
    }

    private fun setupListeners() {
        // You could add a 'Mark all as read' button in the UI if needed
        // For now, let's assume there's a button or menu for it
    }

    private fun onNotificationClick(notification: Notification) {
        // 1. Mark as read in background
        if (!notification.isRead) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    repository.markNotificationRead(notification.id)
                    loadNotifications() // Refresh list
                } catch (_: Exception) {}
            }
        }

        // 2. Handle deep link (actionLink)
        notification.actionLink?.let { link ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            } catch (e: Exception) {
                // If it's an internal app link and fail, we can try to navigate manually
                handleInternalNavigation(link)
            }
        }
    }

    private fun handleInternalNavigation(link: String) {
        // Basic manual fallback if ACTION_VIEW fails for sereluna:// links
        Toast.makeText(requireContext(), "Membuka: $link", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notifications = repository.getNotifications().map { item ->
                    Notification(
                        id = item.id,
                        title = item.title,
                        body = item.body,
                        notifStatus = item.type,
                        isRead = item.is_read,
                        actionLink = item.action_link
                    )
                }
                notificationAdapter.submitList(notifications)
                
                if (notifications.isEmpty()) {
                    binding.rvNotification.visibility = View.GONE
                    // show empty state if you have one
                } else {
                    binding.rvNotification.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
