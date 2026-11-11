package com.example.aira.security

import android.content.Context
import android.util.Log
import kotlin.math.sqrt

sealed class EnrollmentStepResult {
    data class InProgress(val currentStep: Int, val totalSteps: Int, val nextPrompt: String) : EnrollmentStepResult()
    data class Completed(val message: String) : EnrollmentStepResult()
    data class Failed(val error: String) : EnrollmentStepResult()
}

/**
 * Manages the offline voice enrollment flow to register the owner's voice.
 */
class VoiceEnrollmentManager(
    private val context: Context,
    private val verificationManager: SpeakerVerificationManager = SpeakerVerificationManager(context),
    private val storage: VoiceProfileStorage = VoiceProfileStorage(context)
) {

    companion object {
        private const val TAG = "VoiceEnrollmentManager"
        const val REQUIRED_SAMPLES = 1
        const val DEFAULT_PROMPT = "Hey Assistant, this is my voice."
    }

    private val collectedEmbeddings = mutableListOf<FloatArray>()

    fun isEnrolled(): Boolean = storage.hasEnrolledProfile()

    fun clearEnrollment(): Boolean {
        collectedEmbeddings.clear()
        return storage.clearProfile()
    }

    /**
     * Enrolls the owner's voice from a recorded PCM audio sample.
     */
    fun enrollVoiceFromAudio(samples: FloatArray, sampleRate: Int = 16000): Result<Unit> {
        val embedding = verificationManager.extractEmbedding(samples, sampleRate)
            ?: return Result.failure(Exception("Voice sample was too short or quiet. Please speak clearly."))

        val success = storage.saveProfile(embedding)
        return if (success) {
            Log.i(TAG, "Owner voice enrolled successfully!")
            Result.success(Unit)
        } else {
            Result.failure(Exception("Failed to save voice profile to disk."))
        }
    }

    fun processAudioSample(samples: FloatArray, sampleRate: Int = 16000): EnrollmentStepResult {
        val result = enrollVoiceFromAudio(samples, sampleRate)
        return if (result.isSuccess) {
            EnrollmentStepResult.Completed("Voice profile enrolled successfully!")
        } else {
            EnrollmentStepResult.Failed(result.exceptionOrNull()?.localizedMessage ?: "Enrollment failed")
        }
    }
}
