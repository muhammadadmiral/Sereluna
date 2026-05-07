package com.android.capstone.sereluna.data.adapter

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.Notification
import com.android.capstone.sereluna.databinding.NotificationItemListBinding

class NotificationAdapter : ListAdapter<Notification, NotificationAdapter.ViewHolder>(Notification.DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = NotificationItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    inner class ViewHolder(private val binding: NotificationItemListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            binding.tvNotifTitle.text = notification.title
            binding.tvNotifDescription.text = notification.body

            val context = itemView.context
            val (colorRes, iconRes) = when (notification.notifStatus) {
                "profile" -> R.color.notif_blue to R.drawable.ic_account
                "screening" -> R.color.notif_purple to R.drawable.ic_notification
                "diary" -> R.color.notif_yellow to R.drawable.diary
                "reminder" -> R.color.notif_purple to R.drawable.ic_notification
                "Ordered" -> R.color.notif_purple to R.drawable.ic_ordered
                "Canceled" -> R.color.notif_purple to R.drawable.ic_cancel
                "Delivered" -> R.color.notif_blue to R.drawable.ic_delivered
                "Shipped" -> R.color.notif_blue to R.drawable.ic_shipped
                "Confirmed" -> R.color.notif_yellow to R.drawable.ic_confirmed
                "Returned" -> R.color.notif_yellow to R.drawable.ic_return
                else -> R.color.gray to R.drawable.ic_ordered
            }
            val color = ContextCompat.getColor(context, colorRes)
            binding.ivNotifImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.ivNotifImage.setImageResource(iconRes)
        }
    }
}
