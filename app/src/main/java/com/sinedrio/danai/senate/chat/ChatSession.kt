package com.sinedrio.danai.senate.chat

import java.util.UUID

/**
 * Represents an active or archived Sinedrio chat session.
 *
 * A session starts with a [topic], accumulates [ChatMessage]s as agents and the
 * human moderator exchange views, and can be marked inactive once the conversation
 * has concluded.  All mutation returns a new copy so that [LiveData] observers
 * receive proper change notifications.
 *
 * @param id        Unique session identifier.
 * @param topic     The opening theme or question posed to the Sinedrio.
 * @param messages  Ordered list of messages in this session.
 * @param startedAt Session creation time (epoch millis).
 * @param isActive  Whether the session is currently open for new messages.
 */
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val messages: List<ChatMessage> = emptyList(),
    val startedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    /** Returns a new session with [message] appended to the message list. */
    fun withMessage(message: ChatMessage): ChatSession =
        copy(messages = messages + message)

    /** Returns the last [maxMessages] messages for use as context. */
    fun recentHistory(maxMessages: Int = 20): List<ChatMessage> =
        messages.takeLast(maxMessages)

    /** Returns the most recent message posted by the [AutoModerator] or the human. */
    fun lastQuestion(): ChatMessage? =
        messages.lastOrNull { it.isHuman || it.isAutoModerator }
}
