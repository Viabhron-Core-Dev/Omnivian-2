import re

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'r') as f:
    content = f.read()

# Add imports
imports_to_add = """import com.example.engine.db.AppDatabase
import com.example.engine.db.WorkspaceIssueEntity
import com.example.engine.db.WorkspacePullRequestEntity
import java.util.UUID
"""
content = content.replace("import android.provider.OpenableColumns", "import android.provider.OpenableColumns\n" + imports_to_add)

# Change FileExplorer top part to include Tabs
old_top = """    val workspaceName = remember(LocalFileManager.getWorkspaceDir().name) { 
        mutableStateOf(LocalFileManager.getWorkspaceName(LocalFileManager.getWorkspaceDir().name)) 
    }
    Column(modifier = modifier.fillMaxSize()) {"""

new_top = """    val workspaceName = remember(LocalFileManager.getWorkspaceDir().name) { 
        mutableStateOf(LocalFileManager.getWorkspaceName(LocalFileManager.getWorkspaceDir().name)) 
    }
    val workspaceId = LocalFileManager.getWorkspaceDir().name
    
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Files", "Issues", "PRs")

    Column(modifier = modifier.fillMaxSize()) {"""

content = content.replace(old_top, new_top)

# Change FileExplorer horizontal divider part
old_content = """        HorizontalDivider()

        fileTree?.let { rootNode ->"""

new_content = """        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        
        if (selectedTabIndex == 0) {
            fileTree?.let { rootNode ->"""

content = content.replace(old_content, new_content)

# Change FileExplorer end of LazyColumn part
old_end = """        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workspace found")
            }
        }
    }
}"""

new_end = """        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No workspace found")
            }
        }
        } else if (selectedTabIndex == 1) {
            IssuesTab(workspaceId = workspaceId)
        } else if (selectedTabIndex == 2) {
            PullRequestsTab(workspaceId = workspaceId)
        }
    }
}

@Composable
fun IssuesTab(workspaceId: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val issues by db.workspaceIssueDao().getIssuesForWorkspace(workspaceId).collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (issues.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No local issues found.")
                }
            }
        }
        items(issues) { issue ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = issue.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: ${issue.status}", style = MaterialTheme.typography.bodySmall)
                    if (issue.targetFile != null) {
                        Text(text = "Target: ${issue.targetFile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun PullRequestsTab(workspaceId: String) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val prs by db.workspacePullRequestDao().getPullRequestsForWorkspace(workspaceId).collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (prs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No local pull requests found.")
                }
            }
        }
        items(prs) { pr ->
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = pr.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Status: ${pr.status}", style = MaterialTheme.typography.bodySmall)
                    if (pr.targetFile != null) {
                        Text(text = "Target: ${pr.targetFile}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}"""

content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/ui/code/FileExplorer.kt', 'w') as f:
    f.write(content)
