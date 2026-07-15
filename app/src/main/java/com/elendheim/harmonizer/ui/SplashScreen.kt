package com.elendheim.harmonizer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.harmonizer.ui.theme.LogoLight
import com.elendheim.harmonizer.ui.theme.SplashBackground

/**
 * A brief splash: the logo mark over the app name, on near-black to match the
 * logo art. Shown for at most half a second before the app appears.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HarmonizerLogo(modifier = Modifier.size(132.dp))
            Text(
                text = "Elendheim Harmonizer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = LogoLight,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
