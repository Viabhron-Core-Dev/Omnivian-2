import re
path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

state_flow_decl = """    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.asStateFlow(_isRefreshing)
"""

content = re.sub(r'class AiManagerViewModel\(application: Application\) : AndroidViewModel\(application\) \{',
                 lambda m: m.group(0) + '\n' + state_flow_decl, 
                 content)

with open(path, 'w') as f:
    f.write(content)
