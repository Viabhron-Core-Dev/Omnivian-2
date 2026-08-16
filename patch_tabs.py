import re

with open('PHASE_9_OMNIROUTE.md', 'r') as f:
    content = f.read()

# Update 9.1
old_9_1 = """### Phase 9.1: Core Data Layer (Room Database)
* **Objective:** Establish the persistent storage for providers, keys, and metrics.
* **Tasks:**
  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, and `TokenUsage`. Ensure `ApiKey` supports multiple distinct keys/accounts per `ApiProvider` (e.g., multiple Google accounts for Google AI Studio).
  * Include an `alias` field in the `ApiKey` entity for user-friendly naming (e.g., "Personal", "Work").
  * Pre-populate the database with the directory of known AI providers (Google AI Studio, OpenRouter, Groq, Together AI, HuggingFace, etc.) and their API deep links."""

new_9_1 = """### Phase 9.1: Core Data Layer (Room Database)
* **Objective:** Establish the persistent storage for providers, keys, metrics, and ratings.
* **Tasks:**
  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, `TokenUsage`, and `ModelRating`.
  * Ensure `ApiKey` supports multiple distinct keys/accounts per `ApiProvider` (e.g., multiple Google accounts for Google AI Studio).
  * Include an `alias` field in the `ApiKey` entity for user-friendly naming (e.g., "Personal", "Work").
  * Pre-populate the database with a massive directory of known AI providers (Google AI Studio, OpenAI, Anthropic, OpenRouter, Groq, Together AI, Mistral, Cohere, DeepSeek, Fireworks, Perplexity, Anyscale, Replicate, HuggingFace, Novita, etc.) and their API deep links."""

content = content.replace(old_9_1, new_9_1)

# Update 9.2
old_9_2 = """### Phase 9.2: AI Manager Panel UI (Global Settings)
* **Objective:** Build the main management interface.
* **Tasks:**
  * Create a Jetpack Compose screen in the Global Settings.
  * Display the comprehensive Provider Directory, allowing users to add multiple accounts/keys under the exact same provider.
  * **Privacy/Security UI:** Never display the raw API key text in the UI. Keys must be strictly masked (e.g., `sk-...abcd`) or hidden completely, displaying only the user-defined alias (e.g., "Personal Gemini").
  * Build the UI for configuring fallback chains (priority drag-and-drop or list selection)."""

new_9_2 = """### Phase 9.2: AI Manager Panel UI (Professional Dashboard)
* **Objective:** Build the main management interface with a professional, tabbed layout inspired by OmniRoute.
* **Tasks:**
  * Create a Jetpack Compose dashboard screen in the Global Settings with the following tabs:
    * **Directory Tab:** The massive list of all available AI providers. Provides deep links to their login pages for Phase 9.3 integration.
    * **Active Keys Tab:** Shows configured accounts. Allows multiple accounts per provider. Includes separated "Local AI" instances (which can also have multiple configured endpoints/GGUFs). **Privacy/Security UI:** Never display the raw API key text; use strict masking (`sk-...abcd`) and aliases.
    * **Available Models Tab:** Aggregates and lists all models currently available through the active keys.
    * **Token Counter Tab:** Visualizes token usage across models and providers (pulling from `TokenUsage`).
    * **Model Rater Tab:** Displays aggregated stats (thumbs up/down) to judge the overall strength of different AI models (pulling from `ModelRating`)."""

content = content.replace(old_9_2, new_9_2)

# Add chat rating UI note to 9.4 or 9.5
# Let's just create 9.11 for Chat Rating Integration
content += """
### Phase 9.11: Chat Rating Integration
* **Objective:** Allow users to rate AI responses to feed the Model Rater Tab.
* **Tasks:**
  * Add Thumbs Up and Thumbs Down buttons to AI message bubbles in the Chat View.
  * On click, log the rating to the `ModelRating` table, associating it with the specific model that generated the response.
"""

with open('PHASE_9_OMNIROUTE.md', 'w') as f:
    f.write(content)
