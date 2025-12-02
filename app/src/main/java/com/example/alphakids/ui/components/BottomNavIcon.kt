package com.example.alphakids.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.alphakids.ui.theme.AlphakidsTheme

@Composable
fun BottomNavIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    isSelected: Boolean,
    label: String? = null,
    contentDescription: String? = null
) {
    val backgroundModifier = if (isSelected) {
        modifier
            .height(32.dp) // Increased height for pill
            .clip(RoundedCornerShape(16.dp)) // Pill shape
            .background(Color(0xFFCCE8E4)) // Light Green background
            .padding(horizontal = 20.dp, vertical = 4.dp)
    } else {
        modifier
    }

    Box(
        modifier = backgroundModifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription ?: label,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) Color(0xFF006B5F) else Color(0xFF4A6360) // Dark Green vs Grey
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun BottomNavIconPreview() {
    AlphakidsTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BottomNavIcon(
                icon = Icons.Rounded.Home,
                isSelected = true
            )
            BottomNavIcon(
                icon = Icons.Rounded.Home,
                isSelected = false
            )
        }
    }
}
