import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'r') as f:
    content = f.read()

replacement_1 = """                when (selectedTabIndex) {
                    0 -> DirectoryTab(viewModel, onAddKeyClick)
                    1 -> ActiveKeysTab(viewModel)
                    2 -> ModelsTab(viewModel)
                    3 -> MetricsTab(viewModel)
                    4 -> ModelRaterTab(viewModel)
                    5 -> TranslatorTab()
                }"""
content = re.sub(r'                when \(selectedTabIndex\) \{.*?                    5 -> TranslatorTab\(\)\n                \}', replacement_1, content, flags=re.DOTALL)

replacement_2 = """
@Composable
fun ModelRaterTab(viewModel: AiManagerViewModel) {
    val ratings by viewModel.modelRatings.collectAsState()
    
    if (ratings.isEmpty()) {
        CenterTextTab("No ratings yet. Rate messages in Chat!")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ratings.sortedByDescending { it.upvotes - it.downvotes }) { stat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text(stat.modelName) },
                        supportingContent = { Text(stat.providerId) },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Upvotes", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stat.upvotes.toString())
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(Icons.Default.ThumbDown, contentDescription = "Downvotes", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stat.downvotes.toString())
                            }
                        }
                    )
                }
            }
        }
    }
}
"""
content = content + replacement_2

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'w') as f:
    f.write(content)
