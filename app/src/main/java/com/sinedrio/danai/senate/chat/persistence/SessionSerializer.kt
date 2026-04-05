package com.sinedrio.danai.senate.chat.persistence

import com.sinedrio.danai.senate.chat.ChatMessage
import com.sinedrio.danai.senate.chat.ChatSession
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON serialisation / deserialisation helpers for [ChatSession] and [ChatMessage].
 *
 * Uses the `org.json` package already available on Android — no extra dependencies.
 */
object SessionSerializer {

    // ── ChatMessage ────────────────────────────────────────────────────────────

    fun ChatMessage.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("sessionId", sessionId)
        put("senderId", senderId)
        put("senderName", senderName)
        put("senderPersona", senderPersona)
        put("content", content)
        put("timestamp", timestamp)
        put("isHuman", isHuman)
        put("isAutoModerator", isAutoModerator)
    }

    fun messageFromJson(json: JSONObject): ChatMessage = ChatMessage(
        id = json.getString("id"),
        sessionId = json.getString("sessionId"),
        senderId = json.getString("senderId"),
        senderName = json.getString("senderName"),
        senderPersona = json.optString("senderPersona", ""),
        content = json.getString("content"),
        timestamp = json.getLong("timestamp"),
        isHuman = json.getBoolean("isHuman"),
        isAutoModerator = json.getBoolean("isAutoModerator")
    )

    // ── ChatSession ────────────────────────────────────────────────────────────

    fun ChatSession.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("topic", topic)
        put("startedAt", startedAt)
        put("isActive", isActive)
        put("messages", JSONArray().apply {
            for (msg in messages) put(msg.toJson())
        })
    }

    fun sessionFromJson(json: JSONObject): ChatSession = ChatSession(
        id = json.getString("id"),
        topic = json.getString("topic"),
        startedAt = json.getLong("startedAt"),
        isActive = json.getBoolean("isActive"),
        messages = buildList {
            val arr = json.getJSONArray("messages")
            for (i in 0 until arr.length()) {
                add(messageFromJson(arr.getJSONObject(i)))
            }
        }
    )
}
