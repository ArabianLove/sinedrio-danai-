package com.sinedrio.danai.senate

import com.sinedrio.danai.senate.agents.BuilderAgent
import com.sinedrio.danai.senate.agents.DebuggerAgent
import com.sinedrio.danai.senate.agents.DesignerAgent
import com.sinedrio.danai.senate.agents.ExplainerAgent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * The **Senate** — a parliament of the best AI agents on the platform.
 *
 * The Senate maintains a roster of specialised [SenateAgent]s and routes each
 * incoming [SenateTask] to every agent that supports the requested task type.
 * Tasks may only be submitted through a valid (active) [SenateDelegate], ensuring
 * that all agents are 100% under the owner's authority.
 *
 * The Senate never gives up: if any agent encounters an unexpected error it is
 * caught, wrapped in a failed [AgentResult], and the session continues until
 * every capable agent has responded.
 *
 * Usage:
 * ```kotlin
 * val delegate = SenateDelegate(ownerName = "Dan", token = "secret")
 * val senate   = Senate()
 * val results  = senate.convene(delegate, task)
 * ```
 */
class Senate {

    /** Immutable roster of agents seated in this Senate. */
    val agents: List<SenateAgent> = listOf(
        DebuggerAgent(),
        DesignerAgent(),
        BuilderAgent(),
        ExplainerAgent()
    )

    /**
     * Convene the Senate for a single [task].
     *
     * All agents whose [SenateAgent.supportedTasks] include [task]'s type are
     * invoked concurrently.  Results are collected and returned in the order
     * agents are seated.
     *
     * @param delegate  Proof of owner delegation — must be active.
     * @param task      The work to be performed.
     * @return          One [AgentResult] per participating agent.
     * @throws IllegalStateException if the delegate is not active.
     */
    suspend fun convene(delegate: SenateDelegate, task: SenateTask): List<AgentResult> {
        check(delegate.isActive) {
            "Senate: delegate for '${delegate.ownerName}' is not active. " +
                "Only owner-authorised delegates may submit tasks."
        }

        val eligible = agents.filter { task.type in it.supportedTasks }

        return coroutineScope {
            eligible.map { agent ->
                async {
                    runCatching { agent.process(task) }.getOrElse { error ->
                        AgentResult(
                            taskId = task.id,
                            agentName = agent.name,
                            output = "Agent encountered an error: ${error.message}",
                            success = false
                        )
                    }
                }
            }.map { it.await() }
        }
    }

    /**
     * Returns the agent with the given [name], or `null` if no such agent is seated.
     */
    fun findAgent(name: String): SenateAgent? = agents.firstOrNull { it.name == name }
}
