package com.android.capstone.sereluna.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.data.adapter.NotificationAdapter
import com.android.capstone.sereluna.data.repository.NotificationRepository
import com.android.capstone.sereluna.databinding.FragmentNotificationBinding
import com.google.firebase.firestore.ListenerRegistration

class NotificationFragment: Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private val notificationAdapter = NotificationAdapter()
    private val notificationRepository = NotificationRepository()
    private var listenerRegistration: ListenerRegistration? = null

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

        listenerRegistration = notificationRepository.observeNotifications(
            onChanged = { notifications ->
                notificationAdapter.submitList(notifications)
            },
            onError = { error ->
                Toast.makeText(requireContext(), "Gagal memuat notifikasi: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    override fun onDestroyView() {
        listenerRegistration?.remove()
        listenerRegistration = null
        _binding = null
        super.onDestroyView()
    }
}
