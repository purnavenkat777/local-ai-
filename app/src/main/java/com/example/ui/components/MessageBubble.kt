package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaCyanAccent

@Composable
fun MessageBubble(
    role: String,
    content: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    val isUser = role == "user"
    val isTool = role == "tool"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        ) {
            Icon(
                imageVector = if (isUser) Icons.Outlined.Person else Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = if (isUser) DiyaCyanAccent else DiyaAmberPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    isUser -> "Venkat"
                    isTool -> "Diya (Tool Result)"
                    else -> "Diya"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            color = when {
                isUser -> MaterialTheme.colorScheme.surfaceVariant
                isTool -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surface
            },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = if (isUser) 1.dp else 2.dp,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (isUser) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("message_bubble_${role}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // If message contains code blocks, render with monospaced style
                if (content.contains("```")) {
                    RenderMarkdownWithCodeBlocks(content)
                } else {
                    Text(
                        text = content,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }

                if (isStreaming) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "●",
                        color = DiyaCyanAccent,
                        fontSize = 12.sp
                    )
                }

                // Copy affordance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                            isCopied = true
                        },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_message_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy message",
                            tint = if (isCopied) DiyaAmberPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenderMarkdownWithCodeBlocks(content: String) {
    val parts = content.split("```")
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val lines = part.lines()
                val lang = lines.firstOrNull()?.trim() ?: ""
                val code = lines.drop(1).joinToString("\n").trimEnd()

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (lang.isNotEmpty()) {
                            Text(
                                text = lang.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DiyaAmberPrimary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = code.ifEmpty { part },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                if (part.isNotBlank()) {
                    Text(
                        text = part,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}
