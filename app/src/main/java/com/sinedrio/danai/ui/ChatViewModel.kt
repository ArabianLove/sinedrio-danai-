package com.sinedrio.danai.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.chat.ApiChatClient
import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSenateAgent
import com.sinedrio.danai.senate.chat.ChatSession
import com.sinedrio.danai.senate.chat.MockApiChatClient
import com.sinedrio.danai.senate.chat.OpenAiChatClient
import com.sinedrio.danai.senate.chat.SenateChat
import kotlinx.coroutines.launch

/**
 * ViewModel that drives [ChatActivity].
 *
 * Holds the active [SenateChat] instance and exposes [LiveData] streams for the
 * current [ChatSession], processing state, and error messages.  The [apiClient]
 * defaults to [MockApiChatClient] and can be swapped at runtime when the user
 * provides an API key.
 */
class ChatViewModel : ViewModel() {

    private var senateChat = SenateChat()

    /** The four agents seated in this Sinedrio. */
    val agents: List<ChatSenateAgent> get() = senateChat.agents

    private val _session = MutableLiveData<ChatSession?>()
    val session: LiveData<ChatSession?> = _session

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // ── API configuration ──────────────────────────────────────────────────────

    /**
     * Switch to a real OpenAI-compatible backend.
     *
     * Call this when the user has entered an API key in the settings dialog.
     */
    fun configureRealApi(apiKey: String, baseUrl: String, model: String) {
        if (apiKey.isBlank()) return
        val client: ApiChatClient = OpenAiChatClient(
            apiKey = apiKey,
            baseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" },
            model = model.ifBlank { "gpt-4o-mini" }
        )
        senateChat = SenateChat(client)
    }

    /** Revert to the offline [MockApiChatClient]. */
    fun useMockApi() {
        senateChat = SenateChat(MockApiChatClient())
    }

    // ── Session lifecycle ──────────────────────────────────────────────────────

    /**
     * Open a new Sinedrio session on [topic] for [ownerName].
     *
     * The first agent round is launched automatically so that agents greet the
     * room with their initial perspectives on the topic.
     */
    fun startSession(ownerName: String, ownerToken: String, topic: String) {
        if (topic.isBlank()) {
            _errorMessage.value = "Please enter a topic to discuss."
            return
        }
        if (ownerToken.isBlank()) {
            _errorMessage.value = "Owner token cannot be empty."
            return
        }

        val delegate = SenateDelegate(ownerName = ownerName.ifBlank { "Moderatore" }, token = ownerToken)
        val newSession: ChatSession
        try {
            newSession = senateChat.newSession(topic = topic, delegate = delegate)
        } catch (e: IllegalStateException) {
            _errorMessage.value = e.message
            return
        }

        _session.value = newSession
        _messages.value = emptyList()

        // Launch the first agent round so the conversation starts immediately
        runAgentRound(newSession)
    }

    // ── Human moderator ────────────────────────────────────────────────────────

    /**
     * Post a [message] from the human moderator and trigger an agent response round.
     */
    fun sendHumanMessage(ownerName: String, message: String) {
        val current = _session.value ?: return
        if (message.isBlank()) {
            _errorMessage.value = "Message cannot be empty."
            return
        }
        val updated = senateChat.humanMessage(
            session = current,
            ownerName = ownerName.ifBlank { "Moderatore" },
            message = message
        )
        pushSession(updated)
        runAgentRound(updated)
    }

    // ── Auto-moderation ────────────────────────────────────────────────────────

    /**
     * Trigger an auto-moderated round: the [AutoModerator] generates a follow-up
     * question and all agents respond.
     */
    fun autoModerate() {
        val current = _session.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val updated = senateChat.autoModerateRound(current)
                pushSession(updated)
            } catch (e: Exception) {
                _errorMessage.value = "Auto-moderation error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun runAgentRound(session: ChatSession) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val updated = senateChat.agentRound(session)
                pushSession(updated)
            } catch (e: Exception) {
                _errorMessage.value = "Agent error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun pushSession(session: ChatSession) {
        _session.value = session
        _messages.value = session.messages
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
