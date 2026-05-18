package com.android.capstone.sereluna.ui.notification

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
import kotlinx.coroutines.launch

class NotificationFragment: Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val notificationAdapter = NotificationAdapter()
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

        binding.rvNotification.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = notificationAdapter
        }

        loadNotifications()
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
                        notifStatus = item.type
                    )
                }
                notificationAdapter.submitList(notifications)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat notifikasi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
