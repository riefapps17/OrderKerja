package com.example.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.print.PrintAttributes
import android.print.PrintManager
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.R
import com.example.model.FileChooserRequest
import com.example.model.ViewportMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.findActivity(): Activity? {
  var currentContext = this
  while (currentContext is ContextWrapper) {
    if (currentContext is Activity) return currentContext
    currentContext = currentContext.baseContext
  }
  return null
}

class OkRepairJsBridge(
  private val onPrintRequested: () -> Unit,
  private val onWebCameraPermissionRequested: () -> Unit,
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  @JavascriptInterface
  fun printPage() {
    mainHandler.post {
      onPrintRequested()
    }
  }

  @JavascriptInterface
  fun requestCameraPermission() {
    mainHandler.post {
      onWebCameraPermissionRequested()
    }
  }
}

object RepairWebViewController {

  private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 OKRepairAndroid/1.0"

  private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 OKRepairAndroid/1.0"

  /**
   * JavaScript injected automatically to:
   * 1. Detect and intercept `window.print()` calls from the top window OR any nested Google Apps Script
   *    sandbox iframes (`sandboxFrame`, `userHtmlFrame`) and route them directly to Android PrintManager.
   * 2. Detect when the web app requests `navigator.mediaDevices.getUserMedia` for camera access so Android
   *    automatically requests runtime camera permission if not already granted.
   * 3. Ensure mobile responsive viewport meta tag is applied for smooth portrait & landscape rendering.
   */
  val PRINT_AND_RESPONSIVE_INJECTION_JS = """
    (function() {
      try {
        if (!window.__okRepairInjected) {
          window.__okRepairInjected = true;

          // Override window.print() in top window
          window.print = function() {
            if (window.AndroidBridge && window.AndroidBridge.printPage) {
              window.AndroidBridge.printPage();
            }
          };

          // Listen for postMessage print or camera signals from cross-origin Google Apps Script iframes
          window.addEventListener('message', function(event) {
            try {
              if (!event || !event.data) return;
              var data = event.data;
              if (
                data === 'OK_REPAIR_PRINT' ||
                data === 'print' ||
                data.type === 'OK_REPAIR_PRINT' ||
                data.action === 'print' ||
                data.cmd === 'print'
              ) {
                if (window.AndroidBridge && window.AndroidBridge.printPage) {
                  window.AndroidBridge.printPage();
                }
              }
            } catch (e) {}
          }, false);

          // Automatically notify Android bridge when web JS requests camera stream via getUserMedia
          if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
            var origGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
            navigator.mediaDevices.getUserMedia = function(constraints) {
              try {
                if (constraints && constraints.video && window.AndroidBridge && window.AndroidBridge.requestCameraPermission) {
                  window.AndroidBridge.requestCameraPermission();
                }
              } catch (e) {}
              return origGetUserMedia(constraints);
            };
          }
        }

        // Ensure responsive viewport meta tag exists if missing
        var metaViewport = document.querySelector('meta[name="viewport"]');
        if (!metaViewport && document.head) {
          metaViewport = document.createElement('meta');
          metaViewport.name = 'viewport';
          metaViewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
          document.head.appendChild(metaViewport);
        }

        // Hook window.print() inside accessible nested iframes
        var hookFrames = function(win) {
          try {
            win.print = function() {
              if (window.AndroidBridge && window.AndroidBridge.printPage) {
                window.AndroidBridge.printPage();
              } else if (window.top) {
                window.top.postMessage('OK_REPAIR_PRINT', '*');
              }
            };
          } catch (err) {}
          try {
            for (var i = 0; i < win.frames.length; i++) {
              hookFrames(win.frames[i]);
            }
          } catch (err) {}
        };
        hookFrames(window);
      } catch (e) {}
    })();
  """.trimIndent()

