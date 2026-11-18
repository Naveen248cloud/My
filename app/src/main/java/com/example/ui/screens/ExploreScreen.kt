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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PostType
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.UiState
import com.example.util.AudioPlaybackState
import com.example.util.decodeBase64ToBitmap

@Composable
fun ExploreScreen(
  currentUserId: String,
  feedState: UiState<List<SocialPost>>,
  searchQuery: String,
  likedReactions: Map<String, ReactionType>,
  savedPostIds: Set<String>,
  followingIds: Set<String>,
  audioState: AudioPlaybackState,
  onSearchQueryChange: (String) -> Unit,
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
) {
  var exploreType by remember { mutableStateOf<PostType?>(null) }
  var isGridMode by remember { mutableStateOf(true) }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Search & Explore",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          IconButton(
            onClick = { isGridMode = !isGridMode },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("toggle_search_layout"),
          ) {
            Icon(
              imageVector = if (isGridMode) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
              contentDescription = if (isGridMode) "Switch to feed view" else "Switch to grid view",
            )
          }
        }
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search text, videos, photos, songs, files, links...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { onSearchQueryChange("") }) {
                Icon(Icons.Filled.Close, contentDescription = "Clear search")
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth().testTag("explore_search_input"),
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          item {
            FilterChip(
              selected = exploreType == null,
              onClick = { exploreType = null },
              label = { Text("Trending") },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
          }
          items(PostType.entries) { type ->
            FilterChip(
              selected = exploreType == type,
              onClick = { exploreType = if (exploreType == type) null else type },
              label = { Text("${type.label}s") },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = postTypeColor(type),
                  selectedLabelColor = Color.White,
                ),
            )
          }
        }
      }

      Spacer(Modifier.height(10.dp))

      when (feedState) {
        UiState.Loading -> {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Text(
            text = feedState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp),
          )
        }
        is UiState.Success -> {
          val query = searchQuery.trim().lowercase()
          val results =
            feedState.data
              .filter { post ->
                (exploreType == null || post.resolvedPostType() == exploreType) &&
                  (query.isBlank() ||
                    post.caption.lowercase().contains(query) ||
                    post.title.lowercase().contains(query) ||
                    post.subtitle.lowercase().contains(query) ||
                    post.authorName.lowercase().contains(query) ||
                    post.fileName.lowercase().contains(query) ||
                    post.mediaUrl.lowercase().contains(query))
              }
              .sortedByDescending {
                it.viewsCount + (it.likesCount * 2) + (it.commentsCount * 3) + it.sharesCount
              }

          if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = "No matching posts found.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else if (isGridMode && query.isBlank()) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(2),
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize().testTag("explore_grid"),
            ) {
              items(results, key = { it.id }) { post ->
                LaunchedEffect(post.id) { onPostViewed(post) }
                ExploreGridTile(
                  post = post,
                  onClick = {
                    onPostViewed(post)
                    if (post.resolvedPostType() == PostType.VIDEO) {
                      onOpenVideoClick(post)
                    } else {
                      isGridMode = false
                    }
                  },
                )
              }
            }
          } else {
            VerticalSocialFeed(
              posts = results,
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
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ExploreGridTile(post: SocialPost, onClick: () -> Unit) {
  val type = post.resolvedPostType()
  val badgeColor = postTypeColor(type)
  val thumb = remember(post.mediaBase64) { decodeBase64ToBitmap(post.mediaBase64) }
  val typeIcon =
    when (type) {
      PostType.TEXT -> Icons.Filled.TextFields
      PostType.IMAGE -> Icons.Filled.Image
      PostType.VIDEO -> Icons.Filled.PlayCircle
      PostType.SONG -> Icons.Filled.AudioFile
      PostType.FILE -> Icons.Filled.AttachFile
      PostType.LINK -> Icons.Filled.Link
    }

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .aspectRatio(0.95f)
        .clip(RoundedCornerShape(18.dp))
        .background(badgeColor.copy(alpha = 0.16f))
        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
        .clickable(onClick = onClick)
        .testTag("explore_tile_${post.id}"),
  ) {
    when {
      thumb != null -> {
        Image(
          bitmap = thumb.asImageBitmap(),
          contentDescription = post.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      type == PostType.IMAGE && post.mediaUrl.isNotBlank() -> {
        AsyncImage(
          model = post.mediaUrl,
          contentDescription = post.caption,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      else -> {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Brush.linearGradient(listOf(badgeColor.copy(alpha = 0.85f), badgeColor.copy(alpha = 0.45f))))
              .padding(14.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = post.title.ifBlank { post.caption.ifBlank { type.label } },
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))
          )
    )

    Surface(
      color = Color.Black.copy(alpha = 0.55f),
      shape = RoundedCornerShape(50),
      modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
    ) {
      Icon(
        imageVector = typeIcon,
        contentDescription = type.label,
        tint = Color.White,
        modifier = Modifier.padding(6.dp).size(16.dp),
      )
    }

    Row(
      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = post.authorName.substringBefore(" "),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(Modifier.width(4.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Visibility,
          contentDescription = "Views",
          tint = Color.White,
          modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
          text = formatCompactCount(post.viewsCount),
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
        )
      }
    }
  }
}
