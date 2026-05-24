package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.api.DoctorDto
import com.android.capstone.sereluna.databinding.ItemDoctorBinding
import com.squareup.picasso.Picasso

class DoctorAdapter(
    private var doctors: List<DoctorDto>,
    private val onWhatsAppClick: (DoctorDto) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(private val binding: ItemDoctorBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(doctor: DoctorDto) {
            binding.tvDoctorName.text = doctor.name
            binding.tvDoctorSpecialty.text = doctor.specialty

            if (!doctor.imageUrl.isNullOrEmpty()) {
                Picasso.get().load(doctor.imageUrl)
                    .placeholder(R.drawable.doctor_icon)
                    .error(R.drawable.doctor_icon)
                    .into(binding.ivDoctorAvatar)
            } else {
                binding.ivDoctorAvatar.setImageResource(R.drawable.doctor_icon)
            }

            binding.btnWhatsApp.setOnClickListener {
                onWhatsAppClick(doctor)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        holder.bind(doctors[position])
    }

    override fun getItemCount(): Int = doctors.size

    fun updateData(newDoctors: List<DoctorDto>) {
        doctors = newDoctors
        notifyDataSetChanged()
    }
}