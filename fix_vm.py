path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

import re

# Insert isRefreshing flow
state_flow_decl = """    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
"""
content = re.sub(r'class AiManagerViewModel\([^)]*\)\s*:\s*ViewModel\(\)\s*\{', 
                 lambda m: m.group(0) + '\n' + state_flow_decl, 
                 content)

# Update refreshModels
old_refresh = """    fun refreshModels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {"""

new_refresh = """    fun refreshModels() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isRefreshing.value = true
            try {"""

content = content.replace(old_refresh, new_refresh)

# Add finally to refreshModels
# We need to find the end of the try block in refreshModels.
# A simple way is to replace `// Note: Error handling...` or whatever is at the end.
