package com.android.capstone.sereluna.data.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.data.model.Diary
import com.android.capstone.sereluna.databinding.ItemDiaryListBinding

class DiaryAdapter : ListAdapter<Diary, DiaryAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(private val binding: ItemDiaryListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(diary: Diary) {
            binding.tvDiaryDate.text = diary.date // Menampilkan tanggal diary
            binding.tvDiaryDescription.text = diary.content // Menampilkan konten diary

            // Menambahkan tindakan jika item diary di klik, disesuaikan sesuai kebutuhan
            binding.root.setOnClickListener {
                // Lakukan aksi, seperti membuka detail diary
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
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Diary> = object : DiffUtil.ItemCallback<Diary>() {
            override fun areItemsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                // Bandingkan ID untuk memastikan apakah item adalah yang sama
                return oldItem.id == newItem.id
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: Diary, newItem: Diary): Boolean {
                // Bandingkan semua properti untuk memastikan apakah kontennya sama
                return oldItem == newItem
            }
        }
    }
}
