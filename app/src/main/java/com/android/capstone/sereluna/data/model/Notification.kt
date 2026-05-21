package com.android.capstone.sereluna.data.model

import androidx.recyclerview.widget.DiffUtil
import java.util.Date

data class Notification(
    val id: String,
    val title: String,
    val body: String,
    val notifStatus: String = "",
    val isRead: Boolean = false,
    val actionLink: String? = null,
    val createdAt: Date? = null
) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Notification>() {
            override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
                return oldItem == newItem
            }
        }
    }
}
