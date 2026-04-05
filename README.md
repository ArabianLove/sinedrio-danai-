# Sinedrio Danai — The AI Senate

> *"You must build and maintain the Senate, an assembly, another council, synod, diet, a parliament
> composed of the main best artificial intelligences of the planet."*

## Overview

**Sinedrio Danai** is an Android application that implements an **AI Senate** — a parliament of
specialised AI agents that collaborate to produce perfect code.  Every agent is 100% delegated by
the owner and never stops until the goal is achieved.

The Sinedrio features a **solemn, aulic** dark interface — gold accents on a deep-night background — designed to evoke the gravity and ceremony of a true senate chamber.

### Senate Members (Chat Personas)

| Persona | Character | Voice |
|---------|-----------|-------|
| **Il Detective** | The Detective | Sceptical investigator — hunts edge-cases, questions assumptions |
| **Il Visionario** | The Visionary | Creative architect — sees the big picture, loves design patterns |
| **L'Ingegnere** | The Engineer | Pragmatic builder — concrete implementations, always mentions tests |
| **Il Saggio** | The Sage | Wise teacher — first principles, CS pioneers, historical context |

### Recommended LLM Members

The Sinedrio is designed to host any LLM behind an OpenAI-compatible API.
These models are pre-configured in `SenateMembers`:

| Model | Provider | Strength |
|-------|----------|----------|
| **GPT-4o** | OpenAI | Flagship — excels at reasoning, analysis, creative tasks |
| **GPT-4o mini** | OpenAI | Cost-efficient — fast and capable for high-throughput rounds |
| **Claude Sonnet 4** | Anthropic | Nuanced reasoning, long context, careful analysis |
| **Claude Haiku 4** | Anthropic | Fastest Anthropic model — excellent for rapid debate |
| **Gemini 2.5 Pro** | Google | Strong at code, mathematics, multi-step reasoning |
| **Gemini 2.5 Flash** | Google | Low-latency — keeps the conversation flowing |
| **Llama 3.1** | Meta / Ollama | Open-weight, privacy-first, no API cost |
| **Mistral** | Mistral / Ollama | Compact European model — fast on modest hardware |

> **Tip**: Use the overflow menu → *API Settings* to connect any model at runtime.

### Batch Agents (Classic Senate)

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
│   ├── agents/              # Concrete batch-processing agents
│   └── chat/
│       ├── SenateChat.kt        # Orchestrates multi-turn conversations
│       ├── ChatSenateAgent.kt   # Contract for conversational agents
│       ├── ChatSession.kt       # Immutable session model
│       ├── ChatMessage.kt       # Single message in a session
│       ├── AutoModerator.kt     # Auto-generated follow-up questions
│       ├── PersonaDefinition.kt # Persona catalogue (decoupled from agents)
│       ├── PersonaNegotiator.kt # Dynamic role assignment at session start
│       ├── DynamicChatAgent.kt  # Agent with runtime-assigned persona
│       ├── SenateMembers.kt     # Registry of recommended LLM models
│       ├── ApiChatClient.kt     # API contract
│       ├── OpenAiChatClient.kt  # OpenAI-compatible HTTP client
│       ├── MockApiChatClient.kt # Offline mock for demos/tests
│       ├── agents/              # Fixed-persona agent implementations
│       └── persistence/
│           ├── SessionRepository.kt       # Storage contract
│           ├── InMemorySessionRepository.kt # Local fallback
│           ├── RemoteSessionRepository.kt   # External server storage
│           └── SessionSerializer.kt         # JSON serialisation
└── ui/
    ├── MainActivity.kt      # Task-submission screen
    ├── SenateActivity.kt    # Senate roster + session results
    ├── ChatActivity.kt      # Sinedrio chat room
    ├── ChatViewModel.kt     # MVVM ViewModel (persistence + negotiation)
    └── Adapters…
```

## Key Features

### 🗄️ Persistent Memory (Session Storage)

Sessions can be saved to and loaded from an **external server**, so that
conversations can be resumed across app restarts and devices.

- **In-memory** (default): sessions live in process memory.
- **Remote server**: configure via overflow menu → *Session Server*.

The server API is a simple REST protocol:

| Method | Path | Description |
|--------|------|-------------|
| `PUT` | `{baseUrl}/{id}` | Save a session (JSON body) |
| `GET` | `{baseUrl}/{id}` | Load a session |
| `GET` | `{baseUrl}` | List all sessions |
| `DELETE` | `{baseUrl}/{id}` | Delete a session |

### 🎭 Dynamic Persona Negotiation

Instead of hard-coding which model plays which role, the **PersonaNegotiator**
lets the moderator decide at session start:

| Mode | Behaviour |
|------|-----------|
| **Default** | Fixed roles (Detective → Visionary → Engineer → Sage) |
| **Shuffle** | Random persona assignment each session |
| **Negotiate** | The moderator API proposes the best assignment based on the topic |

Configure via overflow menu → *Persona Assignment*.

### 🏛️ Solemn Dark UI

The interface uses a **dark ceremonial aesthetic** — deep night background
(`#0D0D1A`) with gold accents (`#D4AF37`), evoking an aulic senate chamber.
Simple, linear, and dignified.

## How to Use

1. Enter your **Owner Name** and **Delegation Token** in the main screen.
2. Select the **task type** (Debug, Design, Build, or Explain).
3. Paste your code or describe your requirements in the input field.
4. Tap **Convene the Senate** — the relevant agent processes the task immediately.
5. Results appear on the **Senate** screen.

### Sinedrio Chat

1. Tap **Open Sinedrio Chat** on the main screen.
2. The four AI personas greet you with their initial perspectives.
3. Type a message and tap **Send** — all agents respond concurrently.
4. Tap **Auto** to let the auto-moderator generate the next question.
5. Use the overflow menu to:
   - Configure a real API key and model.
   - Connect a session server for persistent storage.
   - Choose persona assignment mode (Default / Shuffle / Negotiate).

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