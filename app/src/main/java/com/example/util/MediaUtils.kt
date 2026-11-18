package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.google.firebase.Timestamp
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FileMeta(
  val fileName: String,
  val mimeType: String,
  val fileSizeBytes: Long,
)

data class AudioMeta(
  val title: String,
  val artist: String,
  val durationMs: Long,
  val albumArtBase64: String,
  val fileName: String,
  val mimeType: String,
  val fileSizeBytes: Long,
)

data class VideoMeta(
  val fileName: String,
  val mimeType: String,
  val fileSizeBytes: Long,
  val durationMs: Long,
  val thumbnailBase64: String,
)

fun persistReadUriPermission(context: Context, uri: Uri) {
  try {
    context.contentResolver.takePersistableUriPermission(
      uri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION,
    )
  } catch (_: Exception) {
    // Photo Picker URIs or transient providers may not allow persistable grants; ignore safely.
  }
}

suspend fun compressImageUriToBase64(
  context: Context,
  uri: Uri,
  maxDimension: Int = 800,
  maxByteLength: Int = 280_000,
): String? =
  withContext(Dispatchers.IO) {
    try {
      val resolver = context.contentResolver
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
      if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

      var sampleSize = 1
      while (
        bounds.outWidth / sampleSize > maxDimension * 2 ||
          bounds.outHeight / sampleSize > maxDimension * 2
      ) {
        sampleSize *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
      val decoded =
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
          ?: return@withContext null

      val scaled = scaleBitmapDown(decoded, maxDimension)
      val result = compressBitmapToBase64(scaled, maxByteLength)
      if (scaled !== decoded) decoded.recycle()
      scaled.recycle()
      result
    } catch (_: Exception) {
      null
    }
  }

fun decodeBase64ToBitmap(base64: String): Bitmap? {
  if (base64.isBlank()) return null
  return try {
    val bytes = Base64.decode(base64, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
  } catch (_: Exception) {
    null
  }
}

suspend fun extractFileMetadata(context: Context, uri: Uri): FileMeta =
  withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var name = "attachment"
    var size = 0L
    try {
      resolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
          if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
        }
      }
    } catch (_: Exception) {}
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    FileMeta(fileName = name, mimeType = mime, fileSizeBytes = size)
  }

suspend fun extractAudioMetadata(context: Context, uri: Uri): AudioMeta =
  withContext(Dispatchers.IO) {
    val fileMeta = extractFileMetadata(context, uri)
    val retriever = MediaMetadataRetriever()
    var title = fileMeta.fileName.substringBeforeLast('.')
    var artist = "Unknown Artist"
    var durationMs = 0L
    var artBase64 = ""
    try {
      retriever.setDataSource(context, uri)
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf {
        it.isNotBlank()
      }?.let { title = it }
      retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf {
        it.isNotBlank()
      }?.let { artist = it }
      durationMs =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
          ?: 0L
      val picture = retriever.embeddedPicture
      if (picture != null && picture.isNotEmpty()) {
        val rawBmp = BitmapFactory.decodeByteArray(picture, 0, picture.size)
        if (rawBmp != null) {
          val scaled = scaleBitmapDown(rawBmp, 400)
          artBase64 = compressBitmapToBase64(scaled, 120_000) ?: ""
          if (scaled !== rawBmp) rawBmp.recycle()
          scaled.recycle()
        }
      }
    } catch (_: Exception) {} finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }
    AudioMeta(
      title = title.ifBlank { fileMeta.fileName },
      artist = artist,
      durationMs = durationMs,
      albumArtBase64 = artBase64,
      fileName = fileMeta.fileName,
      mimeType = fileMeta.mimeType,
      fileSizeBytes = fileMeta.fileSizeBytes,
    )
  }

suspend fun extractVideoMetadata(context: Context, uri: Uri): VideoMeta =
  withContext(Dispatchers.IO) {
    val fileMeta = extractFileMetadata(context, uri)
    val retriever = MediaMetadataRetriever()
    var durationMs = 0L
    var thumbBase64 = ""
    try {
      retriever.setDataSource(context, uri)
      durationMs =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
          ?: 0L
      val frame = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      if (frame != null) {
        val scaled = scaleBitmapDown(frame, 640)
        thumbBase64 = compressBitmapToBase64(scaled, 180_000) ?: ""
        if (scaled !== frame) frame.recycle()
        frame.recycle()
      }
    } catch (_: Exception) {} finally {
      try {
        retriever.release()
      } catch (_: Exception) {}
    }
    VideoMeta(
      fileName = fileMeta.fileName,
      mimeType = fileMeta.mimeType,
      fileSizeBytes = fileMeta.fileSizeBytes,
      durationMs = durationMs,
      thumbnailBase64 = thumbBase64,
    )
  }

private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
  val w = bitmap.width
  val h = bitmap.height
  if (w <= maxDim && h <= maxDim) return bitmap
  val ratio = maxDim.toFloat() / max(w, h).toFloat()
  val targetW = (w * ratio).toInt().coerceAtLeast(1)
  val targetH = (h * ratio).toInt().coerceAtLeast(1)
  return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
}

private fun compressBitmapToBase64(bitmap: Bitmap, maxByteLength: Int): String? {
  var quality = 82
  var encoded = ""
  while (quality >= 35) {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    val bytes = out.toByteArray()
    if (bytes.size * 4 / 3 <= maxByteLength || quality <= 35) {
      encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
      break
    }
    quality -= 12
  }
  return encoded.ifBlank { null }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0L) return "Unknown size"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
    mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
    kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
    else -> "$bytes B"
  }
}

fun formatDuration(ms: Long): String {
  if (ms <= 0L) return "0:00"
  val totalSec = ms / 1000L
  val min = totalSec / 60L
  val sec = totalSec % 60L
  return String.format(Locale.US, "%d:%02d", min, sec)
}

fun formatRelativeTime(timestamp: Timestamp?): String {
  if (timestamp == null) return "Just now"
  val diffSec = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
  return when {
    diffSec < 60L -> "Just now"
    diffSec < 3600L -> "${diffSec / 60L}m ago"
    diffSec < 86400L -> "${diffSec / 3600L}h ago"
    diffSec < 604800L -> "${diffSec / 86400L}d ago"
    else -> "${diffSec / 604800L}w ago"
  }
}

fun extractDomain(url: String): String {
  return try {
    val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
    URI(normalized).host?.removePrefix("www.") ?: url
  } catch (_: Exception) {
    url
  }
}

fun normalizeWebUrl(url: String): String {
  val trimmed = url.trim()
  return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
    trimmed
  } else {
    "https://$trimmed"
  }
}
