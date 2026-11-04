package com.tripfair.app.ui.screens.group

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.ListCard
import com.tripfair.app.ui.components.ListDivider
import com.tripfair.app.ui.components.PrimaryButton
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.sample.SampleGroup
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

@Composable
fun CreateGroupScreen(onBack: () -> Unit, onInvite: () -> Unit, onContinue: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by rememberSaveable { mutableStateOf(SampleGroup.NAME) }

    Scaffold(
        topBar = { TripFairTopBar(title = stringResource(R.string.group_create), onBack = onBack) },
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 8.dp)) {
                PrimaryButton(text = stringResource(R.string.create_group_continue), onClick = onContinue, height = 52.dp)
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
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GroupHeading(
                title = stringResource(R.string.create_group_heading),
                text = stringResource(R.string.create_group_intro),
                extra = SampleGroup.DESTINATION_LINE,
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                CapsLabel(text = stringResource(R.string.create_group_name_label))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
                                .border(1.dp, colors.outline, MaterialTheme.shapes.small)
                                .padding(horizontal = 15.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { inner() }
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.create_group_members),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp, letterSpacing = 0.sp),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.create_group_members_status),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.primary,
                    )
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primaryContainer)
                        .clickable(role = Role.Button, onClick = onInvite)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_plus), contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                    Text(
                        text = stringResource(R.string.create_group_invite),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                    )
                }
                ListCard {
                    SampleGroup.members.forEachIndexed { index, member ->
                        if (index > 0) ListDivider()
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MemberAvatar(index = index, size = 36.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (member.isYou) stringResource(R.string.member_you, member.name) else member.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = if (member.isYou) member.preferences else stringResource(R.string.member_waiting),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            MemberBadge(text = stringResource(if (member.isYou) R.string.member_leader else R.string.member_invited))
                        }
                    }
                }
            }
            GroupHint(text = stringResource(R.string.create_group_hint))
        }
    }
}

@ThemePreviews
@Composable
private fun CreateGroupScreenPreview() {
    TripFairTheme {
        CreateGroupScreen(onBack = {}, onInvite = {}, onContinue = {})
    }
}
