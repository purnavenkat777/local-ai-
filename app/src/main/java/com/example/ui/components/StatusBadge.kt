package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.ModelStatusState
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaCyanAccent
import com.example.ui.theme.DiyaEmeraldGreen
import com.example.ui.theme.DiyaRoseError

@Composable
fun StatusBadge(
    status: ConnectionStatus,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when (status.state) {
        ModelStatusState.READY -> DiyaEmeraldGreen to "READY"
        ModelStatusState.GENERATING -> DiyaCyanAccent to "GENERATING"
        ModelStatusState.LOADING -> DiyaAmberPrimary to "LOADING"
        ModelStatusState.CONNECTING -> DiyaAmberPrimary to "CONNECTING"
        ModelStatusState.OFFLINE -> DiyaRoseError to "OFFLINE"
        ModelStatusState.ERROR -> DiyaRoseError to "ERROR"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.testTag("status_badge_container")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "LOCAL AI",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "● $statusLabel",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (status.state == ModelStatusState.OFFLINE || status.state == ModelStatusState.ERROR) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Retry connection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onRetryClick)
                        .testTag("retry_connection_button")
                )
            }
        }
    }
}
