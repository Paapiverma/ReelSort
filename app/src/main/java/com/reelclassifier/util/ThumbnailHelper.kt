package com.reelclassifier.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ThumbnailHelper {

    fun capture(context: Context, reelUrl: String, onComplete: (String?) -> Unit) {
        val shortcode = Regex("/(?:reel|p|tv)/([A-Za-z0-9_-]+)").find(reelUrl)?.groupValues?.get(1)
        if (shortcode == null) {
            onComplete(null)
            return
        }

        val targetFile = File(context.filesDir, "thumb_$shortcode.jpg")
        if (targetFile.exists() && targetFile.length() > 0) {
            onComplete(targetFile.absolutePath)
            return
        }

        Handler(Looper.getMainLooper()).post {
            val webView = WebView(context.applicationContext)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.layout(0, 0, 480, 600)

            var done = false
            fun finish(path: String?) {
                if (done) return
                done = true
                try { webView.destroy() } catch (_: Exception) {}
                onComplete(path)
            }

            // 6 second timeout safety
            Handler(Looper.getMainLooper()).postDelayed({ finish(null) }, 6000)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // Give JS 1.5 seconds to render the media inside embed
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (done) return@postDelayed
                        webView.evaluateJavascript(
                            "(function() { var img = document.querySelector('img.EmbeddedMediaImage') || document.querySelector('img[src*=\"cdninstagram\"]'); return img ? img.src : ''; })();"
                        ) { result ->
                            val cleanUrl = result?.trim('"', '\'', ' ', '\\')
                            if (!cleanUrl.isNullOrEmpty() && cleanUrl != "null" && cleanUrl.startsWith("http")) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val path = downloadImage(cleanUrl, targetFile)
                                    withContext(Dispatchers.Main) { finish(path) }
                                }
                            } else {
                                // Fallback: snapshot the top portion of the WebView
                                try {
                                    val bitmap = Bitmap.createBitmap(480, 520, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    webView.draw(canvas)
                                    FileOutputStream(targetFile).use { out ->
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                                    }
                                    finish(targetFile.absolutePath)
                                } catch (_: Exception) {
                                    finish(null)
                                }
                            }
                        }
                    }, 1500)
                }
            }
            webView.loadUrl("https://www.instagram.com/reel/$shortcode/embed/")
        }
    }

    private fun downloadImage(imageUrl: String, targetFile: File): String? {
        return try {
            val conn = URL(imageUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
