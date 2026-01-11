package com.example.rendinxr.feature.review.presentation.components

import android.content.ContentValues.TAG
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.rendinxr.core.domain.model.Defect
import com.example.rendinxr.core.domain.model.SurfaceType
import io.github.sceneview.Scene
import io.github.sceneview.collision.HitResult
import io.github.sceneview.math.Position
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView

/**
 * 3D spatial view showing defects as colored spheres in a 3D space.
 * Uses SceneView's Scene component (non-AR) for 3D rendering.
 *
 * Features:
 * - Spheres represent defects, colored by surface type
 * - Vertical lines show height from floor
 * - Floor grid for spatial reference
 * - Rotate by dragging, zoom by pinching
 * - Tap spheres to select defects
 */
/**
 * 3D spatial view showing defects as colored spheres.
 * Uses SceneView's Scene component with proper lifecycle handling.
 */
private const val TAG = "Spatial3DView"

/**
 * 3D spatial view showing defects as colored spheres.
 * Uses SceneView's Scene component with proper lifecycle handling.
 */
@Composable
fun Spatial3DView(
    defects: List<Defect>,
    selectedDefect: Defect?,
    onDefectSelected: (Defect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (defects.isEmpty()) {
        EmptyState3D(modifier = modifier)
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    // Track lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
            Log.d(TAG, "Lifecycle: $event, isResumed: $isResumed")
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "Disposing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Only render Scene when resumed - use key to force recreation
        if (isResumed) {
            key(defects.hashCode()) {
                Scene3DContent(
                    defects = defects,
                    selectedDefect = selectedDefect,
                    onDefectSelected = onDefectSelected
                )
            }
        } else {
            // Show placeholder when not resumed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading 3D view...", color = Color.White)
            }
        }

        // Overlays (always visible)
        LegendOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )

        InfoPanel(
            defectCount = defects.size,
            selectedDefect = selectedDefect,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )

        InstructionOverlay(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
        )
    }
}

@Composable
private fun Scene3DContent(
    defects: List<Defect>,
    selectedDefect: Defect?,
    onDefectSelected: (Defect) -> Unit
) {

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val scene = rememberScene(engine)
    val cameraNode = rememberCameraNode(engine)
    val childNodes = rememberNodes()

    // Camera manipulator for orbit controls
    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(0f, 1.5f, 4f),
        targetPosition = Position(0f, 0f, 0f)
    )

    // Calculate normalization parameters
    val sceneConfig = remember(defects) { calculateSceneConfig(defects) }

    // Create nodes
    LaunchedEffect(defects, selectedDefect) {
        Log.d(TAG, "Building nodes for ${defects.size} defects")

        // Clear existing nodes
        childNodes.forEach { node ->
            try {
                node.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying node", e)
            }
        }
        childNodes.clear()

        // Create floor
        try {
            val floorNode = CubeNode(
                engine = engine,
                size = io.github.sceneview.math.Size(5f, 0.02f, 5f),
                materialInstance = materialLoader.createColorInstance(
                    color = Color(0xFF2A2A3A),
                    metallic = 0.0f,
                    roughness = 0.9f,
                    reflectance = 0.1f
                )
            ).apply {
                position = Position(0f, -0.5f, 0f)
                name = "floor"
            }
            childNodes.add(floorNode)
            Log.d(TAG, "Floor created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floor", e)
        }

        // Create grid lines on floor
        for (i in -2..2) {
            try {
                // X direction lines
                val xLineNode = CubeNode(
                    engine = engine,
                    size = io.github.sceneview.math.Size(5f, 0.005f, 0.01f),
                    materialInstance = materialLoader.createColorInstance(
                        color = Color.White.copy(alpha = 0.2f),
                        metallic = 0.0f,
                        roughness = 1.0f,
                        reflectance = 0.0f
                    )
                ).apply {
                    position = Position(0f, -0.49f, i.toFloat())
                }
                childNodes.add(xLineNode)

                // Z direction lines
                val zLineNode = CubeNode(
                    engine = engine,
                    size = io.github.sceneview.math.Size(0.01f, 0.005f, 5f),
                    materialInstance = materialLoader.createColorInstance(
                        color = Color.White.copy(alpha = 0.2f),
                        metallic = 0.0f,
                        roughness = 1.0f,
                        reflectance = 0.0f
                    )
                ).apply {
                    position = Position(i.toFloat(), -0.49f, 0f)
                }
                childNodes.add(zLineNode)
            } catch (e: Exception) {
                // Skip grid line
            }
        }

        // Create defect spheres
        defects.forEachIndexed { index, defect ->
            val isSelected = defect.id == selectedDefect?.id

            // Normalize position
            val normalizedX = (defect.worldX - sceneConfig.centerX) * sceneConfig.scale
            val normalizedY = (defect.worldY - sceneConfig.centerY) * sceneConfig.scale
            val normalizedZ = (defect.worldZ - sceneConfig.centerZ) * sceneConfig.scale

            val posX = normalizedX.coerceIn(-2f, 2f)
            val posY = normalizedY.coerceIn(-0.4f, 2f)
            val posZ = normalizedZ.coerceIn(-2f, 2f)

            val color = if (isSelected) Color.Yellow else getSurfaceTypeColor(defect.surfaceType)
            val radius = if (isSelected) 0.12f else 0.08f

            try {
                // Height pole
                val poleHeight = (posY + 0.5f).coerceAtLeast(0.01f)
                val poleNode = CubeNode(
                    engine = engine,
                    size = io.github.sceneview.math.Size(0.015f, poleHeight, 0.015f),
                    materialInstance = materialLoader.createColorInstance(
                        color = color.copy(alpha = 0.4f),
                        metallic = 0.0f,
                        roughness = 1.0f,
                        reflectance = 0.0f
                    )
                ).apply {
                    position = Position(posX, -0.5f + poleHeight / 2f, posZ)
                }
                childNodes.add(poleNode)

                // Sphere
                val sphereNode = SphereNode(
                    engine = engine,
                    radius = radius,
                    materialInstance = materialLoader.createColorInstance(
                        color = color,
                        metallic = 0.2f,
                        roughness = 0.6f,
                        reflectance = 0.4f
                    )
                ).apply {
                    position = Position(posX, posY, posZ)
                    name = defect.id  // Store defect ID in node name
                }
                childNodes.add(sphereNode)

                Log.d(TAG, "Created sphere $index at ($posX, $posY, $posZ)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create defect node", e)
            }
        }

        Log.d(TAG, "Total nodes: ${childNodes.size}")
    }

    // Setup camera
    LaunchedEffect(cameraNode) {
        cameraNode.position = Position(0f, 1.5f, 4f)
        cameraNode.lookAt(Position(0f, 0f, 0f))
        Log.d(TAG, "Camera positioned")
    }

    Scene(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        engine = engine,
        modelLoader = modelLoader,
        view = view,
        renderer = renderer,
        scene = scene,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        childNodes = childNodes,
        onFrame = { frameTimeNanos ->

        },
        onTouchEvent = { motionEvent: MotionEvent, hitResult: HitResult? ->
            // Let cameraManipulator handle the touch first for orbit/zoom
            // Only intercept taps on nodes
            if (motionEvent.action == MotionEvent.ACTION_UP &&
                motionEvent.eventTime - motionEvent.downTime < 200) {
                hitResult?.node?.let { node ->
                    val nodeName = node.name
                    Log.d(TAG, "Tapped node: $nodeName")
                    if (nodeName != null && nodeName != "floor" && !nodeName.startsWith("grid")) {
                        defects.find { it.id == nodeName }?.let { defect ->
                            Log.d(TAG, "Selected defect: ${defect.description}")
                            onDefectSelected(defect)
                            return@Scene true
                        }
                    }
                }
            }
            // Return false to let cameraManipulator handle the event
            false
        }
    )
}

