package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.engine.db.AppDatabase
import com.example.engine.db.ArtifactEntity
import com.example.ui.artifacts.ChecklistItem
import com.example.ui.artifacts.VoiceNoteCard
import com.example.ui.artifacts.parseNotes
import com.example.ui.artifacts.serializeNotes
import com.example.ui.chat.ChatMessage
import com.example.ui.chat.MessageRole
import com.example.ui.chat.OmniRootClient
import com.example.utils.LogKeeper
import com.example.utils.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class QuickChatWidgetProvider : AppWidgetProvider() {

    companion object {
        const val PREFS_NAME = "omniroot_widget_prefs"
        const val KEY_IS_TEMP_MODE = "is_temp_mode"
        const val KEY_IS_RECORDING = "is_widget_recording"
        const val KEY_RECORDING_FILE = "widget_recording_file"

        const val ACTION_TOGGLE_MODE = "com.example.widget.ACTION_TOGGLE_MODE"
        const val ACTION_VOICE_TAP = "com.example.widget.ACTION_VOICE_TAP"
        const val ACTION_VOICE_LONG_PRESS = "com.example.widget.ACTION_VOICE_LONG_PRESS"
        const val ACTION_CAMERA = "com.example.widget.ACTION_CAMERA"

        private val widgetScope = CoroutineScope(Dispatchers.IO)

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isTemp = prefs.getBoolean(KEY_IS_TEMP_MODE, false)
            val isRecording = prefs.getBoolean(KEY_IS_RECORDING, false)

            val views = RemoteViews(context.packageName, R.layout.widget_quick_chat)

            // 1. Top-Left: New Chat
            val newChatUri = Uri.parse("omniroot://chat/new?temp=$isTemp")
            val newChatIntent = Intent(context, MainActivity::class.java).apply {
                data = newChatUri
                putExtra("action", "open_chat")
                putExtra("is_temp", isTemp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val newChatPendingIntent = PendingIntent.getActivity(
                context,
                101,
                newChatIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_new_chat, newChatPendingIntent)

            // 2. Top-Right: Mode Toggle (Persistent vs Temp)
            views.setTextViewText(R.id.widget_text_mode, if (isTemp) "Temp" else "Persistent")
            views.setTextColor(R.id.widget_text_mode, if (isTemp) 0xFFFFB74D.toInt() else 0xFF80D8FF.toInt())
            val toggleIntent = Intent(context, QuickChatWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_MODE
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_toggle_mode, togglePendingIntent)

            // 3. Bottom-Left: Camera
            val cameraUri = Uri.parse("omniroot://chat/camera?temp=$isTemp")
            val cameraIntent = Intent(context, MainActivity::class.java).apply {
                data = cameraUri
                putExtra("action", "open_camera")
                putExtra("is_temp", isTemp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val cameraPendingIntent = PendingIntent.getActivity(
                context,
                103,
                cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_camera, cameraPendingIntent)

            // 4. Bottom-Right: Voice Action
            if (isRecording) {
                views.setTextViewText(R.id.widget_text_voice, "🔴 Tap Send")
                views.setTextColor(R.id.widget_text_voice, 0xFFFF5252.toInt())
            } else {
                views.setTextViewText(R.id.widget_text_voice, "Voice AI")
                views.setTextColor(R.id.widget_text_voice, 0xFFC8E6C9.toInt())
            }

            val voiceTapIntent = Intent(context, QuickChatWidgetProvider::class.java).apply {
                action = ACTION_VOICE_TAP
            }
            val voiceTapPendingIntent = PendingIntent.getBroadcast(
                context,
                104,
                voiceTapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_voice, voiceTapPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, QuickChatWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when (action) {
            ACTION_TOGGLE_MODE -> {
                val current = prefs.getBoolean(KEY_IS_TEMP_MODE, false)
                val newMode = !current
                prefs.edit().putBoolean(KEY_IS_TEMP_MODE, newMode).apply()
                val label = if (newMode) "Temporary Chat Mode" else "Persistent Chat Mode"
                Toast.makeText(context, "Widget: $label", Toast.LENGTH_SHORT).show()
                refreshAllWidgets(context)
            }

            ACTION_VOICE_TAP -> {
                handleVoiceTap(context)
            }

            ACTION_VOICE_LONG_PRESS -> {
                handleVoiceNoteCreation(context)
            }
        }
    }

    private fun handleVoiceTap(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRecording = prefs.getBoolean(KEY_IS_RECORDING, false)

        if (!isRecording) {
            // First Tap: Start Headless Audio Recording
            prefs.edit().putBoolean(KEY_IS_RECORDING, true).apply()
            refreshAllWidgets(context)
            Toast.makeText(context, "🎙️ Listening... Tap Voice button again to send to AI", Toast.LENGTH_LONG).show()

            VoiceManager.startListening(
                context = context,
                onAudioRecorded = { audioFile ->
                    prefs.edit().putString(KEY_RECORDING_FILE, audioFile.name).apply()
                },
                onFinalResult = { text ->
                    // Also captured
                },
                onError = { err ->
                    prefs.edit().putBoolean(KEY_IS_RECORDING, false).apply()
                    refreshAllWidgets(context)
                    Toast.makeText(context, "Voice error: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            // Second Tap: Stop Recording & Send to Model in background
            prefs.edit().putBoolean(KEY_IS_RECORDING, false).apply()
            refreshAllWidgets(context)
            Toast.makeText(context, "⏳ Processing voice query with OmniRoot AI...", Toast.LENGTH_SHORT).show()

            VoiceManager.stopListening { recordedFile ->
                sendAudioToAiAndSpeak(context, recordedFile)
            }
        }
    }

    private fun sendAudioToAiAndSpeak(context: Context, audioFile: File) {
        widgetScope.launch {
            try {
                LogKeeper.log("WidgetVoice", "SendingVoiceToModel", "Audio file: ${audioFile.name}")
                val messageText = "[Audio: ${audioFile.name}]"
                val chatMessages = listOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = messageText,
                        role = MessageRole.USER
                    )
                )

                val result = OmniRootClient.generateContent(
                    messages = chatMessages,
                    model = "omni-default"
                )

                val replyText = result.text ?: "I heard your voice note, but could not generate a response."
                LogKeeper.log("WidgetVoice", "AiResponseReceived", replyText)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "AI: $replyText", Toast.LENGTH_LONG).show()
                    VoiceManager.speak(context, replyText)
                }
            } catch (e: Exception) {
                LogKeeper.log("WidgetVoice", "AiError", e.message ?: "Error calling AI", e.stackTraceToString())
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "AI query error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleVoiceNoteCreation(context: Context) {
        Toast.makeText(context, "🎙️ Recording Voice Note for Artifacts...", Toast.LENGTH_SHORT).show()
        val timeStamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())

        VoiceManager.startListening(
            context = context,
            onAudioRecorded = { audioFile ->
                saveVoiceNoteToArtifacts(context, "Voice Memo ($timeStamp)", "Voice audio saved: ${audioFile.name}")
            },
            onFinalResult = { text ->
                saveVoiceNoteToArtifacts(context, "Voice Note ($timeStamp)", text)
            },
            onError = { err ->
                Toast.makeText(context, "Note recording error: $err", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun saveVoiceNoteToArtifacts(context: Context, title: String, contentText: String) {
        widgetScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val artifactDao = db.artifactDao()
                val existing = artifactDao.getAllArtifacts().find { it.id == "system_default_notes" }

                val currentNotes = if (existing != null) parseNotes(existing.content).toMutableList() else mutableListOf()
                val newNote = VoiceNoteCard(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    body = contentText,
                    colorHex = "#FFF9C4",
                    isPinned = true,
                    audioTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                currentNotes.add(0, newNote)

                val updatedEntity = (existing ?: ArtifactEntity(
                    id = "system_default_notes",
                    title = "My Voice & Color Notes",
                    type = "COLOR_NOTES",
                    content = "",
                    updatedAt = System.currentTimeMillis()
                )).copy(
                    content = serializeNotes(currentNotes),
                    updatedAt = System.currentTimeMillis()
                )

                artifactDao.insertArtifact(updatedEntity)
                LogKeeper.log("WidgetNotes", "NoteSaved", "Saved voice note to Room: '$title'")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved to Notes: $title", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                LogKeeper.log("WidgetNotes", "SaveError", e.message ?: "Failed to save note", e.stackTraceToString())
            }
        }
    }
}
