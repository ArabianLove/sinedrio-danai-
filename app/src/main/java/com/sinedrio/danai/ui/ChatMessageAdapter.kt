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

        /** Colour palette per agent id — lighter tones for readability on dark background. */
        private val AGENT_COLORS = mapOf(
            "detective" to Color.parseColor("#7986CB"),   // indigo 300
            "visionary" to Color.parseColor("#BA68C8"),   // purple 300
            "engineer" to Color.parseColor("#66BB6A"),    // green 400
            "sage" to Color.parseColor("#FFB74D"),        // orange 300
            "auto_moderator" to Color.parseColor("#D4AF37") // gold
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
            VIEW_TYPE_MODERATOR -> {
                val binding = ItemChatMessageAgentBinding.inflate(inflater, parent, false)
                // Apply centred margins once at creation time for the auto-moderator style
                val density = parent.context.resources.displayMetrics.density
                val margin48 = (48 * density).toInt()
                val params = binding.root.layoutParams as? ViewGroup.MarginLayoutParams
                params?.marginStart = margin48
                params?.marginEnd = margin48
                binding.root.layoutParams = params
                AgentViewHolder(binding)
            }
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
            binding.textContent.text = msg.content
            binding.textTime.text = TIME_FORMAT.format(Date(msg.timestamp))

            if (msg.isAutoModerator) {
                binding.textPersona.visibility = View.GONE
            } else {
                binding.textPersona.text = msg.senderPersona
                binding.textPersona.visibility = View.VISIBLE
            }
        }
    }
}
