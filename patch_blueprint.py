import re

with open('BLUEPRINT.md', 'r') as f:
    bp = f.read()

# Replace the specific Phase 9 text to remove mentions of embedding OmniRoute/NodeJS/Playwright
# and reflect the new Guided WebView approach
old_text = "- **Phase 9 (Native AI Manager & Omni-Router)**: Implement a lightweight, pure-Kotlin AI Gateway. Features WebView-based automatic token extraction, local token monitoring, fallback routing (Agent Shifter), and a Foreground Service proxy. Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases)."
new_text = "- **Phase 9 (Native AI Manager & Router)**: Implement a lightweight, pure-Kotlin local AI Gateway. Features a stable, guided WebView for API key generation (Direct-to-Key), local token monitoring, fallback routing (Agent Shifter), and a Foreground Service proxy. Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases)."

bp = bp.replace(old_text, new_text)

with open('BLUEPRINT.md', 'w') as f:
    f.write(bp)
