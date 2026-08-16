with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'modifier = Modifier.size(20.dp).clickable { /* TODO: Revert */ },',
    'modifier = Modifier.size(20.dp).clickable { android.widget.Toast.makeText(context, "Revert action placeholder", android.widget.Toast.LENGTH_SHORT).show() },'
)

content = content.replace(
    'modifier = Modifier.size(20.dp).clickable { /* TODO: Diff */ },',
    'modifier = Modifier.size(20.dp).clickable { android.widget.Toast.makeText(context, "Diff viewer placeholder", android.widget.Toast.LENGTH_SHORT).show() },'
)

with open('app/src/main/java/com/example/ui/chat/ChatScreen.kt', 'w') as f:
    f.write(content)

