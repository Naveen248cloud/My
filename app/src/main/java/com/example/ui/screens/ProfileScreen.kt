package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.util.AudioPlaybackState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileScreen(
  currentUserId: String,
  userProfile: UserProfile?,
  myPostsState: UiState<List<SocialPost>>,
  publicFeedState: UiState<List<SocialPost>>,
  likedReactions: Map<String, ReactionType>,
  savedPostIds: Set<String>,
  followingIds: Set<String>,
  audioState: AudioPlaybackState,
  onUpdateProfile: (displayName: String, bio: String) -> Unit,
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
  onOpenSettingsClick: () -> Unit,
) {
  val authUser = Firebase.auth.currentUser
  val displayName =
    userProfile?.displayName?.ifBlank { null }
      ?: authUser?.displayName?.ifBlank { null }
      ?: "VibeShare Creator"
  val photoUrl =
    userProfile?.photoUrl?.ifBlank { null }
      ?: authUser?.photoUrl?.toString().orEmpty()
  val bio = userProfile?.bio.orEmpty()

  var selectedTab by remember { mutableIntStateOf(0) }
  var showEditDialog by remember { mutableStateOf(false) }

  val myPosts = (myPostsState as? UiState.Success)?.data.orEmpty()
  val publicPosts = (publicFeedState as? UiState.Success)?.data.orEmpty()
  val allKnownPosts = remember(publicPosts, myPosts) { (publicPosts + myPosts).distinctBy { it.id } }
  val savedPosts =
    remember(allKnownPosts, savedPostIds) { allKnownPosts.filter { savedPostIds.contains(it.id) } }
  val likedPosts =
    remember(allKnownPosts, likedReactions) { allKnownPosts.filter { likedReactions.containsKey(it.id) } }

  val totalLikesReceived = myPosts.sumOf { it.likesCount }
  val totalViewsReceived = myPosts.sumOf { it.viewsCount }

  val activeList =
    when (selectedTab) {
      0 -> myPosts
      1 -> savedPosts
      else -> likedPosts
    }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Profile",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Row {
          IconButton(
            onClick = { showEditDialog = true },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("edit_profile_button"),
          ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
          }
          IconButton(
            onClick = onOpenSettingsClick,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_settings_button"),
          ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
          }
        }
      }

      if (selectedTab == 0 && myPostsState is UiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        VerticalSocialFeed(
          posts = activeList,
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
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
          headerContent = {
            item {
              Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier =
                  Modifier.fillMaxWidth()
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                      RoundedCornerShape(24.dp),
                    ),
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  AuthorAvatar(name = displayName, photoUrl = photoUrl, size = 80.dp)
                  Spacer(Modifier.height(12.dp))
                  Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  if (bio.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                      text = bio,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(Modifier.height(18.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                  ) {
                    StatColumn(value = formatCompactCount(myPosts.size.toLong()), label = "Posts")
                    StatColumn(value = formatCompactCount(totalViewsReceived), label = "Views")
                    StatColumn(value = formatCompactCount(totalLikesReceived), label = "Likes")
                    StatColumn(value = formatCompactCount(followingIds.size.toLong()), label = "Following")
                  }
                }
              }
            }

            item {
              TabRow(selectedTabIndex = selectedTab) {
                Tab(
                  selected = selectedTab == 0,
                  onClick = { selectedTab = 0 },
                  icon = { Icon(Icons.Filled.GridView, contentDescription = null) },
                  text = { Text("Posts") },
                  modifier = Modifier.testTag("profile_tab_posts"),
                )
                Tab(
                  selected = selectedTab == 1,
                  onClick = { selectedTab = 1 },
                  icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
                  text = { Text("Saved (${savedPosts.size})") },
                  modifier = Modifier.testTag("profile_tab_saved"),
                )
                Tab(
                  selected = selectedTab == 2,
                  onClick = { selectedTab = 2 },
                  icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                  text = { Text("Liked (${likedPosts.size})") },
                  modifier = Modifier.testTag("profile_tab_liked"),
                )
              }
            }
          },
          emptyContent = {
            item {
              Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    when (selectedTab) {
                      0 -> "You haven't posted anything yet."
                      1 -> "Tap the bookmark icon on any post to save it here."
                      else -> "Posts you like or react to appear here."
                    },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
        )
      }
    }
  }

  if (showEditDialog) {
    var draftName by remember { mutableStateOf(displayName) }
    var draftBio by remember { mutableStateOf(bio) }
    Dialog(onDismissRequest = { showEditDialog = false }) {
      Surface(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text("Edit Profile", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(12.dp))
          OutlinedTextField(
            value = draftName,
            onValueChange = { if (it.length <= 80) draftName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(Modifier.height(10.dp))
          OutlinedTextField(
            value = draftBio,
            onValueChange = { if (it.length <= 240) draftBio = it },
            label = { Text("Bio") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(Modifier.height(16.dp))
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
              onClick = {
                onUpdateProfile(draftName, draftBio)
                showEditDialog = false
              },
              enabled = draftName.isNotBlank(),
            ) {
              Text("Save")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatColumn(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
