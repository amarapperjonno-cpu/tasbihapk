package io.github.amarapperjonno.tasbih

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TARGET_URL = "https://amarapperjonno-cpu.github.io/tasbih/"
        private const val ALLOWED_HOST = "amarapperjonno-cpu.github.io"
        private const val ENABLE_PULL_TO_REFRESH = true
        private const val KEEP_SCREEN_ON = true
        private const val OFFLINE_CACHE = true
        private const val DOUBLE_BACK_TO_EXIT = true
        private const val FULLSCREEN_MODE = false
        private const val OPEN_EXTERNAL_LINKS_IN_BROWSER = true
    }

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var splashOverlay: LinearLayout
    private lateinit var errorOverlay: LinearLayout
    private lateinit var retryButton: Button

    private var lastBackPressTime = 0L
    private var initialPageLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (KEEP_SCREEN_ON) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (FULLSCREEN_MODE) {
            applyFullscreen()
        }

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        splashOverlay = findViewById(R.id.splashOverlay)
        errorOverlay = findViewById(R.id.errorOverlay)
        retryButton = findViewById(R.id.retryButton)

        setupSwipeRefresh()
        setupWebView()
        setupBackNavigation()

        retryButton.setOnClickListener {
            errorOverlay.visibility = View.GONE
            configureCacheMode()
            if (webView.url.isNullOrBlank()) {
                webView.loadUrl(TARGET_URL)
            } else {
                webView.reload()
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
            splashOverlay.visibility = View.GONE
            initialPageLoaded = true
        } else {
            webView.loadUrl(TARGET_URL)
        }
    }

    private fun applyFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.isEnabled = ENABLE_PULL_TO_REFRESH
        swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_status_bar)
        swipeRefresh.setOnRefreshListener {
            configureCacheMode()
            webView.reload()
        }
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            webView.scrollY > 0
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.mediaPlaybackRequiresUserGesture = false

        configureCacheMode()

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.overScrollMode = View.OVER_SCROLL_NEVER

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                errorOverlay.visibility = View.GONE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
                if (!initialPageLoaded) {
                    initialPageLoaded = true
                    splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(220L)
                        .withEndAction { splashOverlay.visibility = View.GONE }
                        .start()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme?.lowercase() ?: return false

                if (scheme == "tel" || scheme == "mailto" || scheme == "whatsapp") {
                    openSystemIntent(uri)
                    return true
                }

                if (scheme != "http" && scheme != "https") {
                    return true
                }

                val targetHost = uri.host?.lowercase() ?: ""
                if (targetHost == ALLOWED_HOST.lowercase() || !OPEN_EXTERNAL_LINKS_IN_BROWSER) {
                    return false
                }

                openExternalUrl(uri)
                return true
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    swipeRefresh.isRefreshing = false
                    progressBar.visibility = View.GONE
                    splashOverlay.visibility = View.GONE
                    errorOverlay.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun configureCacheMode() {
        webView.settings.cacheMode = if (OFFLINE_CACHE && !isNetworkAvailable()) {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        } else {
            WebSettings.LOAD_DEFAULT
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun openExternalUrl(uri: Uri) {
        try {
            val customTabs = CustomTabsIntent.Builder().build()
            customTabs.launchUrl(this, uri)
        } catch (_: Exception) {
            openSystemIntent(uri)
        }
    }

    private fun openSystemIntent(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                    return
                }
                if (DOUBLE_BACK_TO_EXIT) {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000L) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.press_back_again),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    finish()
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
