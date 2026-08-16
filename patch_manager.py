import re

# Update BLUEPRINT.md
with open('BLUEPRINT.md', 'r') as f:
    bp = f.read()

bp = bp.replace('OmniRoute Dashboard', 'AI Manager Panel')
bp = bp.replace('**OmniRoute**: OmniRoute settings page', '**AI Manager**: AI Manager settings page')
bp = bp.replace('- **Phase 9 (Native Omni-Router)**', '- **Phase 9 (Native AI Manager & Omni-Router)**')

with open('BLUEPRINT.md', 'w') as f:
    f.write(bp)

# Update PHASE_9_OMNIROUTE.md
with open('PHASE_9_OMNIROUTE.md', 'r') as f:
    p9 = f.read()

p9 = p9.replace('OmniRoute Dashboard', 'AI Manager Panel')
p9 = p9.replace('### Phase 9.5: Omni-Router Management UI', '### Phase 9.5: AI Manager Panel & Provider Directory')

directory_tasks = """* **Objective:** Allow the user to manage their keys, fallback chains, and access a comprehensive directory of AI providers.
* **Tasks:**
  * Build a Jetpack Compose dashboard screen (The AI Manager Panel).
  * **Provider Directory:** Pre-populate the app with a comprehensive list of AI providers (e.g., Google AI Studio, OpenRouter, Groq, Together AI, Cohere, HuggingFace, etc.), mirroring the extensive list found in OmniRoute.
  * Show a prioritized list of fallback providers.
  * Provide buttons to trigger the Phase 9.1 WebView login flows for automatic key extraction from the directory.
  * Display visual status indicators for active rate limits and token health."""

p9 = re.sub(r'\* \*\*Objective:\*\* Allow the user to manage their keys and fallback chains\..*?token health\.', directory_tasks, p9, flags=re.DOTALL)

with open('PHASE_9_OMNIROUTE.md', 'w') as f:
    f.write(p9)
