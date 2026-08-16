sed -i 's/fun WorkspaceActionsBottomSheet(onDismiss: () -> Unit) {/fun WorkspaceActionsBottomSheet(onDismiss: () -> Unit, onExportClick: () -> Unit = {}) {/g' app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt
sed -i 's/onClick = { \/\* TODO: Export \*\/ onDismiss() }/onClick = onExportClick/g' app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt
