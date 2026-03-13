package com.sinedrio.danai.senate.agents

import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.SenateAgent
import com.sinedrio.danai.senate.SenateTask
import com.sinedrio.danai.senate.SenateTask.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Senate agent responsible for **designing** code architecture.
 *
 * Reviews requirements described in the task input and produces a high-level
 * design blueprint including recommended patterns and component boundaries.
 */
class DesignerAgent : SenateAgent {

    override val name: String = "Designer"
    override val description: String =
        "Creates architecture blueprints, recommends design patterns, and defines component boundaries."
    override val supportedTasks: Set<TaskType> = setOf(TaskType.DESIGN)

    override suspend fun process(task: SenateTask): AgentResult = withContext(Dispatchers.Default) {
        require(task.type == TaskType.DESIGN) {
            "DesignerAgent only handles DESIGN tasks, received ${task.type}"
        }

        val blueprint = buildString {
            appendLine("=== Architecture Blueprint ===")
            appendLine("Requirements summary:")
            appendLine("  ${task.input.take(200)}${if (task.input.length > 200) "…" else ""}")
            appendLine()
            appendLine("Recommended Architecture: Clean Architecture (MVVM)")
            appendLine()
            appendLine("Layers:")
            appendLine("  1. Presentation  — Activities / Fragments + ViewModels")
            appendLine("  2. Domain        — Use Cases + Repository interfaces")
            appendLine("  3. Data          — Repository implementations + data sources")
            appendLine()
            appendLine("Key Design Patterns:")
            appendLine("  • Repository   — abstracts data sources from the domain layer")
            appendLine("  • Observer     — LiveData / StateFlow for reactive UI updates")
            appendLine("  • Factory      — agent creation inside Senate")
            appendLine("  • Strategy     — each SenateAgent is an interchangeable strategy")
            appendLine()
            appendLine("Component Boundaries:")
            appendLine("  senate/       — orchestration core (Senate, SenateAgent, SenateDelegate)")
            appendLine("  senate/agents/— concrete agent implementations")
            appendLine("  ui/           — Android UI layer")
            appendLine()
            appendLine("✅ Blueprint ready. Proceed to BuilderAgent for implementation.")
        }

        AgentResult(
            taskId = task.id,
            agentName = name,
            output = blueprint,
            success = true
        )
    }
}
