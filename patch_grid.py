import re

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'r') as f:
    content = f.read()

old_layout = """        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionGridItem(
                    icon = Icons.Default.Settings,
                    label = "Thread Settings",
                    onClick = { showExportOptions = false; onThreadSettingsClick() },
                    modifier = Modifier.size(80.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Share,
                    label = "Export",
                    onClick = { showExportOptions = true },
                    modifier = Modifier.size(80.dp)
                )
                ActionGridItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = "Remix",
                    onClick = { showRemixDialog = true },
                    modifier = Modifier.size(80.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Dashboard,
                    label = "OmniRoute Dashboard",
                    onClick = { showOmniRouteDialog = true },
                    modifier = Modifier.size(80.dp)
                )
            }
        }"""

new_layout = """        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionGridItem(
                    icon = Icons.Default.Settings,
                    label = "Thread Settings",
                    onClick = { showExportOptions = false; onThreadSettingsClick() },
                    modifier = Modifier.size(110.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Share,
                    label = "Export",
                    onClick = { showExportOptions = true },
                    modifier = Modifier.size(110.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionGridItem(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    label = "Remix",
                    onClick = { showRemixDialog = true },
                    modifier = Modifier.size(110.dp)
                )
                ActionGridItem(
                    icon = Icons.Default.Dashboard,
                    label = "OmniRoute Dashboard",
                    onClick = { showOmniRouteDialog = true },
                    modifier = Modifier.size(110.dp)
                )
            }
        }"""

content = content.replace(old_layout, new_layout)

with open('app/src/main/java/com/example/ui/bottomnav/WorkspaceActionsBottomSheet.kt', 'w') as f:
    f.write(content)
