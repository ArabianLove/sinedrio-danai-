package com.sinedrio.danai.senate.chat

/**
 * Registry of recommended LLM models for the Sinedrio.
 *
 * Each [SenateModel] represents an LLM that can sit as a member of the
 * AI senate.  The models are grouped by provider and ranked by their
 * suitability for multi-agent debate.
 *
 * The actual API key and endpoint are configured at runtime in the
 * [ChatActivity] settings dialog — this class only describes the models.
 */
object SenateMembers {

    /**
     * A recommended LLM model for the Sinedrio.
     *
     * @param id            Model identifier sent to the API (e.g. `gpt-4o`).
     * @param displayName   Human-readable name for UI display.
     * @param provider      The provider / vendor name.
     * @param baseUrl       Default API base URL for this provider.
     * @param description   Short description of the model's strengths.
     */
    data class SenateModel(
        val id: String,
        val displayName: String,
        val provider: String,
        val baseUrl: String,
        val description: String
    )

    // ── OpenAI ────────────────────────────────────────────────────────────────

    val GPT_4O = SenateModel(
        id = "gpt-4o",
        displayName = "GPT-4o",
        provider = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        description = "Flagship multimodal model — excels at reasoning, analysis, and creative tasks."
    )

    val GPT_4O_MINI = SenateModel(
        id = "gpt-4o-mini",
        displayName = "GPT-4o mini",
        provider = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        description = "Cost-efficient model — fast and capable, ideal for high-throughput rounds."
    )

    // ── Anthropic ─────────────────────────────────────────────────────────────

    val CLAUDE_SONNET = SenateModel(
        id = "claude-sonnet-4-20250514",
        displayName = "Claude Sonnet 4",
        provider = "Anthropic",
        baseUrl = "https://api.anthropic.com/v1",
        description = "Strong at nuanced reasoning, long context, and careful analysis."
    )

    val CLAUDE_HAIKU = SenateModel(
        id = "claude-haiku-4-20250514",
        displayName = "Claude Haiku 4",
        provider = "Anthropic",
        baseUrl = "https://api.anthropic.com/v1",
        description = "Fastest Anthropic model — excellent for rapid debate rounds."
    )

    // ── Google ─────────────────────────────────────────────────────────────────

    val GEMINI_PRO = SenateModel(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        provider = "Google",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        description = "Google's flagship — strong at code, mathematics, and multi-step reasoning."
    )

    val GEMINI_FLASH = SenateModel(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        provider = "Google",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        description = "Low-latency variant — ideal for keeping the conversation flowing."
    )

    // ── Local / Ollama ────────────────────────────────────────────────────────

    val LLAMA_3 = SenateModel(
        id = "llama3.1",
        displayName = "Llama 3.1 (Ollama)",
        provider = "Meta / Ollama",
        baseUrl = "http://localhost:11434/v1",
        description = "Open-weight model running locally — privacy-first, no API cost."
    )

    val MISTRAL = SenateModel(
        id = "mistral",
        displayName = "Mistral (Ollama)",
        provider = "Mistral / Ollama",
        baseUrl = "http://localhost:11434/v1",
        description = "Compact European open-weight model — fast and capable on modest hardware."
    )

    // ── All recommended models ────────────────────────────────────────────────

    /** Complete catalogue ordered by general capability. */
    val ALL: List<SenateModel> = listOf(
        GPT_4O,
        GPT_4O_MINI,
        CLAUDE_SONNET,
        CLAUDE_HAIKU,
        GEMINI_PRO,
        GEMINI_FLASH,
        LLAMA_3,
        MISTRAL
    )

    /** Models grouped by provider. */
    val BY_PROVIDER: Map<String, List<SenateModel>> = ALL.groupBy { it.provider }
}
