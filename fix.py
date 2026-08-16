with open('app/src/main/java/com/example/ui/settings/SettingsPlaceholders.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '''    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {''',
    '''    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {'''
)

old_end = """                            )
                        }
                    }
                }
            }
        }
        
        FloatingActionButton(
            onClick = { /* TODO: Open Create Tool Dialog */ },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Custom JS Tool")
        }
    }
}"""

new_end = """                            )
                        }
                    }
                }
            }
        }
    }
}"""
content = content.replace(old_end, new_end)

with open('app/src/main/java/com/example/ui/settings/SettingsPlaceholders.kt', 'w') as f:
    f.write(content)

