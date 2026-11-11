package com.tripfair.app.ui.screens.group

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tripfair.app.R
import com.tripfair.app.data.trips.TripDetailDto
import com.tripfair.app.ui.AppViewModelProvider
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleTripPlan
import com.tripfair.app.ui.screens.trip.TripLoadScaffold
import com.tripfair.app.ui.screens.trip.tripDateRange
import com.tripfair.app.ui.screens.trip.tripLength
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun InviteFriendsRoute(
    tripId: Int,
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(tripId) { viewModel.start(tripId) }
    val trip = viewModel.trip
    if (trip == null) {
        TripLoadScaffold(
            title = stringResource(R.string.groups_invite),
            loading = viewModel.loading,
            error = viewModel.error,
            onRetry = viewModel::load,
            onBack = onBack,
        )
    } else {
        InviteFriendsScreen(trip = trip, onBack = onBack)
    }
}

/**
 * "Mời bạn": the group's invite code, to copy, share, or send by SMS or email through the
 * phone's own apps (TripFair itself sends nothing).
 */
@Composable
fun InviteFriendsScreen(trip: TripDetailDto, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    val copiedMessage = stringResource(R.string.invite_copied)
    val noAppMessage = stringResource(R.string.invite_no_app)
    val name = trip.name ?: trip.city
    val code = trip.inviteCode.orEmpty()
    val shareText = stringResource(R.string.invite_share_code_text, name, trip.city, code)
    val detail = stringResource(
        R.string.invite_group_detail,
        tripDateRange(LocalDate.parse(trip.startDate), LocalDate.parse(trip.endDate)),
        tripLength(trip.days),
        trip.memberCount,
    )
    // Opens another app with the invite text ready; the user still presses send there.
    val open: (Intent) -> Unit = { intent ->
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(noAppMessage) }
        }
    }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.groups_invite), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GroupInfoCard(name = name, detail = detail)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CapsLabel(text = stringResource(R.string.join_group_code_label))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 0.sp),
                    color = colors.primary,
                )
                PrimaryButton(
                    text = stringResource(R.string.invite_copy_code),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(name, code)))
                            snackbar.showSnackbar(copiedMessage)
                        }
                    },
                    height = 44.dp,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.primary,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.invite_share_code),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    open(Intent.createChooser(send, null))
                },
                height = 44.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = stringResource(R.string.invite_contact_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
                    )
                    Text(text = stringResource(R.string.invite_contact_intro), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                ContactField(
                    label = stringResource(R.string.invite_phone_label),
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = stringResource(R.string.invite_phone_placeholder),
                    icon = R.drawable.ic_phone,
                    keyboardType = KeyboardType.Phone,
                )
                ContactField(
                    label = stringResource(R.string.invite_email_label),
                    value = email,
                    onValueChange = { email = it },
                    placeholder = stringResource(R.string.invite_email_placeholder),
                    icon = R.drawable.ic_mail,
                    keyboardType = KeyboardType.Email,
                )
                PrimaryButton(
                    text = stringResource(R.string.invite_send),
                    onClick = {
                        if (phone.isNotBlank()) {
                            open(Intent(Intent.ACTION_SENDTO, "smsto:${phone.trim()}".toUri()).putExtra("sms_body", shareText))
                        } else {
                            val mail = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                                .putExtra(Intent.EXTRA_EMAIL, arrayOf(email.trim()))
                                .putExtra(Intent.EXTRA_SUBJECT, name)
                                .putExtra(Intent.EXTRA_TEXT, shareText)
                            open(mail)
                        }
                    },
                    height = 52.dp,
                    enabled = phone.isNotBlank() || email.isNotBlank(),
                )
                Text(text = stringResource(R.string.invite_footer), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ContactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes icon: Int,
    keyboardType: KeyboardType,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = colors.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                        .border(1.dp, colors.outline, MaterialTheme.shapes.small)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(painter = painterResource(icon), contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(text = placeholder, style = textStyle, color = colors.onSurfaceVariant)
                        inner()
                    }
                }
            },
        )
    }
}

@ThemePreviews
@Composable
private fun InviteFriendsScreenPreview() {
    TripFairTheme {
        InviteFriendsScreen(trip = SampleTripPlan.trip, onBack = {})
    }
}
