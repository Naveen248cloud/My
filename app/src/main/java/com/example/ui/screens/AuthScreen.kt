package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.BadgeFile
import com.example.ui.theme.BadgeLink
import com.example.ui.theme.BadgePhoto
import com.example.ui.theme.BadgeSong
import com.example.ui.theme.BadgeVideo
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoSecondary
import com.example.util.ApkDownloadState
import com.example.util.ApkDownloader
import com.example.util.formatFileSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var isCheckingSession by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var downloadState by remember { mutableStateOf<ApkDownloadState>(ApkDownloadState.Idle) }
  val apkSizeLabel = remember(context) { formatFileSize(ApkDownloader.getApkSizeBytes(context)) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingSession = false
        onAuthSuccess()
      },
      onUnauthenticated = { isCheckingSession = false },
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.background,
              )
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier =
            Modifier.size(44.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(
                Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary, CyanTertiary))
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
          )
        }
        Spacer(Modifier.width(12.dp))
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      Spacer(Modifier.height(20.dp))

      Card(
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Box {
          Image(
            painter = painterResource(id = R.drawable.img_login_hero),
            contentDescription = "VibeShare social media creator studio",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(210.dp),
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(210.dp)
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                  )
                )
                .padding(18.dp),
            contentAlignment = Alignment.BottomStart,
          ) {
            Column {
              Text(
                text = "Create. Share. Connect.",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
              )
              Text(
                text = "Like, comment, save, and discover stories & media from creators.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.88f),
              )
            }
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      Text(
        text = "Everything you share, in one feed",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = "Upload videos, photos, songs, files, or web links and engage with posts in real time.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(Modifier.height(18.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        FeaturePill(Icons.Filled.Image, "Photos", BadgePhoto)
        FeaturePill(Icons.Filled.PlayCircle, "Videos", BadgeVideo)
        FeaturePill(Icons.Filled.AudioFile, "Songs", BadgeSong)
        FeaturePill(Icons.Filled.AttachFile, "Files", BadgeFile)
        FeaturePill(Icons.Filled.Link, "Links", BadgeLink)
      }

      Spacer(Modifier.height(28.dp))

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        ) {
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
          )
        }
      }

      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false },
          )
        },
        enabled = !isLoading && !isCheckingSession,
        shape = RoundedCornerShape(18.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier =
          Modifier.fillMaxWidth()
            .height(56.dp)
            .testTag("google_sign_in_button"),
      ) {
        if (isLoading || isCheckingSession) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(Modifier.width(12.dp))
          Text(
            text = if (isCheckingSession) "Checking account..." else "Signing in...",
            style = MaterialTheme.typography.labelLarge,
          )
        } else {
          Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.Login,
              contentDescription = null,
              tint = CoralPrimary,
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.sign_in_google),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Spacer(Modifier.height(12.dp))
      Text(
        text = "Secured with Google Sign-In & Cloud Firestore",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(22.dp))

      // Pre-login Automatic App (APK) Download Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.dp,
              Brush.linearGradient(
                listOf(CoralPrimary.copy(alpha = 0.55f), IndigoSecondary.copy(alpha = 0.55f))
              ),
              RoundedCornerShape(22.dp),
            ),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(40.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary))),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.download_app_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  "${stringResource(R.string.download_app_no_login_hint)} • $apkSizeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(Modifier.height(12.dp))

          when (val state = downloadState) {
            ApkDownloadState.Idle -> {
              OutlinedButton(
                onClick = {
                  scope.launch {
                    ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                  }
                },
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(50.dp)
                    .testTag("auth_download_app_button"),
              ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.download_app_button),
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            is ApkDownloadState.Downloading -> {
              LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
              )
              Spacer(Modifier.height(8.dp))
              Text(
                text = "Downloading VibeShare-v1.0.apk (${(state.progress * 100).toInt()}%)...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            is ApkDownloadState.Completed -> {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(10.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "App downloaded automatically!",
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "Saved to ${state.displayPath}",
                      style = MaterialTheme.typography.bodySmall,
                    )
                  }
                }
              }
              Spacer(Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
                  onClick = {
                    scope.launch {
                      ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                    }
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("auth_download_app_button"),
                ) {
                  Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("Download Again")
                }
                OutlinedButton(
                  onClick = { ApkDownloader.shareApkFile(context, state.shareableFile) },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("auth_share_apk_button"),
                ) {
                  Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(stringResource(R.string.share_apk_button))
                }
              }
            }

            is ApkDownloadState.Failed -> {
              Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
              Spacer(Modifier.height(8.dp))
              OutlinedButton(
                onClick = {
                  scope.launch {
                    ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                  }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_download_app_button"),
              ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry Download")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, color: Color) {
  Surface(
    color = color.copy(alpha = 0.12f),
    shape = RoundedCornerShape(50),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(16.dp),
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
