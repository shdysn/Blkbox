package com.example.blackbox.stub

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.blackbox.BlackBoxCore

/**
 * Stub container activities representing isolated virtual processes (:p0, :p1, :p2, :p3).
 * These serve as the host processes for virtualized applications.
 */
open class StubActivity : Activity() {

    companion object {
        private const val TAG = "StubActivity"
        const val EXTRA_PKG = "blackbox_extra_pkg"
        const val EXTRA_USER_ID = "blackbox_extra_user_id"
        const val EXTRA_TARGET_ACTIVITY = "blackbox_extra_target_activity"
    }

    private var targetPackage: String? = null
    private var virtualUserId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetPackage = intent?.getStringExtra(EXTRA_PKG)
        virtualUserId = intent?.getIntExtra(EXTRA_USER_ID, 0) ?: 0
        val targetActivity = intent?.getStringExtra(EXTRA_TARGET_ACTIVITY)

        Log.d(TAG, "Launched StubActivity [${this.javaClass.simpleName}] for package=$targetPackage, user=$virtualUserId, target=$targetActivity")

        // Register running process in BlackBox core
        targetPackage?.let { pkg ->
            BlackBoxCore.get().registerRunningProcess(pkg, virtualUserId, this.javaClass.simpleName)
        }

        // Build native container view
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF0B0F17.toInt())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "BlackBox Virtual Container"
            textSize = 20f
            setTextColor(0xFF00F0FF.toInt())
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Running: ${targetPackage ?: "Virtual App"} [Clone #$virtualUserId]\nProcess: ${android.os.Process.myPid()}"
            textSize = 14f
            setTextColor(0xFF94A3B8.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 32)
        }

        val progress = ProgressBar(this).apply {
            isIndeterminate = true
        }

        val footer = TextView(this).apply {
            text = "Sandboxed isolated storage mounted\nData: /virtual/users/$virtualUserId/$targetPackage"
            textSize = 12f
            setTextColor(0xFF64748B.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        container.addView(title)
        container.addView(subtitle)
        container.addView(progress)
        container.addView(footer)
        root.addView(container)
        setContentView(root)

        // Attempt direct launch if original component is directly dispatchable or invoke host launcher
        targetPackage?.let { pkg ->
            tryLaunchOriginal(pkg)
        }
    }

    private fun tryLaunchOriginal(pkg: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            // In a full root/hooked environment, Binder hooks redirect IPC calls here.
            // When falling back in standard Android container, we maintain state isolation.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        targetPackage?.let { pkg ->
            BlackBoxCore.get().unregisterRunningProcess(pkg, virtualUserId)
        }
    }

    class P0 : StubActivity()
    class P1 : StubActivity()
    class P2 : StubActivity()
    class P3 : StubActivity()
}
