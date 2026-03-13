package com.sinedrio.danai.senate.agents

import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.SenateAgent
import com.sinedrio.danai.senate.SenateTask
import com.sinedrio.danai.senate.SenateTask.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Senate agent responsible for **explaining** code.
 *
 * Reads the provided source, identifies its key constructs, and returns a
 * plain-language summary that any developer can understand quickly.
 */
class ExplainerAgent : SenateAgent {

    override val name: String = "Explainer"
    override val description: String =
        "Produces plain-language summaries and inline documentation for any code snippet."
    override val supportedTasks: Set<TaskType> = setOf(TaskType.EXPLAIN)

    override suspend fun process(task: SenateTask): AgentResult = withContext(Dispatchers.Default) {
        require(task.type == TaskType.EXPLAIN) {
            "ExplainerAgent only handles EXPLAIN tasks, received ${task.type}"
        }

        val src = task.input
        val lines = src.lines()

        val explanation = buildString {
            appendLine("=== Code Explanation ===")
            appendLine()

            // Package
            lines.firstOrNull { it.startsWith("package ") }?.let {
                appendLine("📦 Package  : ${it.removePrefix("package ").trim()}")
            }

            // Imports
            val imports = lines.filter { it.startsWith("import ") }
            if (imports.isNotEmpty()) {
                appendLine("📥 Imports  : ${imports.size} import(s)")
            }

            // Classes / Objects / Interfaces
            val typeDeclarations = lines.filter {
                it.contains(Regex("\\b(class|object|interface|enum class|data class)\\b"))
            }
            if (typeDeclarations.isNotEmpty()) {
                appendLine("🏛  Types     :")
                typeDeclarations.forEach { appendLine("    • ${it.trim()}") }
            }

            // Functions
            val functions = lines.filter { it.trimStart().startsWith("fun ") || it.trimStart().startsWith("suspend fun ") }
            if (functions.isNotEmpty()) {
                appendLine("⚙  Functions (${functions.size}):")
                functions.forEach { appendLine("    • ${it.trim()}") }
            }

            // Comments / KDoc
            val docLines = lines.count { it.trimStart().startsWith("//") || it.trimStart().startsWith("*") }
            appendLine()
            appendLine("📝 Documentation lines : $docLines")
            appendLine("📄 Total lines         : ${lines.size}")
            appendLine()
            appendLine("Summary:")
            appendLine(
                if (typeDeclarations.isNotEmpty() && functions.isNotEmpty())
                    "  This file defines ${typeDeclarations.size} type(s) with ${functions.size} function(s)."
                else
                    "  Code snippet processed — see details above."
            )
            appendLine()
            appendLine("✅ Explanation complete.")
        }

        AgentResult(
            taskId = task.id,
            agentName = name,
            output = explanation,
            success = true
        )
    }
}
