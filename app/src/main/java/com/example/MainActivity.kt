package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.PostType
import com.example.data.model.SocialPost
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.SocialRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommentsBottomSheet
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlayerDialog
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.VibeShareTheme
import com.example.ui.viewmodel.SocialViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object NavRoutes {
  const val ROUTE_HOME = "home"
  const val ROUTE_SEARCH = "search"
  const val ROUTE_CREATE = "create"
  const val ROUTE_REELS = "reels"
  const val ROUTE_PROFILE = "profile"
  const val ROUTE_SETTINGS = "settings"
}

private data class NavTab(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val mainTabs =
  listOf(
    NavTab(NavRoutes.ROUTE_HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavTab(NavRoutes.ROUTE_SEARCH, R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search, "nav_search"),
    NavTab(NavRoutes.ROUTE_CREATE, R.string.nav_create, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_create"),
    NavTab(NavRoutes.ROUTE_REELS, R.string.nav_reels, Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline, "nav_reels"),
    NavTab(NavRoutes.ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
  )

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val scope = rememberCoroutineScope()
      val themePrefs = remember(context) { ThemePreferences(context.applicationContext) }
      val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

      VibeShareTheme(themeMode = themeMode) {
        AppRoot(
          themeMode = themeMode,
          onThemeModeChange = { mode -> scope.launch { themePrefs.setThemeMode(mode) } },
        )
      }
    }
  }
}

@Composable
private fun AppRoot(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
) {
  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      AuthenticatedAppShell(
        firebaseUser = user,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
      )
    }
  }
}

