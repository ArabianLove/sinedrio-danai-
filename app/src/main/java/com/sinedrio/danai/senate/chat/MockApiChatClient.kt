package com.sinedrio.danai.senate.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Offline [ApiChatClient] that generates realistic-sounding responses without
 * any network connection.
 *
 * Each persona is detected from the [systemPrompt] keyword and the reply is
 * constructed from a small vocabulary of characteristic phrases combined with
 * keywords extracted from the [userMessage].  Intended for demos, tests, and
 * situations where no API key has been configured.
 */
class MockApiChatClient : ApiChatClient {

    override suspend fun complete(
        systemPrompt: String,
        history: List<ChatMessage>,
        userMessage: String
    ): String = withContext(Dispatchers.Default) {
        // Simulate realistic API latency
        delay(400L + (Math.random() * 900).toLong())
        val persona = detectPersona(systemPrompt)
        buildResponse(persona, userMessage, history)
    }

    // ── Persona detection ─────────────────────────────────────────────────────

    private enum class Persona { DETECTIVE, VISIONARY, ENGINEER, SAGE, MODERATOR, GENERIC }

    private fun detectPersona(systemPrompt: String): Persona = when {
        "Detective" in systemPrompt -> Persona.DETECTIVE
        "Visionary" in systemPrompt -> Persona.VISIONARY
        "Engineer" in systemPrompt -> Persona.ENGINEER
        "Sage" in systemPrompt -> Persona.SAGE
        "moderator" in systemPrompt.lowercase() -> Persona.MODERATOR
        else -> Persona.GENERIC
    }

    // ── Response builder ──────────────────────────────────────────────────────

    private fun buildResponse(persona: Persona, message: String, history: List<ChatMessage>): String {
        val topic = extractTopics(message)
        val prevContext = history.lastOrNull { !it.isHuman && !it.isAutoModerator }?.content?.take(60) ?: ""

        return when (persona) {
            Persona.DETECTIVE -> buildDetectiveResponse(topic, message, prevContext)
            Persona.VISIONARY -> buildVisionaryResponse(topic, message, prevContext)
            Persona.ENGINEER -> buildEngineerResponse(topic, message, prevContext)
            Persona.SAGE -> buildSageResponse(topic, message, prevContext)
            Persona.MODERATOR -> buildModeratorQuestion(topic, message)
            Persona.GENERIC -> "That is a thoughtful point. Let me consider it from first principles: $message"
        }
    }

    // ── Per-persona response factories ────────────────────────────────────────

    private fun buildDetectiveResponse(topic: Topic, message: String, prevContext: String): String {
        val openers = listOf(
            "Something doesn't add up here.",
            "Hold on — I need to examine this more closely.",
            "I've seen this pattern before, and it rarely ends well.",
            "Before we proceed, let me ask: what are we actually assuming?",
            "The obvious explanation is almost never the right one."
        )
        val middles = when (topic) {
            Topic.DEBUGGING -> listOf(
                "The real bug is usually two layers deeper than the stack trace suggests. Have you verified the invariants at each call boundary?",
                "I'd start by eliminating the most dangerous assumption first. In my experience the null check you 'don't need' is exactly where things explode.",
                "Run the reproducer under the debugger and watch for state that changes between the last known-good moment and the failure."
            )
            Topic.ARCHITECTURE -> listOf(
                "Every architecture decision hides a trade-off someone didn't document. We need to surface those hidden costs.",
                "I don't trust a design that hasn't been stress-tested. What happens when the happy path breaks?",
                "The coupling you tolerate today becomes the incident you investigate at 2 AM."
            )
            Topic.IMPLEMENTATION -> listOf(
                "Show me the edge cases. The core path is rarely where problems live — it's the boundaries that kill you.",
                "Every assumption baked into this code is a potential crime scene. Let's enumerate them.",
                "I'd instrument this thoroughly before shipping. Silent failures are the hardest to diagnose."
            )
            Topic.GENERAL -> listOf(
                "The first thing I do is question every 'obvious' fact on the table. What's being taken for granted?",
                "There's a clue in what's *not* being said. What constraint or requirement is everyone ignoring?",
                "Before we theorise, let's gather evidence. What does the data actually tell us?"
            )
        }
        val closers = listOf(
            "The case isn't closed until every loose end is explained.",
            "I'll keep digging. The truth is in the details.",
            "Document your findings — future-you will thank present-you.",
            "Never ship code you don't fully understand."
        )
        return "${openers.random()} ${middles.random()} ${closers.random()}"
    }

