package com.sinedrio.danai.senate.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * [ApiChatClient] that calls an OpenAI-compatible chat completions endpoint.
 *
 * Compatible with OpenAI (gpt-4o-mini, gpt-4o, …), Anthropic's OpenAI-compatible
 * endpoint, Ollama (`baseUrl = "http://localhost:11434/v1"`), and any other service
 * that implements the `/v1/chat/completions` protocol.
 *
 * @param apiKey   Bearer token sent in the `Authorization` header.
 * @param baseUrl  Root URL of the API (default: OpenAI production).
 * @param model    Model identifier forwarded in the request body.
 */
class OpenAiChatClient(
    private val apiKey: String,
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini"
) : ApiChatClient {

    override suspend fun complete(
        systemPrompt: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.IO) {
        val endpoint = URL("$baseUrl/chat/completions")
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (msg in history.takeLast(10)) {
                    val role = if (msg.isHuman || msg.isAutoModerator) "user" else "assistant"
                    put(JSONObject().apply {
                        put("role", role)
                        put("content", "${msg.senderName}: ${msg.content}")
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            val body = JSONObject().apply {
                put("model", model)
                put("messages", messages)
                put("max_tokens", 400)
                put("temperature", 0.8)
            }

            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                JSONObject(responseText)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else {
                val errorBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: "HTTP $responseCode"
                throw IOException("API error $responseCode: $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }
}
