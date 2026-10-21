package com.tripfair.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R

/** Formats whole dong with dot separators, e.g. 2500000 -> "2.500.000đ". */
fun formatVnd(amount: Long): String =
    amount.toString().reversed().chunked(3).joinToString(".").reversed() + "đ"

/**
 * Large money input ("Nhập ngân sách" in Figma). [digits] holds only the number; the field shows
 * it formatted and keeps the cursor before "đ". An empty field means 0đ, which is allowed.
 */
@Composable
fun BudgetField(
    digits: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
) {
    val colors = MaterialTheme.colorScheme
    val text = formatVnd(digits.toLongOrNull() ?: 0L)
    BasicTextField(
        value = TextFieldValue(text, selection = TextRange(text.length - 1)),
        onValueChange = { new -> onDigitsChange(new.text.filter { it.isDigit() }.trimStart('0').take(12)) },
        modifier = modifier,
        singleLine = true,
        textStyle = textStyle.copy(color = colors.onSurface),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(colors.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.primary, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.weight(1f)) { inner() }
                Icon(
                    painter = painterResource(R.drawable.ic_pencil),
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}
