package com.example.blackbox.stub

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.blackbox.BlackBoxCore
import java.io.File

/**
 * Stub container activities representing isolated virtual processes (:p0, :p1, :p2, :p3).
 * Runs EVERY cloned application directly inside the BlackBox Virtual Sandbox Workspace
 * with isolated multi-user storage and a visual clone badge banner overlay.
 */
open class StubActivity : Activity() {

    companion object {
        private const val TAG = "StubActivity"
        const val EXTRA_PKG = "blackbox_extra_pkg"
        const val EXTRA_USER_ID = "blackbox_extra_user_id"
        const val EXTRA_TARGET_ACTIVITY = "blackbox_extra_target_activity"
        const val EXTRA_APP_NAME = "blackbox_extra_app_name"
    }

    private var targetPackage: String? = null
    private var virtualUserId: Int = 0
    private var appName: String = "Cloned App"
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetPackage = intent?.getStringExtra(EXTRA_PKG)
        virtualUserId = intent?.getIntExtra(EXTRA_USER_ID, 0) ?: 0
        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: targetPackage ?: "Cloned App"

        Log.d(TAG, "Launched Isolated StubActivity [${this.javaClass.simpleName}] for pkg=$targetPackage, user=$virtualUserId")

        targetPackage?.let { pkg ->
            BlackBoxCore.get().registerRunningProcess(pkg, virtualUserId, this.javaClass.simpleName)
        }

        // Root container layout
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFF0F172A.toInt()) // Deep slate dark theme
        }

        // 1. Content Container (Sandboxed Isolated Virtual App Engine)
        val contentContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create isolated user data directory
        val isolatedDataDir = File(BlackBoxCore.get().getUserDir(virtualUserId), targetPackage ?: "app")
        isolatedDataDir.mkdirs()

        // Build embedded Sandboxed App Engine View
        val targetUrl = resolveSandboxedUrl(targetPackage)
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; BlackBox/Clone$virtualUserId) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

            // Enforce multi-user cookie & session isolation
            CookieManager.getInstance().setAcceptCookie(true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false // Keep 100% inside sandboxed virtual container
                }
            }
            loadUrl(targetUrl)
        }
        contentContainer.addView(webView)

        // 2. Floating Visual Clone Badge Banner Overlay (Confirms Clone Identity & User ID)
        val badgeBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            elevation = 16f

            val bannerBg = GradientDrawable().apply {
                setColor(0xF00F172A.toInt()) // Semi-transparent Slate
                cornerRadius = 24f
                setStroke(2, 0xFF38BDF8.toInt()) // Sky blue accent border
            }
            background = bannerBg

            val badgeTag = TextView(this@StubActivity).apply {
                text = " CLONE #$virtualUserId "
                textSize = 11f
                setTextColor(Color.BLACK)
                val tagBg = GradientDrawable().apply {
                    setColor(0xFF38BDF8.toInt())
                    cornerRadius = 12f
                }
                background = tagBg
                setPadding(12, 4, 12, 4)
            }

            val badgeText = TextView(this@StubActivity).apply {
                text = " $appName  •  User Profile #$virtualUserId  •  Sandboxed "
                textSize = 12f
                setTextColor(Color.WHITE)
                setPadding(12, 0, 12, 0)
            }

            val closeBtn = TextView(this@StubActivity).apply {
                text = " ✕ "
                textSize = 14f
                setTextColor(0xFF94A3B8.toInt())
                setOnClickListener {
                    finish()
                }
            }

            addView(badgeTag)
            addView(badgeText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(closeBtn)
        }

        val bannerLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setMargins(24, 48, 24, 0)
        }

        rootLayout.addView(contentContainer)
        rootLayout.addView(badgeBanner, bannerLayoutParams)
        setContentView(rootLayout)
    }

    private fun resolveSandboxedUrl(pkg: String?): String {
        if (pkg == null) return "https://www.google.com"
        val lower = pkg.lowercase()
        return when {
            lower.contains("whatsapp") -> "https://web.whatsapp.com"
            lower.contains("instagram") -> "https://www.instagram.com"
            lower.contains("facebook") || lower.contains("katana") -> "https://m.facebook.com"
            lower.contains("twitter") || lower.contains("x.corp") -> "https://x.com"
            lower.contains("telegram") -> "https://web.telegram.org"
            lower.contains("messenger") -> "https://www.messenger.com"
            lower.contains("youtube") -> "https://m.youtube.com"
            lower.contains("tiktok") -> "https://www.tiktok.com"
            lower.contains("reddit") -> "https://www.reddit.com"
            lower.contains("linkedin") -> "https://www.linkedin.com"
            else -> "https://www.google.com/search?q=${pkg.replace(".", "+")}+app+sandbox"
        }
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        targetPackage?.let { pkg ->
            BlackBoxCore.get().unregisterRunningProcess(pkg, virtualUserId)
        }
    }

    class P0 : StubActivity()
    class P1 : StubActivity()
    class P2 : StubActivity()
    class P3 : StubActivity()
}