    private fun buildVisionaryResponse(topic: Topic, message: String, prevContext: String): String {
        val openers = listOf(
            "The real question here isn't what to build — it's *why* this structure exists at all.",
            "I see a fascinating pattern emerging from this discussion.",
            "Step back for a moment. The problem is telling us something about its deeper shape.",
            "What if we're solving the wrong problem? Let me reframe this.",
            "Every system is a mirror of its organisational structure. What is this one reflecting?"
        )
        val middles = when (topic) {
            Topic.DEBUGGING -> listOf(
                "Bugs are symptoms of a design that's fighting against itself. Fix the architecture and the bugs often dissolve.",
                "When I see recurring errors in the same module, I ask: does the abstraction boundary sit in the right place?",
                "The best debugging is proactive design — constraints that make incorrect states unrepresentable."
            )
            Topic.ARCHITECTURE -> listOf(
                "I'd reach for a hexagonal architecture here: keep the domain pure and push all I/O to adapters at the boundary.",
                "The Strategy pattern solves this elegantly — each behaviour becomes a first-class object you can swap at runtime.",
                "Think in layers: what changes frequently vs. rarely? That boundary is your most important interface."
            )
            Topic.IMPLEMENTATION -> listOf(
                "Implementation is strategy made concrete. The pattern I see fitting here is the Builder for complex construction and an Event Bus for loose coupling.",
                "Clean Architecture demands that nothing in the domain layer knows about the delivery mechanism. Keep that boundary sacred.",
                "The Observer pattern would give you reactive updates without tight coupling — LiveData or StateFlow in the Android context."
            )
            Topic.GENERAL -> listOf(
                "All complex systems share a handful of universal patterns. Once you can name the pattern, you can apply known solutions.",
                "The map is not the territory. Our models are approximations — useful fictions that help us navigate complexity.",
                "I believe the most elegant solution is usually the simplest one that respects all the constraints."
            )
        }
        val closers = listOf(
            "Design for change — the only constant is that requirements will evolve.",
            "The architecture should make the right thing easy and the wrong thing hard.",
            "Build the simplest thing that could possibly work, then refactor toward the ideal.",
            "A good design is one you can explain in two sentences."
        )
        return "${openers.random()} ${middles.random()} ${closers.random()}"
    }

    private fun buildEngineerResponse(topic: Topic, message: String, prevContext: String): String {
        val openers = listOf(
            "Let me cut straight to implementation.",
            "Here's how I'd approach this concretely.",
            "Theory is great, but let's talk about what actually ships.",
            "From a practical standpoint, there are three steps here.",
            "I've built something similar before — here's what worked."
        )
        val middles = when (topic) {
            Topic.DEBUGGING -> listOf(
                "First: reproduce it reliably. Second: isolate the smallest failing case. Third: fix, then write a regression test that would have caught this.",
                "Add structured logging around the suspect area, deploy to staging, and capture the exact state at failure. Guessing is expensive.",
                "Binary search your way to the fault: comment out half the code path, confirm the bug disappears, then narrow from there."
            )
            Topic.ARCHITECTURE -> listOf(
                "I'd set up the module boundaries first: one Gradle module per layer. The build system enforces the dependency rules so nobody accidentally imports the wrong layer.",
                "Keep the data classes in a `:domain` module with zero Android dependencies. That way the logic unit-tests run in milliseconds on the JVM.",
                "Start with interfaces, write the tests against the interface, then fill in the implementation. Dependency inversion from day one saves enormous refactoring time."
            )
            Topic.IMPLEMENTATION -> listOf(
                "Coroutines for async, Room for local persistence, Retrofit for network. That trio covers 90 % of Android use cases with minimal boilerplate.",
                "Use sealed classes for state: `Loading`, `Success(data)`, `Error(message)`. The `when` expression then enforces exhaustive handling at compile time.",
                "Extract the business logic into pure functions first — they're trivial to test. Push side effects (IO, UI) to the edges where they belong."
            )
            Topic.GENERAL -> listOf(
                "Pragmatism beats purity. Choose the solution you can maintain at 2 AM when the on-call alert fires.",
                "Write the test before the code. It forces you to define 'done' before you start, which saves an enormous amount of rework.",
                "If it isn't monitored, it doesn't exist in production. Instrument everything that matters."
            )
        }
        val closers = listOf(
            "Write it, test it, ship it — then iterate.",
            "Done is better than perfect, but tested is non-negotiable.",
            "The best code is code that future-you can read and trust.",
            "Every abstraction has a cost. Make sure the benefit justifies it."
        )
        return "${openers.random()} ${middles.random()} ${closers.random()}"
    }

