package com.tripfair.app.ui.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tripfair.app.R
import com.tripfair.app.ui.theme.ThemePreviews
import com.tripfair.app.ui.theme.TripFairTheme

// Placeholder target for "Đăng nhập hợp lệ"; the designed home screen replaces it.
@Composable
fun HomeScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@ThemePreviews
@Composable
private fun HomeScreenPreview() {
    TripFairTheme {
        HomeScreen()
    }
}
