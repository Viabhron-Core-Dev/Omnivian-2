import re

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'r') as f:
    content = f.read()

# Remove Revert Dialog state
content = content.replace("    var showRevertDialog by remember { mutableStateOf(false) }\n", "")

# Remove Menu item for Revert
menu_revert = """                                    DropdownMenuItem(
                                        text = { Text("File History (Revert)") },
                                        onClick = { 
                                            showRevertDialog = true
                                            showMenu = false
                                        }
                                    )"""
content = content.replace(menu_revert, "")

# Remove FileRevertDialog usage
dialog_revert = re.search(r'                    if \(showRevertDialog && selectedFile != null\) \{.*?                    \}', content, re.DOTALL)
if dialog_revert:
    content = content.replace(dialog_revert.group(0), "")

# Update Download logic
download_old = """                                IconButton(onClick = {
                                    Toast.makeText(context, "Download not fully implemented in preview", Toast.LENGTH_SHORT).show()
                                }) {"""
download_new = """                                IconButton(onClick = {
                                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                                    val fileToDownload = selectedFile?.file
                                    if (fileToDownload != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                                        try {
                                            fileToDownload.copyTo(java.io.File(downloadsDir, fileToDownload.name), overwrite = true)
                                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {"""
content = content.replace(download_old, download_new)

with open('app/src/main/java/com/example/ui/code/CodeScreen.kt', 'w') as f:
    f.write(content)
