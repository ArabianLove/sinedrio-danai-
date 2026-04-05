package com.sinedrio.danai.senate.chat

/**
 * Maps a [PersonaDefinition] to a concrete agent seat in the Sinedrio.
 *
 * After negotiation, each agent receives a [PersonaAssignment] that tells it
 * which role it should play for the current session.
 *
 * @param agentIndex Index of the agent in the [SenateChat.agents] list.
 * @param persona    The persona this agent should embody.
 */
data class PersonaAssignment(
    val agentIndex: Int,
    val persona: PersonaDefinition
)
