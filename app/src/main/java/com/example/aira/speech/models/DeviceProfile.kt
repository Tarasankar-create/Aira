package com.example.aira.speech.models

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.util.Log

enum class DeviceProfile(
    val title: String,
    val description: String,
    val recommendedMinRamGb: Int,
    val maxThreads: Int,
    val supportsConversationalAi: Boolean,
    val speakerVerificationAllowed: Boolean
) {
    LITE(
        title = "Lite (4 GB RAM)",
        description = "Optimized for low RAM. Single INT8 ASR model on demand, conversational LLM disabled.",
        recommendedMinRamGb = 4,
        maxThreads = 2,
        supportsConversationalAi = false,
        speakerVerificationAllowed = true
    ),
    BALANCED(
        title = "Balanced (6 GB RAM)",
        description = "Default profile. FastConformer/IndicConformer INT8 models, speaker verification on demand.",
        recommendedMinRamGb = 6,
        maxThreads = 2,
        supportsConversationalAi = true,
        speakerVerificationAllowed = true
    ),
    ENHANCED(
        title = "Enhanced (8 GB RAM)",
        description = "High responsiveness. Parallel speaker verification & full conversational AI support.",
        recommendedMinRamGb = 8,
        maxThreads = 4,
        supportsConversationalAi = true,
        speakerVerificationAllowed = true
    ),
    ADVANCED(
        title = "Advanced (12+ GB RAM)",
        description = "Maximum throughput for flagship hardware. Extended context and multi-model pipeline.",
        recommendedMinRamGb = 12,
        maxThreads = 4,
        supportsConversationalAi = true,
        speakerVerificationAllowed = true
    );

    companion object {
        private const val TAG = "DeviceProfileManager"

        fun detectProfile(context: Context): DeviceProfile {
            val totalRamGb = getTotalRamGb(context)
            Log.d(TAG, "Detected total physical device RAM: $totalRamGb GB")

            return when {
                totalRamGb >= 11 -> ADVANCED
                totalRamGb >= 7 -> ENHANCED
                totalRamGb >= 5 -> BALANCED
                else -> LITE
            }
        }

        fun getTotalRamGb(context: Context): Int {
            return try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager?.getMemoryInfo(memInfo)
                val totalBytes = memInfo.totalMem
                val totalGb = (totalBytes / (1024L * 1024L * 1024L)).toInt()
                maxOf(totalGb, 2)
            } catch (e: Exception) {
                Log.w(TAG, "Error checking memory info", e)
                6 // Fallback default to 6GB target
            }
        }

        fun getAvailableMemoryMb(context: Context): Long {
            return try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager?.getMemoryInfo(memInfo)
                memInfo.availMem / (1024L * 1024L)
            } catch (e: Exception) {
                1024L
            }
        }

        fun canSafelyLoadModel(context: Context, requiredMb: Long = 250L): Boolean {
            val avail = getAvailableMemoryMb(context)
            val isSafe = avail > (requiredMb + 200L) // Ensure 200MB safety buffer
            if (!isSafe) {
                Log.w(TAG, "Low memory alert: only ${avail}MB available, required ${requiredMb}MB")
            }
            return isSafe
        }
    }
}
