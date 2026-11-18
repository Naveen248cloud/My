package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NotificationItem
import com.example.data.model.PostComment
import com.example.data.model.PostType
import com.example.data.model.PostVisibility
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.SocialStory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.SocialRepository
import com.example.util.AudioPlayerController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialViewModel(
  private val repository: SocialRepository,
  val currentUserId: String,
) : ViewModel() {

  val audioController = AudioPlayerController(viewModelScope)
  private val recordedViewPostIds = mutableSetOf<String>()

  private val _selectedFilter = MutableStateFlow<PostType?>(null)
  val selectedFilter: StateFlow<PostType?> = _selectedFilter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _activeCommentsPost = MutableStateFlow<SocialPost?>(null)
  val activeCommentsPost: StateFlow<SocialPost?> = _activeCommentsPost.asStateFlow()

  private val _isPublishing = MutableStateFlow(false)
  val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  val publicFeedState: StateFlow<UiState<List<SocialPost>>> =
    repository
      .observePublicFeed()
      .map<List<SocialPost>, UiState<List<SocialPost>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreError(error)
        Log.w(TAG, "Error observing public feed", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load feed"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val myPostsState: StateFlow<UiState<List<SocialPost>>> =
    repository
      .observeUserPosts(currentUserId)
      .map<List<SocialPost>, UiState<List<SocialPost>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreError(error)
        Log.w(TAG, "Error observing user posts", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load your posts"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val storiesState: StateFlow<UiState<List<SocialStory>>> =
    repository
      .observeStories()
      .map<List<SocialStory>, UiState<List<SocialStory>>> { UiState.Success(it) }
      .catch { error ->
        val rootError = unwrapFirestoreError(error)
        Log.w(TAG, "Error observing stories", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load stories"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val likedReactions: StateFlow<Map<String, ReactionType>> =
    repository
      .observeLikedReactions(currentUserId)
      .catch { error ->
        unwrapFirestoreError(error)
        emit(emptyMap())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyMap(),
      )

  val savedPostIds: StateFlow<Set<String>> =
    repository
      .observeSavedPostIds(currentUserId)
      .catch { error ->
        unwrapFirestoreError(error)
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val followingIds: StateFlow<Set<String>> =
    repository
      .observeFollowingIds(currentUserId)
      .catch { error ->
        unwrapFirestoreError(error)
        emit(emptySet())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptySet(),
      )

  val notifications: StateFlow<List<NotificationItem>> =
    repository
      .observeNotifications(currentUserId)
      .catch { error ->
        unwrapFirestoreError(error)
        emit(emptyList())
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val userProfile: StateFlow<UserProfile?> =
    repository
      .observeUserProfile(currentUserId)
      .catch { error ->
        unwrapFirestoreError(error)
        emit(null)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null,
      )

  val activeCommentsState: StateFlow<UiState<List<PostComment>>> =
    _activeCommentsPost
      .flatMapLatest { post ->
        if (post == null) {
          flowOf(UiState.Success(emptyList()))
        } else {
          repository
            .observeComments(post.id)
            .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
            .catch { error ->
              val rootError = unwrapFirestoreError(error)
              Log.w(TAG, "Error observing comments", rootError)
              emit(UiState.Error(rootError.message ?: "Failed to load comments"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  fun syncSignedInProfile(user: FirebaseUser) {
    viewModelScope.launch { repository.ensureUserProfile(user) }
  }

  fun setFilter(type: PostType?) {
    _selectedFilter.value = type
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openComments(post: SocialPost) {
    _activeCommentsPost.value = post
    recordPostViewed(post)
  }

  fun closeComments() {
    _activeCommentsPost.value = null
  }

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun recordPostViewed(post: SocialPost) {
    if (post.id.isBlank() || post.resolvedVisibility() != PostVisibility.PUBLIC) return
    if (!recordedViewPostIds.add(post.id)) return
    viewModelScope.launch { repository.recordView(post.id) }
  }

  fun toggleLike(post: SocialPost) {
    val isCurrentlyLiked = likedReactions.value.containsKey(post.id)
    val nextReaction = if (isCurrentlyLiked) null else ReactionType.LIKE
    viewModelScope.launch {
      repository.setReaction(post, nextReaction, isCurrentlyLiked).onFailure {
        _snackbarMessage.value = it.localizedMessage ?: "Failed to update like"
      }
    }
  }

  fun selectReaction(post: SocialPost, reaction: ReactionType) {
    val current = likedReactions.value[post.id]
    val isCurrentlyLiked = current != null
    val nextReaction = if (current == reaction) null else reaction
    viewModelScope.launch {
      repository.setReaction(post, nextReaction, isCurrentlyLiked).onFailure {
        _snackbarMessage.value = it.localizedMessage ?: "Failed to update reaction"
      }
    }
  }

  fun toggleSave(post: SocialPost) {
    val isCurrentlySaved = savedPostIds.value.contains(post.id)
    viewModelScope.launch {
      repository
        .toggleSave(post.id, isCurrentlySaved)
        .onSuccess {
          _snackbarMessage.value = if (isCurrentlySaved) "Removed from Saved" else "Post saved"
        }
        .onFailure {
          _snackbarMessage.value = it.localizedMessage ?: "Failed to save post"
        }
    }
  }

  fun toggleFollow(targetUserId: String, targetName: String) {
    val isCurrentlyFollowing = followingIds.value.contains(targetUserId)
    viewModelScope.launch {
      repository
        .toggleFollow(targetUserId, targetName, isCurrentlyFollowing)
        .onSuccess {
          _snackbarMessage.value =
            if (isCurrentlyFollowing) "Unfollowed $targetName" else "Following $targetName"
        }
        .onFailure {
          _snackbarMessage.value = it.localizedMessage ?: "Failed to update follow"
        }
    }
  }

  fun submitComment(post: SocialPost, text: String) {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return
    viewModelScope.launch {
      repository
        .addComment(post, trimmed)
        .onFailure {
          _snackbarMessage.value = it.localizedMessage ?: "Could not post comment"
        }
    }
  }

  fun recordShare(post: SocialPost) {
    viewModelScope.launch { repository.recordShare(post) }
  }

  fun deletePost(postId: String) {
    viewModelScope.launch {
      repository
        .deletePost(postId)
        .onSuccess { _snackbarMessage.value = "Post deleted" }
        .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not delete post" }
    }
  }

  fun publishPost(
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
    onPublished: () -> Unit,
  ) {
    viewModelScope.launch {
      _isPublishing.value = true
      val result =
        repository.createPost(
          postType = postType,
          visibility = visibility,
          caption = caption,
          textBgHex = textBgHex,
          title = title,
          subtitle = subtitle,
          mediaUrl = mediaUrl,
          localUri = localUri,
          mediaBase64 = mediaBase64,
          fileName = fileName,
          mimeType = mimeType,
          fileSizeBytes = fileSizeBytes,
          durationMs = durationMs,
        )
      _isPublishing.value = false
      result
        .onSuccess {
          _snackbarMessage.value = "Posted to VibeShare!"
          onPublished()
        }
        .onFailure {
          _snackbarMessage.value = it.localizedMessage ?: "Failed to publish post"
        }
    }
  }

  fun createStory(caption: String, bgHex: String, mediaBase64: String = "") {
    viewModelScope.launch {
      repository
        .createStory(caption, bgHex, mediaBase64)
        .onSuccess { _snackbarMessage.value = "Story shared!" }
        .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Failed to share story" }
    }
  }

  fun deleteStory(storyId: String) {
    viewModelScope.launch { repository.deleteStory(storyId) }
  }

  fun markAllNotificationsRead() {
    viewModelScope.launch { repository.markNotificationsRead(notifications.value) }
  }

  fun updateProfile(displayName: String, bio: String) {
    viewModelScope.launch {
      repository
        .updateProfile(currentUserId, displayName, bio)
        .onSuccess { _snackbarMessage.value = "Profile updated" }
        .onFailure { _snackbarMessage.value = it.localizedMessage ?: "Could not update profile" }
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioController.stop()
  }

  private fun unwrapFirestoreError(error: Throwable): Throwable {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    return firestoreError ?: error
  }

  private companion object {
    const val TAG = "SocialVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
