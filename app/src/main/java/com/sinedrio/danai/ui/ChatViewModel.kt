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
import com.sinedrio.danai.senate.chat.PersonaNegotiator
import com.sinedrio.danai.senate.chat.SenateChat
import com.sinedrio.danai.senate.chat.persistence.InMemorySessionRepository
import com.sinedrio.danai.senate.chat.persistence.RemoteSessionRepository
import com.sinedrio.danai.senate.chat.persistence.SessionRepository
import com.sinedrio.danai.senate.chat.persistence.SessionSummary
import kotlinx.coroutines.launch

/**
 * ViewModel that drives [ChatActivity].
 *
 * Holds the active [SenateChat] instance and exposes [LiveData] streams for the
 * current [ChatSession], processing state, and error messages.  The [apiClient]
 * defaults to [MockApiChatClient] and can be swapped at runtime when the user
 * provides an API key.
 *
 * ## Session persistence
 *
 * A [SessionRepository] is always active (defaults to [InMemorySessionRepository]).
 * When the user configures a remote server URL, sessions are stored on the external
 * server and can be resumed across app restarts.
 *
 * ## Dynamic persona negotiation
 *
 * A [PersonaNegotiator] allows the moderator (or the models themselves) to decide
 * which persona each agent seat embodies at the start of every session.
 */
class ChatViewModel : ViewModel() {

    private var currentApiClient: ApiChatClient = MockApiChatClient()
    private var sessionRepo: SessionRepository = InMemorySessionRepository()
    private var personaNegotiator = PersonaNegotiator(currentApiClient)
    private var senateChat = SenateChat(currentApiClient, sessionRepo, personaNegotiator)

    /** The agents seated in this Sinedrio (may change after persona negotiation). */
    val agents: List<ChatSenateAgent> get() = senateChat.agents

    private val _session = MutableLiveData<ChatSession?>()
    val session: LiveData<ChatSession?> = _session

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _savedSessions = MutableLiveData<List<SessionSummary>>(emptyList())
    val savedSessions: LiveData<List<SessionSummary>> = _savedSessions

    // ── API configuration ──────────────────────────────────────────────────────

    /**
     * Switch to a real OpenAI-compatible backend.
     *
     * Call this when the user has entered an API key in the settings dialog.
     */
    fun configureRealApi(apiKey: String, baseUrl: String, model: String) {
        if (apiKey.isBlank()) return
        currentApiClient = OpenAiChatClient(
            apiKey = apiKey,
            baseUrl = baseUrl.ifBlank { "https://api.openai.com/v1" },
            model = model.ifBlank { "gpt-4o-mini" }
        )
        personaNegotiator = PersonaNegotiator(currentApiClient, personaNegotiator.mode)
        senateChat = SenateChat(currentApiClient, sessionRepo, personaNegotiator)
    }

    /** Revert to the offline [MockApiChatClient]. */
    fun useMockApi() {
        currentApiClient = MockApiChatClient()
        personaNegotiator = PersonaNegotiator(currentApiClient, personaNegotiator.mode)
        senateChat = SenateChat(currentApiClient, sessionRepo, personaNegotiator)
    }

    // ── Session persistence configuration ──────────────────────────────────────

    /**
     * Configure a remote server for persistent session storage.
     *
     * @param serverUrl  Base URL of the session API (e.g. `https://my-server.com/api/sessions`).
     * @param authToken  Optional bearer token for authentication.
     */
    fun configureRemoteStorage(serverUrl: String, authToken: String = "") {
        if (serverUrl.isBlank()) return
        sessionRepo = RemoteSessionRepository(baseUrl = serverUrl, authToken = authToken)
        senateChat = SenateChat(currentApiClient, sessionRepo, personaNegotiator)
    }

    /** Revert to in-memory session storage. */
    fun useLocalStorage() {
        sessionRepo = InMemorySessionRepository()
        senateChat = SenateChat(currentApiClient, sessionRepo, personaNegotiator)
    }

    // ── Persona negotiation configuration ──────────────────────────────────────

    /** Set the persona negotiation mode for future sessions. */
    fun setNegotiationMode(mode: PersonaNegotiator.Mode) {
        personaNegotiator.mode = mode
    }

    // ── Session lifecycle ──────────────────────────────────────────────────────

    /**
     * Open a new Sinedrio session on [topic] for [ownerName].
     *
     * If persona negotiation is enabled, agents are reassigned dynamically.
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

        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val newSession = if (personaNegotiator.mode != PersonaNegotiator.Mode.DEFAULT) {
                    senateChat.newSessionWithNegotiation(topic = topic, delegate = delegate)
                } else {
                    senateChat.newSession(topic = topic, delegate = delegate)
                }

                _session.value = newSession
                _messages.value = emptyList()

                // Launch the first agent round so the conversation starts immediately
                val updated = senateChat.agentRound(newSession)
                pushSession(updated)
            } catch (e: IllegalStateException) {
                _errorMessage.value = e.message
            } catch (e: Exception) {
                _errorMessage.value = "Session error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Resume a previously saved session by [sessionId].
     */
    fun resumeSession(sessionId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _errorMessage.value = null
            try {
                val loaded = senateChat.loadSession(sessionId)
                if (loaded != null) {
                    pushSession(loaded)
                } else {
                    _errorMessage.value = "Session not found."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load session: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /** Refresh the list of saved sessions. */
    fun loadSavedSessions() {
        viewModelScope.launch {
            try {
                _savedSessions.value = sessionRepo.listAll()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to list sessions: ${e.message}"
            }
        }
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
