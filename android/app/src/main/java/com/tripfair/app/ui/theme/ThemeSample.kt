package com.tripfair.app.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Temporary screen that shows the theme tokens for review.
// Delete once the first real screens replace it in MainActivity.
@Composable
fun ThemeSampleScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    val type = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text("TripFair", style = type.headlineSmall)
            Text(
                "Đi cùng nhau · vui công bằng",
                style = type.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = colors.primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chào mừng trở lại", style = type.headlineLarge)
            Text(
                "Tiếp tục hành trình nhóm mà ai cũng thấy hợp lý.",
                style = type.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }

        Text("Màu", style = type.titleMedium)
        SwatchRow(
            "primary" to colors.primary,
            "primaryContainer" to colors.primaryContainer,
            "tertiary" to colors.tertiary,
            "tertiaryContainer" to colors.tertiaryContainer,
        )
        SwatchRow(
            "error" to colors.error,
            "errorContainer" to colors.errorContainer,
            "surface" to colors.surface,
            "card" to colors.surfaceContainerLow,
        )
        SwatchRow(
            "onSurface" to colors.onSurface,
            "onSurfaceVariant" to colors.onSurfaceVariant,
            "outline" to colors.outline,
            "success" to extended.success,
        )

        Text("Chữ", style = type.titleMedium)
        TypeSample("displaySmall", type.displaySmall, "90% hài lòng")
        TypeSample("headlineMedium", type.headlineMedium, "Cầu Vàng")
        TypeSample("titleLarge", type.titleLarge, "Kế hoạch tối ưu")
        TypeSample("titleMedium", type.titleMedium, "Gợi ý cho cả nhóm")
        TypeSample("titleSmall", type.titleSmall, "Mì Quảng Bà Mua")
        TypeSample("bodyLarge", type.bodyLarge, "Cây cầu giữa mây nổi tiếng với đôi bàn tay khổng lồ.")
        TypeSample("bodyMedium", type.bodyMedium, "1.920.000đ · 92%")
        TypeSample("bodySmall", type.bodySmall, "Cả nhóm · 2 phòng, chia đều · Agoda")
        TypeSample("labelLarge", type.labelLarge, "Đăng nhập")
        TypeSample("labelMedium", type.labelMedium, "Số điện thoại hoặc email")
        TypeSample("labelSmall", type.labelSmall, "Thông tin chuyến đi của bạn luôn được bảo vệ")

        Text("Thành phần mẫu", style = type.titleMedium)
        Surface(
            color = colors.surfaceContainerLow,
            shape = shapes.small,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Cầu Vàng", style = type.titleSmall)
                Text("4,8 ★ · Cảnh đẹp", style = type.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        Text(
            "Hợp sở thích 3/4 thành viên · Có thể vượt ngân sách của 1 thành viên",
            style = type.bodySmall,
            color = colors.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, shapes.extraSmall)
                .padding(12.dp),
        )
        Text(
            "✓  Không ai vượt ngân sách cá nhân",
            style = type.labelMedium,
            color = extended.success,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, shapes.small)
                .padding(14.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionSample(
                title = "SOS",
                subtitle = "Khẩn cấp",
                background = extended.errorSurface,
                border = extended.errorBorder,
                iconBackground = colors.errorContainer,
                modifier = Modifier.weight(1f),
            )
            QuickActionSample(
                title = "Nhận diện",
                subtitle = "Bằng camera",
                background = extended.primarySurface,
                border = extended.primaryBorder,
                iconBackground = extended.primaryIconContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary, shapes.large)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("TỐI ƯU CÔNG BẰNG", style = type.labelMedium, color = colors.onPrimary)
            Text("90% hài lòng", style = type.displaySmall, color = colors.onPrimary)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "4,8 ★",
                style = type.bodyMedium,
                color = colors.onPrimary,
                modifier = Modifier
                    .background(colors.primary, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Text("Hạn 20:00", style = type.bodySmall, color = colors.tertiary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(colors.outline, CircleShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.75f)
                    .height(7.dp)
                    .background(colors.primary, CircleShape),
            )
        }
        Button(
            onClick = {},
            shape = shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Text("Đăng nhập", style = type.labelLarge)
        }
        Button(
            onClick = {},
            shape = shapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.error,
                contentColor = colors.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Gửi SMS vị trí cho người thân", style = type.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SwatchRow(vararg swatches: Pair<String, Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { (name, color) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(color, MaterialTheme.shapes.extraSmall)
                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall),
                )
                Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle, text: String) {
    Column {
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = style)
    }
}

@Composable
private fun QuickActionSample(
    title: String,
    subtitle: String,
    background: Color,
    border: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = background,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, border),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(36.dp).background(iconBackground, CircleShape))
            Text(title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(name = "Light", showBackground = true, heightDp = 1800)
@Preview(name = "Dark", showBackground = true, heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ThemeSamplePreview() {
    TripFairTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ThemeSampleScreen()
        }
    }
}
