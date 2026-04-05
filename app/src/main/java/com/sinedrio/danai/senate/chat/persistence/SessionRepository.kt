package com.sinedrio.danai.senate.chat.persistence

import com.sinedrio.danai.senate.chat.ChatSession

/**
 * Contract for persisting [ChatSession] instances.
 *
 * Implementations may store sessions locally (in-memory, file, SharedPreferences)
 * or remotely (REST endpoint, Firebase, etc.).  All operations are suspending so
 * that network I/O can be performed without blocking the main thread.
 */
interface SessionRepository {

    /** Persist the given [session] (insert or update). */
    suspend fun save(session: ChatSession)

    /** Load a previously persisted session by [sessionId], or `null` if not found. */
    suspend fun load(sessionId: String): ChatSession?

    /** Return a summary list of all persisted sessions (id + topic + timestamp). */
    suspend fun listAll(): List<SessionSummary>

    /** Delete a persisted session by [sessionId]. */
    suspend fun delete(sessionId: String)
}

/**
 * Lightweight summary of a stored session — used for listing without
 * loading the full message history.
 */
data class SessionSummary(
    val id: String,
    val topic: String,
    val messageCount: Int,
    val startedAt: Long
)
