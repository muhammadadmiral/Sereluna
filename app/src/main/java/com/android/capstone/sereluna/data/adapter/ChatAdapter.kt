package com.android.capstone.sereluna.data.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.Chat
import de.hdodenhof.circleimageview.CircleImageView

class ChatAdapter(private val chatList: List<Chat>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private val MESSAGE_TYPE_USER = 0
    private val MESSAGE_TYPE_BOT = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == MESSAGE_TYPE_BOT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bot, parent, false)
            ViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chatList[position]
        holder.txtMessage.text = chat.message
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        return if (chat.isBot) {
            MESSAGE_TYPE_BOT
        } else {
            MESSAGE_TYPE_USER
        }
    }
}
