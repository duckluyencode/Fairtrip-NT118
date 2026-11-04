package com.tripfair.app.ui.screens.profile

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.tripfair.app.ui.components.ListRow
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

private val SupportRowPadding = PaddingValues(vertical = 6.dp)

// Topic pages, the other FAQ answers and the contact actions are not designed yet,
// so those rows do nothing for now.
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    var faqExpanded by rememberSaveable { mutableStateOf(true) }
    val sectionStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp)

    Scaffold(topBar = { TripFairTopBar(title = stringResource(R.string.support_title), onBack = onBack) }) { innerPadding ->
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.support_intro),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
                )
                SupportSearch(query = query, onQueryChange = { query = it })
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.support_topics), style = sectionStyle)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TopicCard(
                        icon = R.drawable.ic_users,
                        title = stringResource(R.string.support_topic_group),
                        subtitle = stringResource(R.string.support_topic_group_detail),
                        modifier = Modifier.weight(1f),
                    )
                    TopicCard(
                        icon = R.drawable.ic_wallet,
                        title = stringResource(R.string.support_topic_price),
                        subtitle = stringResource(R.string.support_topic_price_detail),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.support_faq), style = sectionStyle)
                ListCard(contentPadding = PaddingValues(14.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { faqExpanded = !faqExpanded }
                            .padding(bottom = if (faqExpanded) 16.dp else 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.support_faq_group_q),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painter = painterResource(if (faqExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down),
                                contentDescription = stringResource(R.string.cd_expand),
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        if (faqExpanded) {
                            Text(
                                text = stringResource(R.string.support_faq_group_a),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                    ListDivider()
                    ListRow(icon = R.drawable.ic_vote, title = stringResource(R.string.support_faq_vote_q), contentPadding = SupportRowPadding)
                    ListDivider()
                    ListRow(icon = R.drawable.ic_receipt, title = stringResource(R.string.support_faq_price_q), contentPadding = SupportRowPadding)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(R.string.support_contact), style = sectionStyle)
                ListCard(contentPadding = PaddingValues(14.dp)) {
                    ListRow(
                        icon = R.drawable.ic_message_circle,
                        title = stringResource(R.string.support_email),
                        subtitle = stringResource(R.string.support_email_detail),
                        contentPadding = SupportRowPadding,
                    )
                    ListDivider()
                    ListRow(
                        icon = R.drawable.ic_flag,
                        title = stringResource(R.string.support_feedback),
                        subtitle = stringResource(R.string.support_feedback_detail),
                        contentPadding = SupportRowPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportSearch(query: String, onQueryChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        decorationBox = { innerTextField ->
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
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(text = stringResource(R.string.support_search_placeholder), style = textStyle, color = colors.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun TopicCard(@DrawableRes icon: Int, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@ThemePreviews
@Composable
private fun SupportScreenPreview() {
    TripFairTheme {
        SupportScreen(onBack = {})
    }
}
