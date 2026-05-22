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

class ChatAdapter(
    private var chatList: List<Chat>,
    private val onRenderingStateChanged: (Boolean) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val MESSAGE_TYPE_USER = 0
    private val MESSAGE_TYPE_BOT = 1
    private val animatedMessages = mutableSetOf<Int>()
    private var renderingCount = 0

    private fun setRendering(isRendering: Boolean) {
        if (isRendering) renderingCount++ else renderingCount--
        onRenderingStateChanged(renderingCount > 0)
    }

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
                
                if (!statusText.isNullOrBlank()) {
                    holder.txtStatus.text = statusText
                    holder.txtStatus.visibility = View.VISIBLE
                    holder.txtStatus.alpha = 1f
                } else {
                    holder.txtStatus.visibility = View.GONE
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
                    holder.txtMessage.alpha = 1f
                    holder.txtMessage.translationY = 0f
                }
            }
        } else if (holder is UserViewHolder) {
            if (chat.message.isNotEmpty()) {
                holder.txtMessage.text = chat.message
                holder.txtMessage.visibility = View.VISIBLE
            } else {
                holder.txtMessage.visibility = View.GONE
            }

            if (chat.imageUri != null) {
                holder.ivChatImage.visibility = View.VISIBLE
                holder.ivChatImage.setImageURI(chat.imageUri)
            } else {
                holder.ivChatImage.visibility = View.GONE
                holder.ivChatImage.setImageDrawable(null)
            }
        }
    }

    private fun animateTextTypewriter(textView: TextView, text: String) {
        val handler = Handler(Looper.getMainLooper())
        var i = 0
        textView.text = ""
        textView.alpha = 0f
        textView.translationY = 15f
        textView.animate().alpha(1f).translationY(0f).setDuration(300).start()
        
        setRendering(true)
        
        val runnable = object : Runnable {
            override fun run() {
                if (i <= text.length) {
                    textView.text = text.substring(0, i)
                    // Ultra fast typewriter: 4-6 chars per tick
                    val step = if (text.length > 200) 6 else 4
                    i += step
                    if (i > text.length) i = text.length
                    
                    if (i < text.length) {
                        handler.postDelayed(this, 1) // Minimal delay
                    } else {
                        textView.text = text
                        setRendering(false)
                    }
                } else {
                    setRendering(false)
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
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.tvMessage)
        val ivChatImage: android.widget.ImageView = view.findViewById(R.id.ivChatImage)
        init {
            view.alpha = 0f
            view.translationY = 20f
            view.animate().alpha(1f).translationY(0f).setDuration(400).setInterpolator(android.view.animation.OvershootInterpolator(1.2f)).start()
        }
    }
}
