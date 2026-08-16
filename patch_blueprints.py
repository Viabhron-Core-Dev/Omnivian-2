import re

with open('BLUEPRINT.md', 'r') as f:
    bp_content = f.read()

bp_old = """- **Phase 9 (Native AI Manager & Router)**: Implement a lightweight, pure-Kotlin local AI Gateway. Features a stable, guided WebView for API key generation (Direct-to-Key), local token monitoring, fallback routing (Agent Shifter), a Foreground Service proxy, and native on-device execution of `.gguf` models (via `llama.cpp` integration). Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases).
- **Phase 10 (Native Tool Executor)**: Implement Native Tool Executor Integrations (manage OAuth tokens for Google Drive, GitHub API calls, and native tools like Search).
- **Phase 11 (The Brain & Memory)**: Implement core Agent Logic and Diff Parsing. Connect them into the pre-built UI and tool infrastructure. Implement the Modular Memory Architecture for persistent context retention. Implement a multi-agent PR/Issue flow (Planner, Coder, Reviewer) controlled by optional thread/agent settings that utilizes local Issues/PRs like a GitHub MCP workflow to optimize token usage.
- **Phase 12 (Antigravity Orchestration)**: Build the Parallel Agent Execution (Antigravity) orchestrator and Sync logic for handling concurrent sub-agent tasks and background operations, including the multi-agent PR/Issue flow."""

bp_new = """- **Phase 9 (Native AI Manager & Router)**: Implement a lightweight, pure-Kotlin local AI Gateway inspired by OmniRoute. Features a comprehensive provider directory (API-based, Free tiers, Local), advanced analytics (total request counts, token usage, and total cost estimation), dynamic routing strategies (priority lists, combo routing for cost/speed/quality optimization), a stable guided WebView for API key generation (Direct-to-Key), a Foreground Service proxy, and native on-device execution of `.gguf` models (via `llama.cpp` integration). Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases).
- **Phase 10 (Native Tool Executor)**: Implement Native Tool Executor Integrations (manage OAuth tokens for Google Drive, GitHub API calls, and native tools like Search).
- **Phase 11 (The Brain & Memory)**: Implement core Agent Logic and Diff Parsing. Connect them into the pre-built UI and tool infrastructure. Implement the Modular Memory Architecture for persistent context retention. Implement a multi-agent PR/Issue flow (Planner, Coder, Reviewer) controlled by optional thread/agent settings that utilizes local Issues/PRs like a GitHub MCP workflow to optimize token usage.
- **Phase 12 (Antigravity Orchestration)**: Build the Parallel Agent Execution (Antigravity) orchestrator and Sync logic for handling concurrent sub-agent tasks and background operations, including the multi-agent PR/Issue flow. Integrates Antigravity CLI logic as the "AI Brain" to dynamically distribute tasks across Phase 9 providers (e.g., automatically routing background tasks to free-tier providers to save costs)."""

bp_content = bp_content.replace(bp_old, bp_new)

with open('BLUEPRINT.md', 'w') as f:
    f.write(bp_content)

with open('PHASE_9_OMNIROUTE.md', 'r') as f:
    po_content = f.read()

po_old_1 = "  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, `TokenUsage`, and `ModelRating`."
po_new_1 = "  * Define Room entities: `ApiProvider`, `ApiKey`, `FallbackChain`, `TokenUsage`, `ModelRating`, and `RequestLog` (for request counts and cost tracking)."
po_content = po_content.replace(po_old_1, po_new_1)

po_old_2 = "  * Display a visual list of active models, token health, and remaining limits."
po_new_2 = "  * Display a visual list of active models, token health, remaining limits, total request counts, and estimated total costs."
po_content = po_content.replace(po_old_2, po_new_2)

po_old_3 = "  * Create the retry mechanism that instantly shifts the request to the next provider/key in the FallbackChain upon failure."
po_new_3 = "  * Create the retry mechanism that instantly shifts the request to the next provider/key in the FallbackChain upon failure.\n  * Implement advanced routing strategies: Priority Lists (forced ordering) and Combo Routing (automatic selection for speed, cost, or quality)."
po_content = po_content.replace(po_old_3, po_new_3)

with open('PHASE_9_OMNIROUTE.md', 'w') as f:
    f.write(po_content)
