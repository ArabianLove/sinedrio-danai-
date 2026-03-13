package com.sinedrio.danai.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.sinedrio.danai.databinding.ActivityMainBinding
import com.sinedrio.danai.senate.SenateTask

/**
 * Entry point of the app.
 *
 * Presents the owner-delegation form and task-type selector.  When the owner
 * submits a task the [SenateViewModel] convenes the Senate and the user is
 * navigated to [SenateActivity] to review the agents' results.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: SenateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupTaskTypeChips()
        observeViewModel()

        binding.buttonConvene.setOnClickListener { submitTask() }
        binding.buttonViewSenate.setOnClickListener {
            startActivity(Intent(this, SenateActivity::class.java))
        }
    }

    private fun setupTaskTypeChips() {
        // Default selection
        binding.chipDebug.isChecked = true
    }

    private fun submitTask() {
        val ownerName = binding.editOwnerName.text?.toString()?.trim() ?: ""
        val ownerToken = binding.editOwnerToken.text?.toString()?.trim() ?: ""
        val input = binding.editCodeInput.text?.toString() ?: ""
        val taskType = selectedTaskType()

        viewModel.submitTask(
            ownerName = ownerName.ifBlank { "owner" },
            ownerToken = ownerToken,
            taskType = taskType,
            input = input
        )
    }

    private fun selectedTaskType(): SenateTask.TaskType {
        return when {
            binding.chipDesign.isChecked -> SenateTask.TaskType.DESIGN
            binding.chipBuild.isChecked -> SenateTask.TaskType.BUILD
            binding.chipExplain.isChecked -> SenateTask.TaskType.EXPLAIN
            else -> SenateTask.TaskType.DEBUG
        }
    }

    private fun observeViewModel() {
        viewModel.isProcessing.observe(this) { processing ->
            binding.progressBar.visibility = if (processing) View.VISIBLE else View.GONE
            binding.buttonConvene.isEnabled = !processing
        }

        viewModel.results.observe(this) { results ->
            if (results.isNotEmpty()) {
                startActivity(Intent(this, SenateActivity::class.java))
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }
}
