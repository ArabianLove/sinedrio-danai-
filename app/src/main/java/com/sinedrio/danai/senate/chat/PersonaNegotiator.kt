package com.sinedrio.danai.senate.chat

/**
 * Negotiates persona assignments for the Sinedrio at session start.
 *
 * Instead of hard-coding "Model A = Detective, Model B = Visionary", the
 * negotiator asks the [AutoModerator] (or uses a simple heuristic) to decide
 * which persona suits each agent seat best.  This allows the Sinedrio to
 * leverage each LLM's current strengths — a model that excels at creative
 * thinking won't be wasted in the sceptical Detective role.
 *
 * ## Negotiation modes
 *
 * | Mode         | Behaviour                                                |
 * |--------------|----------------------------------------------------------|
 * | [DEFAULT]    | Uses the catalogue order (Detective → Visionary → …).   |
 * | [SHUFFLE]    | Randomly shuffles persona assignments each session.      |
 * | [NEGOTIATE]  | Asks the API to propose assignments based on topic.      |
 *
 * @param apiClient  Backend used when [mode] is [Mode.NEGOTIATE].
 */
class PersonaNegotiator(
    private val apiClient: ApiChatClient,
    var mode: Mode = Mode.DEFAULT
) {

    enum class Mode { DEFAULT, SHUFFLE, NEGOTIATE }

    /**
     * Return a list of [DynamicChatAgent] instances with personas assigned
     * according to the current [mode].
     *
     * @param personas Available persona definitions (defaults to the full catalogue).
     * @param topic    Session topic — used by [Mode.NEGOTIATE] to inform the assignment.
     */
    suspend fun assign(
        personas: List<PersonaDefinition> = PersonaCatalogue.ALL,
        topic: String = ""
    ): List<DynamicChatAgent> {
        val ordered = when (mode) {
            Mode.DEFAULT -> personas
            Mode.SHUFFLE -> personas.shuffled()
            Mode.NEGOTIATE -> negotiateOrder(personas, topic)
        }
        return ordered.map { DynamicChatAgent(it) }
    }

    // ── NEGOTIATE mode ─────────────────────────────────────────────────────────

    private suspend fun negotiateOrder(
        personas: List<PersonaDefinition>,
        topic: String
    ): List<PersonaDefinition> {
        val personaNames = personas.joinToString(", ") { it.persona }
        val prompt = """
            You are the moderator of the Sinedrio — a round-table of AI experts.
            The topic for today's session is: "$topic"
            
            Available personas: $personaNames
            
            Based on the topic, suggest the best order in which these personas should
            speak.  Return ONLY a comma-separated list of persona names in the
            recommended order, nothing else.
        """.trimIndent()

        return try {
            val response = apiClient.complete(
                systemPrompt = "You are a neutral moderator.",
                history = emptyList(),
                userMessage = prompt
            )
            parseNegotiatedOrder(response, personas)
        } catch (_: Exception) {
            // Fallback to shuffle if the API call fails
            personas.shuffled()
        }
    }

    internal fun parseNegotiatedOrder(
        response: String,
        personas: List<PersonaDefinition>
    ): List<PersonaDefinition> {
        val names = response.split(",").map { it.trim().lowercase() }
        val result = mutableListOf<PersonaDefinition>()
        val remaining = personas.toMutableList()

        for (name in names) {
            val match = remaining.firstOrNull { it.persona.lowercase() in name || name in it.persona.lowercase() }
            if (match != null) {
                result.add(match)
                remaining.remove(match)
            }
        }
        // Append any personas not matched by the response
        result.addAll(remaining)
        return result
    }
}
