package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed interface ApkDownloadState {
  data object Idle : ApkDownloadState

  data class Downloading(val progress: Float) : ApkDownloadState

  data class Completed(val displayPath: String, val shareableFile: File) : ApkDownloadState

  data class Failed(val message: String) : ApkDownloadState
}

object ApkDownloader {
  private const val APK_MIME = "application/vnd.android.package-archive"
  private const val FILE_NAME = "VibeShare-v1.0.apk"

  fun getApkSizeBytes(context: Context): Long {
    return try {
      File(context.applicationInfo.sourceDir).length()
    } catch (_: Exception) {
      0L
    }
  }

  fun downloadInstalledApk(context: Context): Flow<ApkDownloadState> =
    flow {
        emit(ApkDownloadState.Downloading(0.05f))
        val sourceApk = File(context.applicationInfo.sourceDir)
        if (!sourceApk.exists() || !sourceApk.canRead()) {
          emit(ApkDownloadState.Failed("App package file is unavailable on this device."))
          return@flow
        }

        val totalBytes = sourceApk.length().coerceAtLeast(1L)
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val shareableCopy = File(exportsDir, FILE_NAME)

        val displayLocation: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val resolver = context.contentResolver
          val values =
            ContentValues().apply {
              put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
              put(MediaStore.Downloads.MIME_TYPE, APK_MIME)
              put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
              put(MediaStore.Downloads.IS_PENDING, 1)
            }
          val itemUri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
              ?: throw IllegalStateException("Could not create file in Downloads.")

          try {
            resolver.openOutputStream(itemUri)?.use { mediaOut ->
              shareableCopy.outputStream().use { cacheOut ->
                FileInputStream(sourceApk).use { input ->
                  val buffer = ByteArray(64 * 1024)
                  var copied = 0L
                  var read: Int
                  while (input.read(buffer).also { read = it } != -1) {
                    mediaOut.write(buffer, 0, read)
                    cacheOut.write(buffer, 0, read)
                    copied += read
                    emit(ApkDownloadState.Downloading((copied.toFloat() / totalBytes).coerceIn(0.05f, 0.99f)))
                  }
                  mediaOut.flush()
                  cacheOut.flush()
                }
              }
            } ?: throw IllegalStateException("Failed to open Downloads output stream.")

            val finalizeValues =
              ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            resolver.update(itemUri, finalizeValues, null, null)
            displayLocation = "Downloads/$FILE_NAME"
          } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            throw e
          }
        } else {
          val downloadsDir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: exportsDir
          if (!downloadsDir.exists()) downloadsDir.mkdirs()
          val destFile = File(downloadsDir, FILE_NAME)

          destFile.outputStream().use { destOut ->
            shareableCopy.outputStream().use { cacheOut ->
              FileInputStream(sourceApk).use { input ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                  destOut.write(buffer, 0, read)
                  cacheOut.write(buffer, 0, read)
                  copied += read
                  emit(ApkDownloadState.Downloading((copied.toFloat() / totalBytes).coerceIn(0.05f, 0.99f)))
                }
                destOut.flush()
                cacheOut.flush()
              }
            }
          }
          displayLocation = destFile.absolutePath
        }

        emit(ApkDownloadState.Completed(displayPath = displayLocation, shareableFile = shareableCopy))
      }
      .catch { e ->
        emit(ApkDownloadState.Failed(e.localizedMessage ?: "Failed to download APK."))
      }
      .flowOn(Dispatchers.IO)

  fun shareApkFile(context: Context, apkFile: File) {
    val uri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = APK_MIME
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "VibeShare Android App (APK)")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, "Share VibeShare APK"))
  }
}
