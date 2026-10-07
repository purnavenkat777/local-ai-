package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ToolCallRequest
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaRoseError

@Composable
fun ToolConfirmationCard(
    request: ToolCallRequest,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, DiyaAmberPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .testTag("tool_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Confirmation required",
                    tint = DiyaAmberPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Action Confirmation Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = request.explanation,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("tool_cancel_button")
                ) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                    modifier = Modifier.testTag("tool_confirm_button")
                ) {
                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm Action")
                }
            }
        }
    }
}
