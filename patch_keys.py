import re

with open('PHASE_9_OMNIROUTE.md', 'r') as f:
    content = f.read()

# Edit 9.1
old_9_1 = """### Phase 9.1: Core Data Layer (Room Database)
* **Objective:** Establish the persistent storage for providers, keys, and metrics.
* **Tasks:**
  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, and `TokenUsage`.
  * Pre-populate the database with the directory of known AI providers (Google AI Studio, OpenRouter, Groq, Together AI, HuggingFace, etc.) and their API deep links."""
new_9_1 = """### Phase 9.1: Core Data Layer (Room Database)
* **Objective:** Establish the persistent storage for providers, keys, and metrics.
* **Tasks:**
  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, and `TokenUsage`. Ensure `ApiKey` supports multiple distinct keys/accounts per `ApiProvider` (e.g., multiple Google accounts for Google AI Studio).
  * Include an `alias` field in the `ApiKey` entity for user-friendly naming (e.g., "Personal", "Work").
  * Pre-populate the database with the directory of known AI providers (Google AI Studio, OpenRouter, Groq, Together AI, HuggingFace, etc.) and their API deep links."""

content = content.replace(old_9_1, new_9_1)

# Edit 9.2
old_9_2 = """### Phase 9.2: AI Manager Panel UI (Global Settings)
* **Objective:** Build the main management interface.
* **Tasks:**
  * Create a Jetpack Compose screen in the Global Settings.
  * Display the comprehensive Provider Directory.
  * Build the UI for configuring fallback chains (priority drag-and-drop or list selection)."""
new_9_2 = """### Phase 9.2: AI Manager Panel UI (Global Settings)
* **Objective:** Build the main management interface.
* **Tasks:**
  * Create a Jetpack Compose screen in the Global Settings.
  * Display the comprehensive Provider Directory, allowing users to add multiple accounts/keys under the exact same provider.
  * **Privacy/Security UI:** Never display the raw API key text in the UI. Keys must be strictly masked (e.g., `sk-...abcd`) or hidden completely, displaying only the user-defined alias (e.g., "Personal Gemini").
  * Build the UI for configuring fallback chains (priority drag-and-drop or list selection)."""

content = content.replace(old_9_2, new_9_2)

with open('PHASE_9_OMNIROUTE.md', 'w') as f:
    f.write(content)
