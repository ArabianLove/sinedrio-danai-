package com.sinedrio.danai.senate.chat

/**
 * The **AutoModerator** steps in when the human moderator is absent.
 *
 * It analyses the recent [ChatSession] history and generates a stimulating
 * follow-up question addressed to all agents, keeping the Sinedrio conversation
 * alive and productive across multiple sessions.
 *
 * The question is crafted to:
 * - Build on a thread raised by one of the agents.
 * - Open a new angle not yet explored.
 * - Challenge each agent to respond from their distinct perspective.
 *
 * @param apiClient The backend used for generating questions (can be [MockApiChatClient]).
 */
class AutoModerator(private val apiClient: ApiChatClient) {

    companion object {
        const val ID = "auto_moderator"
        const val NAME = "Moderatore"
        const val PERSONA = "Auto-Moderator"
    }

    private val systemPrompt = """
        You are a neutral, curious moderator for the Sinedrio — a round-table of expert AI agents
        discussing software engineering and technology.
        
        Your role:
        - Generate a single, thought-provoking open question that builds on the conversation so far.
        - The question should be broad enough for every expert (debugger, architect, engineer, teacher)
          to answer from their own perspective.
        - Keep the question short (1–2 sentences max).
        - Do NOT answer the question yourself — only ask it.
        - Vary the angle: sometimes challenge a recent statement, sometimes open a new sub-topic,
          sometimes ask for a concrete example or a counter-argument.
        
        Respond in the same language as the conversation.
        Return ONLY the question text, nothing else.
    """.trimIndent()

    /**
     * Generate the next moderation question based on the [session] history.
     *
     * @param session The current session whose history provides context.
     * @param agents  The agents seated in the session (names used to tailor questions).
     * @return        A [ChatMessage] from the auto-moderator containing the question.
     */
    suspend fun generateQuestion(
        session: ChatSession,
        agents: List<ChatSenateAgent>
    ): ChatMessage {
        val agentNames = agents.joinToString(", ") { it.name }
        val contextPrompt = buildContextPrompt(session, agentNames)

        val question = apiClient.complete(
            systemPrompt = systemPrompt,
            history = session.recentHistory(maxMessages = 12),
            userMessage = contextPrompt
        )

        return ChatMessage(
            sessionId = session.id,
            senderId = ID,
            senderName = NAME,
            senderPersona = PERSONA,
            content = question.trim(),
            isAutoModerator = true
        )
    }

    private fun buildContextPrompt(session: ChatSession, agentNames: String): String {
        val lastMessages = session.messages.takeLast(4)
            .joinToString("\n") { "- ${it.senderName}: ${it.content.take(120)}" }
        return """
            Topic: ${session.topic}
            Agents present: $agentNames
            
            Recent exchange:
            $lastMessages
            
            Please generate the next moderation question to keep the conversation productive.
        """.trimIndent()
    }
}
