with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

replacement = """                    WorkspaceActionsBottomSheet(
                        onDismiss = { showWorkspaceActions = false },
                        onExportClick = {
                            showWorkspaceActions = false
                            showGithubExport = true
                        },
                        onZipExportClick = {
                            showWorkspaceActions = false
                            scope.launch {
                                val context = navController.context
                                val dir = com.example.engine.fs.LocalFileManager.getWorkspaceDir()
                                val cacheDir = context.cacheDir
                                val zipFile = java.io.File(cacheDir, "workspace_${dir.name}.zip")
                                val result = com.example.engine.fs.LocalFileManager.zipDirectory(dir, zipFile)
                                if (result.isSuccess) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        zipFile
                                    )
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Export Workspace"))
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to create ZIP", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onThreadSettingsClick = {
                            showWorkspaceActions = false
                            navController.navigate("thread_settings")
                        }
                    )"""

content = content.replace(
"""                    WorkspaceActionsBottomSheet(
                        onDismiss = { showWorkspaceActions = false },
                        onExportClick = {
                            showWorkspaceActions = false
                            showGithubExport = true
                        },
                        onThreadSettingsClick = {
                            showWorkspaceActions = false
                            navController.navigate("thread_settings")
                        }
                    )""",
replacement
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

