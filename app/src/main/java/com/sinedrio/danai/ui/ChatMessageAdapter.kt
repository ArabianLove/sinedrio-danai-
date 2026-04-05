package com.sinedrio.danai.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sinedrio.danai.databinding.ItemChatMessageAgentBinding
import com.sinedrio.danai.databinding.ItemChatMessageHumanBinding
import com.sinedrio.danai.senate.chat.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for [ChatMessage]s in the Sinedrio chat room.
 *
 * Three visual styles are used:
 * - **Human** messages: right-aligned blue bubble.
 * - **Auto-Moderator** messages: centered neutral card with question icon.
 * - **Agent** messages: left-aligned card, colour-coded by agent persona.
 */
class ChatMessageAdapter :
    ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_HUMAN = 0
        private const val VIEW_TYPE_AGENT = 1
        private const val VIEW_TYPE_MODERATOR = 2

        private val TIME_FORMAT = SimpleDateFormat("HH:mm", Locale.getDefault())

        /** Colour palette per agent id. */
        private val AGENT_COLORS = mapOf(
            "detective" to Color.parseColor("#1A237E"),   // deep indigo
            "visionary" to Color.parseColor("#4A148C"),   // deep purple
            "engineer" to Color.parseColor("#1B5E20"),    // deep green
            "sage" to Color.parseColor("#E65100"),        // deep amber
            "auto_moderator" to Color.parseColor("#37474F") // blue-grey
        )

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
            override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        return when {
            msg.isHuman -> VIEW_TYPE_HUMAN
            msg.isAutoModerator -> VIEW_TYPE_MODERATOR
            else -> VIEW_TYPE_AGENT
        }
    }

    // ── ViewHolder creation ────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HUMAN -> HumanViewHolder(
                ItemChatMessageHumanBinding.inflate(inflater, parent, false)
            )
            else -> AgentViewHolder(
                ItemChatMessageAgentBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is HumanViewHolder -> holder.bind(msg)
            is AgentViewHolder -> holder.bind(msg)
        }
    }

    // ── ViewHolders ────────────────────────────────────────────────────────────

    inner class HumanViewHolder(private val binding: ItemChatMessageHumanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            binding.textContent.text = msg.content
            binding.textTime.text = TIME_FORMAT.format(Date(msg.timestamp))
            binding.textSenderName.text = msg.senderName
        }
    }

    inner class AgentViewHolder(private val binding: ItemChatMessageAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(msg: ChatMessage) {
            val color = AGENT_COLORS[msg.senderId] ?: Color.parseColor("#455A64")

            binding.viewAccent.setBackgroundColor(color)
            binding.textSenderName.text = msg.senderName
            binding.textSenderName.setTextColor(color)
            binding.textPersona.text = msg.senderPersona
            binding.textContent.text = msg.content
            binding.textTime.text = TIME_FORMAT.format(Date(msg.timestamp))

            if (msg.isAutoModerator) {
                binding.textPersona.visibility = View.GONE
                // Centre auto-moderator cards
                val params = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
                params?.marginStart = 48
                params?.marginEnd = 48
                binding.root.layoutParams = params
            } else {
                binding.textPersona.visibility = View.VISIBLE
            }
        }
    }
}
