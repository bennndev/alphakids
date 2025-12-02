package com.example.alphakids.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alphakids.ui.theme.AlphakidsTheme
import com.example.alphakids.ui.theme.dmSansFamily

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainBottomBar(
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent, // Transparent container
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    BottomNavIcon(
                        icon = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        isSelected = isSelected,
                        label = item.label
                    )
                },
                label = {
                    if (isSelected) {
                        Text(
                            text = item.label,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF006B5F) // Dark Green for active label
                        )
                    } else {
                        Text(
                            text = item.label,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color(0xFF4A6360) // Grey/Green for inactive label
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF006B5F), // Dark Green
                    selectedTextColor = Color(0xFF006B5F),
                    unselectedIconColor = Color(0xFF4A6360),
                    unselectedTextColor = Color(0xFF4A6360),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StudentBottomBarPreview() {
    val studentItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
        BottomNavItem("dictionary", "Mi Diccionario", Icons.Rounded.Book, Icons.Outlined.Book),
        BottomNavItem("achievements", "Mis Logros", Icons.Rounded.WorkspacePremium, Icons.Outlined.WorkspacePremium)
    )

    AlphakidsTheme {
        MainBottomBar(
            items = studentItems,
            currentRoute = "home",
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StudentBottomBarPreviewSelected() {
    val studentItems = listOf(
        BottomNavItem("home", "Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
        BottomNavItem("dictionary", "Mi Diccionario", Icons.Rounded.Book, Icons.Outlined.Book),
        BottomNavItem("achievements", "Mis Logros", Icons.Rounded.WorkspacePremium, Icons.Outlined.WorkspacePremium)
    )

    AlphakidsTheme {
        MainBottomBar(
            items = studentItems,
            currentRoute = "dictionary",
            onNavigate = {}
        )
    }
}
