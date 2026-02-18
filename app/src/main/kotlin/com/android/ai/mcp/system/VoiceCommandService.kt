package com.android.ai.mcp.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.android.ai.mcp.R
import com.android.ai.mcp.ui.MainActivity

class VoiceCommandService : Service() {

    companion object {
        private const val TAG = "VoiceCommandService"
        private const val CHANNEL_ID = "mcp_voice_channel"
        private const val NOTIFICATION_ID = 4343
        private const val DUPLICATE_SUPPRESSION_WINDOW_MS = 1800L

        private const val ACTION_START = "com.android.ai.mcp.voice.START"
        private const val ACTION_STOP = "com.android.ai.mcp.voice.STOP"
        private const val EXTRA_WAKE_WORD = "extra_wake_word"

        const val ACTION_VOICE_COMMAND = "com.android.ai.mcp.voice.COMMAND"
        const val EXTRA_COMMAND_TEXT = "extra_command_text"

        fun start(context: Context, wakeWord: String) {
            val intent = Intent(context, VoiceCommandService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WAKE_WORD, wakeWord)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceCommandService::class.java))
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var wakeWord: String = "AI"
    private var awaitingCommandAfterWake = false
    private var restartBackoffMs = 700L
    private var lastDispatchedCommandNormalized = ""
    private var lastDispatchedAtMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                wakeWord = intent.getStringExtra(EXTRA_WAKE_WORD)?.trim().takeUnless { it.isNullOrEmpty() } ?: "AI"
                awaitingCommandAfterWake = false
                if (!hasRecordAudioPermission()) {
                    Log.w(TAG, "Ignoring voice service start: RECORD_AUDIO permission not granted")
                    stopSelf()
                    return START_NOT_STICKY
                }
                if (!startForegroundAndListeningSafely()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }

            ACTION_STOP -> {
                stopSelf()
            }

            else -> {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopListeningLoop()
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun startListeningLoop() {
        if (!hasRecordAudioPermission()) {
            stopSelf()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }

        if (speechRecognizer == null) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(recognitionListener)
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Failed to create SpeechRecognizer", e)
                stopSelf()
                return
            }
        }

        restartBackoffMs = 700L
        startListening()
    }

    private fun startListening() {
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            recognizer.cancel()
            recognizer.startListening(intent)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception while starting listening", e)
            stopSelf()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "SpeechRecognizer illegal state, retrying", e)
            scheduleRestart()
        } catch (e: RuntimeException) {
            Log.w(TAG, "SpeechRecognizer runtime failure, retrying", e)
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        if (speechRecognizer == null || !hasRecordAudioPermission()) {
            stopSelf()
            return
        }
        val delayMs = restartBackoffMs.coerceAtMost(5_000L)
        mainHandler.postDelayed(
            { startListening() },
            delayMs
        )
        restartBackoffMs = (restartBackoffMs * 2).coerceAtMost(5_000L)
    }

    private fun stopListeningLoop() {
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            handleResults(results)
            scheduleRestart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            handleResults(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun handleResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        if (matches.isEmpty()) return
        val best = matches.first().trim()
        if (best.isEmpty()) return

        val normalizedWakeWord = wakeWord.trim().lowercase()
        val normalizedSpeech = best.lowercase()

        if (normalizedSpeech.startsWith(normalizedWakeWord)) {
            val tail = best.drop(wakeWord.length).trim()
            if (tail.isNotEmpty()) {
                dispatchVoiceCommand(tail)
                awaitingCommandAfterWake = false
            } else {
                awaitingCommandAfterWake = true
            }
            return
        }

        if (awaitingCommandAfterWake) {
            awaitingCommandAfterWake = false
            dispatchVoiceCommand(best)
        }
    }

    private fun dispatchVoiceCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) return
        val normalized = trimmedCommand.lowercase()
        val now = SystemClock.elapsedRealtime()
        if (
            normalized == lastDispatchedCommandNormalized &&
            now - lastDispatchedAtMs < DUPLICATE_SUPPRESSION_WINDOW_MS
        ) {
            return
        }

        lastDispatchedCommandNormalized = normalized
        lastDispatchedAtMs = now

        val broadcast = Intent(ACTION_VOICE_COMMAND).apply {
            `package` = packageName
            putExtra(EXTRA_COMMAND_TEXT, trimmedCommand)
        }
        sendBroadcast(broadcast)
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startForegroundAndListeningSafely(): Boolean {
        return try {
            startForeground(NOTIFICATION_ID, buildNotification())
            startListeningLoop()
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Unable to start microphone foreground service", e)
            false
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state starting voice foreground service", e)
            false
        } catch (e: RuntimeException) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is android.app.ForegroundServiceStartNotAllowedException
            ) {
                Log.e(TAG, "Foreground service start denied by system state", e)
                false
            } else {
                Log.e(TAG, "Unexpected runtime error starting voice service", e)
                false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle(getString(R.string.voice_notification_title))
        .setContentText(getString(R.string.voice_notification_text, wakeWord))
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .build()

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
}
