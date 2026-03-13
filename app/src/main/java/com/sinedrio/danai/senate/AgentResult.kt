package com.sinedrio.danai.senate

/**
 * Represents the outcome of a task processed by a Senate agent.
 *
 * @param taskId    ID of the original [SenateTask].
 * @param agentName Name of the agent that produced this result.
 * @param output    The agent's response or processed output.
 * @param success   Whether the agent completed the task without errors.
 */
data class AgentResult(
    val taskId: String,
    val agentName: String,
    val output: String,
    val success: Boolean
)
