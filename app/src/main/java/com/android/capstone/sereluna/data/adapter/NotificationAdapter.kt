package com.android.capstone.sereluna.data.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.Notification
import com.android.capstone.sereluna.data.model.NotificationStatus
import com.android.capstone.sereluna.data.model.getNotifStatus
import com.android.capstone.sereluna.databinding.NotificationItemListBinding

class NotificationAdapter: ListAdapter<Notification, NotificationAdapter.ViewHolder>(DiffCallback){


    class ViewHolder(private val binding: NotificationItemListBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(notification: Notification) {
            // Set the title & description text
            binding.tvNotifTitle.text = notification.title
            binding.tvNotifDescription.text = notification.body

            val resources = itemView.resources

            val colorDrawable = when (getNotifStatus(notification.notifStatus)) {
                is  NotificationStatus.Ordered -> {
                    ColorDrawable(resources.getColor(R.color.notif_purple))
                }
                is  NotificationStatus.Confirmed -> {
                    ColorDrawable(resources.getColor(R.color.notif_purple))
                }
                is  NotificationStatus.Delivered -> {
                    ColorDrawable(resources.getColor(R.color.notif_blue))
                }
                is  NotificationStatus.Shipped -> {
                    ColorDrawable(resources.getColor(R.color.notif_blue))
                }
                is  NotificationStatus.Canceled -> {
                    ColorDrawable(resources.getColor(R.color.notif_yellow))
                }
                is  NotificationStatus.Returned -> {
                    ColorDrawable(resources.getColor(R.color.notif_yellow))
                }
            }

            binding.ivNotifImage.setImageDrawable(colorDrawable)

            binding.root.setOnClickListener {
                // Uncomment the code below and implement the intent to navigate to DetailAcneActivity
                // val intent = Intent(binding.root.context, DetailAcneActivity::class.java).apply {
                //     putExtra(DetailAcneActivity.EXTRA_STORY_ITEM, history)
                // }
                // binding.root.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int
    ): ViewHolder {
        val binding = NotificationItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DiffCallback: DiffUtil.ItemCallback<Notification> =
            object : DiffUtil.ItemCallback<Notification>() {


                override fun areItemsTheSame(oldItem: Notification, storyItem: Notification): Boolean {
                    return oldItem.id == storyItem.id
                }


                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: Notification, storyItem: Notification): Boolean {
                    return oldItem == storyItem
                }
            }
    }
}