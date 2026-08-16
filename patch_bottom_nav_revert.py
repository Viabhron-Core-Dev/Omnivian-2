with open('app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),""",
"""        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),"""
)

with open('app/src/main/java/com/example/ui/bottomnav/FixedBottomNav.kt', 'w') as f:
    f.write(content)

