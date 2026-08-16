with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'r') as f:
    content = f.read()

# fix the broken first line
content = content.replace("package com.example.ui.settings.omnirouteimport", "package com.example.ui.settings.omniroute\nimport")
content = content.replace("Outline@OptIn", "Outline\n\n@OptIn")
content = content.replace("import androidx", "\nimport androidx")

standard_imports = """
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
"""

# inject right after package
content = content.replace("package com.example.ui.settings.omniroute", "package com.example.ui.settings.omniroute\n" + standard_imports)

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'w') as f:
    f.write(content)
