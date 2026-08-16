import re

with open('BLUEPRINT.md', 'r') as f:
    content = f.read()

old_text = "- **Phase 9 (OmniRoute Android Host)**: Implement OmniRoute AI Gateway via `nodejs-mobile` Foreground Service. Custom-built for low-end `arm64-v8a` devices (3GB RAM Android Go) with persistent notification and local WebView dashboard. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases)."
new_text = "- **Phase 9 (Native Omni-Router)**: Implement a lightweight, pure-Kotlin AI Gateway. Features WebView-based automatic token extraction, local token monitoring, fallback routing (Agent Shifter), and a Foreground Service proxy. Optimized for 3GB RAM Android Go. (See `PHASE_9_OMNIROUTE.md` for detailed sub-phases)."

content = content.replace(old_text, new_text)

with open('BLUEPRINT.md', 'w') as f:
    f.write(content)
