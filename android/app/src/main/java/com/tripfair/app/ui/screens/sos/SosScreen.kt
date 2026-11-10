package com.tripfair.app.ui.screens.sos

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import com.tripfair.app.R
import com.tripfair.app.data.location.LocationProvider
import com.tripfair.app.data.sos.SosMessenger
import com.tripfair.app.data.sos.SosOutcome
import com.tripfair.app.data.sos.mapsLink
import com.tripfair.app.data.places.LocationDto
import com.tripfair.app.ui.components.MapPin
import com.tripfair.app.ui.components.PlacesMap
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.data.contacts.EmergencyContact
import com.tripfair.app.ui.sample.SampleSos
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.util.Locale
import kotlin.math.abs
import com.tripfair.app.ui.theme.extendedColors
import kotlinx.coroutines.launch

private enum class SendState { Idle, Confirming }

/** How long the send button has to be held, to avoid sending by mistake. */
private const val HOLD_MILLIS = 3000

/**
 * Connects the SOS screen to the phone: reads the position once location is allowed, and
 * sends the text by SMS. Sending directly needs the SMS permission, asked at that moment;
 * when it is refused the SMS app opens with the recipients and the text filled in.
 */
@Composable
fun SosRoute(
    contacts: List<EmergencyContact>,
    userName: String,
    locationProvider: LocationProvider,
    onBack: () -> Unit,
    onContacts: () -> Unit,
) {
    val context = LocalContext.current
    val permission = rememberLocationPermission()
    var position by remember { mutableStateOf<LocationDto?>(null) }
    LaunchedEffect(permission.granted) {
        if (permission.granted) position = locationProvider.current()
    }

    val messenger = remember { SosMessenger(context) }
    var outcome by rememberSaveable { mutableStateOf<SosOutcome?>(null) }
    val numbers = contacts.filter { it.receivesSos }.map { it.phone }
    val place = position
    val message = if (place != null) {
        stringResource(R.string.sos_message, userName, mapsLink(place), coordinatesText(place))
    } else {
        stringResource(R.string.sos_message_no_location, userName)
    }
    val deliver: (Boolean) -> Unit = { direct ->
        outcome = if (direct && messenger.sendDirectly(numbers, message)) {
            SosOutcome.Sent
        } else {
            try {
                context.startActivity(messenger.smsAppIntent(numbers, message))
                SosOutcome.OpenedSmsApp
            } catch (_: ActivityNotFoundException) {
                SosOutcome.Failed
            }
        }
    }
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> deliver(granted) }

    SosScreen(
        contacts = contacts,
        hasLocationPermission = permission.granted,
        location = position,
        outcome = outcome,
        onRequestLocation = permission.request,
        onSend = { if (messenger.canSendDirectly()) deliver(true) else smsPermission.launch(Manifest.permission.SEND_SMS) },
        onCloseOutcome = { outcome = null },
        onBack = onBack,
        onContacts = onContacts,
    )
}

/**
 * "SOS khẩn cấp": current location, the SMS to the chosen contacts (confirmed by holding a
 * button for 3 seconds) and quick calls. Without location permission the map is replaced by
 * a card asking for it ("SOS · chưa có quyền vị trí"); calls still work.
 */
