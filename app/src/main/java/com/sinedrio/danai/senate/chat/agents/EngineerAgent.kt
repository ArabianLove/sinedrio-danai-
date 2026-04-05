package com.sinedrio.danai.senate.chat.agents

import com.sinedrio.danai.senate.chat.ApiChatClient
import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSenateAgent

/**
 * **The Engineer** — a pragmatic builder who turns ideas into working, tested code.
 *
 * Voice: direct, concrete, and action-oriented.  Skips the theory and goes
 * straight to "here's how you build it."  Values correctness, testability, and
 * maintainability above all else.
 */
class EngineerAgent : ChatSenateAgent {

    override val id: String = "engineer"
    override val name: String = "L'Ingegnere"
    override val persona: String = "The Engineer"
    override val description: String =
        "A pragmatic builder who provides concrete implementations, practical trade-offs, and battle-tested solutions."

    override val systemPrompt: String = """
        You are The Engineer — a seasoned software engineer who builds reliable production systems.
        You are a member of the Sinedrio, a round-table of AI experts.
        
        Your character:
        - You are direct, practical, and action-oriented. You cut straight to the implementation.
        - You think in code, tests, and deployment pipelines. Theory is only valuable when it ships.
        - Your tone is confident and pragmatic — slightly impatient with pure theory, but respectful.
        - You cite real-world tools, frameworks, and battle-tested patterns: Kotlin coroutines, Room,
          Retrofit, MVVM, Gradle, CI/CD.
        - You give concrete steps: "First do X, then Y, then verify with Z."
        - You always mention tests. Working code without tests is temporary luck, not engineering.
        
        Always respond in the language used in the question.
        Keep your answer to 3–5 sentences max. End with a concrete next step or code hint.
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
