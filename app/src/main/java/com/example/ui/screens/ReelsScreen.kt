package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PostType
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.UiState
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.util.AudioPlaybackState
import com.example.util.decodeBase64ToBitmap

@Composable
fun ReelsScreen(
  currentUserId: String,
  feedState: UiState<List<SocialPost>>,
  likedReactions: Map<String, ReactionType>,
  savedPostIds: Set<String>,
  followingIds: Set<String>,
  audioState: AudioPlaybackState,
  onPostViewed: (SocialPost) -> Unit,
  onLikeClick: (SocialPost) -> Unit,
  onCommentClick: (SocialPost) -> Unit,
  onShareClick: (SocialPost) -> Unit,
  onSaveClick: (SocialPost) -> Unit,
  onFollowClick: (String, String) -> Unit,
  onPlayAudioClick: (SocialPost, String) -> Unit,
  onOpenVideoClick: (SocialPost) -> Unit,
  onCreateVideoClick: () -> Unit,
) {
  Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C12))) {
    when (feedState) {
      UiState.Loading -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = CoralPrimary)
      }
      is UiState.Error -> {
        Text(
          text = feedState.message,
          color = Color.White,
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
        )
      }
      is UiState.Success -> {
        val reelsPosts =
          remember(feedState.data) {
            val prioritized =
              feedState.data.filter {
                it.resolvedPostType() in listOf(PostType.VIDEO, PostType.IMAGE, PostType.SONG)
              }
            prioritized.ifEmpty { feedState.data }
          }

        if (reelsPosts.isEmpty()) {
          Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Filled.PlayCircle,
              contentDescription = null,
              tint = CoralPrimary,
              modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
              text = "No Reels yet",
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
              text = "Upload a video, photo, or song to watch it in full-screen Reels!",
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreateVideoClick) { Text("Upload Reel") }
          }
        } else {
          val pagerState = rememberPagerState(pageCount = { reelsPosts.size })
          VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag("reels_pager"),
          ) { page ->
            val post = reelsPosts[page]
            LaunchedEffect(post.id) { onPostViewed(post) }
            ReelPageCard(
              post = post,
              isOwned = post.userId == currentUserId,
              currentReaction = likedReactions[post.id],
              isSaved = savedPostIds.contains(post.id),
              isFollowing = followingIds.contains(post.userId),
              audioState = audioState,
              onLikeClick = { onLikeClick(post) },
              onCommentClick = { onCommentClick(post) },
              onShareClick = { onShareClick(post) },
              onSaveClick = { onSaveClick(post) },
              onFollowClick = { onFollowClick(post.userId, post.authorName) },
              onPlayAudioClick = { source -> onPlayAudioClick(post, source) },
              onOpenVideoClick = { onOpenVideoClick(post) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReelPageCard(
  post: SocialPost,
  isOwned: Boolean,
  currentReaction: ReactionType?,
  isSaved: Boolean,
  isFollowing: Boolean,
  audioState: AudioPlaybackState,
  onLikeClick: () -> Unit,
  onCommentClick: () -> Unit,
  onShareClick: () -> Unit,
  onSaveClick: () -> Unit,
  onFollowClick: () -> Unit,
  onPlayAudioClick: (String) -> Unit,
  onOpenVideoClick: () -> Unit,
) {
  val type = post.resolvedPostType()
  val bitmap = remember(post.mediaBase64) { decodeBase64ToBitmap(post.mediaBase64) }
  val isLiked = currentReaction != null

  Box(modifier = Modifier.fillMaxSize()) {
    // Background Visual
    when {
      bitmap != null -> {
        Image(
          bitmap = bitmap.asImageBitmap(),
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
      type == PostType.SONG -> {
        Image(
          painter = painterResource(id = R.drawable.img_song_cover),
          contentDescription = post.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      else -> {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Brush.verticalGradient(listOf(IndigoSecondary, Color(0xFF141524)))),
        )
      }
    }

    // Scrim
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                Color.Black.copy(alpha = 0.25f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.82f),
              )
            )
          )
    )

    // Center Play Action for Video or Song
    if (type == PostType.VIDEO) {
      Surface(
        shape = CircleShape,
        color = CoralPrimary.copy(alpha = 0.9f),
        modifier =
          Modifier.align(Alignment.Center)
            .size(74.dp)
            .clip(CircleShape)
            .clickable(onClick = onOpenVideoClick),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play video reel",
            tint = Color.White,
            modifier = Modifier.size(44.dp),
          )
        }
      }
    } else if (type == PostType.SONG) {
      val isPlayingThis = audioState.activePostId == post.id && audioState.isPlaying
      Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        modifier =
          Modifier.align(Alignment.Center)
            .size(74.dp)
            .clip(CircleShape)
            .clickable { onPlayAudioClick(post.mediaUrl.ifBlank { post.localUri }) },
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = "Play song",
            tint = Color.White,
            modifier = Modifier.size(44.dp),
          )
        }
      }
    }

    // Top Views Pill
    Surface(
      shape = RoundedCornerShape(50),
      color = Color.Black.copy(alpha = 0.5f),
      modifier = Modifier.align(Alignment.TopEnd).padding(top = 20.dp, end = 16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Visibility,
          contentDescription = "Views",
          tint = Color.White,
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
          text = "${formatCompactCount(post.viewsCount)} views",
          style = MaterialTheme.typography.labelMedium,
          color = Color.White,
        )
      }
    }

    // Bottom Left Author & Caption
    Column(
      modifier =
        Modifier.align(Alignment.BottomStart)
          .fillMaxWidth(0.78f)
          .padding(start = 16.dp, bottom = 24.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(name = post.authorName, photoUrl = post.authorPhotoUrl, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Text(
          text = post.authorName,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        if (!isOwned && post.userId.isNotBlank()) {
          Spacer(Modifier.width(10.dp))
          Surface(
            shape = RoundedCornerShape(50),
            color = if (isFollowing) Color.White.copy(alpha = 0.2f) else CoralPrimary,
            modifier = Modifier.clickable(onClick = onFollowClick),
          ) {
            Text(
              text = if (isFollowing) "Following" else "Follow",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }
      if (post.title.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(
          text = post.title,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (post.caption.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = post.caption,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.9f),
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // Bottom Right Vertical Action Rail
    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      ReelActionIcon(
        icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        label = formatCompactCount(post.likesCount),
        tint = if (isLiked) CoralPrimary else Color.White,
        onClick = onLikeClick,
      )
      ReelActionIcon(
        icon = Icons.Outlined.ChatBubbleOutline,
        label = formatCompactCount(post.commentsCount),
        tint = Color.White,
        onClick = onCommentClick,
      )
      ReelActionIcon(
        icon = Icons.Filled.Share,
        label = formatCompactCount(post.sharesCount),
        tint = Color.White,
        onClick = onShareClick,
      )
      ReelActionIcon(
        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
        label = if (isSaved) "Saved" else "Save",
        tint = if (isSaved) IndigoSecondary else Color.White,
        onClick = onSaveClick,
      )
    }
  }
}

@Composable
private fun ReelActionIcon(
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
        .padding(4.dp),
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(30.dp))
    Spacer(Modifier.height(2.dp))
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
  }
}
