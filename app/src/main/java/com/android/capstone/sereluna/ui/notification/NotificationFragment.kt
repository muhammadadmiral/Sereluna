package com.android.capstone.sereluna.ui.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.adapter.NotificationAdapter
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.data.model.Notification
import com.android.capstone.sereluna.databinding.FragmentNotificationBinding

class NotificationFragment: Fragment() {

    private lateinit var binding: FragmentNotificationBinding
    private val notificationAdapter = NotificationAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvNotification.adapter = notificationAdapter

        binding.rvNotification.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = notificationAdapter
        }

        notificationAdapter.submitList(getMockData())

    }

    private fun getMockData(): List<Notification> {
        return listOf(
            Notification("1", "Title", getString(R.string.lorem_ipsum_short),"Ordered" ),
            Notification("2", "Title", getString(R.string.lorem_ipsum_short), "Confirmed"),
            Notification("3", "Title", getString(R.string.lorem_ipsum_short),"Delivered" ),
            Notification("4", "Title", getString(R.string.lorem_ipsum_short), "Shipped"),
            Notification("5", "Title", getString(R.string.lorem_ipsum_short),"Canceled" ),
            Notification("6", "Title", getString(R.string.lorem_ipsum_short), "Returned"),
            Notification("7", "Title", getString(R.string.lorem_ipsum_short), "Ordered"),

            )
    }

}