@Composable
private fun AuthenticatedAppShell(
  firebaseUser: FirebaseUser,
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: SocialViewModel =
    viewModel(
      key = firebaseUser.uid,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val dbId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            SocialViewModel(
              repository = SocialRepository(db),
              currentUserId = firebaseUser.uid,
            )
          }
        },
    )

  LaunchedEffect(firebaseUser.uid) { viewModel.syncSignedInProfile(firebaseUser) }

  val feedState by viewModel.publicFeedState.collectAsStateWithLifecycle()
  val myPostsState by viewModel.myPostsState.collectAsStateWithLifecycle()
  val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
  val likedReactions by viewModel.likedReactions.collectAsStateWithLifecycle()
  val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
  val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val activeCommentsPost by viewModel.activeCommentsPost.collectAsStateWithLifecycle()
  val activeCommentsState by viewModel.activeCommentsState.collectAsStateWithLifecycle()
  val isPublishing by viewModel.isPublishing.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
  val audioState by viewModel.audioController.state.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearSnackbar()
    }
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: NavRoutes.ROUTE_HOME

  var initialCreateType by remember { mutableStateOf(PostType.TEXT) }
  var videoPlayerPost by remember { mutableStateOf<SocialPost?>(null) }

  fun navigateToTab(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun sharePostExternally(post: SocialPost) {
    viewModel.recordShare(post)
    val shareText =
      buildString {
          append("Check out ${post.authorName}'s ${post.resolvedPostType().label} on VibeShare")
          if (post.title.isNotBlank()) append(": ${post.title}")
          if (post.caption.isNotBlank()) append("\n\n${post.caption}")
          if (post.mediaUrl.isNotBlank()) append("\n\n${post.mediaUrl}")
        }
        .trim()
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
      }
    context.startActivity(Intent.createChooser(intent, "Share post"))
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 700.dp
    val showNavigationChrome = currentRoute != NavRoutes.ROUTE_SETTINGS

    Row(modifier = Modifier.fillMaxSize()) {
      if (useRail && showNavigationChrome) {
        NavigationRail {
          mainTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationRailItem(
              selected = selected,
              onClick = { navigateToTab(tab.route) },
              icon = {
                Icon(
                  imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                  contentDescription = stringResource(tab.labelRes),
                )
              },
              label = { Text(stringResource(tab.labelRes)) },
              modifier = Modifier.testTag(tab.testTag),
            )
          }
        }
      }

      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
          if (!useRail && showNavigationChrome) {
            NavigationBar {
              mainTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateToTab(tab.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                      contentDescription = stringResource(tab.labelRes),
                    )
                  },
                  label = { Text(stringResource(tab.labelRes)) },
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
        modifier = Modifier.weight(1f),
      ) { innerPadding ->
        NavHost(
          navController = navController,
          startDestination = NavRoutes.ROUTE_HOME,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
          composable(NavRoutes.ROUTE_HOME) {
            FeedScreen(
              currentUserId = firebaseUser.uid,
              feedState = feedState,
              storiesState = storiesState,
              notifications = notifications,
              selectedFilter = selectedFilter,
              likedReactions = likedReactions,
              savedPostIds = savedPostIds,
              followingIds = followingIds,
              audioState = audioState,
              onSelectFilter = viewModel::setFilter,
              onCreatePostClick = { type ->
                initialCreateType = type
                navigateToTab(NavRoutes.ROUTE_CREATE)
              },
              onCreateStory = viewModel::createStory,
              onDeleteStory = viewModel::deleteStory,
              onPostViewed = viewModel::recordPostViewed,
              onLikeClick = viewModel::toggleLike,
              onSelectReaction = viewModel::selectReaction,
              onCommentClick = viewModel::openComments,
              onShareClick = ::sharePostExternally,
              onSaveClick = viewModel::toggleSave,
              onFollowClick = viewModel::toggleFollow,
              onPlayAudioClick = { post, source ->
                viewModel.audioController.togglePlay(context, post.id, source)
              },
              onOpenVideoClick = { post -> videoPlayerPost = post },
              onDeletePostClick = { post -> viewModel.deletePost(post.id) },
              onMarkNotificationsRead = viewModel::markAllNotificationsRead,
              onOpenSettingsClick = { navController.navigate(NavRoutes.ROUTE_SETTINGS) },
            )
          }

          composable(NavRoutes.ROUTE_SEARCH) {
            ExploreScreen(
              currentUserId = firebaseUser.uid,
              feedState = feedState,
              searchQuery = searchQuery,
              likedReactions = likedReactions,
              savedPostIds = savedPostIds,
              followingIds = followingIds,
              audioState = audioState,
              onSearchQueryChange = viewModel::setSearchQuery,
              onPostViewed = viewModel::recordPostViewed,
              onLikeClick = viewModel::toggleLike,
              onSelectReaction = viewModel::selectReaction,
              onCommentClick = viewModel::openComments,
              onShareClick = ::sharePostExternally,
              onSaveClick = viewModel::toggleSave,
              onFollowClick = viewModel::toggleFollow,
              onPlayAudioClick = { post, source ->
                viewModel.audioController.togglePlay(context, post.id, source)
              },
              onOpenVideoClick = { post -> videoPlayerPost = post },
              onDeletePostClick = { post -> viewModel.deletePost(post.id) },
            )
          }

          composable(NavRoutes.ROUTE_CREATE) {
            CreatePostScreen(
              initialType = initialCreateType,
              isPublishing = isPublishing,
              onPublish = { type, vis, caption, textBgHex, title, subtitle, mediaUrl, localUri, base64, fileName, mime, size, dur ->
                viewModel.publishPost(
                  postType = type,
                  visibility = vis,
                  caption = caption,
                  textBgHex = textBgHex,
                  title = title,
                  subtitle = subtitle,
                  mediaUrl = mediaUrl,
                  localUri = localUri,
                  mediaBase64 = base64,
                  fileName = fileName,
                  mimeType = mime,
                  fileSizeBytes = size,
                  durationMs = dur,
                  onPublished = { navigateToTab(NavRoutes.ROUTE_HOME) },
                )
              },
            )
          }

          composable(NavRoutes.ROUTE_REELS) {
            ReelsScreen(
              currentUserId = firebaseUser.uid,
              feedState = feedState,
              likedReactions = likedReactions,
              savedPostIds = savedPostIds,
              followingIds = followingIds,
              audioState = audioState,
              onPostViewed = viewModel::recordPostViewed,
              onLikeClick = viewModel::toggleLike,
              onCommentClick = viewModel::openComments,
              onShareClick = ::sharePostExternally,
              onSaveClick = viewModel::toggleSave,
              onFollowClick = viewModel::toggleFollow,
              onPlayAudioClick = { post, source ->
                viewModel.audioController.togglePlay(context, post.id, source)
              },
              onOpenVideoClick = { post -> videoPlayerPost = post },
              onCreateVideoClick = {
                initialCreateType = PostType.VIDEO
                navigateToTab(NavRoutes.ROUTE_CREATE)
              },
            )
          }

          composable(NavRoutes.ROUTE_PROFILE) {
            ProfileScreen(
              currentUserId = firebaseUser.uid,
              userProfile = userProfile,
              myPostsState = myPostsState,
              publicFeedState = feedState,
              likedReactions = likedReactions,
              savedPostIds = savedPostIds,
              followingIds = followingIds,
              audioState = audioState,
              onUpdateProfile = viewModel::updateProfile,
              onPostViewed = viewModel::recordPostViewed,
              onLikeClick = viewModel::toggleLike,
              onSelectReaction = viewModel::selectReaction,
              onCommentClick = viewModel::openComments,
              onShareClick = ::sharePostExternally,
              onSaveClick = viewModel::toggleSave,
              onFollowClick = viewModel::toggleFollow,
              onPlayAudioClick = { post, source ->
                viewModel.audioController.togglePlay(context, post.id, source)
              },
              onOpenVideoClick = { post -> videoPlayerPost = post },
              onDeletePostClick = { post -> viewModel.deletePost(post.id) },
              onOpenSettingsClick = { navController.navigate(NavRoutes.ROUTE_SETTINGS) },
            )
          }

          composable(NavRoutes.ROUTE_SETTINGS) {
            SettingsScreen(
              themeMode = themeMode,
              onThemeModeChange = onThemeModeChange,
              onSignOutClick = {
                viewModel.audioController.stop()
                signOutUser(credentialManager, scope)
              },
              onBackClick = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }

  activeCommentsPost?.let { post ->
    CommentsBottomSheet(
      post = post,
      commentsState = activeCommentsState,
      onDismiss = viewModel::closeComments,
      onSubmitComment = { text -> viewModel.submitComment(post, text) },
    )
  }

  videoPlayerPost?.let { post ->
    VideoPlayerDialog(
      post = post,
      onDismiss = { videoPlayerPost = null },
    )
  }
}
