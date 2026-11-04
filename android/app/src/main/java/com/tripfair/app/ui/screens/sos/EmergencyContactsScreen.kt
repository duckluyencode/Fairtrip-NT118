package com.tripfair.app.ui.screens.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.OutlineButton
import com.tripfair.app.ui.components.TopBarTextAction
import com.tripfair.app.ui.components.TripFairCheckbox
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.EmergencyContact
import com.tripfair.app.ui.sample.SampleSos
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

/**
 * "Liên hệ khẩn cấp": the people who can receive the SOS text. Ticking a card decides who
 * receives it; the pencil opens the contact form.
 */
@Composable
fun EmergencyContactsScreen(
    contacts: List<EmergencyContact>,
    onToggle: (id: String) -> Unit,
    onSelectAll: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (id: String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val receivers = contacts.filter { it.receivesSos }
    Scaffold(
        topBar = {
            TripFairTopBar(title = stringResource(R.string.emergency_contacts_title), onBack = onBack) {
                TopBarTextAction(text = stringResource(R.string.action_save), onClick = onSave)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, MaterialTheme.shapes.small)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(colors.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painter = painterResource(R.drawable.ic_users_round), contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.contacts_intro_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    Text(
                        text = stringResource(R.string.contacts_intro_text),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.contacts_section), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.contacts_count, contacts.size, contacts.size),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                contacts.forEachIndexed { index, contact ->
                    ContactCard(contact = contact, index = index, onToggle = { onToggle(contact.id) }, onEdit = { onEdit(contact.id) })
                }
            }
            OutlineButton(text = stringResource(R.string.contacts_add), onClick = onAdd, leadingIcon = R.drawable.ic_user_plus, subtle = true)
            ReceiversCard(receivers = receivers, onSelectAll = onSelectAll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.tertiaryContainer, MaterialTheme.shapes.extraSmall)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(painter = painterResource(R.drawable.ic_users), contentDescription = null, tint = colors.tertiary, modifier = Modifier.size(17.dp))
                Text(text = SampleSos.TRIP_NOTE, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** Contact card; the 2dp teal border and the checkbox mark someone who receives the SOS text. */
@Composable
private fun ContactCard(contact: EmergencyContact, index: Int, onToggle: () -> Unit, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val checkLabel = stringResource(R.string.cd_receives_sos, contact.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(shape)
            .background(colors.surfaceContainerLow)
            .border(if (contact.receivesSos) 2.dp else 1.dp, if (contact.receivesSos) colors.primary else colors.outline, shape)
            .toggleable(value = contact.receivesSos, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = checkLabel }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        TripFairCheckbox(checked = contact.receivesSos, modifier = Modifier.size(22.dp))
        ContactAvatar(contact = contact, index = index, size = 44)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = contact.name, style = MaterialTheme.typography.titleSmall)
            Text(text = contact.relation, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Text(text = contact.phone, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        }
        val editLabel = stringResource(R.string.cd_edit_contact, contact.name)
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.background)
                .clickable(role = Role.Button, onClickLabel = editLabel, onClick = onEdit)
                .semantics { contentDescription = editLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(R.drawable.ic_pencil), contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
        }
    }
}

/** "Sẽ nhận tin SOS": chips of the people who will receive the text. */
@Composable
private fun ReceiversCard(receivers: List<EmergencyContact>, onSelectAll: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_message_circle), contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.contacts_receivers),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
            )
            TopBarTextAction(text = stringResource(R.string.contacts_select_all), onClick = onSelectAll)
        }
        if (receivers.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                receivers.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .background(colors.primaryContainer, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                        )
                    }
                }
            }
        }
        Text(
            text = when (receivers.size) {
                0 -> stringResource(R.string.contacts_receivers_none)
                2 -> stringResource(R.string.contacts_receivers_note_two)
                else -> pluralStringResource(R.plurals.contacts_receivers_note, receivers.size, receivers.size)
            },
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
            color = colors.onSurfaceVariant,
        )
    }
}

/** Round avatar with initials; colors alternate coral and teal down the list, as in Figma. */
@Composable
internal fun ContactAvatar(contact: EmergencyContact, index: Int, size: Int) {
    val colors = MaterialTheme.colorScheme
    val coral = index % 2 == 0
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(if (coral) colors.tertiaryContainer else colors.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = contact.initials,
            style = MaterialTheme.typography.titleSmall,
            color = if (coral) colors.tertiary else colors.primary,
        )
    }
}

@ThemePreviews
@Composable
private fun EmergencyContactsScreenPreview() {
    TripFairTheme {
        EmergencyContactsScreen(
            contacts = SampleSos.contacts,
            onToggle = {},
            onSelectAll = {},
            onBack = {},
            onSave = {},
            onAdd = {},
            onEdit = {},
        )
    }
}
