package com.example.aira.speech

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioRecorder(
    private val onAudioChunk: (ByteArray, Int) -> Unit,
    private val onAmplitudeChanged: ((Float) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    companion object {
        private const val TAG = "AiraAudioRecorder"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val CHUNK_SIZE_BYTES = 3200 // 100ms at 16kHz 16-bit mono
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start(): Boolean {
        if (isRecording.get()) {
            Log.d(TAG, "AudioRecorder is already recording")
            return true
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid buffer size: $minBufferSize")
            return false
        }

        val bufferSize = maxOf(minBufferSize * 2, CHUNK_SIZE_BYTES * 2)

        // Prefer VOICE_RECOGNITION audio source, fallback to MIC if needed
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize with VOICE_RECOGNITION, trying MIC", e)
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to initialize AudioRecord with MIC", ex)
                null
            }
        }

        val record = audioRecord ?: return false
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            record.release()
            audioRecord = null
            return false
        }

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            record.release()
            audioRecord = null
            return false
        }

        isRecording.set(true)
        isPaused.set(false)

        recordingThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            val buffer = ByteArray(CHUNK_SIZE_BYTES)

            while (isRecording.get()) {
                val bytesRead = record.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    if (!isPaused.get()) {
                        onAudioChunk(buffer, bytesRead)
                    }

                    // Compute amplitude/RMS for UI visualizer
                    onAmplitudeChanged?.let { callback ->
                        var sum = 0.0
                        var sampleCount = 0
                        var i = 0
                        while (i < bytesRead - 1) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            val shortSample = sample.toShort()
                            sum += shortSample * shortSample
                            sampleCount++
                            i += 2
                        }
                        if (sampleCount > 0) {
                            val rms = sqrt(sum / sampleCount).toFloat()
                            val normalized = (rms / 32767f).coerceIn(0f, 1f)
                            callback(normalized)
                        }
                    }
                } else if (bytesRead < 0) {
                    Log.w(TAG, "AudioRecord read returned error code: $bytesRead")
                }
            }
        }, "AiraAudioRecordingThread").apply {
            start()
        }

        Log.d(TAG, "Audio recording started successfully")
        return true
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    @Synchronized
    fun stop() {
        if (!isRecording.getAndSet(false)) {
            return
        }
        try {
            recordingThread?.join(500)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Interrupted while joining recording thread", e)
        }
        recordingThread = null

        audioRecord?.let {
            try {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing AudioRecord", e)
            }
        }
        audioRecord = null
        onAmplitudeChanged?.invoke(0f)
        Log.d(TAG, "Audio recording stopped")
    }

    fun isRecording(): Boolean = isRecording.get()
}
