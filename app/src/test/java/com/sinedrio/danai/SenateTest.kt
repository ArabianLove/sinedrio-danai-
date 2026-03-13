package com.sinedrio.danai

import com.sinedrio.danai.senate.AgentResult
import com.sinedrio.danai.senate.Senate
import com.sinedrio.danai.senate.SenateDelegate
import com.sinedrio.danai.senate.SenateTask
import com.sinedrio.danai.senate.agents.BuilderAgent
import com.sinedrio.danai.senate.agents.DebuggerAgent
import com.sinedrio.danai.senate.agents.DesignerAgent
import com.sinedrio.danai.senate.agents.ExplainerAgent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenateTest {

    private val senate = Senate()

    // ── Delegation checks ─────────────────────────────────────────────────────

    @Test(expected = IllegalStateException::class)
    fun `convene throws when delegate is inactive (blank token)`(): Unit = runBlocking {
        val inactiveDelegate = SenateDelegate(ownerName = "owner", token = "")
        val task = debugTask("val x = 1")
        senate.convene(inactiveDelegate, task)
    }

    @Test
    fun `convene succeeds with active delegate`(): Unit = runBlocking {
        val delegate = activeDelegate()
        val task = debugTask("package com.test\nval x = 1")
        val results = senate.convene(delegate, task)
        assertTrue("Expected at least one result", results.isNotEmpty())
    }

    // ── Debugger ──────────────────────────────────────────────────────────────

    @Test
    fun `DebuggerAgent reports no issues for clean code`(): Unit = runBlocking {
        val agent = DebuggerAgent()
        val task = debugTask("package com.example\nval x = 42\n")
        val result = agent.process(task)
        assertTrue(result.success)
        assertTrue(result.output.contains("No common issues detected"))
    }

    @Test
    fun `DebuggerAgent detects TODO marker`(): Unit = runBlocking {
        val agent = DebuggerAgent()
        val task = debugTask("// TODO: fix this\nval x = 1")
        val result = agent.process(task)
        assertTrue(result.success)
        assertTrue(result.output.contains("unresolved marker"))
    }

    @Test
    fun `DebuggerAgent detects unsafe non-null assertion`(): Unit = runBlocking {
        val agent = DebuggerAgent()
        val task = debugTask("val x = nullable!!\n")
        val result = agent.process(task)
        assertTrue(result.output.contains("non-null assertion"))
    }

    // ── Designer ──────────────────────────────────────────────────────────────

    @Test
    fun `DesignerAgent produces blueprint with Clean Architecture recommendation`(): Unit = runBlocking {
        val agent = DesignerAgent()
        val task = designTask("Build a login screen that talks to a REST API")
        val result = agent.process(task)
        assertTrue(result.success)
        assertTrue(result.output.contains("Clean Architecture"))
        assertTrue(result.output.contains("MVVM"))
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    @Test
    fun `BuilderAgent passes checks for minimal valid Kotlin source`(): Unit = runBlocking {
        val agent = BuilderAgent()
        val src = """
            package com.sinedrio.test
            class Foo {
                fun bar() {}
            }
        """.trimIndent()
        val task = buildTask(src)
        val result = agent.process(task)
        assertTrue(result.success)
        assertTrue(result.output.contains("✅ Code is ready for build"))
    }

    @Test
    fun `BuilderAgent fails check for empty source`(): Unit = runBlocking {
        val agent = BuilderAgent()
        val task = buildTask("")
        val result = agent.process(task)
        assertTrue(result.success) // agent itself succeeds; the check within fails
        assertTrue(result.output.contains("❌"))
    }

    // ── Explainer ─────────────────────────────────────────────────────────────

    @Test
    fun `ExplainerAgent summarises class and function count`(): Unit = runBlocking {
        val agent = ExplainerAgent()
        val src = """
            package com.sinedrio.test
            class MyClass {
                fun foo() {}
                fun bar() {}
            }
        """.trimIndent()
        val task = explainTask(src)
        val result = agent.process(task)
        assertTrue(result.success)
        assertTrue(result.output.contains("MyClass"))
        assertTrue(result.output.contains("2"))
    }

    // ── Senate routing ────────────────────────────────────────────────────────

    @Test
    fun `Senate routes DEBUG task only to DebuggerAgent`(): Unit = runBlocking {
        val results = senate.convene(activeDelegate(), debugTask("val a = 1"))
        assertEquals(1, results.size)
        assertEquals("Debugger", results.first().agentName)
    }

    @Test
    fun `Senate routes DESIGN task only to DesignerAgent`(): Unit = runBlocking {
        val results = senate.convene(activeDelegate(), designTask("design a login screen"))
        assertEquals(1, results.size)
        assertEquals("Designer", results.first().agentName)
    }

    @Test
    fun `Senate routes BUILD task only to BuilderAgent`(): Unit = runBlocking {
        val results = senate.convene(activeDelegate(), buildTask("package com.x\nclass A {}"))
        assertEquals(1, results.size)
        assertEquals("Builder", results.first().agentName)
    }

    @Test
    fun `Senate routes EXPLAIN task only to ExplainerAgent`(): Unit = runBlocking {
        val results = senate.convene(activeDelegate(), explainTask("val x = 1"))
        assertEquals(1, results.size)
        assertEquals("Explainer", results.first().agentName)
    }

    @Test
    fun `findAgent returns correct agent by name`() {
        assertNotNull(senate.findAgent("Debugger"))
        assertNotNull(senate.findAgent("Designer"))
        assertNotNull(senate.findAgent("Builder"))
        assertNotNull(senate.findAgent("Explainer"))
    }

    @Test
    fun `findAgent returns null for unknown name`() {
        val agent = senate.findAgent("NonExistentAgent")
        assertEquals(null, agent)
    }

    // ── AgentResult / SenateDelegate ─────────────────────────────────────────

    @Test
    fun `SenateDelegate with blank token is inactive`() {
        val delegate = SenateDelegate(ownerName = "alice", token = "")
        assertFalse(delegate.isActive)
    }

    @Test
    fun `SenateDelegate with non-blank token is active`() {
        val delegate = SenateDelegate(ownerName = "alice", token = "secret123")
        assertTrue(delegate.isActive)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun activeDelegate() = SenateDelegate(ownerName = "test-owner", token = "test-token")

    private fun debugTask(input: String) = SenateTask(
        id = "t1", type = SenateTask.TaskType.DEBUG, input = input
    )

    private fun designTask(input: String) = SenateTask(
        id = "t2", type = SenateTask.TaskType.DESIGN, input = input
    )

    private fun buildTask(input: String) = SenateTask(
        id = "t3", type = SenateTask.TaskType.BUILD, input = input
    )

    private fun explainTask(input: String) = SenateTask(
        id = "t4", type = SenateTask.TaskType.EXPLAIN, input = input
    )
}
