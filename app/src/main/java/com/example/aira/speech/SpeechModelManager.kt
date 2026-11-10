package com.example.aira.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class SpeechModelManager(private val context: Context) {

    data class ModelInfo(
        val name: String,
        val displayName: String,
        val description: String,
        val downloadUrl: String,
        val approxSizeMb: Int
    )

    private var currentLoadedModel: Model? = null
    private var currentModelName: String? = null

    companion object {
        private const val TAG = "AiraSpeechModelManager"

        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                name = "vosk-model-small-en-us-0.15",
                displayName = "Small US English (Fast & Lightweight)",
                description = "Recommended for real-time offline performance and 4GB RAM devices (~40MB).",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
                approxSizeMb = 40
            ),
            ModelInfo(
                name = "vosk-model-en-us-0.22-lgraph",
                displayName = "Large Graph US English (High Accuracy)",
                description = "Larger vocabulary model for higher accuracy (~128MB).",
                downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-en-us-0.22-lgraph.zip",
                approxSizeMb = 128
            )
        )
    }

    fun getModelsDirectory(): File {
        val externalDir = context.getExternalFilesDir(null)
        val dir = if (externalDir != null) {
            File(externalDir, "models")
        } else {
            File(context.filesDir, "models")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getModelDir(modelName: String): File {
        return File(getModelsDirectory(), modelName)
    }

    fun isModelInstalled(modelName: String): Boolean {
        val modelDir = getModelDir(modelName)
        return validateModelDirectory(modelDir)
    }

    fun validateModelDirectory(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return false

        // Check for essential Vosk files (either directly or in subdirectories)
        val hasAcousticModel = File(dir, "am/final.mdl").exists() || File(dir, "final.mdl").exists()
        val hasGraph = File(dir, "graph/HCLG.fst").exists() ||
                       File(dir, "graph/Gr.fst").exists() ||
                       File(dir, "HCLG.fst").exists() ||
                       File(dir, "Gr.fst").exists()
        val hasConf = File(dir, "conf/mfcc.conf").exists() || File(dir, "mfcc.conf").exists()

        return (hasAcousticModel || hasGraph || hasConf)
    }

    @Synchronized
    fun getOrLoadModel(modelName: String): Model? {
        if (currentModelName == modelName && currentLoadedModel != null) {
            return currentLoadedModel
        }

        val modelDir = getModelDir(modelName)
        if (!validateModelDirectory(modelDir)) {
            Log.w(TAG, "Model directory not valid: ${modelDir.absolutePath}")
            return null
        }

        // Release previous model to conserve RAM on 4GB devices
        releaseModel()

        return try {
            Log.d(TAG, "Loading Vosk model from ${modelDir.absolutePath}...")
            val model = Model(modelDir.absolutePath)
            currentLoadedModel = model
            currentModelName = modelName
            Log.d(TAG, "Vosk model loaded successfully: $modelName")
            model
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate Vosk model: $modelName", e)
            null
        }
    }

    @Synchronized
    fun releaseModel() {
        currentLoadedModel?.close()
        currentLoadedModel = null
        currentModelName = null
        System.gc()
    }

    suspend fun downloadAndInstallModel(
        modelInfo: ModelInfo,
        onProgress: (Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val targetDir = getModelDir(modelInfo.name)
        val tempZipFile = File(getModelsDirectory(), "${modelInfo.name}.zip.tmp")

        try {
            Log.d(TAG, "Starting download from: ${modelInfo.downloadUrl}")
            var currentUrl = modelInfo.downloadUrl
            var connection: HttpURLConnection
            var redirects = 0
            val maxRedirects = 5

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/109.0 Firefox/109.0")
                    setRequestProperty("Accept-Encoding", "identity")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    redirects++
                    if (redirects > maxRedirects) {
                        return@withContext Result.failure(Exception("Too many HTTP redirects from server"))
                    }
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newLocation.isNullOrBlank()) {
                        currentUrl = if (newLocation.startsWith("http")) newLocation else URL(url, newLocation).toString()
                        continue
                    } else {
                        return@withContext Result.failure(Exception("Redirected with missing Location header"))
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(Exception("HTTP error $responseCode: ${connection.responseMessage}"))
                }
                break
            }

            val fileLength = connection.contentLengthLong
            val expectedBytes = if (fileLength > 0) fileLength else (modelInfo.approxSizeMb * 1024L * 1024L)

            val inputStream = BufferedInputStream(connection.inputStream, 32768)
            val outputStream = FileOutputStream(tempZipFile)

            val data = ByteArray(16384)
            var totalBytesRead: Long = 0
            var bytesRead: Int
            var lastReportedProgress = -1

            while (inputStream.read(data).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                outputStream.write(data, 0, bytesRead)
                if (expectedBytes > 0) {
                    val progress = ((totalBytesRead * 100) / expectedBytes).toInt().coerceIn(0, 99)
                    if (progress != lastReportedProgress) {
                        lastReportedProgress = progress
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            onProgress(100)
            Log.d(TAG, "Download finished ($totalBytesRead bytes). Unzipping model to ${getModelsDirectory().absolutePath}...")

            // Clean up any stale partial target directory before extracting
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }

            unzip(tempZipFile, getModelsDirectory())

            if (tempZipFile.exists()) {
                tempZipFile.delete()
            }

            if (validateModelDirectory(targetDir)) {
                Log.d(TAG, "Model validated successfully: ${modelInfo.name}")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Extracted model validation failed in ${targetDir.name}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model", e)
            if (tempZipFile.exists()) {
                tempZipFile.delete()
            }
            Result.failure(e)
        }
    }

    private fun unzip(zipFile: File, targetDirectory: File) {
        val zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
        var zipEntry = zipInputStream.nextEntry

        while (zipEntry != null) {
            val newFile = File(targetDirectory, zipEntry.name)
            // Guard against zip slip vulnerability
            if (!newFile.canonicalPath.startsWith(targetDirectory.canonicalPath)) {
                throw SecurityException("Zip entry is outside of target dir: ${zipEntry.name}")
            }

            if (zipEntry.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                FileOutputStream(newFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                }
            }
            zipInputStream.closeEntry()
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }
}
