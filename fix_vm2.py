path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

import re

# Insert isRefreshing flow
state_flow_decl = """    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRefreshing.asStateFlow()
"""
content = re.sub(r'class AiManagerViewModel\([^)]*\)\s*:\s*ViewModel\(\)\s*\{', 
                 lambda m: m.group(0) + '\n' + state_flow_decl, 
                 content)

# Update refreshModels to set isRefreshing properly with finally
old_refresh = """    fun refreshModels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {"""

new_refresh = """    fun refreshModels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRefreshing.value = true
            try {"""

content = content.replace(old_refresh, new_refresh)

content = content.replace("""            } catch (e: Exception) {
                Log.e("AiManager", "Error in refreshModels", e)
            }
        }
    }""", """            } catch (e: Exception) {
                Log.e("AiManager", "Error in refreshModels", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }""")

with open(path, 'w') as f:
    f.write(content)
