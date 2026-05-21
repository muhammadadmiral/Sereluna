package com.android.capstone.sereluna.data.adapter

import android.graphics.PorterDuff
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.Notification
import com.android.capstone.sereluna.databinding.NotificationItemListBinding

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.ViewHolder>(Notification.DIFF_CALLBACK) {

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
            
            // Handle Read/Unread UI
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            if (notification.isRead) {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, if (isNight) R.color.dark_card else R.color.light_surface)
                )
                binding.tvNewBadge.visibility = android.view.View.GONE
                binding.root.alpha = 0.74f
            } else {
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(context, if (isNight) R.color.dark_surface else R.color.white)
                )
                binding.tvNewBadge.visibility = android.view.View.VISIBLE
                binding.root.alpha = 1.0f
            }

            val (colorRes, iconRes) = when (notification.notifStatus) {
                "profile" -> R.color.notif_blue to R.drawable.ic_account
                "screening" -> R.color.notif_purple to R.drawable.ic_notification
                "diary" -> R.color.notif_yellow to R.drawable.diary
                "reminder" -> R.color.notif_purple to R.drawable.ic_notification
                "system" -> R.color.brand_purple_primary to R.drawable.ic_stat_notification
                else -> R.color.gray_500 to R.drawable.ic_notification
            }
            
            val color = ContextCompat.getColor(context, colorRes)
            binding.ivNotifImage.setColorFilter(color, PorterDuff.Mode.SRC_IN)
            binding.ivNotifImage.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onItemClick(notification)
            }
        }
    }
}
