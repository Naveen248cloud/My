package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.NotificationItem
import com.example.data.model.PostType
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.SocialStory
import com.example.data.model.UiState
import com.example.ui.theme.BadgeFile
import com.example.ui.theme.BadgeLink
import com.example.ui.theme.BadgePhoto
import com.example.ui.theme.BadgeSong
import com.example.ui.theme.BadgeText
import com.example.ui.theme.BadgeVideo
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.StoryGradientEnd
import com.example.ui.theme.StoryGradientMid
import com.example.ui.theme.StoryGradientStart
import com.example.util.AudioPlaybackState
import com.example.util.decodeBase64ToBitmap

@Composable
fun FeedScreen(
  currentUserId: String,
  feedState: UiState<List<SocialPost>>,
  storiesState: UiState<List<SocialStory>>,
  notifications: List<NotificationItem>,
  selectedFilter: PostType?,
  likedReactions: Map<String, ReactionType>,
  savedPostIds: Set<String>,
  followingIds: Set<String>,
  audioState: AudioPlaybackState,
  onSelectFilter: (PostType?) -> Unit,
  onCreatePostClick: (PostType) -> Unit,
  onCreateStory: (caption: String, bgHex: String, mediaBase64: String) -> Unit,
  onDeleteStory: (String) -> Unit,
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
  onMarkNotificationsRead: () -> Unit,
  onOpenSettingsClick: () -> Unit,
) {
  var showCreateStory by remember { mutableStateOf(false) }
  var showNotifications by remember { mutableStateOf(false) }
  var activeStory by remember { mutableStateOf<SocialStory?>(null) }
  val unreadCount = notifications.count { !it.read }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
      // Top Brand Header
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier =
              Modifier.size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary, CyanTertiary))
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Bolt,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(Modifier.width(10.dp))
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { showNotifications = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("notifications_button"),
          ) {
            BadgedBox(
              badge = {
                if (unreadCount > 0) {
                  Badge { Text(if (unreadCount > 9) "9+" else "$unreadCount") }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onBackground,
              )
            }
          }
          IconButton(
            onClick = { onCreatePostClick(PostType.IMAGE) },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("top_create_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Create post",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
          IconButton(
            onClick = onOpenSettingsClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("top_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onBackground,
            )
          }
        }
      }

      when (feedState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          ) {
            Text(
              text = feedState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          val filteredPosts =
            if (selectedFilter == null) feedState.data
            else feedState.data.filter { it.resolvedPostType() == selectedFilter }

          VerticalSocialFeed(
            posts = filteredPosts,
            currentUserId = currentUserId,
            likedReactions = likedReactions,
            savedPostIds = savedPostIds,
            followingIds = followingIds,
            audioState = audioState,
            onPostViewed = onPostViewed,
            onLikeClick = onLikeClick,
            onSelectReaction = onSelectReaction,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onSaveClick = onSaveClick,
            onFollowClick = onFollowClick,
            onPlayAudioClick = onPlayAudioClick,
            onOpenVideoClick = onOpenVideoClick,
            onDeletePostClick = onDeletePostClick,
            headerContent = {
              item {
                val stories = (storiesState as? UiState.Success)?.data.orEmpty()
                StoriesStrip(
                  stories = stories,
                  onAddStoryClick = { showCreateStory = true },
                  onStoryClick = { activeStory = it },
                )
              }
              item { QuickShareCard(onCreatePostClick = onCreatePostClick) }
              item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  item {
                    FilterChip(
                      selected = selectedFilter == null,
                      onClick = { onSelectFilter(null) },
                      label = { Text("All") },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primary,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                      modifier = Modifier.testTag("filter_all"),
                    )
                  }
                  items(PostType.entries) { type ->
                    FilterChip(
                      selected = selectedFilter == type,
                      onClick = { onSelectFilter(if (selectedFilter == type) null else type) },
                      label = { Text(type.label) },
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor = postTypeColor(type),
                          selectedLabelColor = Color.White,
                        ),
                      modifier = Modifier.testTag("filter_${type.name.lowercase()}"),
                    )
                  }
                }
              }
            },
            emptyContent = {
              item {
                EmptyFeedCard(
                  hasFilter = selectedFilter != null,
                  onCreateClick = { onCreatePostClick(selectedFilter ?: PostType.TEXT) },
                )
              }
            },
          )
        }
      }
    }
  }

  if (showCreateStory) {
    CreateStoryDialog(
      onDismiss = { showCreateStory = false },
      onPublishStory = onCreateStory,
    )
  }

  if (showNotifications) {
    NotificationsBottomSheet(
      notifications = notifications,
      onMarkAllRead = onMarkNotificationsRead,
      onDismiss = { showNotifications = false },
    )
  }

  activeStory?.let { story ->
    StoryViewerDialog(
      story = story,
      isOwned = story.userId == currentUserId,
      onDelete = { onDeleteStory(story.id) },
      onDismiss = { activeStory = null },
    )
  }
}

