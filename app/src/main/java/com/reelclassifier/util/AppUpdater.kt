package com.reelclassifier.util

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.reelclassifier.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    // Points to a JSON file hosting the latest version info and APK direct download URL
    var UPDATE_URL = "https://raw.githubusercontent.com/Paapiverma/ReelSort/main/version.json"

    fun checkForUpdates(activity: AppCompatActivity) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conn = URL(UPDATE_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                if (conn.responseCode != 200) return@launch
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonStr)
                val latestCode = json.optInt("versionCode", 0)
                val latestName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val changelog = json.optString("changelog", "Bug fixes & improvements")

                if (latestCode > BuildConfig.VERSION_CODE && apkUrl.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(activity, latestName, apkUrl, changelog)
                    }
                }
            } catch (_: Exception) {
                // Offline or check failed - silent skip
            }
        }
    }

    private fun showUpdateDialog(activity: AppCompatActivity, version: String, apkUrl: String, changelog: String) {
        AlertDialog.Builder(activity)
            .setTitle("Update Available ($version)")
            .setMessage("What's new:\n$changelog")
            .setCancelable(false)
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstall(activity, apkUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstall(activity: AppCompatActivity, apkUrl: String) {
        Toast.makeText(activity, "Downloading update...", Toast.LENGTH_SHORT).show()
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conn = URL(apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 30000
                val file = File(activity.getExternalFilesDir(null), "update.apk")
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    installApk(activity, file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(activity: AppCompatActivity, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                )
                Toast.makeText(activity, "Allow install permission, then tap Update", Toast.LENGTH_LONG).show()
                return
            }
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }
}
