package com.reelclassifier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageButton
import com.reelclassifier.R
import com.reelclassifier.ui.MainActivity

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var overlayBtn: ImageButton? = null

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "reelsort_overlay"
        private const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        setupNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        addFloatingButton()
    }

    private fun addFloatingButton() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 250
        }

        overlayBtn = ImageButton(this).apply {
            setImageResource(R.drawable.ic_bookmark)
            setBackgroundResource(R.drawable.bg_overlay_button)
            contentDescription = "Open ReelSort"
            setOnClickListener {
                startActivity(Intent(this@OverlayService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }
        }
        wm.addView(overlayBtn, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        overlayBtn?.let { wm.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "ReelSort Overlay", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps the floating button visible" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ReelSort")
            .setContentText("Floating button active — tap to open app")
            .setSmallIcon(R.drawable.ic_bookmark)
            .setContentIntent(pi)
            .build()
    }
}
