package com.example.aira.security

import android.content.Context
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VoiceProfileStorage(private val context: Context) {

    companion object {
        private const val TAG = "VoiceProfileStorage"
        private const val PROFILE_FILE_NAME = "owner_voice_profile.bin"
        private const val PROFILE_MAGIC = 0x41495241 // 'AIRA'
    }

    private val profileFile: File
        get() = File(context.filesDir, PROFILE_FILE_NAME)

    fun hasEnrolledProfile(): Boolean {
        return profileFile.exists() && profileFile.length() > 64L
    }

    @Synchronized
    fun saveProfile(embedding: FloatArray): Boolean {
        return try {
            val file = profileFile
            val tempFile = File(context.filesDir, "$PROFILE_FILE_NAME.tmp")
            val dos = DataOutputStream(FileOutputStream(tempFile))

            dos.writeInt(PROFILE_MAGIC)
            dos.writeInt(embedding.size)
            for (value in embedding) {
                dos.writeFloat(value)
            }
            dos.flush()
            dos.close()

            if (file.exists()) file.delete()
            tempFile.renameTo(file)
            Log.i(TAG, "Successfully stored owner voice profile (${embedding.size} dimensions)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving voice profile", e)
            false
        }
    }

    @Synchronized
    fun loadProfile(): FloatArray? {
        val file = profileFile
        if (!file.exists()) return null

        return try {
            val dis = DataInputStream(FileInputStream(file))
            val magic = dis.readInt()
            if (magic != PROFILE_MAGIC) {
                Log.w(TAG, "Corrupt voice profile magic header")
                dis.close()
                return null
            }

            val size = dis.readInt()
            if (size <= 0 || size > 4096) {
                Log.w(TAG, "Invalid embedding dimension: $size")
                dis.close()
                return null
            }

            val embedding = FloatArray(size)
            for (i in 0 until size) {
                embedding[i] = dis.readFloat()
            }
            dis.close()
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "Error reading voice profile", e)
            null
        }
    }

    @Synchronized
    fun clearProfile(): Boolean {
        return try {
            if (profileFile.exists()) {
                profileFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing voice profile", e)
            false
        }
    }
}
