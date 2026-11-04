package com.tripfair.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink

/** Muted sentence that ends in a teal tappable link, e.g. "Chưa có tài khoản? Đăng ký ngay". */
@Composable
fun TextWithLink(
    prefix: String,
    link: String,
    onLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Center,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        append(prefix)
        append(" ")
        appendLink(link, linkColor, onLinkClick)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

/** Appends [text] as a link in [color] that calls [onClick]; [bold] matches links set in bold in the design. */
fun AnnotatedString.Builder.appendLink(text: String, color: Color, onClick: () -> Unit, bold: Boolean = false) {
    val linkStyle = SpanStyle(color = color, fontWeight = if (bold) FontWeight.Bold else null)
    withLink(LinkAnnotation.Clickable(tag = text, styles = TextLinkStyles(linkStyle)) { onClick() }) {
        append(text)
    }
}
