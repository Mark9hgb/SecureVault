package com.vaultapp.securevault.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.vaultapp.securevault.data.database.VideoEntity
import com.vaultapp.securevault.media.EncryptedVideoDataSource
import com.vaultapp.securevault.ui.viewmodel.PlayerViewModel
import java.io.File

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    video: VideoEntity,
    dataSourceFactory: EncryptedVideoDataSource.Factory,
    viewModel: PlayerViewModel = hiltViewModel(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val subtitleUri by viewModel.subtitleUri.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply { playWhenReady = true }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.playWhenReady = true
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(video) {
        val encryptedFile = File(video.encryptedFilePath)
        val ivFile = File("${video.encryptedFilePath}.iv")

        ivFile.writeBytes(video.iv)

        val mediaUri = Uri.Builder()
            .scheme("file")
            .path(encryptedFile.absolutePath)
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(mediaUri)

        val subtitlePath = subtitleUri
        if (subtitlePath != null) {
            val subUri = Uri.Builder()
                .scheme("file")
                .path(subtitlePath)
                .build()

            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subUri)
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage("en")
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .build()

            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            if (ivFile.exists()) {
                ivFile.delete()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val subtitleFile = File(context.cacheDir, "subtitle_${System.currentTimeMillis()}.vtt")
            context.contentResolver.openInputStream(it)?.use { input ->
                subtitleFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.loadSubtitle(subtitleFile.absolutePath)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video.originalFileName,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { subtitlePickerLauncher.launch("text/*") }) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Load Subtitles"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowSubtitleButton(true)

                        subtitleView?.setStyle(
                            CaptionStyleCompat(
                                CaptionStyleCompat.DEFAULT.foregroundColor,
                                CaptionStyleCompat.DEFAULT.backgroundColor,
                                CaptionStyleCompat.DEFAULT.windowColor,
                                CaptionStyleCompat.DEFAULT.edgeType,
                                CaptionStyleCompat.DEFAULT.edgeColor,
                                CaptionStyleCompat.DEFAULT.typeface
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
