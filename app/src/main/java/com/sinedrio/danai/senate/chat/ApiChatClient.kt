package com.sinedrio.danai.senate.chat

/**
 * Contract for any backend that can generate AI chat completions.
 *
 * Implementations may target real APIs (OpenAI, Anthropic, Ollama, …) or
 * provide an offline mock for demonstration and testing.
 *
 * @see MockApiChatClient
 * @see OpenAiChatClient
 */
interface ApiChatClient {

    /**
     * Generate a completion from the AI backend.
     *
     * @param systemPrompt  Persona / instruction prompt that shapes the AI's voice.
     * @param history       Ordered list of previous messages for context.
     * @param userMessage   The new message the AI should respond to.
     * @return              The AI's textual response.
     */
    suspend fun complete(
        systemPrompt: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String
}
