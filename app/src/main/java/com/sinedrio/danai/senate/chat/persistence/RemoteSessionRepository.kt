package com.sinedrio.danai.senate.chat.persistence

import com.sinedrio.danai.senate.chat.ChatSession
import com.sinedrio.danai.senate.chat.persistence.SessionSerializer.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * [SessionRepository] that persists sessions on a configurable remote server.
 *
 * The server is expected to expose a simple REST-ish JSON API:
 *
 * | Method | Path                   | Body / Response                 |
 * |--------|------------------------|---------------------------------|
 * | PUT    | `{baseUrl}/{id}`       | Session JSON → 200/201          |
 * | GET    | `{baseUrl}/{id}`       | → Session JSON or 404           |
 * | GET    | `{baseUrl}`            | → JSON array of session objects  |
 * | DELETE | `{baseUrl}/{id}`       | → 200/204                       |
 *
 * Compatible with any backend that speaks this protocol (Node/Express, Flask,
 * Firebase Cloud Functions, Supabase Edge Functions, etc.).
 *
 * @param baseUrl Root URL of the session-storage endpoint
 *                (e.g. `https://my-server.example.com/api/sessions`).
 * @param authToken Optional `Authorization: Bearer` token.
 */
class RemoteSessionRepository(
    private val baseUrl: String,
    private val authToken: String = ""
) : SessionRepository {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    override suspend fun save(session: ChatSession): Unit = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/${session.id}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "PUT"
            connection.setRequestProperty("Content-Type", "application/json")
            applyAuth(connection)
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            val body = session.toJson().toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Remote save failed with HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── Load ───────────────────────────────────────────────────────────────────

    override suspend fun load(sessionId: String): ChatSession? = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/$sessionId")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            applyAuth(connection)
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            when (connection.responseCode) {
                200 -> {
                    val text = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                    SessionSerializer.sessionFromJson(JSONObject(text))
                }
                404 -> null
                else -> throw IOException("Remote load failed with HTTP ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── List all ───────────────────────────────────────────────────────────────

    override suspend fun listAll(): List<SessionSummary> = withContext(Dispatchers.IO) {
        val url = URL(baseUrl)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            applyAuth(connection)
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            if (connection.responseCode != 200) {
                throw IOException("Remote listAll failed with HTTP ${connection.responseCode}")
            }

            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
            val array = JSONArray(text)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SessionSummary(
                            id = obj.getString("id"),
                            topic = obj.getString("topic"),
                            messageCount = obj.optInt("messageCount", 0),
                            startedAt = obj.optLong("startedAt", 0L)
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    override suspend fun delete(sessionId: String): Unit = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/$sessionId")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            applyAuth(connection)
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS

            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Remote delete failed with HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun applyAuth(connection: HttpURLConnection) {
        if (authToken.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $authToken")
        }
    }
}
