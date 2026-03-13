package com.sinedrio.danai.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.sinedrio.danai.databinding.ActivitySenateBinding

/**
 * Displays the list of [com.sinedrio.danai.senate.SenateAgent]s and the results
 * produced by the last Senate session.
 */
class SenateActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySenateBinding
    private val viewModel: SenateViewModel by viewModels()
    private val adapter = AgentResultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySenateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.recyclerResults.adapter = adapter

        // Show agent roster
        val rosterText = viewModel.agentRoster.joinToString(separator = "\n") { agent ->
            "• ${agent.name}: ${agent.description}"
        }
        binding.textRoster.text = rosterText

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.results.observe(this) { results ->
            adapter.submitList(results)
            binding.textNoResults.visibility =
                if (results.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
