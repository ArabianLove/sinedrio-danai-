package com.sinedrio.danai

import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.chat.ApiChatClient
import com.sinedrio.danai.senate.chat.AutoModerator
import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSession
import com.sinedrio.danai.senate.chat.MockApiChatClient
import com.sinedrio.danai.senate.chat.SenateChat
import com.sinedrio.danai.senate.chat.agents.DetectiveAgent
import com.sinedrio.danai.senate.chat.agents.EngineerAgent
import com.sinedrio.danai.senate.chat.agents.SageAgent
import com.sinedrio.danai.senate.chat.agents.VisionaryAgent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenateChatTest {

    private val delegate = SenateDelegate(ownerName = "test-owner", token = "test-token")
    private val chat = SenateChat(MockApiChatClient())

    // ── ChatSession ────────────────────────────────────────────────────────────

    @Test
    fun `ChatSession withMessage appends message immutably`() {
        val session = ChatSession(topic = "Test")
        val msg = humanMessage(session.id, "Hello")
        val updated = session.withMessage(msg)

        assertEquals(0, session.messages.size)
        assertEquals(1, updated.messages.size)
        assertEquals(msg, updated.messages.first())
    }

    @Test
    fun `ChatSession recentHistory returns at most maxMessages entries`() {
        var session = ChatSession(topic = "Test")
        repeat(30) { i ->
            session = session.withMessage(humanMessage(session.id, "msg $i"))
        }
        assertEquals(20, session.recentHistory(maxMessages = 20).size)
        assertEquals(10, session.recentHistory(maxMessages = 10).size)
    }

    @Test
    fun `ChatSession lastQuestion returns the most recent human or moderator message`() {
        val session = ChatSession(topic = "Test")
        val human = humanMessage(session.id, "My question")
        val agentMsg = agentMessage(session.id, "My answer")
        val updated = session.withMessage(human).withMessage(agentMsg)

        assertEquals(human.id, updated.lastQuestion()?.id)
    }

    @Test
    fun `ChatSession lastQuestion returns null for empty session`() {
        val session = ChatSession(topic = "Empty")
        assertEquals(null, session.lastQuestion())
    }

    // ── SenateChat — session lifecycle ────────────────────────────────────────

    @Test
    fun `SenateChat newSession creates session with correct topic`() {
        val session = chat.newSession("Clean Architecture", delegate)
        assertEquals("Clean Architecture", session.topic)
        assertTrue(session.messages.isEmpty())
        assertTrue(session.isActive)
    }

    @Test(expected = IllegalStateException::class)
    fun `SenateChat newSession throws for inactive delegate`() {
        val inactive = SenateDelegate(ownerName = "owner", token = "")
        chat.newSession("Topic", inactive)
    }

    // ── SenateChat — human messages ───────────────────────────────────────────

    @Test
    fun `SenateChat humanMessage appends a human-flagged message`() {
        val session = chat.newSession("Topic", delegate)
        val updated = chat.humanMessage(session, "Dan", "My thoughts")
        assertEquals(1, updated.messages.size)
        assertTrue(updated.messages.first().isHuman)
        assertEquals("Dan", updated.messages.first().senderName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SenateChat humanMessage throws for blank message`() {
        val session = chat.newSession("Topic", delegate)
        chat.humanMessage(session, "Dan", "   ")
    }

    // ── SenateChat — agent round ──────────────────────────────────────────────

    @Test
    fun `SenateChat agentRound produces one message per agent`(): Unit = runBlocking {
        val session = chat.newSession("Testing", delegate)
        val updated = chat.agentRound(session)
        assertEquals(chat.agents.size, updated.messages.size)
    }

    @Test
    fun `SenateChat agentRound messages are not flagged as human or auto-moderator`(): Unit = runBlocking {
        val session = chat.newSession("Testing", delegate)
        val updated = chat.agentRound(session)
        assertTrue(updated.messages.none { it.isHuman })
        assertTrue(updated.messages.none { it.isAutoModerator })
    }

    @Test
    fun `SenateChat agentRound each message has correct sessionId`(): Unit = runBlocking {
        val session = chat.newSession("Testing", delegate)
        val updated = chat.agentRound(session)
        assertTrue(updated.messages.all { it.sessionId == session.id })
    }

    // ── SenateChat — auto-moderate round ─────────────────────────────────────

    @Test
    fun `SenateChat autoModerateRound appends moderator question then agent replies`(): Unit = runBlocking {
        val session = chat.newSession("Design patterns", delegate)
        val updated = chat.autoModerateRound(session)

        // First message is the auto-moderator's question
        assertTrue(updated.messages.first().isAutoModerator)
        // Followed by one reply per agent
        assertEquals(1 + chat.agents.size, updated.messages.size)
    }

    // ── Chat agents ───────────────────────────────────────────────────────────

    @Test
    fun `DetectiveAgent produces non-blank response`(): Unit = runBlocking {
        val agent = DetectiveAgent()
        val msg = agent.chat("What could go wrong here?", "s1", emptyList(), MockApiChatClient())
        assertTrue(msg.content.isNotBlank())
        assertEquals("detective", msg.senderId)
        assertFalse(msg.isHuman)
        assertFalse(msg.isAutoModerator)
    }

    @Test
    fun `VisionaryAgent produces non-blank response`(): Unit = runBlocking {
        val agent = VisionaryAgent()
        val msg = agent.chat("How do we design this?", "s1", emptyList(), MockApiChatClient())
        assertTrue(msg.content.isNotBlank())
        assertEquals("visionary", msg.senderId)
    }

    @Test
    fun `EngineerAgent produces non-blank response`(): Unit = runBlocking {
        val agent = EngineerAgent()
        val msg = agent.chat("How do we implement this?", "s1", emptyList(), MockApiChatClient())
        assertTrue(msg.content.isNotBlank())
        assertEquals("engineer", msg.senderId)
    }

    @Test
    fun `SageAgent produces non-blank response`(): Unit = runBlocking {
        val agent = SageAgent()
        val msg = agent.chat("What are the fundamentals?", "s1", emptyList(), MockApiChatClient())
        assertTrue(msg.content.isNotBlank())
        assertEquals("sage", msg.senderId)
    }

    // ── AutoModerator ─────────────────────────────────────────────────────────

    @Test
    fun `AutoModerator generates non-blank question message`(): Unit = runBlocking {
        val moderator = AutoModerator(MockApiChatClient())
        val session = ChatSession(topic = "Clean Architecture")
        val question = moderator.generateQuestion(session, chat.agents)

        assertTrue(question.content.isNotBlank())
        assertTrue(question.isAutoModerator)
        assertEquals(AutoModerator.ID, question.senderId)
        assertEquals(session.id, question.sessionId)
    }

    // ── MockApiChatClient ─────────────────────────────────────────────────────

    @Test
    fun `MockApiChatClient returns non-blank response for Detective persona`(): Unit = runBlocking {
        val client = MockApiChatClient()
        val response = client.complete(
            systemPrompt = "You are The Detective…",
            history = emptyList(),
            userMessage = "What bugs might this have?"
        )
        assertTrue(response.isNotBlank())
    }

    @Test
    fun `MockApiChatClient returns non-blank response for moderator persona`(): Unit = runBlocking {
        val client = MockApiChatClient()
        val response = client.complete(
            systemPrompt = "You are a neutral moderator…",
            history = emptyList(),
            userMessage = "Generate a question about architecture"
        )
        assertTrue(response.isNotBlank())
    }

    // ── Integration: full conversation flow ───────────────────────────────────

    @Test
    fun `Full conversation: open session, human message, agent round, auto-moderate`(): Unit = runBlocking {
        var session = chat.newSession("Software craftsmanship", delegate)

        // First agent round
        session = chat.agentRound(session)
        assertEquals(chat.agents.size, session.messages.size)

        // Human posts
        session = chat.humanMessage(session, "Dan", "Interesting! What about testability?")
        session = chat.agentRound(session)
        // 4 (first round) + 1 (human) + 4 (second round) = 9
        assertEquals(chat.agents.size * 2 + 1, session.messages.size)

        // Auto-moderate round
        session = chat.autoModerateRound(session)
        // 9 + 1 (moderator question) + 4 (third round) = 14
        assertEquals(chat.agents.size * 3 + 2, session.messages.size)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun humanMessage(sessionId: String, content: String) = ChatMessage(
        sessionId = sessionId,
        senderId = "human_test",
        senderName = "Test User",
        content = content,
        isHuman = true
    )

    private fun agentMessage(sessionId: String, content: String) = ChatMessage(
        sessionId = sessionId,
        senderId = "detective",
        senderName = "Il Detective",
        senderPersona = "The Detective",
        content = content
    )
}
