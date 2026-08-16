import re
path = 'app/src/main/java/com/example/engine/omniroot/service/OmniRootProxyServer.kt'
with open(path, 'r') as f:
    content = f.read()

bad_code = """                            val localResponse = OmniResponse(
                                id = "chatcmpl-local",
                                model = actualModelName,
                                choices = listOf(
                                    OmniChoice(
                                        index = 0,
                                        message = OmniMessage("assistant", prediction),
                                        finish_reason = "stop"
                                    )
                                )
                            )"""

good_code = """                            val localResponse = OmniResponse(
                                choices = listOf(
                                    OmniChoice(
                                        message = OmniMessage("assistant", prediction)
                                    )
                                )
                            )"""

content = content.replace(bad_code, good_code)

with open(path, 'w') as f:
    f.write(content)
