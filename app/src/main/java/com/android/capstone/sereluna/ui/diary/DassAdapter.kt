package com.android.capstone.sereluna.ui.diary

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.databinding.ItemDassQuestionBinding

class DassAdapter(
    private val questions: List<DassQuestion>,
    private val onAnswerChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<DassAdapter.DassViewHolder>() {

    private val answers = IntArray(questions.size) { -1 }

    inner class DassViewHolder(val binding: ItemDassQuestionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DassViewHolder {
        val binding = ItemDassQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DassViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DassViewHolder, position: Int) {
        val question = questions[position]
        holder.binding.tvQuestion.text = "${question.number}. ${question.text}"

        val radioIds = listOf(
            holder.binding.rb0.id to 0,
            holder.binding.rb1.id to 1,
            holder.binding.rb2.id to 2,
            holder.binding.rb3.id to 3
        )

        holder.binding.rgAnswers.setOnCheckedChangeListener(null)
        val saved = answers[position]
        if (saved in 0..3) {
            val rb: RadioButton? = holder.binding.root.findViewById(
                radioIds.first { it.second == saved }.first
            )
            rb?.isChecked = true
        } else {
            holder.binding.rgAnswers.clearCheck()
        }

        holder.binding.rgAnswers.setOnCheckedChangeListener { _, checkedId ->
            val value = radioIds.firstOrNull { it.first == checkedId }?.second ?: -1
            answers[position] = value
            if (value >= 0) onAnswerChanged(position, value)
        }
    }

    override fun getItemCount(): Int = questions.size
}
