package com.sinedrio.danai

import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.chat.DynamicChatAgent
import com.sinedrio.danai.senate.chat.MockApiChatClient
import com.sinedrio.danai.senate.chat.PersonaCatalogue
import com.sinedrio.danai.senate.chat.PersonaNegotiator
import com.sinedrio.danai.senate.chat.SenateChat
import com.sinedrio.danai.senate.chat.SenateMembers
import com.sinedrio.danai.senate.chat.persistence.InMemorySessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonaNegotiationTest {

    private val mockClient = MockApiChatClient()
    private val delegate = SenateDelegate(ownerName = "test-owner", token = "test-token")

    // ── PersonaCatalogue ──────────────────────────────────────────────────────

    @Test
    fun `PersonaCatalogue contains four personas`() {
        assertEquals(4, PersonaCatalogue.ALL.size)
    }

    @Test
    fun `PersonaCatalogue personas have unique ids`() {
        val ids = PersonaCatalogue.ALL.map { it.id }.toSet()
        assertEquals(4, ids.size)
    }

    @Test
    fun `PersonaCatalogue DETECTIVE has correct id and name`() {
        assertEquals("detective", PersonaCatalogue.DETECTIVE.id)
        assertEquals("Il Detective", PersonaCatalogue.DETECTIVE.name)
    }

    // ── DynamicChatAgent ──────────────────────────────────────────────────────

    @Test
    fun `DynamicChatAgent adopts persona from PersonaDefinition`() {
        val agent = DynamicChatAgent(PersonaCatalogue.VISIONARY)
        assertEquals("visionary", agent.id)
        assertEquals("Il Visionario", agent.name)
        assertEquals("The Visionary", agent.persona)
    }

    @Test
    fun `DynamicChatAgent produces non-blank response`(): Unit = runBlocking {
        val agent = DynamicChatAgent(PersonaCatalogue.ENGINEER)
        val msg = agent.chat("How do we implement this?", "s1", emptyList(), mockClient)
        assertTrue(msg.content.isNotBlank())
        assertEquals("engineer", msg.senderId)
    }

    // ── PersonaNegotiator — DEFAULT mode ──────────────────────────────────────

    @Test
    fun `PersonaNegotiator DEFAULT mode returns agents in catalogue order`(): Unit = runBlocking {
        val negotiator = PersonaNegotiator(mockClient, PersonaNegotiator.Mode.DEFAULT)
        val agents = negotiator.assign()
        assertEquals(4, agents.size)
        assertEquals("detective", agents[0].id)
        assertEquals("visionary", agents[1].id)
        assertEquals("engineer", agents[2].id)
        assertEquals("sage", agents[3].id)
    }

    // ── PersonaNegotiator — SHUFFLE mode ─────────────────────────────────────

    @Test
    fun `PersonaNegotiator SHUFFLE mode returns all four agents`(): Unit = runBlocking {
        val negotiator = PersonaNegotiator(mockClient, PersonaNegotiator.Mode.SHUFFLE)
        val agents = negotiator.assign()
        assertEquals(4, agents.size)
        val ids = agents.map { it.id }.toSet()
        assertTrue(ids.contains("detective"))
        assertTrue(ids.contains("visionary"))
        assertTrue(ids.contains("engineer"))
        assertTrue(ids.contains("sage"))
    }

    // ── PersonaNegotiator — parseNegotiatedOrder ─────────────────────────────

    @Test
    fun `parseNegotiatedOrder reorders personas by response`() {
        val negotiator = PersonaNegotiator(mockClient)
        val result = negotiator.parseNegotiatedOrder(
            "The Engineer, The Sage, The Detective, The Visionary",
            PersonaCatalogue.ALL
        )
        assertEquals("engineer", result[0].id)
        assertEquals("sage", result[1].id)
        assertEquals("detective", result[2].id)
        assertEquals("visionary", result[3].id)
    }

    @Test
    fun `parseNegotiatedOrder handles partial matches`() {
        val negotiator = PersonaNegotiator(mockClient)
        val result = negotiator.parseNegotiatedOrder(
            "The Sage, The Detective",
            PersonaCatalogue.ALL
        )
        // Sage and Detective matched first, then unmatched Visionary and Engineer appended
        assertEquals("sage", result[0].id)
        assertEquals("detective", result[1].id)
        assertEquals(4, result.size)
    }

    // ── SenateChat with negotiation ──────────────────────────────────────────

    @Test
    fun `SenateChat newSessionWithNegotiation uses negotiator`(): Unit = runBlocking {
        val negotiator = PersonaNegotiator(mockClient, PersonaNegotiator.Mode.SHUFFLE)
        val chat = SenateChat(mockClient, negotiator = negotiator)
        val session = chat.newSessionWithNegotiation("Testing", delegate)

        assertNotNull(session)
        assertEquals(4, chat.agents.size)
        // All agents should be DynamicChatAgent instances
        assertTrue(chat.agents.all { it is DynamicChatAgent })
    }

    @Test
    fun `SenateChat newSessionWithNegotiation preserves standard newSession behaviour`(): Unit = runBlocking {
        val chat = SenateChat(mockClient)
        val session = chat.newSessionWithNegotiation("Testing", delegate)

        assertEquals("Testing", session.topic)
        assertTrue(session.messages.isEmpty())
        assertTrue(session.isActive)
    }

    // ── SenateChat with persistence ──────────────────────────────────────────

    @Test
    fun `SenateChat agentRound auto-saves to repository`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        val chat = SenateChat(mockClient, repository = repo)
        val session = chat.newSession("Persistence test", delegate)

        val updated = chat.agentRound(session)

        val saved = repo.load(session.id)
        assertNotNull(saved)
        assertEquals(updated.messages.size, saved!!.messages.size)
    }

    @Test
    fun `SenateChat loadSession retrieves saved session`(): Unit = runBlocking {
        val repo = InMemorySessionRepository()
        val chat = SenateChat(mockClient, repository = repo)
        val session = chat.newSession("Load test", delegate)
        chat.saveSession(session)

        val loaded = chat.loadSession(session.id)
        assertNotNull(loaded)
        assertEquals("Load test", loaded!!.topic)
    }

    // ── SenateMembers ────────────────────────────────────────────────────────

    @Test
    fun `SenateMembers ALL contains eight models`() {
        assertEquals(8, SenateMembers.ALL.size)
    }

    @Test
    fun `SenateMembers BY_PROVIDER groups correctly`() {
        val grouped = SenateMembers.BY_PROVIDER
        assertTrue(grouped.containsKey("OpenAI"))
        assertTrue(grouped.containsKey("Anthropic"))
        assertTrue(grouped.containsKey("Google"))
        assertEquals(2, grouped["OpenAI"]!!.size)
    }

    @Test
    fun `SenateMembers models have non-blank ids and descriptions`() {
        for (model in SenateMembers.ALL) {
            assertTrue("Model id blank: ${model.displayName}", model.id.isNotBlank())
            assertTrue("Description blank: ${model.displayName}", model.description.isNotBlank())
            assertTrue("Base URL blank: ${model.displayName}", model.baseUrl.isNotBlank())
        }
    }

    // ── Backward compatibility ────────────────────────────────────────────────

    @Test
    fun `SenateChat without negotiator or repository works as before`(): Unit = runBlocking {
        val chat = SenateChat(mockClient)
        val session = chat.newSession("Backward compat", delegate)
        val updated = chat.agentRound(session)

        assertEquals(4, updated.messages.size)
        assertFalse(chat.agents.any { it is DynamicChatAgent })
    }
}
