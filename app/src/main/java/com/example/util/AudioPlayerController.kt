package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioPlaybackState(
  val activePostId: String? = null,
  val isPlaying: Boolean = false,
  val isLoading: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val error: String? = null,
)

class AudioPlayerController(private val scope: CoroutineScope) {
  private var mediaPlayer: MediaPlayer? = null
  private var progressJob: Job? = null

  private val _state = MutableStateFlow(AudioPlaybackState())
  val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

  fun togglePlay(context: Context, postId: String, sourceUriOrUrl: String) {
    val current = _state.value
    if (current.activePostId == postId && mediaPlayer != null) {
      val mp = mediaPlayer!!
      if (mp.isPlaying) {
        mp.pause()
        _state.value = _state.value.copy(isPlaying = false)
        progressJob?.cancel()
      } else {
        mp.start()
        _state.value = _state.value.copy(isPlaying = true)
        startProgressTicker()
      }
      return
    }

    stop()
    if (sourceUriOrUrl.isBlank()) {
      _state.value =
        AudioPlaybackState(
          activePostId = postId,
          error = "No playable audio source attached to this post.",
        )
      return
    }

    _state.value = AudioPlaybackState(activePostId = postId, isLoading = true)
    try {
      val player =
        MediaPlayer().apply {
          setAudioAttributes(
            AudioAttributes.Builder()
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .build()
          )
          if (sourceUriOrUrl.startsWith("http://") || sourceUriOrUrl.startsWith("https://")) {
            setDataSource(sourceUriOrUrl)
          } else {
            setDataSource(context, Uri.parse(sourceUriOrUrl))
          }
          setOnPreparedListener { mp ->
            mp.start()
            _state.value =
              _state.value.copy(
                isLoading = false,
                isPlaying = true,
                durationMs = mp.duration.toLong().coerceAtLeast(0L),
                error = null,
              )
            startProgressTicker()
          }
          setOnCompletionListener {
            progressJob?.cancel()
            _state.value =
              _state.value.copy(
                isPlaying = false,
                currentPositionMs = 0L,
              )
          }
          setOnErrorListener { _, _, _ ->
            progressJob?.cancel()
            _state.value =
              _state.value.copy(
                isLoading = false,
                isPlaying = false,
                error = "Unable to play this audio file on this device.",
              )
            true
          }
          prepareAsync()
        }
      mediaPlayer = player
    } catch (e: Exception) {
      _state.value =
        AudioPlaybackState(
          activePostId = postId,
          error = e.localizedMessage ?: "Audio playback failed",
        )
    }
  }

  private fun startProgressTicker() {
    progressJob?.cancel()
    progressJob =
      scope.launch(Dispatchers.Main) {
        while (true) {
          val mp = mediaPlayer ?: break
          if (mp.isPlaying) {
            _state.value =
              _state.value.copy(
                currentPositionMs = mp.currentPosition.toLong().coerceAtLeast(0L),
                durationMs = mp.duration.toLong().coerceAtLeast(0L),
              )
          }
          delay(250L)
        }
      }
  }

  fun stop() {
    progressJob?.cancel()
    progressJob = null
    try {
      mediaPlayer?.stop()
    } catch (_: Exception) {}
    try {
      mediaPlayer?.release()
    } catch (_: Exception) {}
    mediaPlayer = null
    _state.value = AudioPlaybackState()
  }
}
