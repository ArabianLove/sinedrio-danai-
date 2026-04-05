package com.sinedrio.danai.senate.chat

/**
 * Contract for an AI agent that participates in a [ChatSession].
 *
 * Unlike the batch-oriented [com.sinedrio.danai.senate.SenateAgent], a
 * [ChatSenateAgent] is conversational: it receives the full session history and
 * produces a [ChatMessage] reply that expresses its unique [persona].
 *
 * Each implementation should embody a distinct character so that the Sinedrio
 * feels like a genuine gathering of different minds.
 */
interface ChatSenateAgent {

    /** Stable unique identifier (used as [ChatMessage.senderId]). */
    val id: String

    /** Display name shown in the chat room. */
    val name: String

    /** One-line character descriptor (e.g. "The Detective"). */
    val persona: String

    /** Short description of this agent's specialisation. */
    val description: String

    /**
     * System-prompt text sent to the AI backend that establishes this agent's
     * voice, tone, and areas of expertise.
     */
    val systemPrompt: String

    /**
     * Produce a conversational reply to [question] given the session [history].
     *
     * @param question    The message or question the agent should respond to.
     * @param sessionId   ID of the owning session (used to stamp the returned message).
     * @param history     Recent messages for context.
     * @param apiClient   The backend to call for text generation.
     * @return            A [ChatMessage] authored by this agent.
     */
    suspend fun chat(
        question: String,
        sessionId: String,
        history: List<ChatMessage>,
        apiClient: ApiChatClient
    ): ChatMessage
}
