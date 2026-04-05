package com.sinedrio.danai.persistence

import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSession
import com.sinedrio.danai.senate.chat.persistence.InMemorySessionRepository
import com.sinedrio.danai.senate.chat.persistence.SessionSerializer
import com.sinedrio.danai.senate.chat.persistence.SessionSerializer.toJson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPersistenceTest {

    // ── SessionSerializer — ChatMessage ────────────────────────────────────────

    @Test
    fun `ChatMessage round-trips through JSON`() {
        val original = ChatMessage(
            id = "msg-1",
            sessionId = "sess-1",
            senderId = "detective",
            senderName = "Il Detective",
            senderPersona = "The Detective",
            content = "Something doesn't add up here.",
            timestamp = 1700000000000L,
            isHuman = false,
            isAutoModerator = false
        )

        val json = with(SessionSerializer) { original.toJson() }
        val restored = SessionSerializer.messageFromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.sessionId, restored.sessionId)
        assertEquals(original.senderId, restored.senderId)
        assertEquals(original.senderName, restored.senderName)
        assertEquals(original.senderPersona, restored.senderPersona)
        assertEquals(original.content, restored.content)
        assertEquals(original.timestamp, restored.timestamp)
        assertEquals(original.isHuman, restored.isHuman)
        assertEquals(original.isAutoModerator, restored.isAutoModerator)
    }

    @Test
    fun `Human ChatMessage round-trips through JSON`() {
        val original = ChatMessage(
            id = "msg-2",
            sessionId = "sess-1",
            senderId = "human_Dan",
            senderName = "Dan",
            senderPersona = "Human Moderator",
            content = "What do you think about modularisation?",
            timestamp = 1700000001000L,
            isHuman = true,
            isAutoModerator = false
        )

        val json = with(SessionSerializer) { original.toJson() }
        val restored = SessionSerializer.messageFromJson(json)

        assertEquals(original.id, restored.id)
        assertTrue(restored.isHuman)
    }

    // ── SessionSerializer — ChatSession ────────────────────────────────────────

    @Test
    fun `ChatSession round-trips through JSON`() {
        val msg1 = ChatMessage(
            id = "m1", sessionId = "s1", senderId = "human_Dan",
            senderName = "Dan", content = "Hello", isHuman = true
        )
        val msg2 = ChatMessage(
            id = "m2", sessionId = "s1", senderId = "detective",
            senderName = "Il Detective", senderPersona = "The Detective",
            content = "Let me investigate."
        )

        val original = ChatSession(
            id = "s1",
            topic = "Clean Architecture",
            messages = listOf(msg1, msg2),
            startedAt = 1700000000000L,
            isActive = true
        )

        val json = with(SessionSerializer) { original.toJson() }
        val restored = SessionSerializer.sessionFromJson(json)

        assertEquals(original.id, restored.id)
        assertEquals(original.topic, restored.topic)
        assertEquals(original.startedAt, restored.startedAt)
        assertEquals(original.isActive, restored.isActive)
        assertEquals(2, restored.messages.size)
        assertEquals("Hello", restored.messages[0].content)
        assertEquals("Let me investigate.", restored.messages[1].content)
    }

    @Test
    fun `Empty session round-trips through JSON`() {
        val original = ChatSession(id = "s2", topic = "Testing", startedAt = 1700000000L)

        val json = with(SessionSerializer) { original.toJson() }
        val restored = SessionSerializer.sessionFromJson(json)

        assertEquals(original.id, restored.id)
        assertTrue(restored.messages.isEmpty())
    }

    @Test
    fun `Session JSON produces valid JSON string`() {
        val session = ChatSession(id = "s3", topic = "JSON Test")
        val json = with(SessionSerializer) { session.toJson() }
        val text = json.toString()

        // Should be parseable back
        val parsed = JSONObject(text)
        assertEquals("s3", parsed.getString("id"))
    }

    // ── InMemorySessionRepository ──────────────────────────────────────────────

    @Test
    fun `InMemorySessionRepository save and load round-trip`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        val session = ChatSession(id = "s1", topic = "Persistence test")

        repo.save(session)
        val loaded = repo.load("s1")

        assertNotNull(loaded)
        assertEquals("s1", loaded!!.id)
        assertEquals("Persistence test", loaded.topic)
    }

    @Test
    fun `InMemorySessionRepository returns null for unknown id`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        assertNull(repo.load("nonexistent"))
    }

    @Test
    fun `InMemorySessionRepository listAll returns saved sessions`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        repo.save(ChatSession(id = "s1", topic = "Topic A", startedAt = 100L))
        repo.save(ChatSession(id = "s2", topic = "Topic B", startedAt = 200L))

        val list = repo.listAll()
        assertEquals(2, list.size)
        // Sorted by descending startedAt
        assertEquals("s2", list[0].id)
        assertEquals("s1", list[1].id)
    }

    @Test
    fun `InMemorySessionRepository delete removes session`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        repo.save(ChatSession(id = "s1", topic = "Will be deleted"))
        repo.delete("s1")
        assertNull(repo.load("s1"))
    }

    @Test
    fun `InMemorySessionRepository save updates existing session`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        val original = ChatSession(id = "s1", topic = "Version 1")
        repo.save(original)

        val updated = original.withMessage(
            ChatMessage(
                id = "m1", sessionId = "s1", senderId = "human",
                senderName = "Dan", content = "New message", isHuman = true
            )
        )
        repo.save(updated)

        val loaded = repo.load("s1")
        assertNotNull(loaded)
        assertEquals(1, loaded!!.messages.size)
    }

    @Test
    fun `InMemorySessionRepository listAll returns correct message counts`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        var session = ChatSession(id = "s1", topic = "Counting messages")
        session = session.withMessage(
            ChatMessage(id = "m1", sessionId = "s1", senderId = "a", senderName = "A", content = "1")
        )
        session = session.withMessage(
            ChatMessage(id = "m2", sessionId = "s1", senderId = "b", senderName = "B", content = "2")
        )
        repo.save(session)

        val summaries = repo.listAll()
        assertEquals(1, summaries.size)
        assertEquals(2, summaries[0].messageCount)
    }
}
