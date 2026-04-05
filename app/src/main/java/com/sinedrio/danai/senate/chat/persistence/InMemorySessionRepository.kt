package com.sinedrio.danai.senate.chat.persistence

import com.sinedrio.danai.senate.chat.ChatSession

/**
 * [SessionRepository] backed by an in-memory map.
 *
 * Useful for testing and as a fallback when no external server is configured.
 * Data is lost when the process terminates.
 */
class InMemorySessionRepository : SessionRepository {

    private val store = mutableMapOf<String, ChatSession>()

    override suspend fun save(session: ChatSession) {
        store[session.id] = session
    }

    override suspend fun load(sessionId: String): ChatSession? =
        store[sessionId]

    override suspend fun listAll(): List<SessionSummary> =
        store.values.map { session ->
            SessionSummary(
                id = session.id,
                topic = session.topic,
                messageCount = session.messages.size,
                startedAt = session.startedAt
            )
        }.sortedByDescending { it.startedAt }

    override suspend fun delete(sessionId: String) {
        store.remove(sessionId)
    }
}
