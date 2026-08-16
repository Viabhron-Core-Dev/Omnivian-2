package com.example.ui.bottomnav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppTab {
    CHAT, CODE
}

@Composable
fun FixedBottomNav(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onMoreClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Placeholder for left alignment to center the toggle
            Spacer(modifier = Modifier.width(48.dp))
            
            // Pill shaped toggle
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabButton(
                    text = "Chat",
                    isSelected = currentTab == AppTab.CHAT,
                    onClick = { onTabSelected(AppTab.CHAT) }
                )
                TabButton(
                    text = "Code",
                    isSelected = currentTab == AppTab.CODE,
                    onClick = { onTabSelected(AppTab.CODE) }
                )
            }
            
            // 3 dots menu
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More options")
            }
        }
    }
}

@Composable
private fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 36.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}
