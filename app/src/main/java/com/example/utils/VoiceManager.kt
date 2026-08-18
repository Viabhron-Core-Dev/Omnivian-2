package com.example.utils

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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

    const val ENGINE_ANDROID_NATIVE = "android_native"
    const val ENGINE_DIRECT_AUDIO = "direct_audio"
    const val ENGINE_CUSTOM_MODEL = "custom_model"

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

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
     * Starts listening using the configured STT engine from AudioSettings.
     * Default: Android SpeechRecognizer for real-time speech-to-text conversion.
     */
    fun startListening(
        context: Context,
        onAudioRecorded: ((File) -> Unit)? = null,
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopListening()

        val engine = getSttEngine(context)
        LogKeeper.log("VoiceManager", "StartListening", "Starting voice recognition with engine: $engine")

        if (engine == ENGINE_ANDROID_NATIVE) {
            // Android Native STT (SpeechRecognizer)
            mainHandler.post {
                startSpeechRecognizer(context, onAudioRecorded, onPartialResult, onFinalResult, onError)
            }
        } else {
            // DIRECT_AUDIO or CUSTOM_MODEL -> Direct microphone recording
            startDirectAudioRecording(context, onAudioRecorded, onError)
        }
    }

    private fun startSpeechRecognizer(
        context: Context,
        onAudioRecorded: ((File) -> Unit)?,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            LogKeeper.log("VoiceManager", "SttUnavailable", "Speech recognition unavailable on device, falling back to direct audio recording")
            startDirectAudioRecording(context, onAudioRecorded, onError)
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        LogKeeper.log("VoiceManager", "SttReady", "Speech recognizer ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _amplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _amplitude.value = 0f
                        val errorText = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                            SpeechRecognizer.ERROR_CLIENT -> "Client recognition error."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error for recognition."
                            else -> "Recognition error code: $error"
                        }
                        LogKeeper.log("VoiceManager", "SttError", errorText)
                        
                        // Fallback gracefully to direct audio on client error
                        if (error == SpeechRecognizer.ERROR_CLIENT) {
                            LogKeeper.log("VoiceManager", "SttFallback", "Switching to direct audio mode due to client error")
                            startDirectAudioRecording(context, onAudioRecorded, onError)
                        } else {
                            onError(errorText)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _amplitude.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        LogKeeper.log("VoiceManager", "SttResults", "Final STT text: '$text'")
                        if (text.isNotBlank()) {
                            onFinalResult(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
            val errorMsg = "Could not start SpeechRecognizer: ${e.message}"
            LogKeeper.log("VoiceManager", "SttException", errorMsg)
            startDirectAudioRecording(context, onAudioRecorded, onError)
        }
    }

    private fun startDirectAudioRecording(
        context: Context,
        onAudioRecorded: ((File) -> Unit)?,
        onError: (String) -> Unit
    ) {
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
     * Stops the active audio recording or speech recognition.
     */
    fun stopListening(onAudioSaved: ((File) -> Unit)? = null) {
        amplitudeJob?.cancel()
        amplitudeJob = null
        _amplitude.value = 0f

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                LogKeeper.log("VoiceManager", "SpeechRecognizerStopError", "Error stopping SpeechRecognizer: ${e.message}")
            }
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

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
