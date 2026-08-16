package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: Long,
    val type: String, // e.g., "ERROR", "CRASH", "FAILURE", "INFO", "STARTUP"
    val component: String,
    val message: String,
    val stackTrace: String? = null
)

object LogKeeper {
    private const val TAG = "LogKeeper"
    private const val PREFS_NAME = "omniroot_log_prefs"
    private const val KEY_ENABLED = "log_keeper_enabled"
    private const val LOG_FILE_NAME = "omniroot_active_logs.jsonl"

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private var appContext: Context? = null
    private var logFile: File? = null
    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isExceptionHandlerInstalled = false

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Ensure LogKeeper is always active from start
        _isEnabled.value = prefs?.getBoolean(KEY_ENABLED, true) ?: true

        logFile = File(app.filesDir, LOG_FILE_NAME)
        loadLogsFromDisk()
        
        installCrashParachute(app)
        
        log("INFO", "LogKeeper", "LogKeeper initialized from app startup. Active logs: ${_logs.value.size}")
    }

    private fun installCrashParachute(context: Context) {
        if (isExceptionHandlerInstalled) return
        isExceptionHandlerInstalled = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTraceString = sw.toString()

                log("CRASH", "UncaughtException", "Fatal crash on thread ${thread.name}: ${throwable.message}", stackTraceString)
                
                // Drop parachute log in Downloads folder
                dropCrashParachute(context, thread, throwable, stackTraceString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to drop crash parachute", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun dropCrashParachute(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        stackTraceString: String
    ) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "OmniRoot_CRASH_PARACHUTE_$timeStamp.txt"

        val targetDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.filesDir
        )

        for (dir in targetDirs) {
            try {
                if (!dir.exists()) dir.mkdirs()
                val crashFile = File(dir, fileName)
                crashFile.printWriter().use { out ->
                    out.println("=======================================================")
                    out.println("          OMNIROOT EMERGENCY CRASH PARACHUTE           ")
                    out.println("=======================================================")
                    out.println("Timestamp:   ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                    out.println("Thread:      ${thread.name} (id: ${thread.id})")
                    out.println("Exception:   ${throwable.javaClass.name}")
                    out.println("Message:     ${throwable.message}")
                    out.println("Device:      ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
                    out.println("Package:     ${context.packageName}")
                    out.println("\n----------------- FATAL STACK TRACE -----------------")
                    out.println(stackTraceString)
                    out.println("\n----------------- PRE-CRASH ACTIVE LOGS -------------")
                    _logs.value.takeLast(100).forEach { entry ->
                        val entryTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
                        out.println("[$entryTime] [${entry.type}] ${entry.component}: ${entry.message}")
                        if (!entry.stackTrace.isNullOrBlank()) {
                            out.println("   Trace: ${entry.stackTrace.lines().take(3).joinToString(" ")}")
                        }
                    }
                    out.println("=======================================================")
                }
                Log.e(TAG, "Emergency Crash Parachute dropped successfully to: ${crashFile.absolutePath}")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error writing crash parachute to ${dir.absolutePath}", e)
            }
        }
    }

    private fun loadLogsFromDisk() {
        val file = logFile ?: return
        if (!file.exists()) return

        try {
            val list = mutableListOf<LogEntry>()
            file.forEachLine { line ->
                if (line.isNotBlank()) {
                    try {
                        val obj = JSONObject(line)
                        list.add(
                            LogEntry(
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                type = obj.optString("type", "INFO"),
                                component = obj.optString("component", "Unknown"),
                                message = obj.optString("message", ""),
                                stackTrace = if (obj.has("stackTrace") && !obj.isNull("stackTrace")) obj.getString("stackTrace") else null
                            )
                        )
                    } catch (_: Exception) {
                        // ignore malformed line
                    }
                }
            }
            _logs.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load persistent logs", e)
        }
    }

    fun toggle(enabled: Boolean) {
        _isEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun log(type: String, component: String, message: String, stackTrace: String? = null) {
        if (!_isEnabled.value) return

        val sanitizedMessage = sanitize(message)
        val sanitizedStackTrace = stackTrace?.let { sanitize(it) }

        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            type = type,
            component = component,
            message = sanitizedMessage,
            stackTrace = sanitizedStackTrace
        )
        
        _logs.value = _logs.value + entry
        Log.d(TAG, "[$type] $component: $sanitizedMessage")

        // Persist to disk asynchronously
        scope.launch {
            try {
                logFile?.let { file ->
                    val obj = JSONObject().apply {
                        put("timestamp", entry.timestamp)
                        put("type", entry.type)
                        put("component", entry.component)
                        put("message", entry.message)
                        if (entry.stackTrace != null) {
                            put("stackTrace", entry.stackTrace)
                        }
                    }
                    file.appendText(obj.toString() + "\n")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to append log entry to disk", e)
            }
        }
    }

    fun exportAndClear(context: Context) {
        if (_logs.value.isEmpty()) return

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            downloadsDir
        } else {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(targetDir, "OmniRoot_Log_$timestamp.txt")
        
        try {
            file.printWriter().use { out ->
                out.println("--- OmniRoot Log Export ---")
                out.println("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                out.println("----------------------------------------")
                _logs.value.forEach { entry ->
                    val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
                    out.println("[$timeString] [${entry.type}] ${entry.component}")
                    out.println("Message: ${entry.message}")
                    if (entry.stackTrace != null) {
                        out.println("StackTrace:\n${entry.stackTrace}")
                    }
                    out.println("----------------------------------------")
                }
            }
            _logs.value = emptyList() // clear active log state
            logFile?.delete() // clear disk file
            Log.d(TAG, "Logs exported and cleared to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export logs", e)
        }
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("(?i)(password|secret|key|token|credential)[\\s=:]+[^\\s,;]+"), "$1=***SANITIZED***")
    }
}
