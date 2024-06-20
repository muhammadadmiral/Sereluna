package com.android.capstone.sereluna.data.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.data.model.getNotifStatus
import com.android.capstone.sereluna.data.utils.getTimeAgoFormat
import com.android.capstone.sereluna.databinding.ItemDiaryListBinding
import com.android.capstone.sereluna.databinding.NotificationItemListBinding

class DiaryAdapter: ListAdapter<Diary,DiaryAdapter.ViewHolder>(DIFF_CALLBACK) {


    class ViewHolder(private val binding: ItemDiaryListBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(diary: Diary) {
            // Set the title & description text
            binding.tvDiaryDate.text = diary.date.getTimeAgoFormat()
            binding.tvDiaryDescription.text = diary.content


            binding.root.setOnClickListener {
                // Uncomment the code below and implement the intent to navigate to DetailAcneActivity
                // val intent = Intent(binding.root.context, DetailAcneActivity::class.java).apply {
                //     putExtra(DetailAcneActivity.EXTRA_STORY_ITEM, history)
                // }
                // binding.root.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiaryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Diary> =
            object : DiffUtil.ItemCallback<Diary>() {


                override fun areItemsTheSame(oldItem: Diary, storyItem: Diary): Boolean {
                    return oldItem.id == storyItem.id
                }


                @SuppressLint("DiffUtilEquals")
                override fun areContentsTheSame(oldItem: Diary, storyItem: Diary): Boolean {
                    return oldItem == storyItem
                }
            }
    }

}