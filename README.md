# Sinedrio Danai — The AI Senate

> *"You must build and maintain the Senate, an assembly, another council, synod, diet, a parliament
> composed of the main best artificial intelligences of the planet."*

## Overview

**Sinedrio Danai** is an Android application that implements an **AI Senate** — a parliament of
specialised AI agents that collaborate to produce perfect code.  Every agent is 100% delegated by
the owner and never stops until the goal is achieved.

### Senate Members

| Agent | Specialisation |
|-------|---------------|
| **Debugger** | Finds bugs, unsafe patterns, and logic flaws |
| **Designer** | Produces architecture blueprints and design-pattern recommendations |
| **Builder** | Validates code structure and generates release checklists |
| **Explainer** | Creates plain-language summaries and inline documentation |

## Architecture

The project follows **Clean Architecture** with an MVVM UI layer:

```
app/src/main/java/com/sinedrio/danai/
├── senate/
│   ├── Senate.kt            # Orchestrator — convenes agents for a task
│   ├── SenateAgent.kt       # Interface every agent must implement
│   ├── SenateDelegate.kt    # Owner-delegation authority token
│   ├── SenateTask.kt        # Task model (DEBUG / DESIGN / BUILD / EXPLAIN)
│   ├── AgentResult.kt       # Result model returned by each agent
│   └── agents/
│       ├── DebuggerAgent.kt
│       ├── DesignerAgent.kt
│       ├── BuilderAgent.kt
│       └── ExplainerAgent.kt
└── ui/
    ├── MainActivity.kt      # Task-submission screen
    ├── SenateActivity.kt    # Senate roster + session results
    ├── SenateViewModel.kt   # MVVM ViewModel
    └── AgentResultAdapter.kt
```

## How to Use

1. Enter your **Owner Name** and **Delegation Token** in the main screen.
2. Select the **task type** (Debug, Design, Build, or Explain).
3. Paste your code or describe your requirements in the input field.
4. Tap **Convene the Senate** — the relevant agent processes the task immediately.
5. Results appear on the **Senate** screen.

## Building

```bash
./gradlew assembleDebug
```

## Running Tests

```bash
./gradlew test
```

## License

MIT — see [LICENSE](LICENSE).