    private fun buildSageResponse(topic: Topic, message: String, prevContext: String): String {
        val openers = listOf(
            "Let us trace this back to first principles.",
            "There is ancient wisdom in software craft that speaks directly to this.",
            "The masters who came before us wrestled with exactly this question.",
            "Understanding the *why* is always more valuable than knowing the *what*.",
            "Allow me to offer a broader perspective."
        )
        val middles = when (topic) {
            Topic.DEBUGGING -> listOf(
                "Dijkstra observed that testing can show the presence of bugs, never their absence. True robustness comes from proving correctness, not merely testing it.",
                "The Zen of Python tells us: 'Errors should never pass silently. Unless explicitly silenced.' Log everything, silence nothing, understand always.",
                "Think of debugging as archaeology — you are reconstructing a sequence of events from fragments of evidence. Patience and method are your tools."
            )
            Topic.ARCHITECTURE -> listOf(
                "Conway's Law states that systems mirror the communication structures of the organisations that build them. To improve the architecture, improve the team's communication first.",
                "The principle of least astonishment: code should behave exactly as a knowledgeable reader would expect. Surprise is the enemy of maintainability.",
                "Martin Fowler's insight endures: any fool can write code a computer understands, but it takes craft to write code a human can understand."
            )
            Topic.IMPLEMENTATION -> listOf(
                "The Unix philosophy guides us well here: write programs that do one thing and do it well, then compose them. Single responsibility at every level.",
                "DRY — Don't Repeat Yourself — is not about avoiding duplication of text, but duplication of *knowledge*. Every piece of domain logic should have a single authoritative home.",
                "SOLID is not a checklist; it is a compass. The Open/Closed principle in particular: open for extension, closed for modification — the root of all good plugin architectures."
            )
            Topic.GENERAL -> listOf(
                "The deepest truth in our craft: simplicity is not the absence of effort — it is the *result* of profound effort. Strive always for clarity.",
                "Knuth warned us about premature optimisation; it remains the root of a majority of evil in software. Measure first, then act.",
                "As with all human endeavour, the process matters as much as the product. A disciplined craft produces enduring work."
            )
        }
        val closers = listOf(
            "Wisdom is knowing which rule to break and when — but first you must master the rules.",
            "The student who questions the fundamentals becomes the master who transcends them.",
            "True expertise is invisible. The best code looks obvious after the fact.",
            "We do not learn from experience; we learn from reflecting on experience."
        )
        return "${openers.random()} ${middles.random()} ${closers.random()}"
    }

    private fun buildModeratorQuestion(topic: Topic, message: String): String {
        return when (topic) {
            Topic.DEBUGGING -> moderatorQuestions.debugging.random()
            Topic.ARCHITECTURE -> moderatorQuestions.architecture.random()
            Topic.IMPLEMENTATION -> moderatorQuestions.implementation.random()
            Topic.GENERAL -> moderatorQuestions.general.random()
        }
    }

    // ── Topic detection ───────────────────────────────────────────────────────

    private enum class Topic { DEBUGGING, ARCHITECTURE, IMPLEMENTATION, GENERAL }

    private fun extractTopics(message: String): Topic {
        val lower = message.lowercase()
        return when {
            lower.containsAny("bug", "error", "crash", "debug", "fix", "issue", "fail", "exception") -> Topic.DEBUGGING
            lower.containsAny("design", "pattern", "architect", "layer", "module", "structure", "mvvm", "clean") -> Topic.ARCHITECTURE
            lower.containsAny("implement", "build", "code", "function", "class", "method", "write", "create") -> Topic.IMPLEMENTATION
            else -> Topic.GENERAL
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    // ── Moderator question bank ───────────────────────────────────────────────

    private object moderatorQuestions {
        val debugging = listOf(
            "How should we balance the cost of comprehensive logging against the performance overhead it introduces?",
            "What strategies do you use to reproduce intermittent bugs that only appear under production load?",
            "When a fix involves touching multiple systems, how do you coordinate rollout to avoid making things worse?",
            "How do you decide when a bug warrants a hotfix versus waiting for the next scheduled release?",
            "What role do feature flags play in your approach to safe bug remediation?"
        )
        val architecture = listOf(
            "At what point does a growing application justify splitting into separate microservices or modules?",
            "How do you keep architectural decisions documented in a living way so they remain discoverable as the team changes?",
            "When business requirements conflict with architectural purity, how do you find the right compromise?",
            "How do you approach migrating a legacy codebase toward a cleaner architecture without stopping feature delivery?",
            "What is the most underrated architectural principle you have found invaluable in practice?"
        )
        val implementation = listOf(
            "How do you decide how much abstraction is the right amount — neither too little nor too much?",
            "What is your philosophy on writing comments in code? When do they help, and when do they obscure?",
            "How do you handle the tension between delivering quickly and delivering cleanly?",
            "What testing strategy do you apply first: unit, integration, or end-to-end, and why?",
            "How do you approach code review to maximise knowledge sharing rather than gatekeeping?"
        )
        val general = listOf(
            "What is the single most important skill a developer should cultivate to become truly senior?",
            "How has your approach to problem-solving changed as you have grown in experience?",
            "What does 'clean code' mean to each of you — and where do your definitions diverge?",
            "How do you stay current with the rapidly changing landscape of tools and practices without spreading yourself too thin?",
            "If you could give one piece of advice to someone starting their journey in software development, what would it be?"
        )
    }
}
