package com.android.capstone.sereluna.data.adapter

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.capstone.sereluna.R
import com.android.capstone.sereluna.data.model.Chat
import com.android.capstone.sereluna.ui.diary.TypingIndicatorDrawable

class ChatAdapter(private var chatList: List<Chat>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MESSAGE_TYPE_USER = 0
    private val MESSAGE_TYPE_BOT = 1
    private val animatedMessages = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == MESSAGE_TYPE_BOT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bot, parent, false)
            BotViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]
        if (holder is BotViewHolder) {
            val messageText = chat.message
            val statusText = chat.status

            if (messageText == "Typing...") {
                holder.txtMessage.text = ""
                holder.txtMessage.setCompoundDrawablesWithIntrinsicBounds(
                    TypingIndicatorDrawable(holder.txtMessage.currentTextColor), null, null, null
                )
                
                // Show thinking status if available
                if (!statusText.isNullOrBlank()) {
                    if (holder.txtStatus.text != statusText) {
                        if (holder.txtStatus.visibility == View.VISIBLE) {
                            // Smooth transition between different status messages
                            holder.txtStatus.animate().alpha(0f).setDuration(200).withEndAction {
                                holder.txtStatus.text = statusText
                                holder.txtStatus.animate().alpha(1f).setDuration(200).start()
                            }.start()
                        } else {
                            // First time showing status
                            holder.txtStatus.text = statusText
                            holder.txtStatus.visibility = View.VISIBLE
                            holder.txtStatus.alpha = 0f
                            holder.txtStatus.animate().alpha(1f).setDuration(500).start()
                        }
                    }
                } else {
                    holder.txtStatus.visibility = View.GONE
                    holder.txtStatus.text = ""
                }
            } else {
                holder.txtMessage.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                holder.txtStatus.visibility = View.GONE
                
                val messageHash = chat.hashCode()
                if (!animatedMessages.contains(messageHash)) {
                    animatedMessages.add(messageHash)
                    animateTextTypewriter(holder.txtMessage, messageText)
                } else {
                    holder.txtMessage.text = messageText
                }
            }
        } else if (holder is UserViewHolder) {
            holder.txtMessage.text = chat.message
        }
    }

    private fun animateTextTypewriter(textView: TextView, text: String) {
        val handler = Handler(Looper.getMainLooper())
        var i = 0
        textView.text = ""
        val runnable = object : Runnable {
            override fun run() {
                if (i <= text.length) {
                    // Type faster by taking 2-3 chars if it's long, or just lower the delay
                    textView.text = text.substring(0, i++)
                    handler.postDelayed(this, 10) // Ultra fast speed
                }
            }
        }
        handler.post(runnable)
    }

    override fun getItemViewType(position: Int): Int {
        val chat = chatList[position]
        return if (chat.isBot) {
            MESSAGE_TYPE_BOT
        } else {
            MESSAGE_TYPE_USER
        }
    }

    fun updateMessages(newMessages: List<Chat>) {
        val oldSize = this.chatList.size
        val newSize = newMessages.size
        
        // If only the last message (typing status) changed, notify only that item
        if (oldSize == newSize && oldSize > 0) {
            this.chatList = newMessages
            notifyItemChanged(oldSize - 1)
            return
        }

        this.chatList = newMessages
        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize)
        } else {
            notifyDataSetChanged()
        }
    }

    class BotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.tvMessage)
        val txtStatus: TextView = view.findViewById(R.id.tvStatus)
        init {
            view.animation = android.view.animation.AnimationUtils.loadAnimation(view.context, R.anim.item_animation_fall_down)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.tvMessage)
        init {
            view.animation = android.view.animation.AnimationUtils.loadAnimation(view.context, R.anim.item_animation_fall_down)
        }
    }
}
