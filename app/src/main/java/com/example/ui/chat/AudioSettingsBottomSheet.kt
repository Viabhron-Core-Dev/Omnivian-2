package com.example.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.LogKeeper
import com.example.utils.VoiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeEngine by remember { mutableStateOf(VoiceManager.getSttEngine(context)) }
    var selectedModelPath by remember { mutableStateOf(VoiceManager.getSelectedModelPath(context)) }
    var importedModels by remember { mutableStateOf(VoiceManager.listImportedModels(context)) }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val contentResolver = context.contentResolver
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "stt_model_${System.currentTimeMillis()}.bin"
                    val destFile = File(VoiceManager.getAudioModelsDir(context), fileName)

                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                importedModels = VoiceManager.listImportedModels(context)
                Toast.makeText(context, "Audio model imported successfully!", Toast.LENGTH_SHORT).show()
                LogKeeper.log("AudioSettingsSheet", "ModelImportSuccess", "Imported model to /files/audio_models/")
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import model: ${e.message}", Toast.LENGTH_LONG).show()
                LogKeeper.log("AudioSettingsSheet", "ModelImportError", "Error: ${e.message}")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Audio & Speech Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Configure how speech is recognized into text in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Engine 1: Android System SpeechRecognizer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeEngine == VoiceManager.ENGINE_ANDROID_NATIVE)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeEngine = VoiceManager.ENGINE_ANDROID_NATIVE
                            VoiceManager.setSttEngine(context, VoiceManager.ENGINE_ANDROID_NATIVE)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activeEngine == VoiceManager.ENGINE_ANDROID_NATIVE,
                            onClick = {
                                activeEngine = VoiceManager.ENGINE_ANDROID_NATIVE
                                VoiceManager.setSttEngine(context, VoiceManager.ENGINE_ANDROID_NATIVE)
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Android Built-In Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Fastest Real-Time", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Live acoustic speech recognition with instantaneous streaming text, zero proxy lag, and zero RAM overhead.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Engine 2: Custom Imported Model
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeEngine == VoiceManager.ENGINE_CUSTOM_MODEL)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeEngine = VoiceManager.ENGINE_CUSTOM_MODEL
                            VoiceManager.setSttEngine(context, VoiceManager.ENGINE_CUSTOM_MODEL)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = activeEngine == VoiceManager.ENGINE_CUSTOM_MODEL,
                                onClick = {
                                    activeEngine = VoiceManager.ENGINE_CUSTOM_MODEL
                                    VoiceManager.setSttEngine(context, VoiceManager.ENGINE_CUSTOM_MODEL)
                                }
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Custom Offline Model", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Import Whisper or Sherpa ONNX/GGUF model files post-install.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = { modelPickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import Model (.bin / .onnx / .tflite)")
                        }
                    }
                }
            }

            // Engine 0: Direct Audio Recording
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeEngine == VoiceManager.ENGINE_DIRECT_AUDIO)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activeEngine = VoiceManager.ENGINE_DIRECT_AUDIO
                            VoiceManager.setSttEngine(context, VoiceManager.ENGINE_DIRECT_AUDIO)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activeEngine == VoiceManager.ENGINE_DIRECT_AUDIO,
                            onClick = {
                                activeEngine = VoiceManager.ENGINE_DIRECT_AUDIO
                                VoiceManager.setSttEngine(context, VoiceManager.ENGINE_DIRECT_AUDIO)
                            }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Direct Audio Recording", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Records microphone audio into AAC/M4A clips to attach directly or transcribe via multimodal LLM.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // List of Imported Models
            if (importedModels.isNotEmpty()) {
                item {
                    Text(
                        "Imported STT Models (${importedModels.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(importedModels) { modelFile ->
                    val isSelected = selectedModelPath == modelFile.absolutePath
                    val fileSizeMb = remember(modelFile) {
                        String.format("%.1f MB", modelFile.length() / (1024.0 * 1024.0))
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedModelPath = modelFile.absolutePath
                                VoiceManager.setSelectedModelPath(context, modelFile.absolutePath)
                                activeEngine = VoiceManager.ENGINE_CUSTOM_MODEL
                                VoiceManager.setSttEngine(context, VoiceManager.ENGINE_CUSTOM_MODEL)
                                Toast.makeText(context, "Selected ${modelFile.name}", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(modelFile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (isSelected) {
                                        Spacer(Modifier.width(6.dp))
                                        Text("• Active", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(fileSizeMb, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {
                                modelFile.delete()
                                importedModels = VoiceManager.listImportedModels(context)
                                if (selectedModelPath == modelFile.absolutePath) {
                                    selectedModelPath = null
                                    VoiceManager.setSelectedModelPath(context, null)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