@Composable
fun SosScreen(
    contacts: List<EmergencyContact>,
    hasLocationPermission: Boolean,
    location: LocationDto?,
    outcome: SosOutcome?,
    onRequestLocation: () -> Unit,
    onSend: () -> Unit,
    onCloseOutcome: () -> Unit,
    onBack: () -> Unit,
    onContacts: () -> Unit,
    initiallyConfirming: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var state by rememberSaveable { mutableStateOf(if (initiallyConfirming) SendState.Confirming else SendState.Idle) }
    val scroll = rememberScrollState()
    val recipients = contacts.filter { it.receivesSos }
    val recipientNames = joinNames(recipients.map { it.name })

    // Opens the phone app with the number filled in; the user still taps call.
    val call: (String) -> Unit = { number ->
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${number.filter { it.isDigit() || it == '+' }}".toUri()))
        } catch (_: ActivityNotFoundException) {
            // No phone app (e.g. a tablet): nothing to open.
        }
    }

    // The confirmation and result cards sit under the quick calls, so bring them into view.
    LaunchedEffect(state, outcome) {
        if (state != SendState.Idle || outcome != null) scroll.animateScrollTo(scroll.maxValue)
    }

    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.sos_title), onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.sos_contacts_action), onClick = onContacts, color = colors.error)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            SafetyNote()
            if (hasLocationPermission) LocationCard(location) else LocationPermissionCard(onRequestLocation)
            PrimaryButton(
                text = stringResource(R.string.sos_send_sms),
                onClick = { state = SendState.Confirming },
                leadingIcon = R.drawable.ic_message_square_warning,
                height = 52.dp,
                containerColor = colors.error,
                contentColor = colors.onError,
                enabled = hasLocationPermission,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.sos_quick_call), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.sos_tap_to_choose), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SampleSos.hotlines.forEach { hotline ->
                    QuickCallTile(
                        title = hotline.number,
                        subtitle = stringResource(hotline.label),
                        onClick = { call(hotline.number) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(colors.errorContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painter = painterResource(hotline.icon), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                recipients.firstOrNull()?.let { contact ->
                    QuickCallTile(
                        title = contact.name,
                        subtitle = stringResource(R.string.sos_saved),
                        onClick = { call(contact.phone) },
                        modifier = Modifier.weight(1f),
                    ) {
                        ContactAvatar(contact = contact, index = 0, size = 38)
                    }
                }
            }
            when {
                outcome != null -> SentCard(outcome = outcome, recipientNames = recipientNames, onClose = onCloseOutcome)
                state == SendState.Confirming -> ConfirmCard(
                    recipientNames = recipientNames,
                    onCancel = { state = SendState.Idle },
                    onSent = {
                        state = SendState.Idle
                        onSend()
                    },
                )
            }
        }
    }
}

/** "Mẹ", "Mẹ và Minh Đức", "A, B và C". */
@Composable
internal fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.first()
    else -> stringResource(R.string.list_and, names.dropLast(1).joinToString(", "), names.last())
}

@Composable
private fun SafetyNote() {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(extended.errorSurface, MaterialTheme.shapes.small)
            .border(1.dp, extended.errorBorder, MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(colors.errorContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.ic_shield_alert), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
        }
        Text(text = stringResource(R.string.sos_warning), style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp))
    }
}

