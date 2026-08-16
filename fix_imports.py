import re
path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

imports = """import com.example.ui.chat.OmniRequest
import com.example.ui.chat.OmniResponse
import com.example.ui.chat.OmniMessage
import com.example.ui.chat.OmniChoice"""

content = content.replace('import com.example.ui.chat.OmniRequest', imports)

with open(path, 'w') as f:
    f.write(content)
print("Fixed imports")
