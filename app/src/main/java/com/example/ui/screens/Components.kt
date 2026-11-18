package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PostType
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.ui.theme.BadgeFile
import com.example.ui.theme.BadgeLink
import com.example.ui.theme.BadgePhoto
import com.example.ui.theme.BadgeSong
import com.example.ui.theme.BadgeText
import com.example.ui.theme.BadgeVideo
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.util.AudioPlaybackState
import com.example.util.decodeBase64ToBitmap
import com.example.util.extractDomain
import com.example.util.formatDuration
import com.example.util.formatFileSize
import com.example.util.formatRelativeTime
import com.example.util.normalizeWebUrl
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AuthorAvatar(
  name: String,
  photoUrl: String,
  size: Dp = 40.dp,
  modifier: Modifier = Modifier,
) {
  if (photoUrl.isNotBlank()) {
    AsyncImage(
      model = photoUrl,
      contentDescription = "$name avatar",
      contentScale = ContentScale.Crop,
      modifier = modifier.size(size).clip(CircleShape),
    )
  } else {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
    Box(
      modifier =
        modifier
          .size(size)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary))),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initial,
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

fun postTypeColor(type: PostType): Color =
  when (type) {
    PostType.TEXT -> BadgeText
    PostType.IMAGE -> BadgePhoto
    PostType.VIDEO -> BadgeVideo
    PostType.SONG -> BadgeSong
    PostType.FILE -> BadgeFile
    PostType.LINK -> BadgeLink
  }