/** Map around the current position with its coordinates ("Vị trí hiện tại"). */
@Composable
private fun LocationCard(location: LocationDto?) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .clip(MaterialTheme.shapes.large),
    ) {
        PlacesMap(
            pins = listOfNotNull(location?.let { MapPin(id = "me", position = it, title = stringResource(R.string.sos_your_position)) }),
            modifier = Modifier.fillMaxSize(),
            showMyLocation = true,
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
                .background(colors.surfaceContainerLow.copy(alpha = 0.95f), MaterialTheme.shapes.small)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(painter = painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(if (location != null) R.string.sos_your_position else R.string.sos_locating),
                    style = MaterialTheme.typography.labelMedium,
                )
                location?.let {
                    Text(
                        text = coordinatesText(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "16.0544° N, 108.2469° E" */
internal fun coordinatesText(location: LocationDto): String = String.format(
    Locale.US,
    "%.4f° %s, %.4f° %s",
    abs(location.lat),
    if (location.lat >= 0) "N" else "S",
    abs(location.lng),
    if (location.lng >= 0) "E" else "W",
)

/** Shown instead of the map when location is not allowed ("Chưa có quyền vị trí"). */
@Composable
private fun LocationPermissionCard(onRequestLocation: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(18.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painter = painterResource(R.drawable.ic_map_pin_off), contentDescription = null, tint = colors.error, modifier = Modifier.size(36.dp))
        Text(
            text = stringResource(R.string.sos_permission_title),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.sos_permission_text),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.sos_permission_action),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onPrimary,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary)
                .clickable(role = Role.Button, onClick = onRequestLocation)
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/** 96dp tile of "Gọi nhanh"; [icon] is the round icon or the contact's avatar. */
@Composable
private fun QuickCallTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val label = stringResource(R.string.cd_call, title)
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(MaterialTheme.shapes.small)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outline, MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
    ) {
        icon()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** "Xác nhận gửi vị trí?" with "Hủy" and the hold-to-send button. */
@Composable
private fun ConfirmCard(recipientNames: String, onCancel: () -> Unit, onSent: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertCard(
        icon = R.drawable.ic_alert_triangle,
        iconTint = colors.error,
        iconBackground = colors.errorContainer,
        title = stringResource(R.string.sos_confirm_title),
        text = if (recipientNames.isEmpty()) {
            stringResource(R.string.sos_no_recipients)
        } else {
            stringResource(R.string.sos_confirm_to, recipientNames)
        },
    ) {
        Text(
            text = stringResource(R.string.sos_hold_hint),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
            color = colors.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(92.dp)
                    .height(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerLow)
                    .border(1.dp, colors.outline, MaterialTheme.shapes.small)
                    .clickable(role = Role.Button, onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            HoldToSendButton(enabled = recipientNames.isNotEmpty(), onSent = onSent, modifier = Modifier.weight(1f))
        }
    }
}

/** Red button that sends only after being held for 3 seconds; the fill shows the progress. */
@Composable
private fun HoldToSendButton(enabled: Boolean, onSent: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnSent by rememberUpdatedState(onSent)
    val label = stringResource(R.string.sos_hold_button)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(if (enabled) colors.error else colors.error.copy(alpha = 0.4f))
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        val hold = scope.launch {
                            progress.animateTo(1f, tween(HOLD_MILLIS, easing = LinearEasing))
                            currentOnSent()
                        }
                        tryAwaitRelease()
                        // Released early: cancel and empty the bar.
                        if (progress.value < 1f) {
                            hold.cancel()
                            scope.launch { progress.animateTo(0f, tween(200)) }
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(colors.onError.copy(alpha = 0.25f)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painter = painterResource(R.drawable.ic_fingerprint), contentDescription = null, tint = colors.onError, modifier = Modifier.size(17.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.onError)
        }
    }
}

/** Result shown in place after sending (not designed in Figma; follows the confirmation card). */
@Composable
private fun SentCard(outcome: SosOutcome, recipientNames: String, onClose: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val success = MaterialTheme.extendedColors.success
    val failed = outcome == SosOutcome.Failed
    AlertCard(
        icon = if (failed) R.drawable.ic_circle_alert else R.drawable.ic_check,
        iconTint = if (failed) colors.error else success,
        iconBackground = if (failed) colors.errorContainer else colors.primaryContainer,
        title = stringResource(
            when (outcome) {
                SosOutcome.Sent -> R.string.sos_sent_title
                SosOutcome.OpenedSmsApp -> R.string.sos_opened_title
                SosOutcome.Failed -> R.string.sos_failed_title
            },
        ),
        text = when (outcome) {
            SosOutcome.Sent -> stringResource(R.string.sos_sent_text, recipientNames)
            SosOutcome.OpenedSmsApp -> stringResource(R.string.sos_opened_text, recipientNames)
            SosOutcome.Failed -> stringResource(R.string.sos_failed_text)
        },
        action = { TopBarTextAction(text = stringResource(R.string.action_close), onClick = onClose) },
    )
}

/** White card with a round icon, a title and a line under it, plus optional content. */
@Composable
private fun AlertCard(
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    text: String,
    action: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, MaterialTheme.shapes.small, ambientColor = colors.scrim, spotColor = colors.scrim)
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.extendedColors.errorBorder, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                Text(text = text, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            action()
        }
        content()
    }
}

@ThemePreviews
@Composable
private fun SosScreenPreview() {
    TripFairTheme {
        SosScreen(
            contacts = SampleSos.contacts,
            hasLocationPermission = true,
            location = LocationDto(16.0544, 108.2469),
            outcome = null,
            onRequestLocation = {},
            onSend = {},
            onCloseOutcome = {},
            onBack = {},
            onContacts = {},
            initiallyConfirming = true,
        )
    }
}

@ThemePreviews
@Composable
private fun SosNoLocationPreview() {
    TripFairTheme {
        SosScreen(
            contacts = SampleSos.contacts,
            hasLocationPermission = false,
            location = null,
            outcome = null,
            onRequestLocation = {},
            onSend = {},
            onCloseOutcome = {},
            onBack = {},
            onContacts = {},
        )
    }
}
