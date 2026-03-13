package com.sinedrio.danai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.Senate
import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.SenateTask
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel that drives both [MainActivity] and [SenateActivity].
 *
 * Holds the active [Senate] and [SenateDelegate] and exposes [LiveData] streams
 * so the UI can react to state changes without coupling to coroutine internals.
 */
class SenateViewModel : ViewModel() {

    private val senate = Senate()

    /** The roster of agents available in the Senate. */
    val agentRoster = senate.agents

    private val _results = MutableLiveData<List<AgentResult>>(emptyList())
    val results: LiveData<List<AgentResult>> = _results

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Submit [input] to the Senate as a task of [taskType].
     *
     * The [ownerToken] is wrapped in a [SenateDelegate] before submission so
     * that only authorised callers can trigger agent work.
     */
    fun submitTask(
        ownerName: String,
        ownerToken: String,
        taskType: SenateTask.TaskType,
        input: String
    ) {
        if (input.isBlank()) {
            _errorMessage.value = "Input cannot be empty."
            return
        }
        if (ownerToken.isBlank()) {
            _errorMessage.value = "Owner token cannot be empty."
            return
        }

        val delegate = SenateDelegate(ownerName = ownerName, token = ownerToken)
        val task = SenateTask(
            id = UUID.randomUUID().toString(),
            type = taskType,
            input = input,
            delegatedBy = ownerName
        )

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val agentResults = senate.convene(delegate, task)
                _results.value = agentResults
            } catch (e: IllegalStateException) {
                _errorMessage.value = e.message
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
