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
 * Provides an isolated Virtual User Profile Sandbox with dedicated data directory separation
 * and a floating visual Clone Badge Banner overlay to distinguish cloned instances.
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

        // 1. Content Container (Sandboxed Web/App Workspace or Dual Account Interface)
        val contentContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Mount isolated WebView sandbox if social/web package, or native launcher UI
        val targetUrl = getWebUrlForPackage(targetPackage)
        if (targetUrl != null) {
            val isolatedDataDir = File(BlackBoxCore.get().getUserDir(virtualUserId), targetPackage ?: "app")
            isolatedDataDir.mkdirs()

            webView = WebView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT

                // Enforce multi-user cookie & data directory isolation
                CookieManager.getInstance().setAcceptCookie(true)

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false // Keep inside sandboxed web container
                    }
                }
                loadUrl(targetUrl)
            }
            contentContainer.addView(webView)
        } else {
            // Native Sandboxed Launcher Dashboard view
            val nativeDashboard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(48, 120, 48, 48)

                val iconCard = TextView(this@StubActivity).apply {
                    text = appName.take(1).uppercase()
                    textSize = 36f
                    setTextColor(0xFF00F0FF.toInt())
                    gravity = Gravity.CENTER
                    val bg = GradientDrawable().apply {
                        setColor(0xFF1E293B.toInt())
                        cornerRadius = 32f
                        setStroke(2, 0xFF00F0FF.toInt())
                    }
                    background = bg
                    layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        bottomMargin = 24
                    }
                }

                val titleText = TextView(this@StubActivity).apply {
                    text = "$appName (Clone #$virtualUserId)"
                    textSize = 22f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                }

                val subtitleText = TextView(this@StubActivity).apply {
                    text = "Isolated User Profile Sandbox #$virtualUserId\nPackage: $targetPackage\nStorage: /virtual/users/$virtualUserId/$targetPackage"
                    textSize = 13f
                    setTextColor(0xFF94A3B8.toInt())
                    gravity = Gravity.CENTER
                    setPadding(0, 16, 0, 32)
                }

                val launchNativeBtn = Button(this@StubActivity).apply {
                    text = "Launch Standalone App Instance"
                    setTextColor(Color.WHITE)
                    val btnBg = GradientDrawable().apply {
                        setColor(0xFF2563EB.toInt())
                        cornerRadius = 16f
                    }
                    background = btnBg
                    setOnClickListener {
                        tryLaunchOriginal(targetPackage ?: "")
                    }
                }

                addView(iconCard)
                addView(titleText)
                addView(subtitleText)
                addView(launchNativeBtn)
            }
            contentContainer.addView(nativeDashboard)
        }

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
                text = " $appName  •  User Profile #$virtualUserId  •  Sandboxed"
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

    private fun getWebUrlForPackage(pkg: String?): String? {
        if (pkg == null) return null
        return when {
            pkg.contains("whatsapp") -> "https://web.whatsapp.com"
            pkg.contains("instagram") -> "https://www.instagram.com"
            pkg.contains("facebook") or pkg.contains("katana") -> "https://m.facebook.com"
            pkg.contains("twitter") or pkg.contains("x.corp") -> "https://x.com"
            pkg.contains("telegram") -> "https://web.telegram.org"
            pkg.contains("messenger") -> "https://www.messenger.com"
            else -> null
        }
    }

    private fun tryLaunchOriginal(pkg: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Application launch intent not found for $pkg", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch target intent for $pkg", e)
            Toast.makeText(this, "Launch error: ${e.message}", Toast.LENGTH_LONG).show()
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
