package com.reelclassifier.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ThumbnailHelper {

    private const val CRAWLER_UA = "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"

    fun capture(context: Context, reelUrl: String, onComplete: (String?) -> Unit) {
        val shortcode = Regex("/(?:reel|p|tv)/([A-Za-z0-9_-]+)").find(reelUrl)?.groupValues?.get(1)
        if (shortcode == null) {
            onComplete(null)
            return
        }

        val targetFile = File(context.filesDir, "thumb_v2_$shortcode.jpg")
        if (targetFile.exists() && targetFile.length() > 1000) {
            onComplete(targetFile.absolutePath)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val imageUrl = fetchOgImage(shortcode)
            val path = if (imageUrl != null) {
                downloadImage(imageUrl, targetFile)
            } else null

            withContext(Dispatchers.Main) {
                onComplete(path)
            }
        }
    }

    private fun fetchOgImage(shortcode: String): String? {
        return fetchOgImageFromUrl("https://www.instagram.com/reel/$shortcode/")
            ?: fetchOgImageFromUrl("https://www.instagram.com/p/$shortcode/")
    }

    private fun fetchOgImageFromUrl(urlString: String): String? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", CRAWLER_UA)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                connectTimeout = 6000
                readTimeout = 6000
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val sb = StringBuilder()
            var line: String?
            var totalChars = 0
            while (reader.readLine().also { line = it } != null && totalChars < 131072) {
                sb.append(line).append('\n')
                totalChars += line!!.length
                if (line!!.contains("og:image")) {
                    for (i in 0..2) {
                        val next = reader.readLine() ?: break
                        sb.append(next).append('\n')
                    }
                    break
                }
            }
            conn.disconnect()

            extractOgImage(sb.toString())
        } catch (_: Exception) {
            null
        }
    }

    private fun extractOgImage(html: String): String? {
        val p1 = Regex("""<meta[^>]+property=['"]og:image['"][^>]+content=['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        val m1 = p1.find(html)?.groupValues?.get(1)
        if (!m1.isNullOrEmpty()) return m1.replace("&amp;", "&")

        val p2 = Regex("""<meta[^>]+content=['"]([^'"]+)['"][^>]+property=['"]og:image['"]""", RegexOption.IGNORE_CASE)
        val m2 = p2.find(html)?.groupValues?.get(1)
        if (!m2.isNullOrEmpty()) return m2.replace("&amp;", "&")

        val p3 = Regex("""https://[^\s"']+\.cdninstagram\.com/[^\s"']+\.jpg[^\s"']*""")
        return p3.find(html)?.value?.replace("&amp;", "&")
    }

    private fun downloadImage(imageUrl: String, targetFile: File): String? {
        val tempFile = File(targetFile.parentFile, targetFile.name + ".tmp")
        return try {
            val conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", CRAWLER_UA)
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = true
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }

            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            conn.disconnect()

            if (tempFile.exists() && tempFile.length() > 1000) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                targetFile.absolutePath
            } else {
                tempFile.delete()
                null
            }
        } catch (_: Exception) {
            try { tempFile.delete() } catch (_: Exception) {}
            null
        }
    }
}
