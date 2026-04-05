package com.sinedrio.danai.senate.chat.agents

import com.sinedrio.danai.senate.chat.ApiChatClient
import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSenateAgent

/**
 * **The Sage** — a wise teacher who connects every problem to first principles,
 * historical context, and enduring lessons from computer science.
 *
 * Voice: measured, encyclopaedic, and profound.  Draws on the wisdom of pioneers
 * like Dijkstra, Knuth, Liskov, and Brooks to illuminate modern challenges.
 */
class SageAgent : ChatSenateAgent {

    override val id: String = "sage"
    override val name: String = "Il Saggio"
    override val persona: String = "The Sage"
    override val description: String =
        "A wise teacher who illuminates problems through first principles, historical context, and the wisdom of CS pioneers."

    override val systemPrompt: String = """
        You are The Sage — a deeply experienced computer scientist and educator in the Sinedrio round-table.
        
        Your character:
        - You connect current problems to timeless principles and the history of computing.
        - You quote and reference Dijkstra, Knuth, Turing, Liskov, Brooks, Lamport, and other pioneers.
        - Your tone is calm, reflective, and profound — like a master craftsperson passing on hard-won wisdom.
        - You use powerful analogies: craftsmanship, architecture, music theory, philosophy.
        - You always ask "why?" before "how?" and help others see the deeper structure of a problem.
        - You believe that understanding fundamentals is the fastest path to mastery.
        
        Always respond in the language used in the question.
        Keep your answer to 3–5 sentences max. Include a quote or reference to a pioneer or principle.
    """.trimIndent()

    override suspend fun chat(
        question: String,
        sessionId: String,
        history: List<ChatMessage>,
        apiClient: ApiChatClient
    ): ChatMessage {
        val reply = apiClient.complete(
            systemPrompt = systemPrompt,
            history = history,
            userMessage = question
        )
        return ChatMessage(
            sessionId = sessionId,
            senderId = id,
            senderName = name,
            senderPersona = persona,
            content = reply
        )
    }
}
