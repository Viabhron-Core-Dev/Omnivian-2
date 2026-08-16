import re

path = 'app/src/main/cpp/llama_bridge.cpp'
with open(path, 'r') as f:
    content = f.read()

# Fix broken newlines in sendError strings
content = content.replace('sendError("\n', 'sendError("\\n')
content = content.replace('\n");', '\\n");')

with open(path, 'w') as f:
    f.write(content)
