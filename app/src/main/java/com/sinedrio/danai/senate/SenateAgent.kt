package com.sinedrio.danai.senate

/**
 * Contract that every member of the Senate must fulfil.
 *
 * Agents are specialised intelligences that handle one or more [SenateTask.TaskType]s.
 * Each agent is 100% delegated by the owner via [SenateDelegate] and must always
 * produce a result — never give up until the code is perfect.
 */
interface SenateAgent {

    /** Human-readable name shown in the Senate roster. */
    val name: String

    /** Short description of what this agent specialises in. */
    val description: String

    /** Task types this agent can handle. */
    val supportedTasks: Set<SenateTask.TaskType>

    /**
     * Process [task] and return a result.
     *
     * Implementations must be non-blocking (use suspend) so that the Senate can
     * run multiple agents concurrently.
     */
    suspend fun process(task: SenateTask): AgentResult
}
