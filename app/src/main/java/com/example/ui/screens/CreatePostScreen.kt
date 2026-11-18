package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PostType
import com.example.data.model.PostVisibility
import com.example.util.compressImageUriToBase64
import com.example.util.decodeBase64ToBitmap
import com.example.util.extractAudioMetadata
import com.example.util.extractDomain
import com.example.util.extractFileMetadata
import com.example.util.extractVideoMetadata
import com.example.util.formatDuration
import com.example.util.formatFileSize
import com.example.util.persistReadUriPermission
import kotlinx.coroutines.launch

@Composable
fun CreatePostScreen(
  initialType: PostType,
  isPublishing: Boolean,
  onPublish: (
    postType: PostType,
    visibility: PostVisibility,
    caption: String,
    textBgHex: String,
    title: String,
    subtitle: String,
    mediaUrl: String,
    localUri: String,
    mediaBase64: String,
    fileName: String,
    mimeType: String,
    fileSizeBytes: Long,
    durationMs: Long,
  ) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedType by remember(initialType) { mutableStateOf(initialType) }
  var visibility by remember { mutableStateOf(PostVisibility.PUBLIC) }
  var caption by remember { mutableStateOf("") }
  val textBgOptions = listOf("", "#5B4BFF", "#FF3B5C", "#0EA5E9", "#10B981", "#8B5CF6")
  var textBgHex by remember { mutableStateOf("#5B4BFF") }
  var title by remember { mutableStateOf("") }
  var subtitle by remember { mutableStateOf("") }
  var mediaUrl by remember { mutableStateOf("") }
  var localUri by remember { mutableStateOf("") }
  var mediaBase64 by remember { mutableStateOf("") }
  var fileName by remember { mutableStateOf("") }
  var mimeType by remember { mutableStateOf("") }
  var fileSizeBytes by remember { mutableLongStateOf(0L) }
  var durationMs by remember { mutableLongStateOf(0L) }
  var isProcessingMedia by remember { mutableStateOf(false) }

  fun resetAttachments() {
    title = ""
    subtitle = ""
    mediaUrl = ""
    localUri = ""
    mediaBase64 = ""
    fileName = ""
    mimeType = ""
    fileSizeBytes = 0L
    durationMs = 0L
  }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          persistReadUriPermission(context, uri)
          localUri = uri.toString()
          mediaBase64 = compressImageUriToBase64(context, uri) ?: ""
          val meta = extractFileMetadata(context, uri)
          fileName = meta.fileName
          mimeType = meta.mimeType
          fileSizeBytes = meta.fileSizeBytes
          isProcessingMedia = false
        }
      }
    }

  val videoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          persistReadUriPermission(context, uri)
          localUri = uri.toString()
          val meta = extractVideoMetadata(context, uri)
          fileName = meta.fileName
          if (title.isBlank()) title = meta.fileName.substringBeforeLast('.')
          mimeType = meta.mimeType
          fileSizeBytes = meta.fileSizeBytes
          durationMs = meta.durationMs
          mediaBase64 = meta.thumbnailBase64
          isProcessingMedia = false
        }
      }
    }

  val audioPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          persistReadUriPermission(context, uri)
          localUri = uri.toString()
          val meta = extractAudioMetadata(context, uri)
          title = meta.title
          subtitle = meta.artist
          durationMs = meta.durationMs
          mediaBase64 = meta.albumArtBase64
          fileName = meta.fileName
          mimeType = meta.mimeType
          fileSizeBytes = meta.fileSizeBytes
          isProcessingMedia = false
        }
      }
    }

  val filePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        isProcessingMedia = true
        scope.launch {
          persistReadUriPermission(context, uri)
          localUri = uri.toString()
          val meta = extractFileMetadata(context, uri)
          fileName = meta.fileName
          if (title.isBlank()) title = meta.fileName
          mimeType = meta.mimeType
          fileSizeBytes = meta.fileSizeBytes
          isProcessingMedia = false
        }
      }
    }

  val hasContentReady =
    when (selectedType) {
      PostType.TEXT -> caption.isNotBlank()
      PostType.IMAGE -> mediaBase64.isNotBlank() || mediaUrl.isNotBlank() || localUri.isNotBlank()
      PostType.VIDEO -> localUri.isNotBlank() || mediaUrl.isNotBlank()
      PostType.SONG -> localUri.isNotBlank() || mediaUrl.isNotBlank() || title.isNotBlank()
      PostType.FILE -> localUri.isNotBlank() || mediaUrl.isNotBlank() || fileName.isNotBlank()
      PostType.LINK -> mediaUrl.isNotBlank()
    }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
      Text(
        text = "Create Post",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
      )
      Text(
        text = "Share text, photos, videos, songs, files, or web links",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(14.dp))

      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PostType.entries) { type ->
          val icon =
            when (type) {
              PostType.TEXT -> Icons.Filled.TextFields
              PostType.IMAGE -> Icons.Filled.Image
              PostType.VIDEO -> Icons.Filled.PlayCircle
              PostType.SONG -> Icons.Filled.AudioFile
              PostType.FILE -> Icons.Filled.AttachFile
              PostType.LINK -> Icons.Filled.Link
            }
          FilterChip(
            selected = selectedType == type,
            onClick = {
              if (selectedType != type) {
                selectedType = type
                resetAttachments()
              }
            },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
            label = { Text(type.label) },
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = postTypeColor(type),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White,
              ),
            modifier = Modifier.testTag("create_tab_${type.name.lowercase()}"),
          )
        }
      }

      Spacer(Modifier.height(16.dp))

      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(22.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          when (selectedType) {
            PostType.TEXT -> {
              Text(
                text = "Card background style",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(Modifier.height(10.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                textBgOptions.forEach { hex ->
                  val swatch =
                    if (hex.isBlank()) MaterialTheme.colorScheme.surfaceVariant
                    else Color(android.graphics.Color.parseColor(hex))
                  Box(
                    modifier =
                      Modifier.size(34.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                          width = if (textBgHex == hex) 3.dp else 1.dp,
                          color =
                            if (textBgHex == hex) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline,
                          shape = CircleShape,
                        )
                        .clickable { textBgHex = hex }
                  )
                }
              }
            }

            PostType.IMAGE -> {
              OutlinedButton(
                onClick = {
                  photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pick_photo_button"),
              ) {
                Icon(Icons.Filled.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Photo from Gallery")
              }
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("Or paste image URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            PostType.VIDEO -> {
              OutlinedButton(
                onClick = {
                  videoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                  )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pick_video_button"),
              ) {
                Icon(Icons.Filled.PlayCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Video from Device")
              }
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 180) title = it },
                label = { Text("Video title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("Or paste MP4 / video URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            PostType.SONG -> {
              OutlinedButton(
                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pick_song_button"),
              ) {
                Icon(Icons.Filled.AudioFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Song / Audio File")
              }
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 180) title = it },
                label = { Text("Song title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = subtitle,
                onValueChange = { if (it.length <= 240) subtitle = it },
                label = { Text("Artist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("Or paste MP3 / audio URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            PostType.FILE -> {
              OutlinedButton(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pick_file_button"),
              ) {
                Icon(Icons.Filled.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Any Document or File")
              }
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 180) title = it },
                label = { Text("Document display title (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = mediaUrl,
                onValueChange = { mediaUrl = it },
                label = { Text("Or paste file download URL (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }

            PostType.LINK -> {
              OutlinedTextField(
                value = mediaUrl,
                onValueChange = {
                  mediaUrl = it
                  if (subtitle.isBlank() && it.contains('.')) {
                    subtitle = extractDomain(it)
                  }
                },
                label = { Text("Website or resource URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("link_url_input"),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 180) title = it },
                label = { Text("Link headline") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("link_title_input"),
              )
              Spacer(Modifier.height(10.dp))
              OutlinedTextField(
                value = subtitle,
                onValueChange = { if (it.length <= 240) subtitle = it },
                label = { Text("Short description") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (isProcessingMedia) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
              Spacer(Modifier.width(10.dp))
              Text("Preparing attachment...", style = MaterialTheme.typography.bodySmall)
            }
          } else if (localUri.isNotBlank() || mediaBase64.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                val previewBmp = remember(mediaBase64) { decodeBase64ToBitmap(mediaBase64) }
                if (previewBmp != null) {
                  Image(
                    bitmap = previewBmp.asImageBitmap(),
                    contentDescription = "Selected preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                  )
                } else {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = postTypeColor(selectedType),
                    modifier = Modifier.size(32.dp),
                  )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = fileName.ifBlank { title.ifBlank { "Attached media" } },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                  )
                  val details =
                    buildList {
                        if (fileSizeBytes > 0L) add(formatFileSize(fileSizeBytes))
                        if (durationMs > 0L) add(formatDuration(durationMs))
                        if (mimeType.isNotBlank()) add(mimeType)
                      }
                      .joinToString(" • ")
                  if (details.isNotBlank()) {
                    Text(
                      text = details,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(16.dp))

      OutlinedTextField(
        value = caption,
        onValueChange = { if (it.length <= 1000) caption = it },
        label = { Text(if (selectedType == PostType.TEXT) "What's on your mind?" else "Write a caption or message...") },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("post_caption_input"),
      )

      Spacer(Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Visibility:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PostVisibility.entries.forEach { vis ->
          val selected = visibility == vis
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (selected) MaterialTheme.colorScheme.secondaryContainer
              else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable { visibility = vis },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
              Icon(
                imageVector = if (vis == PostVisibility.PUBLIC) Icons.Filled.Public else Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint =
                  if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = vis.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(22.dp))

      Button(
        onClick = {
          onPublish(
            selectedType,
            visibility,
            caption,
            if (selectedType == PostType.TEXT) textBgHex else "",
            title,
            subtitle,
            mediaUrl,
            localUri,
            mediaBase64,
            fileName,
            mimeType,
            fileSizeBytes,
            durationMs,
          )
        },
        enabled = !isPublishing && !isProcessingMedia && (hasContentReady || caption.isNotBlank()),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("publish_post_button"),
      ) {
        if (isPublishing) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
          Spacer(Modifier.width(10.dp))
          Text("Publishing...")
        } else {
          Text("Publish ${selectedType.label} Post", fontWeight = FontWeight.Bold)
        }
      }

      Spacer(Modifier.height(24.dp))
    }
  }
}
