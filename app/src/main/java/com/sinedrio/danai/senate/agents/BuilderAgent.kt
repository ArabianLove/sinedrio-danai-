package com.sinedrio.danai.senate.agents

import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.SenateAgent
import com.sinedrio.danai.senate.SenateTask
import com.sinedrio.danai.senate.SenateTask.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Senate agent responsible for **building** code.
 *
 * Validates that the provided source satisfies basic structural requirements and
 * generates a build checklist so nothing is missed before release.
 */
class BuilderAgent : SenateAgent {

    override val name: String = "Builder"
    override val description: String =
        "Validates code structure, generates build checklists, and ensures release readiness."
    override val supportedTasks: Set<TaskType> = setOf(TaskType.BUILD)

    override suspend fun process(task: SenateTask): AgentResult = withContext(Dispatchers.Default) {
        require(task.type == TaskType.BUILD) {
            "BuilderAgent only handles BUILD tasks, received ${task.type}"
        }

        val report = buildString {
            appendLine("=== Build Report ===")

            val src = task.input
            var passed = 0
            var failed = 0

            fun check(label: String, condition: Boolean) {
                if (condition) {
                    appendLine("  ✅ $label")
                    passed++
                } else {
                    appendLine("  ❌ $label")
                    failed++
                }
            }

            appendLine()
            appendLine("Structural checks:")
            check("Source is non-empty", src.isNotBlank())
            check("Contains package declaration", src.contains(Regex("^package\\s+", RegexOption.MULTILINE)))
            check("No syntax: missing closing braces",
                src.count { it == '{' } == src.count { it == '}' })
            check("No syntax: mismatched parentheses",
                src.count { it == '(' } == src.count { it == ')' })
            check("No debug log left in code",
                !src.contains(Regex("Log\\.d\\(|System\\.out\\.print(ln)?\\(")))

            appendLine()
            appendLine("Build checklist:")
            appendLine("  [ ] Unit tests pass")
            appendLine("  [ ] Lint checks pass")
            appendLine("  [ ] ProGuard / R8 configured for release")
            appendLine("  [ ] Version code / name updated")
            appendLine("  [ ] Signing config set")
            appendLine()
            appendLine("Result: $passed checks passed, $failed checks failed.")
            if (failed == 0) {
                appendLine("✅ Code is ready for build.")
            } else {
                appendLine("⚠  Address failed checks before releasing.")
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
