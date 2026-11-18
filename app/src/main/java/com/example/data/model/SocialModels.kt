package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PostType(val label: String) {
  TEXT("Text"),
  IMAGE("Photo"),
  VIDEO("Video"),
  SONG("Song"),
  FILE("File"),
  LINK("Link");

  companion object {
    fun fromRaw(raw: String?): PostType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TEXT
  }
}

enum class PostVisibility(val label: String) {
  PUBLIC("Public"),
  PRIVATE("Only Me");

  companion object {
    fun fromRaw(raw: String?): PostVisibility =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PUBLIC
  }
}

enum class ReactionType(val emoji: String, val label: String) {
  LIKE("👍", "Like"),
  LOVE("❤️", "Love"),
  HAHA("😂", "Haha"),
  WOW("😮", "Wow"),
  FIRE("🔥", "Fire");

  companion object {
    fun fromRaw(raw: String?): ReactionType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: LIKE
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val photoUrl: String = "",
  val bio: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SocialPost(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorPhotoUrl: String = "",
  val postType: String = PostType.IMAGE.name,
  val visibility: String = PostVisibility.PUBLIC.name,
  val caption: String = "",
  val textBgHex: String = "",
  val title: String = "",
  val subtitle: String = "",
  val mediaUrl: String = "",
  val localUri: String = "",
  val mediaBase64: String = "",
  val fileName: String = "",
  val mimeType: String = "",
  val fileSizeBytes: Long = 0L,
  val durationMs: Long = 0L,
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val sharesCount: Long = 0L,
  val viewsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun resolvedPostType(): PostType = PostType.fromRaw(postType)

  fun resolvedVisibility(): PostVisibility = PostVisibility.fromRaw(visibility)
}

@Keep
@IgnoreExtraProperties
data class PostComment(
  @DocumentId val id: String = "",
  val postId: String = "",
  val postVisibility: String = PostVisibility.PUBLIC.name,
  val postOwnerId: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorPhotoUrl: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SocialStory(
  @DocumentId val id: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorPhotoUrl: String = "",
  val visibility: String = PostVisibility.PUBLIC.name,
  val caption: String = "",
  val mediaBase64: String = "",
  val bgHex: String = "#5B4BFF",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserPostRef(
  @DocumentId val id: String = "",
  val postId: String = "",
  val userId: String = "",
  val reaction: String = ReactionType.LIKE.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FollowRef(
  @DocumentId val id: String = "",
  val targetUserId: String = "",
  val targetName: String = "",
  val userId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val actorId: String = "",
  val actorName: String = "",
  val type: String = "LIKE",
  val postId: String = "",
  val message: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}
