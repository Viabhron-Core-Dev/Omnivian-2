import re
path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

old_proxy = """                        val models = runBlocking { db.aiModelDao().getModelsForProvider("local_gguf").first() }
                        val modelEntity = models.firstOrNull { it.modelId == actualModelName }
                        val absolutePath = modelEntity?.description ?: actualModelName"""

new_proxy = """                        val models = runBlocking { db.aiModelDao().getAllModels().first() }
                        val modelEntity = models.firstOrNull { it.providerId == "local_gguf" && it.modelId == actualModelName }
                        val absolutePath = modelEntity?.description ?: actualModelName"""

if old_proxy in content:
    content = content.replace(old_proxy, new_proxy)
    with open(path, 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Proxy logic not found")
