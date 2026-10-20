package com.tripfair.app.ui.screens.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R
import com.tripfair.app.ui.components.TripFairTopBar
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme
import com.tripfair.app.ui.theme.extendedColors

/** Long-form document page used for the privacy policy and the terms of use. */
@Composable
fun LegalDocumentScreen(document: LegalDocument, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Scaffold(topBar = { TripFairTopBar(title = stringResource(document.title), onBack = onBack) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(colors.primaryContainer, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shield_check),
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = stringResource(document.badge), style = type.labelMedium, color = colors.primary)
                            Text(text = stringResource(R.string.legal_draft), style = type.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    Text(text = stringResource(document.intro), style = type.bodyLarge, color = colors.onSurfaceVariant)
                    Text(
                        text = stringResource(document.disclaimer),
                        style = type.bodySmall.copy(lineHeight = 18.sp),
                        color = colors.onSurfaceVariant,
                    )
                }
                TableOfContents(document)
            }
            document.sections.forEachIndexed { index, section ->
                Section(number = index + 1, section = section)
            }
        }
    }
}

@Composable
private fun TableOfContents(document: LegalDocument) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.small)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.legal_toc_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(R.plurals.legal_toc_count, document.sections.size, document.sections.size),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        document.sections.forEachIndexed { index, section ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = number(index + 1),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                    modifier = Modifier.width(20.dp),
                )
                Text(text = stringResource(section.title), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun Section(number: Int, section: LegalSection) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = number(number),
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 25.sp),
            )
        }
        val isWarning = section.warning != null
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isWarning) extended.errorSurface else colors.surfaceContainerLow, MaterialTheme.shapes.small)
                .then(if (isWarning) Modifier.border(1.dp, extended.errorBorder, MaterialTheme.shapes.small) else Modifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isWarning) 12.dp else 14.dp),
        ) {
            if (section.warning != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_circle_alert),
                        contentDescription = null,
                        tint = colors.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(section.warning),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onErrorContainer,
                    )
                }
            }
            section.blocks.forEachIndexed { index, block ->
                when (block) {
                    is LegalBlock.Paragraph -> Paragraph(block)
                    is LegalBlock.Note -> Text(
                        text = stringResource(block.text),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = colors.onSurfaceVariant,
                    )
                    is LegalBlock.Permission -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (index > 0) HorizontalDivider(thickness = 1.dp, color = colors.outline)
                        Text(
                            text = stringResource(block.title),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.primary,
                        )
                        Text(
                            text = stringResource(block.body),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Paragraph(block: LegalBlock.Paragraph) {
    val colors = MaterialTheme.colorScheme
    val lead = block.lead?.let { stringResource(it) }
    val body = stringResource(block.body)
    Text(
        text = buildAnnotatedString {
            if (lead != null) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.onSurface)) { append(lead) }
                append(" ")
            }
            append(body)
        },
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = if (block.muted) colors.onSurfaceVariant else colors.onSurface,
    )
}

/** Two-digit section number, e.g. "01". */
private fun number(value: Int) = value.toString().padStart(2, '0')

@ThemePreviews
@Composable
private fun PrivacyPolicyPreview() {
    TripFairTheme {
        LegalDocumentScreen(document = PrivacyPolicy, onBack = {})
    }
}