fun formatCompactCount(count: Long): String {
  return when {
    count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
  post: SocialPost,
  isOwnedByCurrentUser: Boolean,
  currentReaction: ReactionType?,
  isSaved: Boolean,
  isFollowingAuthor: Boolean,
  audioState: AudioPlaybackState,
  onLikeClick: () -> Unit,
  onSelectReaction: (ReactionType) -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onSaveClick: () -> Unit,
  onFollowClick: () -> Unit,
  onPlayAudioClick: (String) -> Unit,
  onOpenVideoClick: (SocialPost) -> Unit,
  onOpenLinkOrFileClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val postType = post.resolvedPostType()
  val badgeColor = postTypeColor(postType)
  val isLiked = currentReaction != null
  var menuExpanded by remember { mutableStateOf(false) }
  var showReactionPicker by remember { mutableStateOf(false) }
  var showDoubleTapHeart by remember { mutableStateOf(false) }

  LaunchedEffect(showDoubleTapHeart) {
    if (showDoubleTapHeart) {
      delay(650L)
      showDoubleTapHeart = false
    }
  }

  val heartScale by
    animateFloatAsState(
      targetValue = if (isLiked) 1.15f else 1.0f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "like_scale",
    )

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
        .testTag("post_card_${post.id}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Author Header + Follow Button + Type Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        AuthorAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = post.authorName.ifBlank { "VibeShare Creator" },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatRelativeTime(post.createdAt),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = " • ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
              imageVector = Icons.Outlined.Visibility,
              contentDescription = "Views",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
              text = "${formatCompactCount(post.viewsCount)} views",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("post_views_${post.id}"),
            )
          }
        }

        if (!isOwnedByCurrentUser && post.userId.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(50),
            color =
              if (isFollowingAuthor) MaterialTheme.colorScheme.surfaceVariant
              else MaterialTheme.colorScheme.primaryContainer,
            modifier =
              Modifier.clickable(onClick = onFollowClick)
                .padding(end = 6.dp)
                .testTag("follow_btn_${post.id}"),
          ) {
            Text(
              text = if (isFollowingAuthor) "Following" else "Follow",
              style = MaterialTheme.typography.labelSmall,
              color =
                if (isFollowingAuthor) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }

        Surface(
          color = badgeColor.copy(alpha = 0.14f),
          shape = RoundedCornerShape(50),
        ) {
          Text(
            text = postType.label,
            style = MaterialTheme.typography.labelSmall,
            color = badgeColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }

        if (isOwnedByCurrentUser) {
          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Post options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
              DropdownMenuItem(
                text = { Text("Delete post") },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                onClick = {
                  menuExpanded = false
                  onDeleteClick()
                },
              )
            }
          }
        }
      }

      // Caption (for non-styled-text posts, or if styled text has a separate title)
      val hasStyledTextCard = postType == PostType.TEXT && post.textBgHex.isNotBlank()
      if (post.caption.isNotBlank() && !hasStyledTextCard) {
        Spacer(Modifier.height(12.dp))
        Text(
          text = post.caption,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Spacer(Modifier.height(12.dp))

      // Media Content by Type
      when (postType) {
        PostType.TEXT -> {
          if (hasStyledTextCard) {
            val bg =
              remember(post.textBgHex) {
                try {
                  Color(android.graphics.Color.parseColor(post.textBgHex))
                } catch (_: Exception) {
                  IndigoSecondary
                }
              }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 170.dp)
                  .clip(RoundedCornerShape(18.dp))
                  .background(Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.8f))))
                  .pointerInput(isLiked) {
                    detectTapGestures(
                      onDoubleTap = {
                        showDoubleTapHeart = true
                        if (!isLiked) onLikeClick()
                      }
                    )
                  }
                  .padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = post.caption.ifBlank { post.title },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
              )
              androidx.compose.animation.AnimatedVisibility(
                visible = showDoubleTapHeart,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
              ) {
                Icon(
                  imageVector = Icons.Filled.Favorite,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(72.dp),
                )
              }
            }
          } else if (post.title.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(14.dp),
              )
            }
          }
        }

        PostType.IMAGE -> {
          val bitmap = remember(post.mediaBase64) { decodeBase64ToBitmap(post.mediaBase64) }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(isLiked) {
                  detectTapGestures(
                    onDoubleTap = {
                      showDoubleTapHeart = true
                      if (!isLiked) onLikeClick()
                    }
                  )
                },
            contentAlignment = Alignment.Center,
          ) {
            when {
              bitmap != null -> {
                Image(
                  bitmap = bitmap.asImageBitmap(),
                  contentDescription = post.caption.ifBlank { "Shared photo" },
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 340.dp),
                )
              }
              post.mediaUrl.isNotBlank() || post.localUri.isNotBlank() -> {
                AsyncImage(
                  model = post.mediaUrl.ifBlank { post.localUri },
                  contentDescription = post.caption.ifBlank { "Shared photo" },
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 340.dp),
                )
              }
              else -> {
                Box(
                  modifier = Modifier.fillMaxWidth().height(180.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            androidx.compose.animation.AnimatedVisibility(
              visible = showDoubleTapHeart,
              enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
              exit = scaleOut() + fadeOut(),
            ) {
              Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = CoralPrimary,
                modifier = Modifier.size(84.dp),
              )
            }
          }
        }

        PostType.VIDEO -> {
          val thumbBitmap = remember(post.mediaBase64) { decodeBase64ToBitmap(post.mediaBase64) }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF14151F))
                .clickable { onOpenVideoClick(post) }
                .testTag("video_preview_${post.id}"),
            contentAlignment = Alignment.Center,
          ) {
            if (thumbBitmap != null) {
              Image(
                bitmap = thumbBitmap.asImageBitmap(),
                contentDescription = post.title.ifBlank { "Video thumbnail" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
            }
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)))
            Surface(
              shape = CircleShape,
              color = CoralPrimary.copy(alpha = 0.92f),
              modifier = Modifier.size(60.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = "Play video",
                  tint = Color.White,
                  modifier = Modifier.size(34.dp),
                )
              }
            }
            Row(
              modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = post.title.ifBlank { post.fileName.ifBlank { "Video Clip" } },
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              if (post.durationMs > 0L) {
                Spacer(Modifier.width(8.dp))
                Surface(color = Color.Black.copy(alpha = 0.72f), shape = RoundedCornerShape(8.dp)) {
                  Text(
                    text = formatDuration(post.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            }
          }
        }

        PostType.SONG -> {
          val artBitmap = remember(post.mediaBase64) { decodeBase64ToBitmap(post.mediaBase64) }
          val isThisActive = audioState.activePostId == post.id
          val isPlayingThis = isThisActive && audioState.isPlaying
          val audioSource = post.mediaUrl.ifBlank { post.localUri }

          Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (artBitmap != null) {
                  Image(
                    bitmap = artBitmap.asImageBitmap(),
                    contentDescription = "Album cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
                  )
                } else {
                  Image(
                    painter = painterResource(id = R.drawable.img_song_cover),
                    contentDescription = "Song cover artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
                  )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = post.title.ifBlank { post.fileName.ifBlank { "Audio Track" } },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = post.subtitle.ifBlank { "Shared Song" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  val totalDur =
                    if (isThisActive && audioState.durationMs > 0L) audioState.durationMs
                    else post.durationMs
                  if (totalDur > 0L) {
                    Text(
                      text =
                        if (isThisActive) {
                          "${formatDuration(audioState.currentPositionMs)} / ${formatDuration(totalDur)}"
                        } else {
                          formatDuration(totalDur)
                        },
                      style = MaterialTheme.typography.labelSmall,
                      color = BadgeSong,
                    )
                  }
                }
                Surface(
                  shape = CircleShape,
                  color = BadgeSong,
                  modifier =
                    Modifier.size(48.dp)
                      .clip(CircleShape)
                      .clickable { onPlayAudioClick(audioSource) }
                      .testTag("play_song_${post.id}"),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    if (isThisActive && audioState.isLoading) {
                      CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                      )
                    } else {
                      Icon(
                        imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingThis) "Pause song" else "Play song",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                      )
                    }
                  }
                }
              }

              if (isThisActive && audioState.durationMs > 0L) {
                Spacer(Modifier.height(10.dp))
                val progress =
                  (audioState.currentPositionMs.toFloat() / audioState.durationMs.toFloat())
                    .coerceIn(0f, 1f)
                LinearProgressIndicator(
                  progress = { progress },
                  color = BadgeSong,
                  trackColor = BadgeSong.copy(alpha = 0.2f),
                  modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                )
              }
              if (isThisActive && audioState.error != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                  text = audioState.error,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }
          }
        }

        PostType.FILE -> {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = BadgeFile.copy(alpha = 0.1f),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, BadgeFile.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable {
                  onOpenLinkOrFileClick()
                  val target = post.mediaUrl.ifBlank { post.localUri }
                  if (target.isNotBlank()) {
                    try {
                      val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                          setDataAndType(Uri.parse(target), post.mimeType.ifBlank { "*/*" })
                          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                      context.startActivity(Intent.createChooser(intent, "Open file"))
                    } catch (_: Exception) {}
                  }
                },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(14.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BadgeFile.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.AttachFile, contentDescription = "File attachment", tint = BadgeFile)
              }
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = post.fileName.ifBlank { post.title.ifBlank { "Shared Document" } },
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                val sizeLabel =
                  if (post.fileSizeBytes > 0L) formatFileSize(post.fileSizeBytes)
                  else post.mimeType.ifBlank { "Attachment" }
                Text(
                  text = "$sizeLabel • Tap to open",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open file", tint = BadgeFile)
            }
          }
        }

        PostType.LINK -> {
          val url = post.mediaUrl.ifBlank { post.subtitle }
          val domain = remember(url) { extractDomain(url) }
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = BadgeLink.copy(alpha = 0.08f),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, BadgeLink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .clickable {
                  onOpenLinkOrFileClick()
                  if (url.isNotBlank()) {
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizeWebUrl(url)))
                      context.startActivity(intent)
                    } catch (_: Exception) {}
                  }
                },
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(14.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BadgeLink.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(Icons.Filled.Link, contentDescription = "Web link", tint = BadgeLink)
              }
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = post.title.ifBlank { domain },
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
                if (post.subtitle.isNotBlank() && post.subtitle != url) {
                  Text(
                    text = post.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = domain,
                  style = MaterialTheme.typography.labelSmall,
                  color = BadgeLink,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link", tint = BadgeLink)
            }
          }
        }
      }

      // Facebook-style Reaction Picker Tray (on long-press of Like)
      androidx.compose.animation.AnimatedVisibility(visible = showReactionPicker) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceVariant,
          tonalElevation = 4.dp,
          modifier = Modifier.padding(top = 10.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          ) {
            ReactionType.entries.forEach { reaction ->
              Text(
                text = reaction.emoji,
                style = MaterialTheme.typography.titleLarge,
                modifier =
                  Modifier.clip(CircleShape)
                    .clickable {
                      showReactionPicker = false
                      onSelectReaction(reaction)
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Spacer(Modifier.height(12.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
      Spacer(Modifier.height(6.dp))

      // Social Actions Row: Like/Reaction, Comment, Share, Save
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Like button (tap = toggle, long-press = Facebook reactions tray)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .combinedClickable(
                onClick = onLikeClick,
                onLongClick = { showReactionPicker = !showReactionPicker },
              )
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("like_btn_${post.id}"),
        ) {
          if (currentReaction != null && currentReaction != ReactionType.LIKE) {
            Text(
              text = currentReaction.emoji,
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.scale(heartScale),
            )
          } else {
            Icon(
              imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
              contentDescription = "Like",
              tint = if (isLiked) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(21.dp).scale(heartScale),
            )
          }
          Spacer(Modifier.width(6.dp))
          Text(
            text =
              if (post.likesCount > 0) formatCompactCount(post.likesCount)
              else currentReaction?.label ?: "Like",
            style = MaterialTheme.typography.labelMedium,
            color = if (isLiked) CoralPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
          )
        }

        ActionMetricButton(
          icon = Icons.Outlined.ChatBubbleOutline,
          label = if (post.commentsCount > 0) formatCompactCount(post.commentsCount) else "Comment",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          onClick = onCommentClick,
          testTag = "comment_btn_${post.id}",
        )

        ActionMetricButton(
          icon = Icons.Filled.Share,
          label = if (post.sharesCount > 0) formatCompactCount(post.sharesCount) else "Share",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          onClick = onShareClick,
          testTag = "share_btn_${post.id}",
        )

        IconButton(
          onClick = onSaveClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("save_btn_${post.id}"),
        ) {
          Icon(
            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isSaved) "Remove from saved" else "Save post",
            tint = if (isSaved) IndigoSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ActionMetricButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = tint,
      modifier = Modifier.size(21.dp),
    )
    Spacer(Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = tint,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
