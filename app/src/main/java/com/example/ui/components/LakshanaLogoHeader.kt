package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Renders the Lakshana Veggie Express oval logo matching the attached user image:
 * Oval with black border, white background, "LAK" + airplane + "EXPRESS",
 * and "VEGGIE" + green leaf 'S' + "HANA".
 */
@Composable
fun LakshanaLogoHeader(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    showContainerCard: Boolean = true
) {
    if (showContainerCard) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_lakshana_logo),
                    contentDescription = "Lakshana Veggie Express Logo",
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(height),
                    contentScale = ContentScale.Fit
                )
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_lakshana_logo),
                contentDescription = "Lakshana Veggie Express Logo",
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(height),
                contentScale = ContentScale.Fit
            )
        }
    }
}
