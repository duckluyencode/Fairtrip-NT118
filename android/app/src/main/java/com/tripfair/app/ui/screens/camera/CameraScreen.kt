package com.tripfair.app.ui.screens.camera

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
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
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors
import java.io.File

/** How long the sample analysis takes; the real app waits for Google Cloud Vision. */
/** The progress ring fills slowly while waiting, and never claims to be done by itself. */
private const val ANALYSIS_MILLIS = 8000

/** Where the camera app saves the photo; shared through the FileProvider in the manifest. */
private fun newPhotoUri(context: Context): Uri {
    val file = File(context.cacheDir, "photos/capture.jpg").apply { parentFile?.mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}

/**
 * Takes the photo with the phone's own camera app, or picks one with the system photo
 * picker; neither needs a permission. The photo is then sent for recognition.
 */
@Composable
fun CameraRoute(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onResult: (RecognizedPhoto) -> Unit,
    viewModel: RecognitionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = photoUri
        if (saved && uri != null) viewModel.analyze(context, uri)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.analyze(context, uri)
    }
    LaunchedEffect(viewModel.result) {
        viewModel.result?.let {
            onResult(it)
            viewModel.onNavigated()
        }
    }
    CameraScreen(
        onBack = onBack,
        onHelp = onHelp,
        analyzing = viewModel.analyzing,
        error = viewModel.error,
        onCapture = {
            val uri = newPhotoUri(context)
            photoUri = uri
            try {
                takePicture.launch(uri)
            } catch (_: ActivityNotFoundException) {
                viewModel.onNoCamera()
            }
        },
        onLibrary = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onCancel = viewModel::cancel,
    )
}

/**
 * "Camera nhận diện địa điểm": the shutter opens the phone's camera app and "Thư viện" the
 * photo picker; while the photo is being recognized the progress ring shows.
 */
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    analyzing: Boolean,
    @StringRes error: Int?,
    onCapture: () -> Unit,
    onLibrary: () -> Unit,
    onCancel: () -> Unit,
) {
    val camera = MaterialTheme.extendedColors
    val progress = remember { Animatable(0f) }
    val inspecting = LocalInspectionMode.current

    DarkStatusBar()

    LaunchedEffect(analyzing) {
        if (!analyzing) return@LaunchedEffect
        if (inspecting) {
            progress.snapTo(0.65f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(0.92f, tween(ANALYSIS_MILLIS, easing = LinearOutSlowInEasing))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            text = stringResource(if (analyzing) R.string.camera_analyzing else R.string.camera_ready),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = camera.onCamera,
                        )
                    }
                }
                Viewfinder(analyzing = analyzing, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(error ?: R.string.camera_usage_plain),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) camera.cameraAccent else camera.onCamera,
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
            if (analyzing) {
                AnalysisProgress(progress = progress.value, onCancel = onCancel)
            } else {
                CaptureControls(canCapture = true, onCapture = onCapture, onLibrary = onLibrary)
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
        // The phone's camera app switches cameras itself; this keeps the shutter centred.
        Box(modifier = Modifier.size(46.dp))
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

@ThemePreviews
@Composable
private fun CameraScreenPreview() {
    TripFairTheme {
        CameraScreen(onBack = {}, onHelp = {}, analyzing = false, error = null, onCapture = {}, onLibrary = {}, onCancel = {})
    }
}

@ThemePreviews
@Composable
private fun CameraAnalyzingPreview() {
    TripFairTheme {
        CameraScreen(onBack = {}, onHelp = {}, analyzing = true, error = null, onCapture = {}, onLibrary = {}, onCancel = {})
    }
}
