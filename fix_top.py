import re

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'r') as f:
    content = f.read()

# Delete everything from start up to @OptIn
content = re.sub(r'^.*?@OptIn', '@OptIn', content, flags=re.DOTALL)

top = """package com.example.ui.settings.omniroute

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

with open('app/src/main/java/com/example/ui/settings/omniroute/AiManagerPanelScreen.kt', 'w') as f:
    f.write(top + content)
