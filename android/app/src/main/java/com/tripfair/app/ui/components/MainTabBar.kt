package com.tripfair.app.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripfair.app.R

/** The four bottom tabs ("Điều hướng" in Figma); [selectedIcon] is the filled variant. */
enum class MainTab(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
    val iconSize: Int,
) {
    Explore(R.string.tab_explore, R.drawable.ic_discover, R.drawable.ic_discover, 24),
    Trips(R.string.tab_trips, R.drawable.ic_calendar, R.drawable.ic_calendar, 20),
    Favorites(R.string.tab_favorites, R.drawable.ic_heart, R.drawable.ic_heart_filled, 20),
    Profile(R.string.tab_profile, R.drawable.ic_user, R.drawable.ic_person_selected, 20),
}

@Composable
fun MainTabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.background(colors.surfaceContainer)) {
        HorizontalDivider(thickness = 1.dp, color = colors.outline)
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(71.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                            contentDescription = null,
                            tint = if (isSelected) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(tab.iconSize.dp),
                        )
                    }
                    Text(
                        text = stringResource(tab.label),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) colors.primary else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
