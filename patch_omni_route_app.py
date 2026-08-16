import re

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'r') as f:
    content = f.read()

# Add imports
content = content.replace(
    'import com.example.ui.settings.ThreadSettingsScreen\nimport com.example.utils.LogKeeper\nimport kotlinx.coroutines.launch',
    '''import com.example.ui.settings.ThreadSettingsScreen
import com.example.ui.settings.LogKeeperScreen
import com.example.utils.LogKeeper
import kotlinx.coroutines.launch'''
)

# Add init log and navigation log listener
content = content.replace(
    '    val navController = rememberNavController()\n    ModalNavigationDrawer(',
    '''    val navController = rememberNavController()
    
    LaunchedEffect(Unit) {
        LogKeeper.log("Info", "System", "LogKeeper initialized")
    }
    
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let {
                LogKeeper.log("Info", "Navigation", "Navigated to: $it")
            }
        }
    }

    ModalNavigationDrawer('''
)

# Replace FAB onClick to navigate
content = content.replace(
    'onClick = { LogKeeper.exportAndClear(context) },',
    'onClick = { navController.navigate("log_keeper") },'
)

# Handle nested settings routes properly
content = content.replace(
    '''                    onNavigateTo = { route -> 
                         // navController.navigate(route) 
                         // Note: actual nested routes for settings aren't fully implemented in this phase
                    }''',
    '''                    onNavigateTo = { route -> 
                         if (route == "settings/log_keeper") {
                             navController.navigate("log_keeper")
                         }
                    }'''
)

# Add log_keeper composable
content = content.replace(
    '''            composable("settings") {''',
    '''            composable("log_keeper") {
                LogKeeperScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("settings") {'''
)

with open('app/src/main/java/com/example/ui/OmniRouteApp.kt', 'w') as f:
    f.write(content)

