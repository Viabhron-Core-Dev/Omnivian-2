package com.example.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VoiceManager : TextToSpeech.OnInitListener {

    private const val PREFS_NAME = "omniroot_audio_prefs"
    private const val KEY_STT_ENGINE = "stt_engine"
    private const val KEY_SELECTED_MODEL = "selected_model_path"
    private const val KEY_TTS_PITCH = "tts_pitch"
    private const val KEY_TTS_SPEED = "tts_speed"

    const val ENGINE_DIRECT_AUDIO = "direct_audio"
    const val ENGINE_ANDROID_NATIVE = "android_native"
    const val ENGINE_CUSTOM_MODEL = "custom_model"

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    fun init(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                LogKeeper.log("VoiceManager", "TtsInitError", "Could not start TextToSpeech: ${e.message}")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.language = Locale.getDefault()
                isTtsInitialized = true
                LogKeeper.log("VoiceManager", "TtsInitSuccess", "TTS initialized with locale: ${Locale.getDefault()}")
            } catch (e: Exception) {
                LogKeeper.log("VoiceManager", "TtsLocaleError", "Error setting TTS locale: ${e.message}")
            }
        } else {
            LogKeeper.log("VoiceManager", "TtsInitError", "TTS initialization failed with status: $status (TTS voice engine unavailable on device)")
        }
    }

    fun getSttEngine(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_STT_ENGINE, ENGINE_DIRECT_AUDIO) ?: ENGINE_DIRECT_AUDIO
    }

    fun setSttEngine(context: Context, engine: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STT_ENGINE, engine).apply()
        LogKeeper.log("VoiceManager", "EngineChanged", "Audio input mode changed to: $engine")
    }

    fun getSelectedModelPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_MODEL, null)
    }

    fun setSelectedModelPath(context: Context, path: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_MODEL, path).apply()
    }

    fun getTtsPitch(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_TTS_PITCH, 1.0f)
    }

    fun setTtsPitch(context: Context, pitch: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_TTS_PITCH, pitch).apply()
        tts?.setPitch(pitch)
    }

    fun getTtsSpeed(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_TTS_SPEED, 1.0f)
    }

    fun setTtsSpeed(context: Context, speed: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply()
        tts?.setSpeechRate(speed)
    }

    fun getAudioRecordingsDir(context: Context): File {
        return File(context.filesDir, "recordings").apply { mkdirs() }
    }

    fun getAudioModelsDir(context: Context): File {
        return File(context.filesDir, "audio_models").apply { mkdirs() }
    }

    fun listImportedModels(context: Context): List<File> {
        val dir = getAudioModelsDir(context)
        return dir.listFiles()?.filter { it.isFile && (it.name.endsWith(".bin") || it.name.endsWith(".onnx") || it.name.endsWith(".tflite")) } ?: emptyList()
    }

    /**
     * Directly records audio from microphone into an AAC / M4A file without relying
     * on external Speech Recognition services or Google Play Services.
     */
    fun startListening(
        context: Context,
        onAudioRecorded: ((File) -> Unit)? = null,
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopListening()

        try {
            val recordingsDir = getAudioRecordingsDir(context)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val audioFile = File(recordingsDir, "voice_note_$timeStamp.m4a")
            currentRecordingFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isListening.value = true
            LogKeeper.log("VoiceManager", "DirectAudioStarted", "Direct recording started -> ${audioFile.name}")

            // Live amplitude tracking
            amplitudeJob = scope.launch {
                while (isActive && _isListening.value) {
                    try {
                        val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                        val norm = (maxAmp / 32767f).coerceIn(0f, 1f)
                        _amplitude.value = norm
                    } catch (_: Exception) {}
                    delay(100)
                }
            }

        } catch (e: Exception) {
            _isListening.value = false
            val errorMsg = "Could not initialize direct audio recording: ${e.message}"
            LogKeeper.log("VoiceManager", "AudioRecordError", errorMsg, e.stackTraceToString())
            onError(errorMsg)
        }
    }

    /**
     * Stops the active audio recording and returns the recorded File & metadata.
     */
    fun stopListening(onAudioSaved: ((File) -> Unit)? = null) {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitude.value = 0f

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isListening.value = false

            currentRecordingFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    LogKeeper.log("VoiceManager", "DirectAudioSaved", "Audio recording saved: ${file.name} (${file.length()} bytes)")
                    onAudioSaved?.invoke(file)
                }
            }
        } catch (e: Exception) {
            LogKeeper.log("VoiceManager", "AudioStopError", "Error stopping audio recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            _isListening.value = false
        }
    }

    fun speak(context: Context, text: String, onComplete: () -> Unit = {}) {
        if (!isTtsInitialized || tts == null) {
            init(context)
        }

        try {
            tts?.setPitch(getTtsPitch(context))
            tts?.setSpeechRate(getTtsSpeed(context))

            _isSpeaking.value = true
            LogKeeper.log("VoiceManager", "TTSSpeak", "Speaking text of length ${text.length}")

            val utteranceId = "omniroot_tts_${System.currentTimeMillis()}"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } catch (e: Exception) {
            LogKeeper.log("VoiceManager", "TtsSpeakError", "TTS speak failed: ${e.message}")
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }
}
