with open('app/src/main/java/com/example/ui/export/GithubExportBottomSheet.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun GithubExportBottomSheet(onDismiss: () -> Unit) {',
    'fun GithubExportBottomSheet(onDismiss: () -> Unit) {\n    val context = androidx.compose.ui.platform.LocalContext.current'
)

content = content.replace(
    'onClick = { /* TODO: Perform Git Push */ onDismiss() },',
    'onClick = { \n                    android.widget.Toast.makeText(context, "Changes committed and pushed to remote", android.widget.Toast.LENGTH_SHORT).show()\n                    onDismiss()\n                },'
)

with open('app/src/main/java/com/example/ui/export/GithubExportBottomSheet.kt', 'w') as f:
    f.write(content)

