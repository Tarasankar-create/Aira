package com.example.aira.security

import android.content.Context
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

data class VerificationResult(
    val isOwner: Boolean,
    val similarity: Float,
    val threshold: Float,
    val reason: String
)

/**
 * 100% offline speaker biometric verification manager.
 * Extracts acoustic voiceprint embeddings (Mel-filterbank energies, pitch proxy,
 * zero-crossing rate, spectral centroid, spectral tilt) to recognize the owner's voice
 * and reject unauthorized speakers without external native dependencies.
 */
class SpeakerVerificationManager(
    private val context: Context,
    private val storage: VoiceProfileStorage = VoiceProfileStorage(context)
) {

    companion object {
        private const val TAG = "SpeakerVerification"
        const val DEFAULT_THRESHOLD = 0.65f
        const val FEATURE_DIM = 20
    }

    var threshold: Float = DEFAULT_THRESHOLD

    fun isModelAvailable(): Boolean = true

    fun isOwnerEnrolled(): Boolean = storage.hasEnrolledProfile()

    /**
     * Extracts an acoustic voiceprint embedding (20 dimensions) from raw PCM 16kHz audio samples.
     */
    fun extractEmbedding(samples: FloatArray, sampleRate: Int = 16000): FloatArray? {
        if (samples.size < sampleRate / 3) {
            // Need at least ~0.33s of audio to build a valid voiceprint
            return null
        }

        // 1. Voice Activity Filter: isolate active speech frames
        val frameSize = 320 // 20ms at 16kHz
        val activeSamples = mutableListOf<Float>()
        var offset = 0
        while (offset + frameSize <= samples.size) {
            var frameEnergy = 0.0
            for (i in 0 until frameSize) {
                val s = samples[offset + i]
                frameEnergy += s * s
            }
            val rms = sqrt(frameEnergy / frameSize)
            // Filter out silence frames
            if (rms > 0.012) {
                for (i in 0 until frameSize) {
                    activeSamples.add(samples[offset + i])
                }
            }
            offset += frameSize
        }

        val speechData = if (activeSamples.size >= sampleRate / 4) {
            activeSamples.toFloatArray()
        } else {
            samples
        }

        val features = FloatArray(FEATURE_DIM)

        // 2. Compute 16 Mel-spaced frequency sub-band energies via Goertzel discrete filter bank
        val numBands = 16
        val melMin = hzToMel(100.0)
        val melMax = hzToMel(7500.0)
        val melStep = (melMax - melMin) / (numBands + 1)

        val centerFreqs = DoubleArray(numBands) { b ->
            melToHz(melMin + (b + 1) * melStep)
        }

        for (b in 0 until numBands) {
            val fc = centerFreqs[b]
            val omega = 2.0 * PI * fc / sampleRate
            val coeff = 2.0 * cos(omega)

            var q1 = 0.0
            var q2 = 0.0
            for (s in speechData) {
                val q0 = coeff * q1 - q2 + s
                q2 = q1
                q1 = q0
            }
            val power = q1 * q1 + q2 * q2 - coeff * q1 * q2
            features[b] = sqrt(power.coerceAtLeast(0.0) / speechData.size).toFloat()
        }

        // 3. Pitch Proxy via Autocorrelation Peak in human voice range (80 Hz - 350 Hz)
        val minLag = (sampleRate / 350).coerceAtLeast(1)
        val maxLag = (sampleRate / 80).coerceAtMost((speechData.size / 2).coerceAtLeast(2))

        var maxCorr = 0.0
        var bestLag = minLag
        val lagCheckCount = (speechData.size / 2).coerceAtMost(3200)

        for (lag in minLag until maxLag) {
            var corr = 0.0
            for (i in 0 until lagCheckCount) {
                corr += speechData[i] * speechData[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }
        val estimatedPitchHz = (sampleRate.toDouble() / bestLag).coerceIn(80.0, 350.0)
        features[16] = ((estimatedPitchHz - 80.0) / 270.0).toFloat().coerceIn(0f, 1f)

        // 4. Zero-Crossing Rate (vocal articulation proxy)
        var zeroCrossings = 0
        for (i in 1 until speechData.size) {
            if ((speechData[i] >= 0 && speechData[i - 1] < 0) || (speechData[i] < 0 && speechData[i - 1] >= 0)) {
                zeroCrossings++
            }
        }
        features[17] = (zeroCrossings.toDouble() / speechData.size * 4.0).toFloat().coerceIn(0f, 1f)

        // 5. Spectral Centroid proxy (weighted center of gravity of Mel bands)
        var num = 0.0
        var den = 0.0
        for (b in 0 until numBands) {
            num += (b + 1) * features[b]
            den += features[b]
        }
        features[18] = if (den > 1e-6) ((num / den) / numBands).toFloat().coerceIn(0f, 1f) else 0.5f

        // 6. Spectral Tilt (ratio of low frequencies to high frequencies)
        val lowEnergy = (0..5).sumOf { features[it].toDouble() }
        val highEnergy = (10..15).sumOf { features[it].toDouble() }
        features[19] = if (lowEnergy + highEnergy > 1e-6) (lowEnergy / (lowEnergy + highEnergy)).toFloat().coerceIn(0f, 1f) else 0.5f

        // 7. L2-Normalize the final embedding vector
        var normSq = 0.0
        for (f in features) {
            normSq += f * f
        }
        val norm = sqrt(normSq)
        if (norm > 1e-6) {
            for (i in features.indices) {
                features[i] = (features[i] / norm).toFloat()
            }
        }

        return features
    }

    private fun hzToMel(hz: Double): Double = 2595.0 * log10(1.0 + hz / 700.0)
    private fun melToHz(mel: Double): Double = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)

    fun verify(samples: FloatArray, sampleRate: Int = 16000): VerificationResult {
        if (!isOwnerEnrolled()) {
            return VerificationResult(
                isOwner = true,
                similarity = 1.0f,
                threshold = threshold,
                reason = "No voice profile enrolled; open access"
            )
        }

        val enrolledEmbedding = storage.loadProfile()
        if (enrolledEmbedding == null) {
            return VerificationResult(
                isOwner = true,
                similarity = 1.0f,
                threshold = threshold,
                reason = "Could not read enrolled profile; allowing access"
            )
        }

        val currentEmbedding = extractEmbedding(samples, sampleRate)
        if (currentEmbedding == null) {
            Log.w(TAG, "Sample too short for verification; defaulting to reject for security")
            return VerificationResult(
                isOwner = false,
                similarity = 0.0f,
                threshold = threshold,
                reason = "Voice sample too short to verify owner"
            )
        }

        val similarity = computeCosineSimilarity(enrolledEmbedding, currentEmbedding)
        val passed = similarity >= threshold

        Log.i(TAG, "Speaker verification: similarity=$similarity, threshold=$threshold, passed=$passed")

        return VerificationResult(
            isOwner = passed,
            similarity = similarity,
            threshold = threshold,
            reason = if (passed) "Voice matched enrolled owner" else "Voice does not match owner"
        )
    }

    fun computeCosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        if (v1.size != v2.size || v1.isEmpty()) return 1.0f
        var dot = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (i in v1.indices) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }
        val denom = sqrt(norm1) * sqrt(norm2)
        return if (denom > 1e-6) (dot / denom).toFloat() else 1.0f
    }
}
