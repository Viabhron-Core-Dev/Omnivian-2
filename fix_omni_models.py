import re
path = 'app/src/main/java/com/example/ui/chat/OmniRootClient.kt'
with open(path, 'r') as f:
    content = f.read()

new_models = """@JsonClass(generateAdapter = true)
data class OmniMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<OmniToolCall>? = null,
    val tool_call_id: String? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class OmniToolCall(
    val id: String,
    val type: String = "function",
    val function: OmniFunctionCall
)

@JsonClass(generateAdapter = true)
data class OmniFunctionCall(
    val name: String,
    val arguments: String
)

@JsonClass(generateAdapter = true)
data class OmniTool(
    val type: String = "function",
    val function: OmniFunctionDef
)

@JsonClass(generateAdapter = true)
data class OmniFunctionDef(
    val name: String,
    val description: String? = null,
    val parameters: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class OmniRequest(
    val model: String = "omni-default",
    val messages: List<OmniMessage>,
    val tools: List<OmniTool>? = null
)"""

# replace the old models
content = re.sub(r'@JsonClass\(generateAdapter = true\)\ndata class OmniMessage.*?data class OmniRequest\(\s*val model: String = "omni-default",\s*val messages: List<OmniMessage>\s*\)', new_models, content, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(content)
