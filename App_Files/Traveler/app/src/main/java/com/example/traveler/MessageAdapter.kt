package com.example.traveler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<MessageAdapter.MyViewHolder>() {

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftChatView: LinearLayout = itemView.findViewById(R.id.left_chat_view)
        val rightChatView: LinearLayout = itemView.findViewById(R.id.right_chat_view)
        val leftTextView: TextView = itemView.findViewById(R.id.left_chat_text_view)
        val rightTextView: TextView = itemView.findViewById(R.id.right_chat_text_view)

        fun bind(message: Message) {
            if (message.sentBy == Message.SENT_BY_ME) {
                leftChatView.visibility = View.GONE
                rightChatView.visibility = View.VISIBLE
                rightTextView.text = message.message
            } else {
                rightChatView.visibility = View.GONE
                leftChatView.visibility = View.VISIBLE
                leftTextView.text = message.message
            }
        }
    }

    @Override
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val chatView = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return MyViewHolder(chatView)
    }

    @Override
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    @Override
    override fun getItemCount(): Int {
        return messageList.size
    }
}