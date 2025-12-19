package com.example.coach.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.coach.R

@Composable
fun TutorialScreen() {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.tutorial}")
    }

    // We use a DisposableEffect to release the video player when the screen is left
    var videoView: VideoView? = null
    DisposableEffect(Unit) {
        onDispose {
            videoView?.stopPlayback()
        }
    }

    AndroidView(
        factory = {
            VideoView(it).apply {
                setVideoURI(videoUri)
                // Add media controller for play/pause etc.
                val mediaController = android.widget.MediaController(it)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                start() // Start playing automatically
                videoView = this
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
