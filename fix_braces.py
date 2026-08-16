with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerViewModel.kt', 'r') as f:
    content = f.read()
    
# Let's just find the last closing brace and ensure it encloses rateModel
import re

# find the last '}'
idx = content.rfind('}')
if idx != -1:
    print("Found last brace at", idx)
