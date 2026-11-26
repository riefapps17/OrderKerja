package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.OrientationMode
import com.example.model.RepairEndpoint
import com.example.model.RepairUiState
import com.example.ui.components.PhotoSourceBottomSheet
import com.example.ui.components.ServerAndControlsBottomSheet
import com.example.viewmodel.RepairViewModel
import com.example.webview.RepairWebViewController
import com.example.webview.findActivity

@Composable
fun OkRepairPortalScreen(
  viewModel: RepairViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var pendingWebPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

  // Apply orientation preference to host Activity whenever changed
  LaunchedEffect(uiState.orientationMode) {
    val activity = context.findActivity() ?: return@LaunchedEffect
    activity.requestedOrientation = when (uiState.orientationMode) {
      OrientationMode.AUTO_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
      OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
      OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
    }
  }

  // Zero-permission Android Photo Picker (Single Image)
  val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.resolveFileChooserResult(arrayOf(uri))
    } else {
      viewModel.cancelFileChooser()
    }
  }

  // Zero-permission Android Photo Picker (Multiple Images)
  val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(),
  ) { uris: List<Uri> ->
    if (uris.isNotEmpty()) {
      viewModel.resolveFileChooserResult(uris.toTypedArray())
    } else {
      viewModel.cancelFileChooser()
    }
  }

  // Storage Access Framework Document Picker (Images & PDFs)
  val documentPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.resolveFileChooserResult(arrayOf(uri))
    } else {
      viewModel.cancelFileChooser()
    }
  }

  // Direct Camera Capture via FileProvider URI
  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture(),
  ) { success: Boolean ->
    val capturedUri = viewModel.pendingCameraOutputUri
    if (success && capturedUri != null) {
      viewModel.resolveFileChooserResult(arrayOf(capturedUri))
    } else {
      viewModel.cancelFileChooser()
    }
  }

  val launchCameraCapture = {
    val outputUri = RepairWebViewController.createTempCameraFileUri(context)
    if (outputUri != null) {
      viewModel.pendingCameraOutputUri = outputUri
      takePictureLauncher.launch(outputUri)
    } else {
      Toast.makeText(context, "Gagal menyiapkan penyimpanan foto kamera", Toast.LENGTH_SHORT).show()
      viewModel.cancelFileChooser()
    }
  }

  // Runtime CAMERA permission launcher for file input capture
  val cameraPermissionForFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted: Boolean ->
    if (isGranted) {
      launchCameraCapture()
    } else {
      Toast.makeText(
        context,
        context.getString(R.string.camera_permission_denied),
        Toast.LENGTH_LONG,
      ).show()
      // Fallback to Gallery Photo Picker so the user can still upload an image
      singlePhotoPickerLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
      )
    }
  }

  // Runtime CAMERA permission launcher for WebRTC / getUserMedia requests coming from the web page
  val cameraPermissionForWebRtcLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted: Boolean ->
    val req = pendingWebPermissionRequest
    pendingWebPermissionRequest = null
    if (isGranted) {
      req?.grant(req.resources)
    } else {
      req?.deny()
      Toast.makeText(
        context,
        context.getString(R.string.camera_permission_denied),
        Toast.LENGTH_SHORT,
      ).show()
    }
  }

  // Handle physical Back button for WebView history or full screen exit
  BackHandler(
    enabled = uiState.isImmersiveFullScreen || uiState.canGoBack,
  ) {
    when {
      uiState.isImmersiveFullScreen -> viewModel.toggleImmersiveFullScreen()
      webViewInstance?.canGoBack() == true -> webViewInstance?.goBack()
    }
  }

  // Clean up WebView when Composable leaves composition
  DisposableEffect(Unit) {
    onDispose {
      webViewInstance?.stopLoading()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      AnimatedVisibility(
        visible = !uiState.isImmersiveFullScreen,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
      ) {
        OkRepairTopHeaderBar(
          uiState = uiState,
          isLandscape = isLandscape,
          onOpenServerSheet = { viewModel.setShowServerControlSheet(true) },
          onQuickSwitchServer = {
            val nextEndpoint = if (uiState.activeEndpoint == RepairEndpoint.PRIMARY_PORTAL) {
              RepairEndpoint.DIRECT_APPS_SCRIPT
            } else {
              RepairEndpoint.PRIMARY_PORTAL
            }
            viewModel.switchEndpoint(nextEndpoint)
            webViewInstance?.loadUrl(nextEndpoint.url)
          },
          onCycleOrientation = {
            val nextMode = viewModel.cycleOrientationMode()
            Toast.makeText(
              context,
              "Mode Layar: ${nextMode.label}",
              Toast.LENGTH_SHORT,
            ).show()
          },
          onReloadClick = {
            viewModel.clearError()
            webViewInstance?.reload()
          },
          onFullScreenToggle = { viewModel.toggleImmersiveFullScreen() },
        )
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
    ) {
      // Main WebView Container (Full Screen without Bottom Navigation Bar)
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("ok_repair_webview"),
        factory = { ctx ->
          RepairWebViewController.createConfiguredWebView(
            context = ctx,
            initialUrl = uiState.activeEndpoint.url,
            onPageStarted = { url -> viewModel.onPageLoadStarted(url) },
            onProgressChanged = { progress -> viewModel.onProgressChanged(progress) },
            onPageFinished = { url, title, canBack, canFwd ->
              viewModel.onPageLoadFinished(url, title, canBack, canFwd)
            },
            onNavigationUpdated = { canBack, canFwd ->
              viewModel.onNavigationHistoryUpdated(canBack, canFwd)
            },
            onError = { message -> viewModel.onPageLoadError(message) },
            onFileChooserRequested = { callback, request ->
              // Automatically detected <input type="file"> from web app
              if (request.captureEnabled) {
                // If web input explicitly sets capture="camera" / capture="environment", launch camera directly
                viewModel.onShowFileChooser(callback, request.copy(captureEnabled = true))
                val hasCameraPerm = ContextCompat.checkSelfPermission(
                  ctx,
                  Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPerm) {
                  launchCameraCapture()
                } else {
                  cameraPermissionForFileLauncher.launch(Manifest.permission.CAMERA)
                }
              } else {
                // Show Camera / Gallery / File selection sheet automatically
                viewModel.onShowFileChooser(callback, request)
              }
            },
            onPrintRequested = { targetWebView ->
              // Automatically detected window.print() from web app
              RepairWebViewController.triggerAndroidPrintJob(
                context = ctx,
                webView = targetWebView,
                pageTitle = uiState.pageTitle,
                onPrintStarted = { jobName ->
                  viewModel.onPrintTriggered(jobName)
                },
              )
            },
            onWebPermissionRequest = { permissionRequest ->
              // Automatically detected WebRTC camera/media permission request from web app
              val needsCamera = permissionRequest.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
              if (needsCamera) {
                val hasCameraPerm = ContextCompat.checkSelfPermission(
                  ctx,
                  Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasCameraPerm) {
                  permissionRequest.grant(permissionRequest.resources)
                } else {
                  pendingWebPermissionRequest = permissionRequest
                  cameraPermissionForWebRtcLauncher.launch(Manifest.permission.CAMERA)
                }
              } else {
                permissionRequest.grant(permissionRequest.resources)
              }
            },
            onEnsureCameraPermission = {
              val hasCameraPerm = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.CAMERA,
              ) == PackageManager.PERMISSION_GRANTED
              if (!hasCameraPerm) {
                cameraPermissionForWebRtcLauncher.launch(Manifest.permission.CAMERA)
              }
            },
          ).also { createdWebView ->
            webViewInstance = createdWebView
          }
        },
        update = {
          // WebView state is managed imperatively to avoid unnecessary reloads during recomposition
        },
      )

      // Top smooth linear loading indicator
      AnimatedVisibility(
        visible = uiState.isLoading,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopCenter),
      ) {
        LinearProgressIndicator(
          progress = { (uiState.loadingProgress.coerceIn(5, 100)) / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .testTag("webview_progress_bar"),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
      }

      // Initial Loading Banner Overlay
      AnimatedVisibility(
        visible = uiState.isLoading && uiState.loadingProgress < 45 && !uiState.hasError,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
          tonalElevation = 6.dp,
          shadowElevation = 4.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = "${stringResource(R.string.status_loading)} (${uiState.loadingProgress}%)",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      // Error / Connection Fallback Card
      AnimatedVisibility(
        visible = uiState.hasError,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
          .align(Alignment.Center)
          .padding(20.dp),
      ) {
        ConnectionErrorFallbackCard(
          uiState = uiState,
          onRetry = {
            viewModel.clearError()
            webViewInstance?.reload()
          },
          onSwitchEndpoint = {
            val alternate = if (uiState.activeEndpoint == RepairEndpoint.PRIMARY_PORTAL) {
              RepairEndpoint.DIRECT_APPS_SCRIPT
            } else {
              RepairEndpoint.PRIMARY_PORTAL
            }
            viewModel.switchEndpoint(alternate)
            webViewInstance?.loadUrl(alternate.url)
          },
        )
      }

      // Floating button only when in Immersive FullScreen mode to show top header again
      if (uiState.isImmersiveFullScreen) {
        FloatingActionButton(
          onClick = { viewModel.toggleImmersiveFullScreen() },
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
          contentColor = MaterialTheme.colorScheme.primary,
          elevation = FloatingActionButtonDefaults.elevation(4.dp),
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
            .testTag("fab_exit_fullscreen"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.FullscreenExit,
              contentDescription = stringResource(R.string.action_exit_fullscreen),
            )
            Text(
              text = "Panel",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }

  // Automatically triggered Photo & Document Picker Modal Bottom Sheet when web requests file/photo input
  if (uiState.showPhotoSourceSheet && uiState.activeFileRequest?.captureEnabled != true) {
    val allowMultiple = uiState.activeFileRequest?.allowMultiple ?: false
    PhotoSourceBottomSheet(
      allowMultiple = allowMultiple,
      onTakeCameraPhoto = {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPerm) {
          launchCameraCapture()
        } else {
          cameraPermissionForFileLauncher.launch(Manifest.permission.CAMERA)
        }
      },
      onPickFromGallery = {
        if (allowMultiple) {
          multiPhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
          )
        } else {
          singlePhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
          )
        }
      },
      onPickDocument = {
        documentPickerLauncher.launch(arrayOf("image/*", "application/pdf"))
      },
      onDismiss = {
        viewModel.cancelFileChooser()
      },
    )
  }

  // Server & Responsive Controls Modal Bottom Sheet
  if (uiState.showServerControlSheet) {
    ServerAndControlsBottomSheet(
      activeEndpoint = uiState.activeEndpoint,
      orientationMode = uiState.orientationMode,
      viewportMode = uiState.viewportMode,
      onSelectEndpoint = { endpoint ->
        viewModel.switchEndpoint(endpoint)
        webViewInstance?.loadUrl(endpoint.url)
      },
      onSelectOrientation = { mode ->
        viewModel.setOrientationMode(mode)
      },
      onSelectViewport = { mode ->
        viewModel.setViewportMode(mode)
        webViewInstance?.let { RepairWebViewController.applyViewportMode(it, mode) }
      },
      onPrintNow = {},
      onOpenExternalBrowser = {
        try {
          val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uiState.currentUrl))
          context.startActivity(browserIntent)
        } catch (e: Exception) {
          Toast.makeText(context, "Tidak dapat membuka browser eksternal", Toast.LENGTH_SHORT).show()
        }
      },
      onDismiss = {
        viewModel.setShowServerControlSheet(false)
      },
    )
  }
}