@Composable
private fun StoriesStrip(
  stories: List<SocialStory>,
  onAddStoryClick: () -> Unit,
  onStoryClick: (SocialStory) -> Unit,
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(vertical = 4.dp),
  ) {
    item {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.width(72.dp)
            .clickable(onClick = onAddStoryClick)
            .testTag("add_story_button"),
      ) {
        Box(
          modifier =
            Modifier.size(66.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(2.dp, CoralPrimary, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add story",
            tint = CoralPrimary,
            modifier = Modifier.size(28.dp),
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(
          text = "Your Story",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    items(stories, key = { it.id }) { story ->
      val thumbBitmap = remember(story.mediaBase64) { decodeBase64ToBitmap(story.mediaBase64) }
      val ringBrush =
        Brush.sweepGradient(listOf(StoryGradientStart, StoryGradientMid, StoryGradientEnd, StoryGradientStart))
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier.width(72.dp)
            .clickable { onStoryClick(story) }
            .testTag("story_item_${story.id}"),
      ) {
        Box(
          modifier =
            Modifier.size(66.dp)
              .border(3.dp, ringBrush, CircleShape)
              .padding(4.dp)
              .clip(CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          if (thumbBitmap != null) {
            Image(
              bitmap = thumbBitmap.asImageBitmap(),
              contentDescription = story.authorName,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            AuthorAvatar(name = story.authorName, photoUrl = story.authorPhotoUrl, size = 56.dp)
          }
        }
        Spacer(Modifier.height(6.dp))
        Text(
          text = story.authorName.substringBefore(" "),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun QuickShareCard(onCreatePostClick: (PostType) -> Unit) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onCreatePostClick(PostType.TEXT) }
            .testTag("quick_composer_prompt"),
      ) {
        Text(
          text = "What's on your mind? Post text, photo, video, song, file or link...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
      }
      Spacer(Modifier.height(10.dp))
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        item { QuickTypeChip(Icons.Filled.TextFields, "Text", BadgeText) { onCreatePostClick(PostType.TEXT) } }
        item { QuickTypeChip(Icons.Filled.Image, "Photo", BadgePhoto) { onCreatePostClick(PostType.IMAGE) } }
        item { QuickTypeChip(Icons.Filled.PlayCircle, "Video", BadgeVideo) { onCreatePostClick(PostType.VIDEO) } }
        item { QuickTypeChip(Icons.Filled.AudioFile, "Song", BadgeSong) { onCreatePostClick(PostType.SONG) } }
        item { QuickTypeChip(Icons.Filled.AttachFile, "File", BadgeFile) { onCreatePostClick(PostType.FILE) } }
        item { QuickTypeChip(Icons.Filled.Link, "Link", BadgeLink) { onCreatePostClick(PostType.LINK) } }
      }
    }
  }
}

@Composable
private fun QuickTypeChip(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  tint: Color,
  onClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    Spacer(Modifier.height(2.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
  }
}

@Composable
private fun EmptyFeedCard(hasFilter: Boolean, onCreateClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier =
      Modifier.fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_feed),
        contentDescription = "No posts yet",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(16.dp)),
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = if (hasFilter) "No matching posts yet" else "Your VibeShare Home is ready",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Share text updates, videos, photos, songs, files, or links with the community!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(16.dp))
      Button(onClick = onCreateClick, shape = RoundedCornerShape(14.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Create First Post")
      }
    }
  }
}
