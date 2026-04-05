package com.sinedrio.danai.senate.chat

/**
 * A [ChatSenateAgent] whose persona is assigned dynamically at session start
 * rather than being hard-coded.
 *
 * The [PersonaNegotiator] creates one [DynamicChatAgent] per seat in the
 * Sinedrio and assigns it a [PersonaDefinition].  Internally it delegates
 * to the same API-call pattern used by the fixed agents.
 */
class DynamicChatAgent(private val personaDef: PersonaDefinition) : ChatSenateAgent {

    override val id: String get() = personaDef.id
    override val name: String get() = personaDef.name
    override val persona: String get() = personaDef.persona
    override val description: String get() = personaDef.description
    override val systemPrompt: String get() = personaDef.systemPrompt

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