@Composable
private fun OkRepairTopHeaderBar(
  uiState: RepairUiState,
  isLandscape: Boolean,
  onOpenServerSheet: () -> Unit,
  onQuickSwitchServer: () -> Unit,
  onCycleOrientation: () -> Unit,
  onReloadClick: () -> Unit,
  onFullScreenToggle: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.statusBars)
      .testTag("top_header_bar"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = if (isLandscape) 6.dp else 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Brand Icon + Active Server Pill
      Row(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(12.dp))
          .clickable(onClick = onOpenServerSheet)
          .padding(vertical = 4.dp, horizontal = 4.dp)
          .testTag("header_brand_server_selector"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(38.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.Build,
              contentDescription = stringResource(R.string.app_name),
              tint = Color.White,
              modifier = Modifier.size(20.dp),
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (uiState.activeEndpoint == RepairEndpoint.PRIMARY_PORTAL) {
                MaterialTheme.colorScheme.primaryContainer
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
            ) {
              Text(
                text = uiState.activeEndpoint.badgeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.activeEndpoint == RepairEndpoint.PRIMARY_PORTAL) {
                  MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                  MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                maxLines = 1,
              )
            }
          }
          Text(
            text = uiState.activeEndpoint.shortHost,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Quick Switch URL Button (Portal Utama <-> URL Asli)
      IconButton(
        onClick = onQuickSwitchServer,
        modifier = Modifier
          .size(48.dp)
          .testTag("btn_quick_switch_url"),
      ) {
        Icon(
          imageVector = Icons.Filled.SwapHoriz,
          contentDescription = stringResource(R.string.action_switch_server),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }

      // Orientation Lock / Toggle Button
      val orientationIcon = when (uiState.orientationMode) {
        OrientationMode.AUTO_SENSOR -> Icons.Filled.ScreenRotation
        OrientationMode.PORTRAIT -> Icons.Filled.ScreenLockPortrait
        OrientationMode.LANDSCAPE -> Icons.Filled.ScreenLockLandscape
      }
      IconButton(
        onClick = onCycleOrientation,
        modifier = Modifier
          .size(48.dp)
          .testTag("btn_header_orientation"),
      ) {
        Icon(
          imageVector = orientationIcon,
          contentDescription = stringResource(R.string.action_rotate),
          tint = if (uiState.orientationMode == OrientationMode.AUTO_SENSOR) {
            MaterialTheme.colorScheme.onSurfaceVariant
          } else {
            MaterialTheme.colorScheme.primary
          },
        )
      }

      // Reload Page Button
      IconButton(
        onClick = onReloadClick,
        modifier = Modifier
          .size(48.dp)
          .testTag("btn_header_reload"),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = stringResource(R.string.action_reload),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Fullscreen Mode Button
      IconButton(
        onClick = onFullScreenToggle,
        modifier = Modifier
          .size(48.dp)
          .testTag("btn_header_fullscreen"),
      ) {
        Icon(
          imageVector = Icons.Filled.Fullscreen,
          contentDescription = stringResource(R.string.action_fullscreen),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ConnectionErrorFallbackCard(
  uiState: RepairUiState,
  onRetry: () -> Unit,
  onSwitchEndpoint: () -> Unit,
) {
  Card(
    modifier = Modifier
      .widthIn(max = 460.dp)
      .fillMaxWidth()
      .testTag("connection_error_card"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(110.dp)
          .clip(RoundedCornerShape(14.dp)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_repair_hero_1795680877129),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xAA0F172A)),
          contentAlignment = Alignment.Center,
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            modifier = Modifier.size(56.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
              )
            }
          }
        }
      }

      Text(
        text = stringResource(R.string.status_error_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Text(
        text = uiState.errorMessage ?: stringResource(R.string.status_error_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Text(
        text = stringResource(R.string.status_error_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onRetry,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_error_retry"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.btn_retry))
      }

      OutlinedButton(
        onClick = onSwitchEndpoint,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_error_switch_endpoint"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Dns,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (uiState.activeEndpoint == RepairEndpoint.PRIMARY_PORTAL) {
            stringResource(R.string.btn_switch_to_direct)
          } else {
            stringResource(R.string.btn_switch_to_primary)
          },
        )
      }
    }
  }
}
