import re

path = 'app/src/main/java/com/example/ui/settings/omniroot/AiManagerPanelScreen.kt'
with open(path, 'r') as f:
    content = f.read()

imports = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import android.net.Uri
import android.database.Cursor
"""
content = content.replace('import androidx.compose.foundation.clickable\n', 'import androidx.compose.foundation.clickable\n' + imports)

launcher_code = """
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            
            var fileName = "local_model.gguf"
            val cursor: Cursor? = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val displayNameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        fileName = c.getString(displayNameIndex)
                    }
                }
            }
            viewModel.addLocalModel(fileName, it.toString())
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {"""
content = content.replace('    Column(modifier = Modifier.fillMaxSize()) {', launcher_code)

button_code = """
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Models", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { launcher.launch(arrayOf("*/*")) }) {
                    Text("Import .gguf")
                }
                Button(onClick = { viewModel.refreshModels() }) {
                    Text("Refresh")
                }
            }
        }"""
        
content = content.replace("""        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Available Models", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { viewModel.refreshModels() }) {
                Text("Refresh")
            }
        }""", button_code)

with open(path, 'w') as f:
    f.write(content)
