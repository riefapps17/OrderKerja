package com.example.model

enum class RepairEndpoint(
  val id: String,
  val title: String,
  val shortHost: String,
  val url: String,
  val badgeLabel: String,
) {
  PRIMARY_PORTAL(
    id = "primary",
    title = "Portal Utama (Okegas)",
    shortHost = "mahar.okegas.app/orderkerja",
    url = "https://mahar.okegas.app/orderkerja",
    badgeLabel = "PORTAL UTAMA",
  ),
  DIRECT_APPS_SCRIPT(
    id = "direct_gas",
    title = "Server Asli (Google Apps Script)",
    shortHost = "script.google.com/macros/s/…/exec",
    url = "https://script.google.com/macros/s/AKfycbyjQ24dEwnsw3WtF5A5CKM3xhYK7eZ4w4QzIRc96T32R10Rge2QwpTFZfcMzx5TWOk0/exec",
    badgeLabel = "URL ASLI (DIRECT)",
  ),
}

enum class OrientationMode(
  val label: String,
) {
  AUTO_SENSOR("Otomatis"),
  PORTRAIT("Portrait"),
  LANDSCAPE("Landscape"),
}

enum class ViewportMode(
  val label: String,
) {
  MOBILE_RESPONSIVE("Responsif Mobile"),
  DESKTOP_WIDE("Desktop Lebar"),
}

data class FileChooserRequest(
  val acceptTypes: List<String> = emptyList(),
  val allowMultiple: Boolean = false,
  val captureEnabled: Boolean = false,
)

data class RepairUiState(
  val activeEndpoint: RepairEndpoint = RepairEndpoint.PRIMARY_PORTAL,
  val currentUrl: String = RepairEndpoint.PRIMARY_PORTAL.url,
  val pageTitle: String = "OK Repair - Order Kerja",
  val isLoading: Boolean = true,
  val loadingProgress: Int = 0,
  val canGoBack: Boolean = false,
  val canGoForward: Boolean = false,
  val hasError: Boolean = false,
  val errorMessage: String? = null,
  val orientationMode: OrientationMode = OrientationMode.AUTO_SENSOR,
  val viewportMode: ViewportMode = ViewportMode.MOBILE_RESPONSIVE,
  val isImmersiveFullScreen: Boolean = false,
  val showPhotoSourceSheet: Boolean = false,
  val activeFileRequest: FileChooserRequest? = null,
  val showServerControlSheet: Boolean = false,
  val lastPrintStatus: String? = null,
)
