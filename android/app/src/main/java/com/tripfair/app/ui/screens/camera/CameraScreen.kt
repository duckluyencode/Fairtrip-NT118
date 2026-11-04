package com.tripfair.app.ui.screens.camera

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.tripfair.app.R
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleCamera
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

/** How long the sample analysis takes; the real app waits for Google Cloud Vision. */
private const val ANALYSIS_MILLIS = 2400

/**
 * "Camera nhận diện địa điểm". Until CameraX is added the viewfinder shows a sample photo.
 * The shutter and "Thư viện" both start "Đang phân tích ảnh", then [onResult] reports whether
 * a landmark was found (the shutter sample is recognized, the library sample is not).
 * [hasCameraPermission] = false shows the permission card ("Từ chối quyền camera" in Figma).
 */
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onResult: (recognized: Boolean) -> Unit,
    hasCameraPermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    initiallyAnalyzing: Boolean = false,
) {
    val camera = MaterialTheme.extendedColors
    // null while aiming; true/false while analyzing a photo that will / will not be recognized.
    var analyzing by rememberSaveable { mutableStateOf(if (initiallyAnalyzing) true else null) }
    val progress = remember { Animatable(0f) }
    val currentOnResult by rememberUpdatedState(onResult)

    DarkStatusBar()

    LaunchedEffect(analyzing) {
        val expected = analyzing ?: return@LaunchedEffect
        if (initiallyAnalyzing) {
            progress.snapTo(0.65f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(ANALYSIS_MILLIS, easing = LinearEasing))
        currentOnResult(expected)
        analyzing = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(camera.cameraBackground),
    ) {
        TripFairTopBar(title = stringResource(R.string.camera_title), onBack = onBack, contentColor = camera.onCamera) {
            TopBarTextAction(text = stringResource(R.string.camera_help), onClick = onHelp, color = camera.onCamera)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (hasCameraPermission) {
                Image(
                    painter = painterResource(R.drawable.sample_golden_bridge),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(camera.cameraBackground.copy(alpha = if (analyzing != null) 0.5f else 0.22f)),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Flash control needs CameraX; it shows the default "off" state.
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(camera.cameraOverlay, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_zap_off),
                            contentDescription = stringResource(R.string.cd_flash_off),
                            tint = camera.onCamera,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .background(camera.cameraOverlay, CircleShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(camera.cameraAccent, CircleShape),
                        )
                        Text(
                            text = stringResource(if (analyzing != null) R.string.camera_analyzing else R.string.camera_ready),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = camera.onCamera,
                        )
                    }
                }
                if (hasCameraPermission) {
                    Viewfinder(analyzing = analyzing != null, modifier = Modifier.weight(1f))
                } else {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CameraPermissionCard(onRequestPermission = onRequestPermission)
                    }
                }
                Text(
                    text = stringResource(R.string.camera_usage, SampleCamera.TRIP_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = camera.onCamera,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(148.dp)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (analyzing != null) {
                AnalysisProgress(progress = progress.value, onCancel = { analyzing = null })
            } else {
                CaptureControls(
                    canCapture = hasCameraPermission,
                    onCapture = { analyzing = true },
                    onLibrary = { analyzing = false },
                )
            }
        }
    }
}

/** The camera is dark in both modes, so the status bar icons turn light while it is shown. */
@Composable
private fun DarkStatusBar() {
    val view = LocalView.current
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }
}

/** White aiming frame with the alignment hint ("Khung ngắm"). */
@Composable
private fun Viewfinder(analyzing: Boolean, modifier: Modifier = Modifier) {
    val camera = MaterialTheme.extendedColors
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .fillMaxWidth()
            .border(2.dp, camera.onCamera, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(if (analyzing) R.string.camera_analyzing_hint else R.string.camera_frame_hint),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
            color = camera.onCamera,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(camera.cameraOverlay, CircleShape)
                .padding(horizontal = 13.dp, vertical = 8.dp),
        )
        if (!analyzing) {
            Row(
                modifier = Modifier
                    .background(camera.cameraOverlay, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_sun), contentDescription = null, tint = camera.onCamera, modifier = Modifier.size(15.dp))
                Text(text = stringResource(R.string.camera_steady), style = MaterialTheme.typography.labelSmall, color = camera.onCamera)
            }
        }
    }
}

/** Library thumbnail, shutter and camera switch ("Điều khiển chụp"). */
@Composable
private fun CaptureControls(canCapture: Boolean, onCapture: () -> Unit, onLibrary: () -> Unit) {
    val camera = MaterialTheme.extendedColors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(role = Role.Button, onClick = onLibrary),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.sample_golden_bridge_card),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, camera.onCamera, RoundedCornerShape(10.dp)),
            )
            Text(text = stringResource(R.string.camera_library), style = MaterialTheme.typography.labelSmall, color = camera.onCamera)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val shutterLabel = stringResource(R.string.cd_camera_shutter)
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .border(3.dp, camera.onCamera, CircleShape)
                    .clickable(enabled = canCapture, role = Role.Button, onClick = onCapture)
                    .semantics { contentDescription = shutterLabel },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(if (canCapture) camera.onCamera else camera.onCamera.copy(alpha = 0.3f), CircleShape),
                )
            }
        }
        // Switching cameras needs CameraX; as in Figma, the screen stays as it is.
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(camera.onCamera.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(R.drawable.ic_refresh_cw), contentDescription = null, tint = camera.onCamera, modifier = Modifier.size(21.dp))
            }
            Text(text = stringResource(R.string.camera_switch), style = MaterialTheme.typography.labelSmall, color = camera.onCamera)
        }
    }
}

/** Progress while the photo is analyzed, with "Hủy" ("Tiến trình phân tích"). */
@Composable
private fun AnalysisProgress(progress: Float, onCancel: () -> Unit) {
    val camera = MaterialTheme.extendedColors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(camera.onCamera.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(6.dp)
                    .background(camera.cameraAccent, CircleShape),
            )
        }
        Text(
            text = stringResource(R.string.camera_analyzing_time),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = camera.onCamera,
        )
        Text(
            text = stringResource(R.string.action_cancel),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = camera.cameraAccent,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Explains why the camera is needed, like the SOS location card. */
@Composable
private fun CameraPermissionCard(onRequestPermission: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, tint = colors.error, modifier = Modifier.size(36.dp))
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.camera_permission_text),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.camera_permission_action),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onRequestPermission)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun CameraScreenPreview() {
    TripFairTheme {
        CameraScreen(onBack = {}, onHelp = {}, onResult = {})
    }
}

@ThemePreviews
@Composable
private fun CameraAnalyzingPreview() {
    TripFairTheme {
        CameraScreen(onBack = {}, onHelp = {}, onResult = {}, initiallyAnalyzing = true)
    }
}

@ThemePreviews
@Composable
private fun CameraNoPermissionPreview() {
    TripFairTheme {
        CameraScreen(onBack = {}, onHelp = {}, onResult = {}, hasCameraPermission = false)
    }
}
