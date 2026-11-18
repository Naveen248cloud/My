package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NotificationItem
import com.example.data.model.PostComment
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.SocialStory
import com.example.data.model.UiState
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.util.AudioPlaybackState
import com.example.util.compressImageUriToBase64
import com.example.util.decodeBase64ToBitmap
import com.example.util.formatRelativeTime
import kotlinx.coroutines.launch

/**
 * Reusable vertical scrolling feed component displaying Text, Image, Video, Song, File, and Link
 * posts with view tracking and interactive Like/Reaction, Comment, Save, Share, and Follow actions.
 */
@Composable
fun VerticalSocialFeed(
  posts: List<SocialPost>,
  currentUserId: String,
  likedReactions: Map<String, ReactionType>,
  savedPostIds: Set<String>,
  followingIds: Set<String>,
  audioState: AudioPlaybackState,
  onPostViewed: (SocialPost) -> Unit,
  onLikeClick: (SocialPost) -> Unit,
  onSelectReaction: (SocialPost, ReactionType) -> Unit,
  onCommentClick: (SocialPost) -> Unit,
  onShareClick: (SocialPost) -> Unit,
  onSaveClick: (SocialPost) -> Unit,
  onFollowClick: (String, String) -> Unit,
  onPlayAudioClick: (SocialPost, String) -> Unit,
  onOpenVideoClick: (SocialPost) -> Unit,
  onDeletePostClick: (SocialPost) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
  headerContent: (LazyListScope.() -> Unit)? = null,
  emptyContent: (LazyListScope.() -> Unit)? = null,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("vertical_social_feed"),
    contentPadding = contentPadding,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    headerContent?.invoke(this)
    if (posts.isEmpty()) {
      emptyContent?.invoke(this)
    } else {
      items(posts, key = { it.id }) { post ->
        LaunchedEffect(post.id) { onPostViewed(post) }
        PostCard(
          post = post,
          isOwnedByCurrentUser = post.userId == currentUserId,
          currentReaction = likedReactions[post.id],
          isSaved = savedPostIds.contains(post.id),
          isFollowingAuthor = followingIds.contains(post.userId),
          audioState = audioState,
          onLikeClick = { onLikeClick(post) },
          onSelectReaction = { reaction -> onSelectReaction(post, reaction) },
          onCommentClick = { onCommentClick(post) },
          onShareClick = { onShareClick(post) },
          onSaveClick = { onSaveClick(post) },
          onFollowClick = { onFollowClick(post.userId, post.authorName) },
          onPlayAudioClick = { source ->
            onPostViewed(post)
            onPlayAudioClick(post, source)
          },
          onOpenVideoClick = {
            onPostViewed(post)
            onOpenVideoClick(post)
          },
          onOpenLinkOrFileClick = { onPostViewed(post) },
          onDeleteClick = { onDeletePostClick(post) },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
  post: SocialPost,
  commentsState: UiState<List<PostComment>>,
  onDismiss: () -> Unit,
  onSubmitComment: (String) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var draftComment by remember { mutableStateOf("") }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
    ) {
      val liveCount = (commentsState as? UiState.Success)?.data?.size?.toLong() ?: post.commentsCount
      Text(
        text = "Comments ($liveCount)",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(12.dp))

      when (commentsState) {
        UiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Text(
            text = commentsState.message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 24.dp),
          )
        }
        is UiState.Success -> {
          if (commentsState.data.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxWidth().height(120.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "Be the first to comment on this post!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              contentPadding = PaddingValues(vertical = 4.dp),
            ) {
              items(commentsState.data, key = { it.id }) { comment ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                  AuthorAvatar(
                    name = comment.authorName,
                    photoUrl = comment.authorPhotoUrl,
                    size = 34.dp,
                  )
                  Spacer(Modifier.width(10.dp))
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                      ) {
                        Text(
                          text = comment.authorName,
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                          text = formatRelativeTime(comment.createdAt),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      Spacer(Modifier.height(2.dp))
                      Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(14.dp))
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = draftComment,
          onValueChange = { if (it.length <= 500) draftComment = it },
          placeholder = { Text("Write a comment...") },
          singleLine = true,
          shape = RoundedCornerShape(50),
          modifier = Modifier.weight(1f).testTag("comment_input"),
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (draftComment.isNotBlank()) {
              onSubmitComment(draftComment)
              draftComment = ""
            }
          },
          enabled = draftComment.isNotBlank(),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("send_comment_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send comment",
            tint =
              if (draftComment.isNotBlank()) CoralPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
  notifications: List<NotificationItem>,
  onMarkAllRead: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  LaunchedEffect(Unit) { onMarkAllRead() }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
    ) {
      Text(
        text = "Activity & Notifications",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(Modifier.height(12.dp))
      if (notifications.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Filled.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(44.dp),
          )
          Spacer(Modifier.height(8.dp))
          Text(
            text = "No activity yet. Likes, comments, shares & follows appear here!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(notifications, key = { it.id }) { notif ->
            Surface(
              shape = RoundedCornerShape(14.dp),
              color =
                if (!notif.read) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp),
              ) {
                AuthorAvatar(name = notif.actorName, photoUrl = "", size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = notif.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = formatRelativeTime(notif.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun VideoPlayerDialog(post: SocialPost, onDismiss: () -> Unit) {
  val context = LocalContext.current
  val videoSource = post.mediaUrl.ifBlank { post.localUri }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (videoSource.isNotBlank()) {
          AndroidView(
            factory = { ctx ->
              VideoView(ctx).apply {
                val controller = MediaController(ctx)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(Uri.parse(videoSource))
                setOnPreparedListener { mp ->
                  mp.isLooping = true
                  start()
                }
              }
            },
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
          )
        } else {
          Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Filled.PlayCircle,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = "Video was uploaded from another device without a public stream URL.",
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .align(Alignment.TopCenter)
              .background(Color.Black.copy(alpha = 0.45f))
              .padding(horizontal = 16.dp, vertical = 20.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = post.title.ifBlank { post.authorName },
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
            )
            if (post.caption.isNotBlank()) {
              Text(
                text = post.caption,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
          if (post.mediaUrl.startsWith("http")) {
            TextButton(
              onClick = {
                try {
                  context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.mediaUrl)))
                } catch (_: Exception) {}
              }
            ) {
              Text("Open Browser", color = CoralPrimary)
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close video", tint = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun StoryViewerDialog(
  story: SocialStory,
  isOwned: Boolean,
  onDelete: () -> Unit,
  onDismiss: () -> Unit,
) {
  val bgColor =
    remember(story.bgHex) {
      try {
        Color(android.graphics.Color.parseColor(story.bgHex))
      } catch (_: Exception) {
        IndigoSecondary
      }
    }
  val storyBitmap = remember(story.mediaBase64) { decodeBase64ToBitmap(story.mediaBase64) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
      if (storyBitmap != null) {
        Image(
          bitmap = storyBitmap.asImageBitmap(),
          contentDescription = story.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
      }

      Row(
        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AuthorAvatar(name = story.authorName, photoUrl = story.authorPhotoUrl, size = 38.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(text = story.authorName, color = Color.White, style = MaterialTheme.typography.titleSmall)
          Text(
            text = formatRelativeTime(story.createdAt),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
          )
        }
        if (isOwned) {
          IconButton(
            onClick = {
              onDelete()
              onDismiss()
            }
          ) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete story", tint = Color.White)
          }
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
        }
      }

      Text(
        text = story.caption,
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        modifier = Modifier.align(Alignment.Center).padding(32.dp),
      )
    }
  }
}

@Composable
fun CreateStoryDialog(
  onDismiss: () -> Unit,
  onPublishStory: (caption: String, bgHex: String, mediaBase64: String) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var caption by remember { mutableStateOf("") }
  val palette = listOf("#5B4BFF", "#FF3B5C", "#0EA5E9", "#10B981", "#8B5CF6")
  var selectedHex by remember { mutableStateOf(palette.first()) }
  var selectedBase64 by remember { mutableStateOf("") }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch { selectedBase64 = compressImageUriToBase64(context, uri) ?: "" }
      }
    }

  Dialog(onDismissRequest = onDismiss) {
    Card(shape = RoundedCornerShape(24.dp)) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(text = "Add to Story", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
          value = caption,
          onValueChange = { if (it.length <= 200) caption = it },
          label = { Text("Share a quick status or moment") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth().testTag("story_caption_input"),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          palette.forEach { hex ->
            val c = Color(android.graphics.Color.parseColor(hex))
            Box(
              modifier =
                Modifier.size(34.dp)
                  .clip(CircleShape)
                  .background(c)
                  .border(
                    width = if (selectedHex == hex) 3.dp else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                  )
                  .clickable { selectedHex = hex }
            )
          }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
          onClick = {
            photoPicker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Icon(Icons.Filled.Image, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (selectedBase64.isNotBlank()) "Photo attached" else "Attach optional photo")
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
          Spacer(Modifier.width(8.dp))
          Button(
            onClick = {
              onPublishStory(caption, selectedHex, selectedBase64)
              onDismiss()
            },
            enabled = caption.isNotBlank(),
            modifier = Modifier.testTag("publish_story_button"),
          ) {
            Text("Share Story")
          }
        }
      }
    }
  }
}
