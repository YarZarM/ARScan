package com.example.rendinxr.feature.review.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.rendinxr.core.domain.model.Defect
import com.example.rendinxr.core.domain.model.SurfaceType
import com.example.rendinxr.feature.review.presentation.components.Spatial3DView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf("List", "3D View")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Defects (${state.defects.size})") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.defects.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(ReviewEvent.DeleteAll) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete All"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            if (state.defects.isNotEmpty()) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = {
                                when (index) {
                                    0 -> Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = null
                                    )
                                    1 -> Icon(
                                        imageVector = Icons.Default.Warning, // Use 3D icon if available
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.defects.isEmpty() -> {
                        EmptyState(modifier = Modifier.align(Alignment.Center))
                    }

                    else -> {
                        // Use separate if blocks to ensure proper composition/disposal
                        // when switching between tabs - prevents SceneView crashes
                        if (selectedTabIndex == 0) {
                            DefectList(
                                defects = state.defects,
                                onDefectClick = { viewModel.onEvent(ReviewEvent.SelectDefect(it)) },
                                onDeleteClick = { viewModel.onEvent(ReviewEvent.RequestDelete(it)) }
                            )
                        }

                        if (selectedTabIndex == 1) {
                            // Key forces proper recreation when switching back
                            key("spatial3d_${state.defects.size}") {
                                Spatial3DView(
                                    defects = state.defects,
                                    selectedDefect = state.selectedDefect,
                                    onDefectSelected = { viewModel.onEvent(ReviewEvent.SelectDefect(it)) }
                                )
                            }
                        }
                    }
                }

                // Error snackbar
                state.errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.onEvent(ReviewEvent.ClearError) }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }
            }
        }

        state.selectedDefect?.let { defect ->
            DefectDetailDialog(
                defect = defect,
                onDismiss = { viewModel.onEvent(ReviewEvent.ClearSelection) },
                onDelete = { viewModel.onEvent(ReviewEvent.RequestDelete(defect)) }
            )
        }

        if (state.showDeleteConfirmation) {
            DeleteConfirmationDialog(
                isDeleteAll = state.isDeleteAllConfirmation,
                onConfirm = { viewModel.onEvent(ReviewEvent.ConfirmDelete) },
                onDismiss = { viewModel.onEvent(ReviewEvent.CancelDelete) }
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No defects marked yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Go back and tap on surfaces to mark defects",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DefectList(
    defects: List<Defect>,
    onDefectClick: (Defect) -> Unit,
    onDeleteClick: (Defect) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(defects, key = { it.id }) { defect ->
            DefectCard(
                defect = defect,
                onClick = { onDefectClick(defect) },
                onDeleteClick = { onDeleteClick(defect) }
            )
        }
    }
}

@Composable
private fun DefectCard(
    defect: Defect,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imagePath = defect.thumbnailPath ?: defect.imagePath
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Defect image",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = defect.description,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SurfaceTypeChip(surfaceType = defect.surfaceType)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatDate(defect.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SurfaceTypeChip(surfaceType: SurfaceType) {
    val (text, color) = when (surfaceType) {
        SurfaceType.FLOOR -> "Floor" to Color(0xFF4CAF50)
        SurfaceType.WALL -> "Wall" to Color(0xFF2196F3)
        SurfaceType.CEILING -> "Ceiling" to Color(0xFF9C27B0)
        SurfaceType.TABLE -> "Table" to Color(0xFFFF9800)
        SurfaceType.UNKNOWN -> "Unknown" to Color.Gray
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun DefectDetailDialog(
    defect: Defect,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Defect Details",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                AsyncImage(
                    model = File(defect.imagePath),
                    contentDescription = "Defect image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = defect.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(label = "Surface", value = defect.surfaceType.name)
                    DetailRow(label = "Created", value = formatDateTime(defect.createdAt))
                    DetailRow(
                        label = "Position",
                        value = "X: %.2f, Y: %.2f, Z: %.2f".format(
                            defect.worldX,
                            defect.worldY,
                            defect.worldZ
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Defect")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    isDeleteAll: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(if (isDeleteAll) "Delete All Defects?" else "Delete Defect?")
        },
        text = {
            Text(
                if (isDeleteAll) {
                    "This will permanently delete all defects and their images. This action cannot be undone."
                } else {
                    "This action cannot be undone. The defect and its image will be permanently deleted."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isDeleteAll) "Delete All" else "Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}