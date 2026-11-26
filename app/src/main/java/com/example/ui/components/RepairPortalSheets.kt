package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ScreenLockLandscape
import androidx.compose.material.icons.filled.ScreenLockPortrait
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.OrientationMode
import com.example.model.RepairEndpoint
import com.example.model.ViewportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceBottomSheet(
  allowMultiple: Boolean,
  onTakeCameraPhoto: () -> Unit,
  onPickFromGallery: () -> Unit,
  onPickDocument: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("photo_source_bottom_sheet"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.CameraAlt,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.dialog_photo_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.dialog_photo_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      PhotoOptionCard(
        icon = Icons.Filled.CameraAlt,
        title = stringResource(R.string.photo_source_camera),
        subtitle = stringResource(R.string.photo_source_camera_desc),
        accentColor = MaterialTheme.colorScheme.primary,
        testTag = "btn_photo_source_camera",
        onClick = onTakeCameraPhoto,
      )

      PhotoOptionCard(
        icon = Icons.Filled.PhotoLibrary,
        title = if (allowMultiple) {
          "${stringResource(R.string.photo_source_gallery)} (Multi-Foto)"
        } else {
          stringResource(R.string.photo_source_gallery)
        },
        subtitle = stringResource(R.string.photo_source_gallery_desc),
        accentColor = MaterialTheme.colorScheme.secondary,
        testTag = "btn_photo_source_gallery",
        onClick = onPickFromGallery,
      )

      PhotoOptionCard(
        icon = Icons.Filled.Description,
        title = stringResource(R.string.photo_source_document),
        subtitle = stringResource(R.string.photo_source_document_desc),
        accentColor = MaterialTheme.colorScheme.tertiary,
        testTag = "btn_photo_source_document",
        onClick = onPickDocument,
      )

      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("btn_cancel_photo_sheet"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Text(text = stringResource(R.string.btn_cancel))
      }
    }
  }
}

@Composable
private fun PhotoOptionCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.16f),
        modifier = Modifier.size(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(24.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerAndControlsBottomSheet(
  activeEndpoint: RepairEndpoint,
  orientationMode: OrientationMode,
  viewportMode: ViewportMode,
  onSelectEndpoint: (RepairEndpoint) -> Unit,
  onSelectOrientation: (OrientationMode) -> Unit,
  onSelectViewport: (ViewportMode) -> Unit,
  onPrintNow: () -> Unit,
  onOpenExternalBrowser: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("server_controls_bottom_sheet"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Banner Card with generated workshop illustration
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(118.dp)
          .clip(RoundedCornerShape(18.dp)),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_repair_hero_1795680877129),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxWidth(),
        )
        Box(
          modifier = Modifier
            .matchParentSize()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xEE0F172A),
                  Color(0xBB0F172A),
                  Color(0x550F172A),
                ),
              ),
            )
            .padding(16.dp),
          contentAlignment = Alignment.BottomStart,
        ) {
          Column {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "OK REPAIR WORKSTATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(R.string.server_sheet_title),
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = stringResource(R.string.server_sheet_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFCBD5E1),
            )
          }
        }
      }

      // Section 1: Endpoint Selection
      Text(
        text = "Sumber Server Order Kerja",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      RepairEndpoint.entries.forEach { endpoint ->
        val isSelected = endpoint == activeEndpoint
        val borderColor = if (isSelected) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = borderColor,
              shape = RoundedCornerShape(14.dp),
            )
            .clickable { onSelectEndpoint(endpoint) }
            .testTag("endpoint_card_${endpoint.id}"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.CloudSync,
              contentDescription = endpoint.title,
              tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = endpoint.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Text(
                text = endpoint.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

      // Section 2: Orientation Lock / Auto
      Text(
        text = "Orientasi Layar (Portrait / Landscape)",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OrientationMode.entries.forEach { mode ->
          val selected = mode == orientationMode
          val icon = when (mode) {
            OrientationMode.AUTO_SENSOR -> Icons.Filled.ScreenRotation
            OrientationMode.PORTRAIT -> Icons.Filled.ScreenLockPortrait
            OrientationMode.LANDSCAPE -> Icons.Filled.ScreenLockLandscape
          }
          FilterChip(
            selected = selected,
            onClick = { onSelectOrientation(mode) },
            label = { Text(mode.label) },
            leadingIcon = {
              Icon(
                imageVector = icon,
                contentDescription = mode.label,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("chip_orientation_${mode.name.lowercase()}"),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
            ),
          )
        }
      }

      // Section 3: Viewport Responsive Mode
      Text(
        text = "Mode Responsivitas Web",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ViewportMode.entries.forEach { mode ->
          val selected = mode == viewportMode
          val icon = when (mode) {
            ViewportMode.MOBILE_RESPONSIVE -> Icons.Filled.PhoneAndroid
            ViewportMode.DESKTOP_WIDE -> Icons.Filled.DesktopWindows
          }
          FilterChip(
            selected = selected,
            onClick = { onSelectViewport(mode) },
            label = { Text(mode.label) },
            leadingIcon = {
              Icon(
                imageVector = icon,
                contentDescription = mode.label,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("chip_viewport_${mode.name.lowercase()}"),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
              selectedLeadingIconColor = MaterialTheme.colorScheme.secondary,
            ),
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      OutlinedButton(
        onClick = {
          onDismiss()
          onOpenExternalBrowser()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("sheet_btn_external_browser"),
        shape = RoundedCornerShape(12.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.OpenInNew,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.action_open_browser),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
