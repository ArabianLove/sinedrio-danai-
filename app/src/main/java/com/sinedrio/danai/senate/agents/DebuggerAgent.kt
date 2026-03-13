package com.sinedrio.danai.senate.agents

import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.SenateAgent
import com.sinedrio.danai.senate.SenateTask
import com.sinedrio.danai.senate.SenateTask.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Senate agent responsible for **debugging** code.
 *
 * Analyses the submitted code snippet for common issues, reports findings, and
 * suggests fixes so the final code is error-free.
 */
class DebuggerAgent : SenateAgent {

    override val name: String = "Debugger"
    override val description: String =
        "Analyses code for bugs, runtime errors, and logical flaws, then proposes fixes."
    override val supportedTasks: Set<TaskType> = setOf(TaskType.DEBUG)

    override suspend fun process(task: SenateTask): AgentResult = withContext(Dispatchers.Default) {
        require(task.type == TaskType.DEBUG) {
            "DebuggerAgent only handles DEBUG tasks, received ${task.type}"
        }

        val report = buildString {
            appendLine("=== Debug Report ===")
            appendLine("Input length : ${task.input.length} characters")
            appendLine()

            val lines = task.input.lines()
            var issueCount = 0

            lines.forEachIndexed { index, line ->
                val lineNo = index + 1
                // Detect common patterns that suggest issues
                if (line.contains("TODO") || line.contains("FIXME")) {
                    appendLine("⚠  Line $lineNo — unresolved marker: ${line.trim()}")
                    issueCount++
                }
                if (line.trimEnd().endsWith("!!")) {
                    appendLine("⚠  Line $lineNo — unsafe non-null assertion (!!) detected: ${line.trim()}")
                    issueCount++
                }
                if (line.contains("catch (e: Exception)") && !line.contains("Log")) {
                    appendLine("⚠  Line $lineNo — broad exception catch without logging: ${line.trim()}")
                    issueCount++
                }
                if (line.contains("Thread.sleep")) {
                    appendLine("⚠  Line $lineNo — blocking Thread.sleep found; prefer coroutine delay: ${line.trim()}")
                    issueCount++
                }
            }

            if (issueCount == 0) {
                appendLine("✅ No common issues detected. Code looks clean.")
            } else {
                appendLine()
                appendLine("Total issues found: $issueCount")
                appendLine("Recommendation: Address each warning above before building.")
            }
        }

        AgentResult(
            taskId = task.id,
            agentName = name,
            output = report,
            success = true
        )
    }
}
