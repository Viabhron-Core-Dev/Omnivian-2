package com.example.ui.artifacts

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.db.AppDatabase
import com.example.engine.db.ArtifactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class VoiceNoteCard(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var body: String,
    var colorHex: String = "#FFF9C4", // Soft Yellow
    var isPinned: Boolean = false,
    var checklist: List<ChecklistItem> = emptyList(),
    var audioTimestamp: String? = null
)

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    var text: String,
    var isDone: Boolean = false
)

val NOTE_COLORS = listOf(
    "#FFF9C4" to "Sun Yellow",
    "#C8E6C9" to "Mint Green",
    "#E1BEE7" to "Lavender",
    "#FFCCBC" to "Coral Peach",
    "#BBDEFB" to "Sky Blue",
    "#CFD8DC" to "Cool Slate"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtifactsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember(context) { AppDatabase.getDatabase(context) }
    val artifactDao = db.artifactDao()

    val artifactsList by artifactDao.getAllArtifactsFlow().collectAsState(initial = emptyList())
    var selectedArtifact by remember { mutableStateOf<ArtifactEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Seed default persistent voice notes if empty
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val existing = artifactDao.getAllArtifacts()
            if (existing.none { it.id == "system_default_notes" }) {
                val initialNotes = listOf(
                    VoiceNoteCard(
                        id = "note_1",
                        title = "🎙️ Quick Voice Memos",
                        body = "Tap the mic shortcut or widget button anytime to record voice thoughts. Transcriptions will automatically cluster here.",
                        colorHex = "#FFF9C4",
                        isPinned = true
                    ),
                    VoiceNoteCard(
                        id = "note_2",
                        title = "✨ Sprint Action Items",
                        body = "Key milestones and workspace items to finish:",
                        colorHex = "#C8E6C9",
                        isPinned = false,
                        checklist = listOf(
                            ChecklistItem("item_1", "Add OpenAI & Anthropic custom API keys", true),
                            ChecklistItem("item_2", "Sync offline chat threads", true),
                            ChecklistItem("item_3", "Try home screen Quick Chat widget", false)
                        )
                    ),
                    VoiceNoteCard(
                        id = "note_3",
                        title = "💡 Idea: OmniRoot Autonomous Mode",
                        body = "Explore self-correcting tool loops with terminal sandbox in background sync.",
                        colorHex = "#E1BEE7",
                        isPinned = false
                    )
                )
                val jsonNotes = serializeNotes(initialNotes)
                val defaultEntity = ArtifactEntity(
                    id = "system_default_notes",
                    title = "My Voice & Color Notes",
                    type = "COLOR_NOTES",
                    content = jsonNotes,
                    isPinned = true,
                    updatedAt = System.currentTimeMillis()
                )
                artifactDao.insertArtifact(defaultEntity)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Artifacts & Voice Notes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New Artifact")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (artifactsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(artifactsList, key = { it.id }) { artifact ->
                        ArtifactListItemCard(
                            artifact = artifact,
                            onClick = { selectedArtifact = artifact },
                            onDelete = {
                                scope.launch(Dispatchers.IO) {
                                    artifactDao.deleteArtifact(artifact)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Full Screen Opened Artifact Viewer with Top Bar Drag-Down/Close Header (No Swipe Interference)
        selectedArtifact?.let { current ->
            OpenedArtifactViewerDialog(
                artifact = current,
                onDismiss = { selectedArtifact = null },
                onSave = { updatedEntity ->
                    scope.launch(Dispatchers.IO) {
                        artifactDao.updateArtifact(updatedEntity)
                    }
                    selectedArtifact = updatedEntity
                }
            )
        }

        // Create New Artifact Dialog
        if (showCreateDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newType by remember { mutableStateOf("COLOR_NOTES") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create New Artifact") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Artifact Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = newType == "COLOR_NOTES",
                                onClick = { newType = "COLOR_NOTES" },
                                label = { Text("Voice / Notes") }
                            )
                            FilterChip(
                                selected = newType == "HTML",
                                onClick = { newType = "HTML" },
                                label = { Text("HTML / PWA") }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTitle.isNotBlank()) {
                            val newId = UUID.randomUUID().toString()
                            val initialContent = if (newType == "COLOR_NOTES") {
                                serializeNotes(listOf(VoiceNoteCard(title = "New Note", body = "")))
                            } else {
                                "<html><body style='background:#121212;color:white;font-family:sans-serif;padding:20px;'><h2>${newTitle}</h2><p>Interactive web artifact canvas.</p></body></html>"
                            }
                            val newEntity = ArtifactEntity(
                                id = newId,
                                title = newTitle.trim(),
                                type = newType,
                                content = initialContent,
                                updatedAt = System.currentTimeMillis()
                            )
                            scope.launch(Dispatchers.IO) {
                                artifactDao.insertArtifact(newEntity)
                            }
                            showCreateDialog = false
                        }
                    }) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ArtifactListItemCard(
    artifact: ArtifactEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(artifact.updatedAt) {
        SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(artifact.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (artifact.type == "COLOR_NOTES") Color(0xFFFFF176).copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (artifact.type == "COLOR_NOTES") Icons.Default.Mic else Icons.Default.Code,
                    contentDescription = null,
                    tint = if (artifact.type == "COLOR_NOTES") Color(0xFFFFB300) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artifact.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (artifact.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${artifact.type.replace("_", " ")} · $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Top Bar Drag-Down & Close Header Artifact Viewer.
 * Replaces swipe-down modal sheets so that internal note/html scrolling is never cut off or accidentally dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenedArtifactViewerDialog(
    artifact: ArtifactEntity,
    onDismiss: () -> Unit,
    onSave: (ArtifactEntity) -> Unit
) {
    val context = LocalContext.current
    var notesList by remember(artifact.id, artifact.content) {
        mutableStateOf(if (artifact.type == "COLOR_NOTES") parseNotes(artifact.content) else emptyList())
    }
    var htmlContent by remember(artifact.id, artifact.content) {
        mutableStateOf(artifact.content)
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Drag-Down / Close Header Area
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // Drag Indicator Bar (Tap to close)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(40.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { onDismiss() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close Viewer", modifier = Modifier.size(32.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = artifact.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Tap drag-bar or arrow to close",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (artifact.type == "COLOR_NOTES") {
                                Button(
                                    onClick = {
                                        val newCard = VoiceNoteCard(title = "New Voice Note", body = "")
                                        val updated = notesList + newCard
                                        notesList = updated
                                        onSave(artifact.copy(content = serializeNotes(updated), updatedAt = System.currentTimeMillis()))
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Note")
                                }
                            }
                        }
                    }
                }

                // Main Content View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (artifact.type == "COLOR_NOTES") {
                        ColorNotesContentView(
                            notes = notesList,
                            onUpdateNotes = { updated ->
                                notesList = updated
                                onSave(artifact.copy(content = serializeNotes(updated), updatedAt = System.currentTimeMillis()))
                            }
                        )
                    } else {
                        HtmlArtifactContentView(
                            html = htmlContent,
                            onHtmlChange = { newHtml ->
                                htmlContent = newHtml
                                onSave(artifact.copy(content = newHtml, updatedAt = System.currentTimeMillis()))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorNotesContentView(
    notes: List<VoiceNoteCard>,
    onUpdateNotes: (List<VoiceNoteCard>) -> Unit
) {
    if (notes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notes yet. Tap '+ Add Note' to create one.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                VoiceNoteCardItem(
                    note = note,
                    onUpdate = { updatedNote ->
                        val updatedList = notes.map { if (it.id == updatedNote.id) updatedNote else it }
                        onUpdateNotes(updatedList)
                    },
                    onDelete = {
                        val updatedList = notes.filter { it.id != note.id }
                        onUpdateNotes(updatedList)
                    }
                )
            }
        }
    }
}

@Composable
fun VoiceNoteCardItem(
    note: VoiceNoteCard,
    onUpdate: (VoiceNoteCard) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember(note.title) { mutableStateOf(note.title) }
    var body by remember(note.body) { mutableStateOf(note.body) }
    val cardColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (e: Exception) {
            Color(0xFFFFF9C4)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Note Title & Palette Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BasicNoteTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        onUpdate(note.copy(title = it))
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Color picker dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NOTE_COLORS.take(4).forEach { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { onUpdate(note.copy(colorHex = hex)) }
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body text
            BasicNoteTextField(
                value = body,
                onValueChange = {
                    body = it
                    onUpdate(note.copy(body = it))
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black.copy(alpha = 0.85f)),
                modifier = Modifier.fillMaxWidth()
            )

            // Checklist Items
            if (note.checklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(6.dp))
                note.checklist.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isDone,
                            onCheckedChange = { checked ->
                                val updatedChecklist = note.checklist.toMutableList()
                                updatedChecklist[index] = item.copy(isDone = checked)
                                onUpdate(note.copy(checklist = updatedChecklist))
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.Black,
                                uncheckedColor = Color.Black.copy(alpha = 0.6f),
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (item.isDone) Color.Black.copy(alpha = 0.4f) else Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicNoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        modifier = modifier
    )
}

@Composable
fun HtmlArtifactContentView(
    html: String,
    onHtmlChange: (String) -> Unit
) {
    AndroidViewWebView(html = html, modifier = Modifier.fillMaxSize())
}

@Composable
fun AndroidViewWebView(
    html: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = android.webkit.WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = modifier
    )
}

// Helpers to serialize and deserialize voice/color notes to JSON
fun serializeNotes(notes: List<VoiceNoteCard>): String {
    val arr = JSONArray()
    for (n in notes) {
        val obj = JSONObject().apply {
            put("id", n.id)
            put("title", n.title)
            put("body", n.body)
            put("colorHex", n.colorHex)
            put("isPinned", n.isPinned)
            val checkArr = JSONArray()
            for (c in n.checklist) {
                checkArr.put(JSONObject().apply {
                    put("id", c.id)
                    put("text", c.text)
                    put("isDone", c.isDone)
                })
            }
            put("checklist", checkArr)
        }
        arr.put(obj)
    }
    return arr.toString()
}

fun parseNotes(jsonStr: String): List<VoiceNoteCard> {
    val list = mutableListOf<VoiceNoteCard>()
    try {
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val checkList = mutableListOf<ChecklistItem>()
            val checkArr = obj.optJSONArray("checklist")
            if (checkArr != null) {
                for (j in 0 until checkArr.length()) {
                    val cObj = checkArr.getJSONObject(j)
                    checkList.add(
                        ChecklistItem(
                            id = cObj.optString("id", UUID.randomUUID().toString()),
                            text = cObj.optString("text", ""),
                            isDone = cObj.optBoolean("isDone", false)
                        )
                    )
                }
            }
            list.add(
                VoiceNoteCard(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Voice Note"),
                    body = obj.optString("body", ""),
                    colorHex = obj.optString("colorHex", "#FFF9C4"),
                    isPinned = obj.optBoolean("isPinned", false),
                    checklist = checkList
                )
            )
        }
    } catch (e: Exception) {
        // Return default single note if parsing fails
        list.add(VoiceNoteCard(title = "Voice Memo", body = jsonStr))
    }
    return list
}
