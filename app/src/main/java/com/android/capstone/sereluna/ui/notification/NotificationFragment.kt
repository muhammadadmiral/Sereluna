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
    private var notifications: List<Notification> = emptyList()
    private var unreadCount: Int = 0

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
        binding.btnMarkAllRead.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    repository.markAllNotificationsRead()
                    notifications = notifications.map { it.copy(isRead = true) }
                    unreadCount = 0
                    notificationAdapter.submitList(notifications)
                    renderNotificationState()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal menandai notifikasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onNotificationClick(notification: Notification) {
        // 1. Mark as read in background
        if (!notification.isRead) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = repository.markNotificationRead(notification.id)
                    notifications = notifications.map {
                        if (it.id == notification.id) it.copy(isRead = true) else it
                    }
                    unreadCount = response.unread_count ?: notifications.count { !it.isRead }
                    notificationAdapter.submitList(notifications)
                    renderNotificationState()
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
                binding.notificationProgress.visibility = View.VISIBLE
                val response = repository.getNotificationsResponse()
                unreadCount = response.unread_count
                notifications = response.items.map { item ->
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
                renderNotificationState()
            } catch (e: Exception) {
                binding.notificationProgress.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat notifikasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderNotificationState() {
        binding.notificationProgress.visibility = View.GONE
        binding.tvUnreadBadge.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE
        binding.tvUnreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
        binding.btnMarkAllRead.visibility = if (unreadCount > 0) View.VISIBLE else View.GONE

                if (notifications.isEmpty()) {
                    binding.rvNotification.visibility = View.GONE
            binding.tvNotificationEmpty.visibility = View.VISIBLE
                } else {
                    binding.rvNotification.visibility = View.VISIBLE
            binding.tvNotificationEmpty.visibility = View.GONE
                }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
