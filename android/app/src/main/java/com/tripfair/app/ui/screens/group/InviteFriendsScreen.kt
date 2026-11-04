package com.tripfair.app.ui.screens.group

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tripfair.app.R
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import kotlinx.coroutines.launch

/** Copy, share and send actions confirm on this screen (Figma: "xác nhận tại chỗ"). */
@Composable
fun InviteFriendsScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    val copiedMessage = stringResource(R.string.invite_copied)
    val sentMessage = stringResource(R.string.invite_sent)
    val shareText = stringResource(R.string.invite_share_text, SampleGroup.NAME, SampleGroup.INVITE_LINK)

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
            GroupInfoCard()
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
                    text = SampleGroup.CODE,
                    style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = 0.sp),
                    color = colors.primary,
                )
                PrimaryButton(
                    text = stringResource(R.string.invite_copy_code),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(SampleGroup.NAME, SampleGroup.CODE)))
                            snackbar.showSnackbar(copiedMessage)
                        }
                    },
                    height = 44.dp,
                    containerColor = colors.primaryContainer,
                    contentColor = colors.primary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CapsLabel(text = stringResource(R.string.invite_link_label))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(colors.surfaceContainerLow, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_link), contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Text(text = SampleGroup.INVITE_LINK, style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
                PrimaryButton(
                    text = stringResource(R.string.invite_share_link),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(send, null))
                    },
                    height = 44.dp,
                )
            }
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
                        phone = ""
                        email = ""
                        scope.launch { snackbar.showSnackbar(sentMessage) }
                    },
                    height = 52.dp,
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
        InviteFriendsScreen(onBack = {})
    }
}
