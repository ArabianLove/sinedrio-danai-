package com.sinedrio.danai.senate

/**
 * Represents a task that can be delegated to a Senate agent.
 *
 * @param id        Unique identifier for the task.
 * @param type      The category of work required (DEBUG, DESIGN, BUILD, EXPLAIN).
 * @param input     The code or description to work on.
 * @param delegatedBy The owner or delegate who submitted the task.
 */
data class SenateTask(
    val id: String,
    val type: TaskType,
    val input: String,
    val delegatedBy: String = "owner"
) {
    enum class TaskType {
        DEBUG,
        DESIGN,
        BUILD,
        EXPLAIN
    }
}
