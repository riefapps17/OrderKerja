package com.example.viewmodel

import android.net.Uri
import android.webkit.ValueCallback
import androidx.lifecycle.ViewModel
import com.example.model.FileChooserRequest
import com.example.model.OrientationMode
import com.example.model.RepairEndpoint
import com.example.model.RepairUiState
import com.example.model.ViewportMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RepairViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(RepairUiState())
  val uiState: StateFlow<RepairUiState> = _uiState.asStateFlow()

  private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
  var pendingCameraOutputUri: Uri? = null

  fun onPageLoadStarted(url: String?) {
    _uiState.update { state ->
      state.copy(
        isLoading = true,
        hasError = false,
        errorMessage = null,
        currentUrl = url ?: state.currentUrl,
      )
    }
  }

  fun onProgressChanged(progress: Int) {
    _uiState.update { state ->
      state.copy(
        loadingProgress = progress,
        isLoading = progress < 100,
      )
    }
  }

  fun onPageLoadFinished(
    url: String?,
    title: String?,
    canGoBack: Boolean,
    canGoForward: Boolean,
  ) {
    _uiState.update { state ->
      state.copy(
        isLoading = false,
        loadingProgress = 100,
        currentUrl = url ?: state.currentUrl,
        pageTitle = if (!title.isNullOrBlank() && !title.startsWith("http")) title else "OK Repair - Order Kerja",
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onNavigationHistoryUpdated(canGoBack: Boolean, canGoForward: Boolean) {
    _uiState.update { state ->
      state.copy(
        canGoBack = canGoBack,
        canGoForward = canGoForward,
      )
    }
  }

  fun onPageLoadError(message: String) {
    _uiState.update { state ->
      state.copy(
        isLoading = false,
        hasError = true,
        errorMessage = message,
      )
    }
  }

  fun clearError() {
    _uiState.update { state ->
      state.copy(
        hasError = false,
        errorMessage = null,
      )
    }
  }

  fun switchEndpoint(endpoint: RepairEndpoint) {
    _uiState.update { state ->
      state.copy(
        activeEndpoint = endpoint,
        currentUrl = endpoint.url,
        isLoading = true,
        loadingProgress = 10,
        hasError = false,
        errorMessage = null,
        showServerControlSheet = false,
      )
    }
  }

  fun setOrientationMode(mode: OrientationMode) {
    _uiState.update { state ->
      state.copy(orientationMode = mode)
    }
  }

  fun cycleOrientationMode(): OrientationMode {
    val nextMode = when (_uiState.value.orientationMode) {
      OrientationMode.AUTO_SENSOR -> OrientationMode.LANDSCAPE
      OrientationMode.LANDSCAPE -> OrientationMode.PORTRAIT
      OrientationMode.PORTRAIT -> OrientationMode.AUTO_SENSOR
    }
    setOrientationMode(nextMode)
    return nextMode
  }

  fun setViewportMode(mode: ViewportMode) {
    _uiState.update { state ->
      state.copy(viewportMode = mode)
    }
  }

  fun toggleImmersiveFullScreen() {
    _uiState.update { state ->
      state.copy(isImmersiveFullScreen = !state.isImmersiveFullScreen)
    }
  }

  fun setShowServerControlSheet(show: Boolean) {
    _uiState.update { state ->
      state.copy(showServerControlSheet = show)
    }
  }

  fun onShowFileChooser(
    callback: ValueCallback<Array<Uri>>,
    request: FileChooserRequest,
  ) {
    // Cancel any existing unresolved file callback to prevent WebView freeze
    pendingFileCallback?.onReceiveValue(null)
    pendingFileCallback = callback
    _uiState.update { state ->
      state.copy(
        showPhotoSourceSheet = true,
        activeFileRequest = request,
      )
    }
  }

  fun openManualPhotoHelper() {
    _uiState.update { state ->
      state.copy(
        showPhotoSourceSheet = true,
        activeFileRequest = state.activeFileRequest ?: FileChooserRequest(
          acceptTypes = listOf("image/*"),
          allowMultiple = false,
          captureEnabled = false,
        ),
      )
    }
  }

  fun resolveFileChooserResult(uris: Array<Uri>?) {
    val callback = pendingFileCallback
    pendingFileCallback = null
    pendingCameraOutputUri = null
    _uiState.update { state ->
      state.copy(
        showPhotoSourceSheet = false,
        activeFileRequest = null,
      )
    }
    callback?.onReceiveValue(uris)
  }

  fun cancelFileChooser() {
    resolveFileChooserResult(null)
  }

  fun hasPendingWebFileCallback(): Boolean = pendingFileCallback != null

  fun onPrintTriggered(jobName: String) {
    _uiState.update { state ->
      state.copy(lastPrintStatus = "Dokumen siap dicetak: $jobName")
    }
  }

  fun clearPrintStatus() {
    _uiState.update { state ->
      state.copy(lastPrintStatus = null)
    }
  }

  override fun onCleared() {
    super.onCleared()
    pendingFileCallback?.onReceiveValue(null)
    pendingFileCallback = null
  }
}
