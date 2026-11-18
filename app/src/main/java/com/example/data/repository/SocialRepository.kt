package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.FollowRef
import com.example.data.model.NotificationItem
import com.example.data.model.PostComment
import com.example.data.model.PostType
import com.example.data.model.PostVisibility
import com.example.data.model.ReactionType
import com.example.data.model.SocialPost
import com.example.data.model.SocialStory
import com.example.data.model.UserPostRef
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun currentUserName(): String {
    val name = auth.currentUser?.displayName?.trim().orEmpty()
    return if (name.isNotBlank()) name.take(80) else "VibeShare Creator"
  }

  private fun currentUserPhoto(): String {
    return auth.currentUser?.photoUrl?.toString()?.take(500).orEmpty()
  }

  suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
    val uid = user.uid
    val path = "users/$uid"
    return try {
      val docRef = db.collection("users").document(uid)
      val snap = docRef.get().await()
      val displayName =
        user.displayName?.trim()?.take(80)?.takeIf { it.isNotBlank() } ?: "VibeShare Creator"
      val email = user.email?.trim()?.take(160).orEmpty()
      val photoUrl = user.photoUrl?.toString()?.take(500).orEmpty()

      if (!snap.exists()) {
        val payload =
          buildMap<String, Any> {
            put("userId", uid)
            put("displayName", displayName)
            if (email.isNotBlank()) put("email", email)
            if (photoUrl.isNotBlank()) put("photoUrl", photoUrl)
            put("bio", "")
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.set(payload).await()
      } else {
        val updates =
          buildMap<String, Any> {
            put("displayName", displayName)
            if (email.isNotBlank()) put("email", email)
            if (photoUrl.isNotBlank()) put("photoUrl", photoUrl)
            put("updatedAt", FieldValue.serverTimestamp())
          }
        docRef.update(updates).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error -> handleStreamError(error, OperationType.GET, path, userId) }
    )
  }

  suspend fun updateProfile(userId: String, displayName: String, bio: String): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId) { "Refusing cross-user profile update." }
      val cleanName = displayName.trim().take(80)
      require(cleanName.isNotBlank()) { "Display name cannot be empty." }
      val cleanBio = bio.trim().take(240)
      db.collection("users")
        .document(uid)
        .update(
          mapOf(
            "displayName" to cleanName,
            "bio" to cleanBio,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observePublicFeed(): Flow<List<SocialPost>> = flow {
    val uid = requireUserId()
    val path = "posts"
    emitAll(
      db.collection(path)
        .whereEqualTo("visibility", PostVisibility.PUBLIC.name)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SocialPost::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeUserPosts(userId: String): Flow<List<SocialPost>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user private posts observation." }
    val path = "posts"
    emitAll(
      db.collection(path)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SocialPost::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeLikedReactions(userId: String): Flow<Map<String, ReactionType>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user liked posts observation." }
    val path = "users/$uid/liked_posts"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("liked_posts")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              UserPostRef::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .associate { ref ->
              val key = ref.postId.ifBlank { ref.id }
              key to ReactionType.fromRaw(ref.reaction)
            }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeSavedPostIds(userId: String): Flow<Set<String>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user saved posts observation." }
    val path = "users/$uid/saved_posts"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("saved_posts")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              UserPostRef::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .map { it.postId.ifBlank { it.id } }
            .toSet()
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user following observation." }
    val path = "users/$uid/following"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("following")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              FollowRef::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .map { it.targetUserId.ifBlank { it.id } }
            .toSet()
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeNotifications(userId: String): Flow<List<NotificationItem>> = flow {
    val uid = requireUserId()
    check(uid == userId) { "Refusing cross-user notifications observation." }
    val path = "users/$uid/notifications"
    emitAll(
      db.collection("users")
        .document(uid)
        .collection("notifications")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              NotificationItem::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeStories(): Flow<List<SocialStory>> = flow {
    val uid = requireUserId()
    val path = "stories"
    emitAll(
      db.collection(path)
        .whereEqualTo("visibility", PostVisibility.PUBLIC.name)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              SocialStory::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  fun observeComments(postId: String): Flow<List<PostComment>> = flow {
    val uid = requireUserId()
    val path = "posts/$postId/comments"
    emitAll(
      db.collection("posts")
        .document(postId)
        .collection("comments")
        .whereEqualTo("postVisibility", PostVisibility.PUBLIC.name)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              PostComment::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
    )
  }

  suspend fun createPost(
    postType: PostType,
    visibility: PostVisibility,
    caption: String,
    textBgHex: String = "",
    title: String = "",
    subtitle: String = "",
    mediaUrl: String = "",
    localUri: String = "",
    mediaBase64: String = "",
    fileName: String = "",
    mimeType: String = "",
    fileSizeBytes: Long = 0L,
    durationMs: Long = 0L,
  ): Result<Unit> {
    var docPath = "posts"
    return try {
      val uid = requireUserId()
      val docRef = db.collection("posts").document()
      docPath = docRef.path

      val cleanCaption = caption.trim().take(1000)
      val cleanBgHex = textBgHex.trim().take(9)
      val cleanTitle = title.trim().take(180)
      val cleanSubtitle = subtitle.trim().take(240)
      val cleanMediaUrl = mediaUrl.trim().take(1500)
      val cleanLocalUri = localUri.trim().take(1500)
      val cleanBase64 = mediaBase64.take(450_000)
      val cleanFileName = fileName.trim().take(240)
      val cleanMimeType = mimeType.trim().take(120)

      val payload =
        buildMap<String, Any> {
          put("userId", uid)
          put("authorName", currentUserName())
          val photo = currentUserPhoto()
          if (photo.isNotBlank()) put("authorPhotoUrl", photo)
          put("postType", postType.name)
          put("visibility", visibility.name)
          put("caption", cleanCaption)
          if (cleanBgHex.length in 4..9) put("textBgHex", cleanBgHex)
          if (cleanTitle.isNotBlank()) put("title", cleanTitle)
          if (cleanSubtitle.isNotBlank()) put("subtitle", cleanSubtitle)
          if (cleanMediaUrl.isNotBlank()) put("mediaUrl", cleanMediaUrl)
          if (cleanLocalUri.isNotBlank()) put("localUri", cleanLocalUri)
          if (cleanBase64.isNotBlank()) put("mediaBase64", cleanBase64)
          if (cleanFileName.isNotBlank()) put("fileName", cleanFileName)
          if (cleanMimeType.isNotBlank()) put("mimeType", cleanMimeType)
          if (fileSizeBytes > 0L) put("fileSizeBytes", fileSizeBytes)
          if (durationMs > 0L) put("durationMs", durationMs)
          put("likesCount", 0L)
          put("commentsCount", 0L)
          put("sharesCount", 0L)
          put("viewsCount", 0L)
          put("createdAt", FieldValue.serverTimestamp())
          put("updatedAt", FieldValue.serverTimestamp())
        }
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deletePost(postId: String): Result<Unit> {
    val path = "posts/$postId"
    return try {
      requireUserId()
      require(postId.isNotBlank()) { "Post ID must not be blank." }
      db.collection("posts").document(postId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun setReaction(
    post: SocialPost,
    reaction: ReactionType?,
    currentlyLiked: Boolean,
  ): Result<Unit> {
    val path = "posts/${post.id}"
    return try {
      val uid = requireUserId()
      require(post.id.isNotBlank()) { "Post ID must not be blank." }
      val postRef = db.collection("posts").document(post.id)
      val likedRef =
        db.collection("users").document(uid).collection("liked_posts").document(post.id)

      if (reaction == null && currentlyLiked) {
        val batch = db.batch()
        batch.delete(likedRef)
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(-1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.commit().await()
      } else if (reaction != null && currentlyLiked) {
        likedRef.update(mapOf("reaction" to reaction.name)).await()
      } else if (reaction != null && !currentlyLiked) {
        val batch = db.batch()
        batch.set(
          likedRef,
          mapOf(
            "postId" to post.id,
            "userId" to uid,
            "reaction" to reaction.name,
            "createdAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.update(
          postRef,
          mapOf(
            "likesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
        batch.commit().await()
        sendNotification(
          recipientId = post.userId,
          type = "LIKE",
          postId = post.id,
          message = "${currentUserName()} reacted ${reaction.emoji} to your ${post.resolvedPostType().label.lowercase()} post",
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun toggleSave(postId: String, currentlySaved: Boolean): Result<Unit> {
    var path = "users/unknown/saved_posts/$postId"
    return try {
      val uid = requireUserId()
      require(postId.isNotBlank()) { "Post ID must not be blank." }
      val savedRef =
        db.collection("users").document(uid).collection("saved_posts").document(postId)
      path = savedRef.path
      if (currentlySaved) {
        savedRef.delete().await()
      } else {
        savedRef
          .set(
            mapOf(
              "postId" to postId,
              "userId" to uid,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun toggleFollow(
    targetUserId: String,
    targetName: String,
    currentlyFollowing: Boolean,
  ): Result<Unit> {
    var path = "users/unknown/following/$targetUserId"
    return try {
      val uid = requireUserId()
      require(targetUserId.isNotBlank() && targetUserId != uid) { "Invalid follow target." }
      val followRef =
        db.collection("users").document(uid).collection("following").document(targetUserId)
      path = followRef.path
      if (currentlyFollowing) {
        followRef.delete().await()
      } else {
        followRef
          .set(
            buildMap<String, Any> {
              put("targetUserId", targetUserId)
              if (targetName.isNotBlank()) put("targetName", targetName.trim().take(80))
              put("userId", uid)
              put("createdAt", FieldValue.serverTimestamp())
            }
          )
          .await()
        sendNotification(
          recipientId = targetUserId,
          type = "FOLLOW",
          postId = "",
          message = "${currentUserName()} started following you",
        )
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun addComment(post: SocialPost, text: String): Result<Unit> {
    val path = "posts/${post.id}/comments"
    return try {
      val uid = requireUserId()
      val cleanText = text.trim().take(500)
      require(cleanText.isNotBlank()) { "Comment cannot be empty." }
      require(post.id.isNotBlank()) { "Post ID must not be blank." }

      val postRef = db.collection("posts").document(post.id)
      val commentRef = postRef.collection("comments").document()

      val payload =
        buildMap<String, Any> {
          put("postId", post.id)
          put("postVisibility", PostVisibility.PUBLIC.name)
          put("postOwnerId", post.userId)
          put("userId", uid)
          put("authorName", currentUserName())
          val photo = currentUserPhoto()
          if (photo.isNotBlank()) put("authorPhotoUrl", photo)
          put("text", cleanText)
          put("createdAt", FieldValue.serverTimestamp())
        }

      val batch = db.batch()
      batch.set(commentRef, payload)
      batch.update(
        postRef,
        mapOf(
          "commentsCount" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()

      sendNotification(
        recipientId = post.userId,
        type = "COMMENT",
        postId = post.id,
        message = "${currentUserName()} commented: \"${cleanText.take(60)}\"",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun recordShare(post: SocialPost): Result<Unit> {
    val path = "posts/${post.id}"
    return try {
      requireUserId()
      require(post.id.isNotBlank()) { "Post ID must not be blank." }
      db.collection("posts")
        .document(post.id)
        .update(
          mapOf(
            "sharesCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      sendNotification(
        recipientId = post.userId,
        type = "SHARE",
        postId = post.id,
        message = "${currentUserName()} shared your post",
      )
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun recordView(postId: String): Result<Unit> {
    val path = "posts/$postId"
    return try {
      requireUserId()
      if (postId.isBlank()) return Result.success(Unit)
      db.collection("posts")
        .document(postId)
        .update(
          mapOf(
            "viewsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // View increments on private or deleted posts should not interrupt UX; still log cleanly.
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun markNotificationsRead(notifications: List<NotificationItem>): Result<Unit> {
    return try {
      val uid = requireUserId()
      val unread = notifications.filter { !it.read && it.id.isNotBlank() }
      if (unread.isEmpty()) return Result.success(Unit)
      val batch = db.batch()
      unread.forEach { item ->
        val ref = db.collection("users").document(uid).collection("notifications").document(item.id)
        batch.update(ref, mapOf("read" to true))
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "users/${auth.currentUser?.uid}/notifications")
      Result.failure(e)
    }
  }

  suspend fun createStory(
    caption: String,
    bgHex: String,
    mediaBase64: String = "",
  ): Result<Unit> {
    var path = "stories"
    return try {
      val uid = requireUserId()
      val cleanCaption = caption.trim().take(200)
      require(cleanCaption.isNotBlank()) { "Story caption cannot be empty." }
      val storyRef = db.collection("stories").document()
      path = storyRef.path

      val payload =
        buildMap<String, Any> {
          put("userId", uid)
          put("authorName", currentUserName())
          val photo = currentUserPhoto()
          if (photo.isNotBlank()) put("authorPhotoUrl", photo)
          put("visibility", PostVisibility.PUBLIC.name)
          put("caption", cleanCaption)
          val cleanBase64 = mediaBase64.take(450_000)
          if (cleanBase64.isNotBlank()) put("mediaBase64", cleanBase64)
          put("bgHex", bgHex.take(9))
          put("createdAt", FieldValue.serverTimestamp())
        }
      storyRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteStory(storyId: String): Result<Unit> {
    val path = "stories/$storyId"
    return try {
      requireUserId()
      db.collection("stories").document(storyId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  private suspend fun sendNotification(
    recipientId: String,
    type: String,
    postId: String,
    message: String,
  ) {
    val uid = auth.currentUser?.uid ?: return
    if (recipientId.isBlank() || recipientId == uid) return
    try {
      val notifRef = db.collection("users").document(recipientId).collection("notifications").document()
      val payload =
        buildMap<String, Any> {
          put("recipientId", recipientId)
          put("actorId", uid)
          put("actorName", currentUserName())
          put("type", type)
          if (postId.isNotBlank()) put("postId", postId.take(128))
          put("message", message.trim().take(240))
          put("read", false)
          put("createdAt", FieldValue.serverTimestamp())
        }
      notifRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, "users/$recipientId/notifications")
    }
  }

  private fun handleStreamError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    expectedUid: String,
  ): Nothing {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
    throw error
  }
}
