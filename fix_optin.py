with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

if '@OptIn(ExperimentalMaterial3Api::class)' not in content:
    content = content.replace(
        '@Composable\nfun OmniRouteApp()',
        '@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun OmniRouteApp()'
    )

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