  fun triggerAndroidPrintJob(
    context: Context,
    webView: WebView,
    pageTitle: String,
    onPrintStarted: (String) -> Unit,
  ) {
    val activity = context.findActivity()
    val printHostContext = activity ?: context
    val printManager = printHostContext.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
      Toast.makeText(context, "Layanan cetak tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
      return
    }

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    val sanitizedTitle = pageTitle
      .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      .trim('_')
      .ifBlank { context.getString(R.string.print_job_prefix) }
    val jobName = "${sanitizedTitle}_$timestamp"

    val printAdapter = webView.createPrintDocumentAdapter(jobName)
    val printAttributes = PrintAttributes.Builder()
      .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
      .setResolution(PrintAttributes.Resolution("pdf", "PDF / Printer", 300, 300))
      .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
      .build()

    Toast.makeText(
      context,
      context.getString(R.string.print_toast_started),
      Toast.LENGTH_SHORT,
    ).show()

    printManager.print(jobName, printAdapter, printAttributes)
    onPrintStarted(jobName)
  }

  fun createTempCameraFileUri(context: Context): Uri? {
    return try {
      val captureDir = File(context.cacheDir, "camera_captures").apply {
        if (!exists()) mkdirs()
      }
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val imageFile = File(captureDir, "OK_REPAIR_${timeStamp}.jpg")
      androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
      )
    } catch (e: Exception) {
      null
    }
  }

  fun applyViewportMode(webView: WebView, mode: ViewportMode) {
    webView.settings.apply {
      when (mode) {
        ViewportMode.MOBILE_RESPONSIVE -> {
          userAgentString = MOBILE_USER_AGENT
          useWideViewPort = true
          loadWithOverviewMode = true
          textZoom = 100
        }
        ViewportMode.DESKTOP_WIDE -> {
          userAgentString = DESKTOP_USER_AGENT
          useWideViewPort = true
          loadWithOverviewMode = true
          textZoom = 95
        }
      }
    }
    webView.reload()
  }

  @SuppressLint("SetJavaScriptEnabled")
  fun createConfiguredWebView(
    context: Context,
    initialUrl: String,
    onPageStarted: (String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onPageFinished: (url: String?, title: String?, canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onNavigationUpdated: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    onError: (String) -> Unit,
    onFileChooserRequested: (ValueCallback<Array<Uri>>, FileChooserRequest) -> Unit,
    onPrintRequested: (WebView) -> Unit,
    onWebPermissionRequest: (PermissionRequest) -> Unit,
    onEnsureCameraPermission: () -> Unit,
  ): WebView {
    return WebView(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )

      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      cookieManager.setAcceptThirdPartyCookies(this, true)

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        useWideViewPort = true
        loadWithOverviewMode = true
        builtInZoomControls = true
        displayZoomControls = false
        setSupportZoom(true)
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
        userAgentString = MOBILE_USER_AGENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          safeBrowsingEnabled = true
        }
      }

      addJavascriptInterface(
        OkRepairJsBridge(
          onPrintRequested = { onPrintRequested(this) },
          onWebCameraPermissionRequested = onEnsureCameraPermission,
        ),
        "AndroidBridge",
      )

      webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
          super.onPageStarted(view, url, favicon)
          onPageStarted(url)
          view?.evaluateJavascript(PRINT_AND_RESPONSIVE_INJECTION_JS, null)
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
          super.onPageCommitVisible(view, url)
          view?.evaluateJavascript(PRINT_AND_RESPONSIVE_INJECTION_JS, null)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          view?.evaluateJavascript(PRINT_AND_RESPONSIVE_INJECTION_JS, null)
          onPageFinished(
            url,
            view?.title,
            view?.canGoBack() ?: false,
            view?.canGoForward() ?: false,
          )
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
          super.doUpdateVisitedHistory(view, url, isReload)
          onNavigationUpdated(
            view?.canGoBack() ?: false,
            view?.canGoForward() ?: false,
          )
        }

        override fun shouldOverrideUrlLoading(
          view: WebView?,
          request: WebResourceRequest?,
        ): Boolean {
          val targetUri = request?.url ?: return false
          val scheme = targetUri.scheme?.lowercase(Locale.ROOT) ?: ""

          if (scheme == "http" || scheme == "https") {
            return false // Load inside our WebView (supports Google Apps Script redirects and iframes)
          }

          // Handle external schemes like tel:, mailto:, whatsapp:, intent:
          return try {
            val externalIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(externalIntent)
            true
          } catch (e: Exception) {
            true
          }
        }

        override fun onReceivedError(
          view: WebView?,
          request: WebResourceRequest?,
          error: WebResourceError?,
        ) {
          super.onReceivedError(view, request, error)
          if (request?.isForMainFrame == true) {
            val desc = error?.description?.toString() ?: "Koneksi terputus atau halaman tidak merespons."
            onError(desc)
          }
        }
      }

      webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
          super.onProgressChanged(view, newProgress)
          onProgressChanged(newProgress)
          if (newProgress >= 60) {
            view?.evaluateJavascript(PRINT_AND_RESPONSIVE_INJECTION_JS, null)
          }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
          super.onReceivedTitle(view, title)
          onPageFinished(
            view?.url,
            title,
            view?.canGoBack() ?: false,
            view?.canGoForward() ?: false,
          )
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
          if (request != null) {
            onWebPermissionRequest(request)
          }
        }

        override fun onShowFileChooser(
          webView: WebView?,
          filePathCallback: ValueCallback<Array<Uri>>?,
          fileChooserParams: FileChooserParams?,
        ): Boolean {
          if (filePathCallback == null) return false
          val acceptTypes = fileChooserParams?.acceptTypes
            ?.filter { it.isNotBlank() }
            ?: emptyList()
          val allowMultiple =
            fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
          val captureEnabled = fileChooserParams?.isCaptureEnabled ?: false

          onFileChooserRequested(
            filePathCallback,
            FileChooserRequest(
              acceptTypes = acceptTypes,
              allowMultiple = allowMultiple,
              captureEnabled = captureEnabled,
            ),
          )
          return true
        }

        // Handle target="_blank" or window.open() often used by web print preview popups
        override fun onCreateWindow(
          view: WebView?,
          isDialog: Boolean,
          isUserGesture: Boolean,
          resultMsg: Message?,
        ): Boolean {
          val parentWebView = view ?: return false
          val popupWebView = WebView(parentWebView.context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            addJavascriptInterface(
              OkRepairJsBridge(
                onPrintRequested = { onPrintRequested(this) },
                onWebCameraPermissionRequested = onEnsureCameraPermission,
              ),
              "AndroidBridge",
            )
            webViewClient = object : WebViewClient() {
              override fun shouldOverrideUrlLoading(
                subView: WebView?,
                request: WebResourceRequest?,
              ): Boolean {
                val url = request?.url?.toString()
                if (!url.isNullOrBlank() && url != "about:blank") {
                  parentWebView.loadUrl(url)
                  return true
                }
                return false
              }

              override fun onPageFinished(subView: WebView?, url: String?) {
                super.onPageFinished(subView, url)
                subView?.evaluateJavascript(PRINT_AND_RESPONSIVE_INJECTION_JS, null)
                // If a popup window was opened with HTML content for printing, trigger print automatically
                if (url == "about:blank" || url.isNullOrBlank()) {
                  subView?.let { onPrintRequested(it) }
                }
              }
            }
          }
          val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
          transport.webView = popupWebView
          resultMsg.sendToTarget()
          return true
        }
      }

      setDownloadListener { url, _, _, _, _ ->
        try {
          val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(downloadIntent)
        } catch (e: Exception) {
          Toast.makeText(context, "Tidak dapat mengunduh file", Toast.LENGTH_SHORT).show()
        }
      }

      loadUrl(initialUrl)
    }
  }
}
