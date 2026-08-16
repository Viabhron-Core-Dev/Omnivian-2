import re
path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

old_proxy = """                        // We use the model name as the URI since we stored it in the DB (or could fetch it)
                        val llama = com.example.engine.omniroot.local.LlamaEngine(context)
                        val loaded = llama.loadModelSafely(actualModelName)"""

new_proxy = """                        // Retrieve the absolute path stored during import
                        val models = runBlocking { db.aiModelDao().getModelsForProvider("local_gguf").first() }
                        val modelEntity = models.firstOrNull { it.modelId == actualModelName }
                        val absolutePath = modelEntity?.description ?: actualModelName
                        
                        val llama = com.example.engine.omniroot.local.LlamaEngine(context)
                        val loaded = llama.loadModelSafely(absolutePath)"""

if old_proxy in content:
    content = content.replace(old_proxy, new_proxy)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Proxy logic not found")
