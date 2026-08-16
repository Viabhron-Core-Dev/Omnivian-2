import re

with open('BLUEPRINT.md', 'r') as f:
    bp = f.read()

old_text = "- **AI Manager Panel**: AI Manager settings page."
new_text = "- **AI Manager Panel**: A professional, tabbed dashboard (Directory, Active Keys, Available Models, Token Counter, Model Rater) inspired by OmniRoute."

bp = bp.replace(old_text, new_text)

with open('BLUEPRINT.md', 'w') as f:
    f.write(bp)
