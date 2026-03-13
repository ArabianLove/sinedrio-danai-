package com.sinedrio.danai.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sinedrio.danai.databinding.ItemAgentResultBinding
import com.sinedrio.danai.senate.AgentResult

/**
 * Adapter that displays a list of [AgentResult]s from the Senate session.
 */
class AgentResultAdapter :
    ListAdapter<AgentResult, AgentResultAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemAgentResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(result: AgentResult) {
            binding.textAgentName.text = result.agentName
            binding.textOutput.text = result.output
            binding.iconStatus.setImageResource(
                if (result.success)
                    android.R.drawable.ic_menu_send
                else
                    android.R.drawable.ic_dialog_alert
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAgentResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AgentResult>() {
            override fun areItemsTheSame(old: AgentResult, new: AgentResult) =
                old.taskId == new.taskId && old.agentName == new.agentName

            override fun areContentsTheSame(old: AgentResult, new: AgentResult) = old == new
        }
    }
}
