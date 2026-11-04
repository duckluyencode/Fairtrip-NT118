package com.tripfair.app.ui.screens.sos

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tripfair.app.R
import com.tripfair.app.ui.components.LabeledTextField
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairChip
import com.tripfair.app.ui.components.TripFairSwitch
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.EmergencyContact
import com.tripfair.app.ui.sample.SampleSos
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import java.util.UUID

/**
 * "Thêm liên hệ khẩn cấp" (or edit when [contact] is given). "Chọn từ danh bạ" opens the
 * system contact picker, which grants access to the picked number only, so no permission is needed.
 */
@Composable
fun ContactFormScreen(
    contact: EmergencyContact?,
    onBack: () -> Unit,
    onSave: (EmergencyContact) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(contact?.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(contact?.phone.orEmpty()) }
    var relation by rememberSaveable { mutableStateOf(contact?.relation ?: SampleSos.relations.first()) }
    var receivesSos by rememberSaveable { mutableStateOf(contact?.receivesSos ?: true) }

    val pickContact = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) return@rememberLauncherForActivityResult
        val columns = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0).orEmpty()
                phone = cursor.getString(1).orEmpty()
            }
        }
    }

    Scaffold(
        topBar = {
            TripFairTopBar(
                title = stringResource(if (contact == null) R.string.contacts_add else R.string.contacts_edit),
                onBack = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlineButton(
                    text = stringResource(R.string.contact_form_pick),
                    onClick = {
                        pickContact.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                    },
                )
                PrimaryButton(
                    text = stringResource(R.string.contact_form_save),
                    onClick = {
                        onSave(
                            EmergencyContact(
                                id = contact?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                relation = relation,
                                phone = phone.trim(),
                                receivesSos = receivesSos,
                            ),
                        )
                    },
                    height = 52.dp,
                    enabled = name.isNotBlank() && phone.isNotBlank(),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = stringResource(R.string.contact_form_section), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            LabeledTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.contact_form_name),
                leadingIcon = R.drawable.ic_user_round,
                placeholder = stringResource(R.string.contact_form_name_hint),
            )
            LabeledTextField(
                value = phone,
                onValueChange = { phone = it },
                label = stringResource(R.string.contact_form_phone),
                leadingIcon = R.drawable.ic_phone,
                placeholder = stringResource(R.string.contact_form_phone_hint),
                keyboardType = KeyboardType.Phone,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.contact_form_relation), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SampleSos.relations.forEach { option ->
                        TripFairChip(text = option, selected = option == relation, onClick = { relation = option })
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = stringResource(R.string.contact_form_receives), style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = stringResource(R.string.contact_form_receives_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                TripFairSwitch(checked = receivesSos, onCheckedChange = { receivesSos = it }, large = true)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_circle_help), contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.contact_form_note_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                    Text(
                        text = stringResource(R.string.contact_form_note_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun ContactFormScreenPreview() {
    TripFairTheme {
        ContactFormScreen(contact = null, onBack = {}, onSave = {})
    }
}
