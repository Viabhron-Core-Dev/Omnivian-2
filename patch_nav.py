import re

with open('app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.material.icons.filled.MoreVert', 'import androidx.compose.material.icons.filled.MoreHoriz')
content = content.replace('Icons.Default.MoreVert', 'Icons.Default.MoreHoriz')
content = content.replace('.padding(horizontal = 24.dp, vertical = 8.dp)', '.padding(horizontal = 36.dp, vertical = 8.dp)')

with open('app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt', 'w') as f:
    f.write(content)
