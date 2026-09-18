package com.calistenia.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calistenia.app.data.ExerciseVideo
import com.calistenia.app.data.ExerciseVideos
import com.calistenia.app.data.local.ExerciseEntity
import kotlinx.coroutines.delay

@Composable internal fun ExerciseVideoCard(exercise: ExerciseEntity) {
    val video = ExerciseVideos.all[exercise.id] ?: return
    var open by rememberSaveable(exercise.id) { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Veja o movimento", style = MaterialTheme.typography.titleMedium)
            Text("YouTube • precisa de internet", style = MaterialTheme.typography.bodySmall)
            video.variation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text("Vídeo de ${video.author}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button({ open = true }, Modifier.fillMaxWidth()) { Text("Ver vídeo") }
        }
    }
    if (open) ExerciseVideoDialog(exercise.name, video) { open = false }
}

@Composable internal fun ExerciseVideoDialog(name: String, video: ExerciseVideo, close: () -> Unit) {
    val context = LocalContext.current
    var attempt by remember(video.youtubeId) { mutableIntStateOf(0) }
    Dialog(close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 680.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(name, style = MaterialTheme.typography.titleLarge)
                Text("Demonstração • ${video.author}", style = MaterialTheme.typography.bodyMedium)
                key(video.youtubeId, attempt) { ExerciseYouTubePlayer(video) }
                Text("Toque no play para assistir. O YouTube pode exibir anúncios e usar dados de navegação. O áudio pode estar em outro idioma.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton({ attempt++ }, Modifier.fillMaxWidth()) { Text("Tentar novamente") }
                OutlinedButton({ openVideoLink(context, Uri.parse(video.watchUrl)) }, Modifier.fillMaxWidth()) { Text("Abrir no YouTube") }
                Button(close, Modifier.fillMaxWidth()) { Text("Voltar às instruções") }
            }
        }
    }
}

private fun openVideoLink(context: Context, uri: Uri) {
    if (uri.scheme != "https") return
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo disponível para abrir o vídeo.", Toast.LENGTH_LONG).show()
    }
}

internal enum class VideoStatus { LOADING, READY, ERROR }

/** Only accepts status signals; no app data or privileged actions are exposed to web content. */
internal class VideoStatusBridge(private val update: (VideoStatus) -> Unit) {
    @JavascriptInterface fun ready() = update(VideoStatus.READY)
    @JavascriptInterface fun error() = update(VideoStatus.ERROR)
}

internal const val VIDEO_ORIGIN = "https://com.calistenia.app/"

internal fun videoHtml(video: ExerciseVideo): String = """
    <!doctype html><html lang="pt-BR"><head>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="referrer" content="strict-origin-when-cross-origin">
    <style>html,body{margin:0;width:100%;height:100%;background:#000}#player{width:100%;height:100%;min-height:200px}</style>
    </head><body><div id="player"></div>
    <script>
    var player;
    function onYouTubeIframeAPIReady() {
      player = new YT.Player('player', {
        videoId: '${video.youtubeId}',
        playerVars: {autoplay:0, playsinline:1, controls:1, rel:0, fs:0, origin:'https://com.calistenia.app'},
        events: {onReady:function(){VideoStatus.ready()}, onError:function(){VideoStatus.error()}}
      });
    }
    </script><script src="https://www.youtube.com/iframe_api" onerror="VideoStatus.error()"></script>
    </body></html>
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun ExerciseYouTubePlayer(video: ExerciseVideo) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var status by remember { mutableStateOf(VideoStatus.LOADING) }
    val webView = remember(video.youtubeId) {
        try { WebView(context).apply {
            webChromeClient = WebChromeClient()
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
        } } catch (_: RuntimeException) { null }
    }
    if (webView == null) {
        Text("O player não está disponível neste aparelho. Abra o vídeo no YouTube ou atualize o Android System WebView.", style = MaterialTheme.typography.bodySmall)
        return
    }
    DisposableEffect(webView, lifecycle) {
        var disposed = false
        webView.addJavascriptInterface(VideoStatusBridge { next ->
            webView.post { if (!disposed) status = next }
        }, "VideoStatus")
        webView.webViewClient = object : WebViewClient() {
            // Preserve player click-throughs without navigating the local page with its status bridge.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                if (request.hasGesture()) openVideoLink(context, request.url)
                return true
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && !disposed) status = VideoStatus.ERROR
            }
        }
        webView.loadDataWithBaseURL(VIDEO_ORIGIN, videoHtml(video), "text/html", "UTF-8", null)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                webView.evaluateJavascript("if(window.player && player.pauseVideo){player.pauseVideo();}", null)
                webView.onPause()
            } else if (event == Lifecycle.Event.ON_RESUME) webView.onResume()
        }
        lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            webView.stopLoading()
            webView.onPause()
            webView.removeJavascriptInterface("VideoStatus")
            webView.destroy()
        }
    }
    LaunchedEffect(webView) {
        delay(20_000)
        if (status == VideoStatus.LOADING) status = VideoStatus.ERROR
    }
    AndroidView({ webView }, Modifier.fillMaxWidth().height(220.dp))
    when (status) {
        VideoStatus.LOADING -> Text("Carregando vídeo…", style = MaterialTheme.typography.bodySmall)
        VideoStatus.ERROR -> Text("Não foi possível carregar o vídeo. Confira sua conexão, tente novamente ou abra no YouTube. As instruções continuam disponíveis.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        VideoStatus.READY -> Unit
    }
}