private data class SceneConfig(
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val scale: Float
)

private fun calculateSceneConfig(defects: List<Defect>): SceneConfig {
    if (defects.isEmpty()) {
        return SceneConfig(0f, 0f, 0f, 1f)
    }

    val minX = defects.minOf { it.worldX }
    val maxX = defects.maxOf { it.worldX }
    val minY = defects.minOf { it.worldY }
    val maxY = defects.maxOf { it.worldY }
    val minZ = defects.minOf { it.worldZ }
    val maxZ = defects.maxOf { it.worldZ }

    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f
    val centerZ = (minZ + maxZ) / 2f

    val rangeX = maxX - minX
    val rangeY = maxY - minY
    val rangeZ = maxZ - minZ
    val maxRange = maxOf(rangeX, rangeY, rangeZ, 0.5f)

    return SceneConfig(centerX, centerY, centerZ, scale = 2f / maxRange)
}

private fun getSurfaceTypeColor(surfaceType: SurfaceType): Color {
    return when (surfaceType) {
        SurfaceType.FLOOR -> Color(0xFF4CAF50)
        SurfaceType.WALL -> Color(0xFF2196F3)
        SurfaceType.CEILING -> Color(0xFF9C27B0)
        SurfaceType.TABLE -> Color(0xFFFF9800)
        SurfaceType.UNKNOWN -> Color.Gray
    }
}

@Composable
private fun EmptyState3D(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No defects to display",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun LegendOverlay(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Surface Type",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            LegendItem(color = Color(0xFF4CAF50), label = "Floor")
            LegendItem(color = Color(0xFF2196F3), label = "Wall")
            LegendItem(color = Color(0xFF9C27B0), label = "Ceiling")
            LegendItem(color = Color(0xFFFF9800), label = "Table")
            Spacer(modifier = Modifier.height(4.dp))
            LegendItem(color = Color.Yellow, label = "Selected")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun InfoPanel(
    defectCount: Int,
    selectedDefect: Defect?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Total: $defectCount defects",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )

            selectedDefect?.let { defect ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = defect.description.take(25) + if (defect.description.length > 25) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Yellow,
                    maxLines = 1
                )
                Text(
                    text = "Surface: ${defect.surfaceType.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Pos: (%.2f, %.2f, %.2f)".format(
                        defect.worldX, defect.worldY, defect.worldZ
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun InstructionOverlay(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Drag to rotate • Pinch to zoom • Tap sphere to select",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
