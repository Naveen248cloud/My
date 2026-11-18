package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.IndigoSecondary
import com.example.ui.theme.ThemeMode
import com.example.util.ApkDownloadState
import com.example.util.ApkDownloader
import com.example.util.formatFileSize
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  themeMode: ThemeMode,
  onThemeModeChange: (ThemeMode) -> Unit,
  onSignOutClick: () -> Unit,
  onBackClick: () -> Unit,
) {
  BackHandler(onBack = onBackClick)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val authUser = Firebase.auth.currentUser
  val apkSizeLabel = remember(context) { formatFileSize(ApkDownloader.getApkSizeBytes(context)) }
  var downloadState by remember { mutableStateOf<ApkDownloadState>(ApkDownloadState.Idle) }

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_back_button"),
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.nav_settings),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      // Automatic App Download Card (APK)
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.5.dp,
              Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary)),
              RoundedCornerShape(24.dp),
            ),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(48.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Brush.linearGradient(listOf(CoralPrimary, IndigoSecondary))),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
              )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.download_app_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "VibeShare v1.0 • $apkSizeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Spacer(Modifier.height(12.dp))
          Text(
            text = stringResource(R.string.download_app_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(Modifier.height(16.dp))

          when (val state = downloadState) {
            ApkDownloadState.Idle -> {
              Button(
                onClick = {
                  scope.launch {
                    ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                  }
                },
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(52.dp)
                    .testTag("download_app_button"),
              ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download_app_button), fontWeight = FontWeight.Bold)
              }
            }

            is ApkDownloadState.Downloading -> {
              Column(modifier = Modifier.fillMaxWidth()) {
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
            }

            is ApkDownloadState.Completed -> {
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(12.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(Modifier.width(10.dp))
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
              Spacer(Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = {
                    scope.launch {
                      ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                    }
                  },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).testTag("download_app_button"),
                ) {
                  Icon(Icons.Filled.Download, contentDescription = null)
                  Spacer(Modifier.width(6.dp))
                  Text("Download Again")
                }
                OutlinedButton(
                  onClick = { ApkDownloader.shareApkFile(context, state.shareableFile) },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.weight(1f).testTag("share_apk_button"),
                ) {
                  Icon(Icons.Filled.Share, contentDescription = null)
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
              Spacer(Modifier.height(10.dp))
              Button(
                onClick = {
                  scope.launch {
                    ApkDownloader.downloadInstalledApk(context).collect { downloadState = it }
                  }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("download_app_button"),
              ) {
                Text("Retry Download")
              }
            }
          }
        }
      }

      // Appearance Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = IndigoSecondary)
            Spacer(Modifier.width(10.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
          }
          Spacer(Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = themeMode == mode,
                onClick = { onThemeModeChange(mode) },
                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
              )
            }
          }
        }
      }

      // Account Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text("Signed-In Account", style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(6.dp))
          Text(
            text = authUser?.displayName ?: "VibeShare User",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
          )
          if (!authUser?.email.isNullOrBlank()) {
            Text(
              text = authUser?.email.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Spacer(Modifier.height(14.dp))
          OutlinedButton(
            onClick = onSignOutClick,
            colors =
              ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.sign_out))
          }
        }
      }
    }
  }
}
