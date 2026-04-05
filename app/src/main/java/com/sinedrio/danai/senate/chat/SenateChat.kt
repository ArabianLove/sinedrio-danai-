package com.sinedrio.danai.senate.chat

import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.chat.agents.DetectiveAgent
import com.sinedrio.danai.senate.chat.agents.EngineerAgent
import com.sinedrio.danai.senate.chat.agents.SageAgent
import com.sinedrio.danai.senate.chat.agents.VisionaryAgent
import com.sinedrio.danai.senate.chat.persistence.SessionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * The **SenateChat** orchestrates the Sinedrio as a live, multi-turn conversation.
 *
 * It seats four AI agents — each with a distinct persona — around a virtual round
 * table and routes every message to all of them concurrently, producing a rich
 * choral response.  An [AutoModerator] keeps the conversation going when the human
 * moderator is absent.
 *
 * ## Dynamic persona assignment
 *
 * When a [PersonaNegotiator] is supplied, persona roles are assigned dynamically at
 * session start (via [newSessionWithNegotiation]) instead of using the hard-coded
 * agent classes.  This allows the Sinedrio to exploit each LLM's current strengths.
 *
 * ## Persistent sessions
 *
 * When a [SessionRepository] is supplied, every session mutation is automatically
 * saved so that sessions can be resumed across app restarts.
 *
 * Typical flow:
 * ```
 * val chat    = SenateChat()
 * var session = chat.newSession("Clean Architecture in Android", delegate)
 * session     = chat.agentRound(session)            // all agents respond to the topic
 * session     = chat.humanMessage(session, "Dan", "What about modularisation?")
 * session     = chat.agentRound(session)            // agents respond to Dan's question
 * session     = chat.autoModerateRound(session)     // moderator asks; agents respond
 * ```
 *
 * @param apiClient  Back-end used for all agent completions (defaults to offline mock).
 * @param repository Optional session persistence (in-memory, remote server, etc.).
 * @param negotiator Optional persona negotiator for dynamic role assignment.
 */
class SenateChat(
    val apiClient: ApiChatClient = MockApiChatClient(),
    val repository: SessionRepository? = null,
    val negotiator: PersonaNegotiator? = null
) {

    /** The four agents permanently seated in this Sinedrio. */
    var agents: List<ChatSenateAgent> = listOf(
        DetectiveAgent(),
        VisionaryAgent(),
        EngineerAgent(),
        SageAgent()
    )
        private set

    private val autoModerator = AutoModerator(apiClient)

    // ── Session lifecycle ─────────────────────────────────────────────────────

    /**
     * Create a new [ChatSession] for the given [topic].
     *
     * The [delegate] is verified before the session opens — only owner-authorised
     * delegates may convene the Sinedrio.
     *
     * @throws IllegalStateException if the delegate is not active.
     */
    fun newSession(topic: String, delegate: SenateDelegate): ChatSession {
        check(delegate.isActive) {
            "SenateChat: delegate for '${delegate.ownerName}' is not active. " +
                "Only owner-authorised delegates may open a session."
        }
        return ChatSession(topic = topic)
    }

    /**
     * Create a new session **with dynamic persona negotiation**.
     *
     * The [negotiator] (if available) decides which persona each agent seat
     * should embody, based on the [topic] and the current negotiation [mode].
     * If no negotiator is configured, falls back to [newSession] behaviour.
     *
     * @throws IllegalStateException if the delegate is not active.
     */
    suspend fun newSessionWithNegotiation(
        topic: String,
        delegate: SenateDelegate
    ): ChatSession {
        check(delegate.isActive) {
            "SenateChat: delegate for '${delegate.ownerName}' is not active. " +
                "Only owner-authorised delegates may open a session."
        }
        negotiator?.let {
            agents = it.assign(topic = topic)
        }
        return ChatSession(topic = topic)
    }

    // ── Session persistence ───────────────────────────────────────────────────

    /**
     * Persist the given session via the configured [repository].
     * No-op if no repository is set.
     */
    suspend fun saveSession(session: ChatSession) {
        repository?.save(session)
    }

    /**
     * Load a previously saved session by [sessionId].
     * Returns `null` if no repository is set or the session is not found.
     */
    suspend fun loadSession(sessionId: String): ChatSession? =
        repository?.load(sessionId)

    // ── Human moderator ───────────────────────────────────────────────────────

    /**
     * Post a message from the human moderator into the session.
     *
     * Returns an updated [ChatSession] with the new human message appended.
     * Does **not** trigger an agent round — call [agentRound] separately if
     * you want the agents to respond immediately.
     */
    fun humanMessage(
        session: ChatSession,
        ownerName: String,
        message: String
    ): ChatSession {
        require(message.isNotBlank()) { "Human message must not be blank." }
        val humanMsg = ChatMessage(
            sessionId = session.id,
            senderId = "human_$ownerName",
            senderName = ownerName,
            senderPersona = "Human Moderator",
            content = message,
            isHuman = true
        )
        return session.withMessage(humanMsg)
    }

    // ── Agent rounds ──────────────────────────────────────────────────────────

    /**
     * Invite all agents to respond to the most recent question in the session.
     *
     * Agents run **concurrently**; their replies are collected and appended to
     * the session in the order agents are seated.  If an agent throws, the error
     * is caught and a failure message is appended so the session continues.
     *
     * @return Updated [ChatSession] with each agent's reply appended.
     */
    suspend fun agentRound(session: ChatSession): ChatSession = coroutineScope {
        val question = session.lastQuestion()?.content ?: session.topic
        val history = session.recentHistory()

        val agentMessages = agents.map { agent ->
            async {
                runCatching {
                    agent.chat(
                        question = question,
                        sessionId = session.id,
                        history = history,
                        apiClient = apiClient
                    )
                }.getOrElse { error ->
                    ChatMessage(
                        sessionId = session.id,
                        senderId = agent.id,
                        senderName = agent.name,
                        senderPersona = agent.persona,
                        content = "⚠ Agent encountered an error: ${error.message}"
                    )
                }
            }
        }.map { it.await() }

        val updated = agentMessages.fold(session) { acc, msg -> acc.withMessage(msg) }
        saveSession(updated)
        updated
    }

    // ── Auto-moderation ───────────────────────────────────────────────────────

    /**
     * Run a full auto-moderated round:
     * 1. [AutoModerator] generates a follow-up question based on the history.
     * 2. All agents respond to that question via [agentRound].
     *
     * Use this when the human moderator is unavailable, to keep the conversation
     * productive across sessions.
     *
     * @return Updated [ChatSession] with the moderator question and all agent replies.
     */
    suspend fun autoModerateRound(session: ChatSession): ChatSession {
        val questionMsg = autoModerator.generateQuestion(session, agents)
        val updatedSession = session.withMessage(questionMsg)
        val result = agentRound(updatedSession)
        saveSession(result)
        return result
    }
}
