package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MemoryEntity
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaCyanAccent
import com.example.ui.theme.DiyaRoseError

@Composable
fun MemoryScreen(
    memories: List<MemoryEntity>,
    onAddMemory: (String, String, String, Int) -> Unit,
    onUpdateMemory: (MemoryEntity) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var memoryToEdit by remember { mutableStateOf<MemoryEntity?>(null) }
    var memoryToDelete by remember { mutableStateOf<MemoryEntity?>(null) }

    val filteredMemories = if (searchQuery.isBlank()) {
        memories
    } else {
        memories.filter {
            it.key.contains(searchQuery, ignoreCase = true) ||
            it.value.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    val grouped = filteredMemories.groupBy { it.category }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_memory_button")) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = DiyaAmberPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Personal Memory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { isAddDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_memory_button")
                ) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Memory", fontSize = 12.sp)
                }
            }
        }

        // Info card explaining relevance retrieval
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Diya retrieves only memories relevant to your prompt. Memories serve as background context, never overriding your active commands.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp)
            )
        }

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search memories...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_memory_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Memories List Grouped by Category
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            grouped.forEach { (category, items) ->
                item {
                    Text(
                        text = category.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DiyaCyanAccent,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                    )
                }

                items(items, key = { it.id }) { memory ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("memory_item_${memory.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = memory.key,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "★ ${memory.importance}",
                                    fontSize = 11.sp,
                                    color = DiyaAmberPrimary
                                )
                                IconButton(
                                    onClick = { memoryToEdit = memory },
                                    modifier = Modifier.size(28.dp).testTag("edit_memory_${memory.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { memoryToDelete = memory },
                                    modifier = Modifier.size(28.dp).testTag("delete_memory_${memory.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = memory.value,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (isAddDialogOpen) {
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Personal") }
        var newImportance by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { isAddDialogOpen = false },
            title = { Text("Add Personal Memory") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Topic / Key (e.g. Learning)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_key_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Memory Fact (e.g. User is learning C++)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_memory_value_input"),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Category (Personal, Preferences, Projects)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            val imp = newImportance.toIntOrNull() ?: 3
                            onAddMemory(newKey.trim(), newValue.trim(), newCategory.trim(), imp)
                            isAddDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                    modifier = Modifier.testTag("save_memory_button")
                ) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isAddDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Memory Dialog
    if (memoryToEdit != null) {
        val current = memoryToEdit!!
        var editKey by remember { mutableStateOf(current.key) }
        var editValue by remember { mutableStateOf(current.value) }
        var editCategory by remember { mutableStateOf(current.category) }

        AlertDialog(
            onDismissRequest = { memoryToEdit = null },
            title = { Text("Edit Memory") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editKey,
                        onValueChange = { editKey = it },
                        label = { Text("Topic / Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text("Fact") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMemory(current.copy(key = editKey.trim(), value = editValue.trim(), category = editCategory.trim()))
                        memoryToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary)
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Memory Confirmation
    if (memoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { memoryToDelete = null },
            title = { Text("Delete Memory?") },
            text = { Text("Remove '${memoryToDelete?.key}' from persistent memory?") },
            confirmButton = {
                Button(
                    onClick = {
                        memoryToDelete?.id?.let { onDeleteMemory(it) }
                        memoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaRoseError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
