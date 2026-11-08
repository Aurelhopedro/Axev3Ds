package com.example.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ViewerScreen(modelUrl: String?) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        if (modelUrl != null) {
                            view?.evaluateJavascript("loadModel('$modelUrl')", null)
                        }
                    }
                }
                loadUrl("file:///android_asset/viewer.html")
            }
        },
        update = { webView ->
            if (modelUrl != null) {
                webView.evaluateJavascript("loadModel('$modelUrl')", null)
            }
        }
    )
}
