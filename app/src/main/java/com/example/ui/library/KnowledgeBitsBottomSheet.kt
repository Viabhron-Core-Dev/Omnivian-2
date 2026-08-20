package com.example.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.db.AppDatabase
import com.example.engine.db.KnowledgeBitEntity
import com.example.engine.knowledge.KnowledgeBitsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBitsBottomSheet(
    onDismiss: () -> Unit,
    onInsertToChat: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val bitsList by db.knowledgeBitDao().getAllBits().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingBit by remember { mutableStateOf<KnowledgeBitEntity?>(null) }
    var isRefreshingId by remember { mutableStateOf<String?>(null) }

    val filteredBits = remember(bitsList, selectedTab, searchQuery) {
        bitsList.filter { bit ->
            val matchesTab = when (selectedTab) {
                "PINNED" -> bit.isPinned
                "CODE" -> bit.contentType == "CODE"
                "ARTICLE" -> bit.contentType == "ARTICLE" || bit.contentType == "DOCUMENT"
                "TABLE" -> bit.contentType == "TABLE"
                "TEMP" -> !bit.isPinned
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                bit.title.contains(searchQuery, ignoreCase = true) ||
                bit.content.contains(searchQuery, ignoreCase = true) ||
                (bit.sourceUrl?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesTab && matchesQuery
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.92f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Knowledge Bits",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Active reference cache across chat sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val pruned = KnowledgeBitsManager.pruneExpired(context)
                                Toast.makeText(context, if (pruned > 0) "Pruned $pruned expired bits" else "No expired bits to prune", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "Prune Expired", tint = MaterialTheme.colorScheme.secondary)
                    }
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Bit", fontSize = 13.sp)
                    }
                }
            }

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search knowledge bits...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )

            // Filter Chips
            ScrollableTabRow(
                selectedTabIndex = listOf("ALL", "PINNED", "CODE", "ARTICLE", "TABLE", "TEMP").indexOf(selectedTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                listOf(
                    "ALL" to "All (${bitsList.size})",
                    "PINNED" to "Pinned 📌",
                    "CODE" to "Code / Repos 💻",
                    "ARTICLE" to "Articles 📄",
                    "TABLE" to "Tables 📊",
                    "TEMP" to "Temporary ⏱️"
                ).forEach { (key, label) ->
                    Tab(
                        selected = selectedTab == key,
                        onClick = { selectedTab = key },
                        text = {
                            Text(
                                label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == key) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // List of Bits
            if (filteredBits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FindInPage,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No matching knowledge bits" else "No knowledge bits in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Cache GitHub files, API docs, or data tables for multi-turn AI context",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredBits, key = { it.id }) { bit ->
                        KnowledgeBitCard(
                            bit = bit,
                            isRefreshing = isRefreshingId == bit.id,
                            onView = { viewingBit = bit },
                            onTogglePin = {
                                scope.launch {
                                    KnowledgeBitsManager.togglePin(context, bit.id, !bit.isPinned)
                                    Toast.makeText(context, if (!bit.isPinned) "Pinned bit" else "Unpinned bit", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRefresh = {
                                if (!bit.sourceUrl.isNullOrBlank()) {
                                    scope.launch {
                                        isRefreshingId = bit.id
                                        val res = KnowledgeBitsManager.refreshBit(context, bit.id)
                                        isRefreshingId = null
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Refreshed from upstream source", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Refresh failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onInsert = {
                                val formatted = KnowledgeBitsManager.formatBitForPrompt(bit)
                                onInsertToChat?.invoke(formatted)
                                onDismiss()
                            },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Knowledge Bit", bit.content))
                                Toast.makeText(context, "Content copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                scope.launch {
                                    KnowledgeBitsManager.deleteBit(context, bit.id)
                                    Toast.makeText(context, "Deleted knowledge bit", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Add Knowledge Bit
    if (showAddDialog) {
        AddKnowledgeBitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, sourceUrl, type, isPinned ->
                scope.launch {
                    if (!sourceUrl.isNullOrBlank() && content.isBlank()) {
                        val res = KnowledgeBitsManager.fetchAndCacheUrl(
                            context = context,
                            url = sourceUrl,
                            customTitle = title.ifBlank { null },
                            contentType = type,
                            isPinned = isPinned
                        )
                        if (res.isSuccess) {
                            Toast.makeText(context, "Fetched & cached knowledge bit", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                        } else {
                            Toast.makeText(context, "Fetch failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        KnowledgeBitsManager.saveBit(
                            context = context,
                            title = title,
                            content = content,
                            sourceUrl = sourceUrl.ifBlank { null },
                            contentType = type,
                            isPinned = isPinned
                        )
                        Toast.makeText(context, "Saved knowledge bit", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    }
                }
            }
        )
    }

    // Dialog: View Full Knowledge Bit
    viewingBit?.let { bit ->
        ViewKnowledgeBitDialog(
            bit = bit,
            onDismiss = { viewingBit = null },
            onInsert = {
                val formatted = KnowledgeBitsManager.formatBitForPrompt(bit)
                onInsertToChat?.invoke(formatted)
                viewingBit = null
                onDismiss()
            }
        )
    }
}

@Composable
fun KnowledgeBitCard(
    bit: KnowledgeBitEntity,
    isRefreshing: Boolean,
    onView: () -> Unit,
    onTogglePin: () -> Unit,
    onRefresh: () -> Unit,
    onInsert: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (bit.contentType) {
        "CODE" -> Color(0xFF10B981) // Emerald
        "ARTICLE", "DOCUMENT" -> Color(0xFF3B82F6) // Blue
        "TABLE" -> Color(0xFFA855F7) // Purple
        "PRESENTATION" -> Color(0xFFEC4899) // Pink
        else -> Color(0xFFF59E0B) // Amber
    }

    val now = System.currentTimeMillis()
    val ageHours = (now - bit.cachedAt) / (1000 * 60 * 60)
    val isStale = ageHours > 72 && !bit.sourceUrl.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = if (bit.isPinned) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Type Badge
                    Surface(
                        color = typeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            bit.contentType,
                            color = typeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        bit.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bit.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (bit.isPinned) Icons.Default.PushPin else Icons.Default.PinInvoke,
                            contentDescription = "Pin toggle",
                            tint = if (bit.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Source URL (if present)
            if (!bit.sourceUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        bit.sourceUrl,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Summary snippet
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                bit.summary ?: bit.content.take(120),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Metadata footer & action buttons
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status tag & hits
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isStale) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            if (isStale) "⚠️ Stale" else "✅ Fresh",
                            color = if (isStale) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "🔥 ${bit.accessCount} uses",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!bit.sourceUrl.isNullOrBlank()) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onInsert, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.AddComment, contentDescription = "Insert into prompt", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKnowledgeBitDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, sourceUrl: String, type: String, isPinned: Boolean) -> Unit
) {
    var mode by remember { mutableStateOf("URL") } // "URL" or "RAW"
    var title by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var rawContent by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CODE") }
    var isPinned by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Add Knowledge Bit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Source Tabs
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = mode == "URL",
                        onClick = { mode = "URL" },
                        label = { Text("From GitHub / URL") },
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = mode == "RAW",
                        onClick = { mode = "RAW" },
                        label = { Text("Custom Text / Table") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (Optional)") },
                    placeholder = { Text(if (mode == "URL") "Auto-inferred from URL" else "e.g. Auth Architecture Specs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (mode == "URL") {
                    OutlinedTextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("Source URL / GitHub File Link *") },
                        placeholder = { Text("https://github.com/user/repo/blob/main/Engine.kt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = rawContent,
                        onValueChange = { rawContent = it },
                        label = { Text("Content / Code / CSV Table *") },
                        placeholder = { Text("Paste code snippet, markdown, or table...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("CODE", "ARTICLE", "TABLE", "NOTE").forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Pin toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                    Text("Pin permanently (exempt from auto-pruning)", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(title, rawContent, sourceUrl, selectedType, isPinned)
                        },
                        enabled = (mode == "URL" && sourceUrl.isNotBlank()) || (mode == "RAW" && rawContent.isNotBlank())
                    ) {
                        Text(if (mode == "URL") "Fetch & Cache" else "Save Bit")
                    }
                }
            }
        }
    }
}

@Composable
fun ViewKnowledgeBitDialog(
    bit: KnowledgeBitEntity,
    onDismiss: () -> Unit,
    onInsert: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bit.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (!bit.sourceUrl.isNullOrBlank()) {
                            Text(bit.sourceUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Content
                SelectionContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                bit.content,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = if (bit.contentType == "CODE" || bit.contentType == "TABLE") FontFamily.Monospace else FontFamily.Default,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Knowledge Bit", bit.content))
                            Toast.makeText(context, "Copied content", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }

                    Button(onClick = onInsert) {
                        Icon(Icons.Default.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Insert in Chat")
                    }
                }
            }
        }
    }
}
