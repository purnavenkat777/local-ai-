package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DiyaScreen
import com.example.ui.theme.DiyaAmberPrimary

@Composable
fun DiyaDrawer(
    currentScreen: DiyaScreen,
    onSelectScreen: (DiyaScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxHeight()
            .width(280.dp)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        fontWeight = FontWeight.Bold,
                        color = DiyaAmberPrimary,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Diya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Local AI Assistant",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Items
            DrawerNavItem(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = "Active Chat",
                isSelected = currentScreen == DiyaScreen.CHAT,
                onClick = { onSelectScreen(DiyaScreen.CHAT) },
                testTag = "drawer_chat"
            )

            DrawerNavItem(
                icon = Icons.Outlined.Forum,
                label = "Conversations",
                isSelected = currentScreen == DiyaScreen.CONVERSATIONS,
                onClick = { onSelectScreen(DiyaScreen.CONVERSATIONS) },
                testTag = "drawer_conversations"
            )

            DrawerNavItem(
                icon = Icons.Outlined.Psychology,
                label = "Personal Memory",
                isSelected = currentScreen == DiyaScreen.MEMORY,
                onClick = { onSelectScreen(DiyaScreen.MEMORY) },
                testTag = "drawer_memory"
            )

            DrawerNavItem(
                icon = Icons.Outlined.Memory,
                label = "GGUF Models",
                isSelected = currentScreen == DiyaScreen.MODELS,
                onClick = { onSelectScreen(DiyaScreen.MODELS) },
                testTag = "drawer_models"
            )

            DrawerNavItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                isSelected = currentScreen == DiyaScreen.SETTINGS,
                onClick = { onSelectScreen(DiyaScreen.SETTINGS) },
                testTag = "drawer_settings"
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Local-First • SQLite & llama.cpp",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) DiyaAmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
