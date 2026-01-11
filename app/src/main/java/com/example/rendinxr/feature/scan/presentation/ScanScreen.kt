package com.example.rendinxr.feature.scan.presentation

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.rendinxr.core.domain.model.SurfaceType
import com.google.android.filament.Engine
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState as ARTrackingState
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.node.Node
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.jar.Manifest

private const val TAG = "ScanScreen"

@Composable
fun ScanScreen(
    onNavigateToReview: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isActive by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Track lifecycle to prevent AR operations during navigation
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isActive = true
                    Log.d(TAG, "ScanScreen ON_RESUME, isActive = true")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isActive = false
                    Log.d(TAG, "ScanScreen ON_PAUSE, isActive = false")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "ScanScreen disposing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { navigationEvent ->
            when (navigationEvent) {
                NavigationEvent.ToReview -> {
                    // Stop AR operations BEFORE navigating
                    isActive = false
                    Log.d(TAG, "Navigating to Review, isActive = false")
                    // Give AR session and nodes time to properly shut down
                    // This allows LaunchedEffect(isActive) to clear nodes
                    onNavigateToReview()
                }
            }
        }
    }

    if(!hasCameraPermission) {
        PermissionDeniedContent(
            onRequestPermission = { permissionLauncher.launch(android.Manifest.permission.CAMERA)}
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ARSceneContent(
            state = state,
            onEvent = viewModel::onEvent,
            isActive = isActive
        )

        // Overlay black screen when navigating away
        if (!isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Only show overlay when active
        if (isActive) {
            ScanOverlay(
                state = state,
                onEvent = viewModel::onEvent
            )
        }

        if (state.showDescriptionDialog) {
            DefectDescriptionDialog(
                description = state.descriptionText,
                onDescriptionChanged = { viewModel.onEvent(ScanEvent.DescriptionChanged(it))},
                onSave = {viewModel.onEvent(ScanEvent.SaveDefect)},
                onDismiss = {viewModel.onEvent(ScanEvent.DismissDialog)}
            )
        }

        state.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.onEvent(ScanEvent.ClearError) }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }

        if (state.isCapturingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
private fun ARSceneContent(
    state: ScanState,
    onEvent: (ScanEvent) -> Unit,
    isActive: Boolean
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val view = rememberView(engine)

    var frame by remember { mutableStateOf<Frame?>(null) }
    val childNodes = rememberNodes()

    // Track pending image capture
    var pendingImageCapture by remember { mutableStateOf<String?>(null) }

    // Track detected plane types for UI feedback
    var horizontalPlaneCount by remember { mutableIntStateOf(0) }
    var verticalPlaneCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            destroyNodesSafely(engine, childNodes)
        }
    }

    ARScene(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        view = view,
        planeRenderer = isActive,
        sessionConfiguration = { session, config ->
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.focusMode = Config.FocusMode.AUTO
            config.depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }
        },
        cameraStream = rememberARCameraStream(materialLoader),
        childNodes = childNodes,
        onSessionCreated = { session ->
            onEvent(ScanEvent.ARSessionReady)
        },
        onSessionUpdated = { session, updatedFrame ->
            frame = updatedFrame

            // Update tracking state
            val camera = updatedFrame.camera
            val trackingState = when (camera.trackingState) {
                ARTrackingState.TRACKING -> TrackingState.TRACKING
                ARTrackingState.PAUSED -> TrackingState.LIMITED
                ARTrackingState.STOPPED -> TrackingState.STOPPED
            }
            onEvent(ScanEvent.TrackingStateChanged(trackingState))

            // Check for detected planes
            val planes = session.getAllTrackables(Plane::class.java)
            val trackingPlanes = planes.filter { it.trackingState == ARTrackingState.TRACKING }

            horizontalPlaneCount = trackingPlanes.count {
                it.type == Plane.Type.HORIZONTAL_UPWARD_FACING ||
                        it.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING
            }
            verticalPlaneCount = trackingPlanes.count { it.type == Plane.Type.VERTICAL }

            if (trackingPlanes.isNotEmpty()) {
                onEvent(ScanEvent.PlaneDetected)
            }

            // Log plane detection for debugging
            if (trackingPlanes.isNotEmpty()) {
                Log.d(TAG, "Planes: ${trackingPlanes.size} (H:$horizontalPlaneCount, V:$verticalPlaneCount)")
            }

            // Handle pending image capture
            pendingImageCapture?.let { defectId ->
                try {
                    val bitmap = acquireFrameBitmap(updatedFrame)
                    bitmap?.let {
                        onEvent(ScanEvent.ImageCaptured(defectId, it))
                    }
                } catch (e: Exception) {
                    // Will retry next frame
                }
                pendingImageCapture = null
            }
        },
        onSessionFailed = { exception ->
            onEvent(ScanEvent.TrackingStateChanged(TrackingState.STOPPED))
        },
        onTrackingFailureChanged = { reason ->
            val trackingState = when (reason) {
                null -> TrackingState.TRACKING
                TrackingFailureReason.NONE -> TrackingState.TRACKING
                else -> TrackingState.LIMITED
            }
            onEvent(ScanEvent.TrackingStateChanged(trackingState))
        },
        onTouchEvent = { motionEvent, _ ->
            // Skip touch events if not active (navigating away)
            if (!isActive) return@ARScene true

            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                handleTap(
                    motionEvent = motionEvent,
                    frame = frame,
                    state = state,
                    engine = engine,
                    materialLoader = materialLoader,
                    childNodes = childNodes,
                    onEvent = onEvent,
                    onPendingImageCapture = { defectId -> pendingImageCapture = defectId }
                )
            }
            true
        }
    )
}

private suspend fun destroyNodesSafely(engine: Engine, nodes: MutableList<Node>) {
    val nodesToDestroy = nodes.toList()

    nodesToDestroy.forEach { runCatching { it.parent = null } }
    nodes.clear()

    // Let one frame render without them
    withFrameNanos { }
    withFrameNanos { }

    // Destroy in reverse order (children first if you added sphere then anchor)
    nodesToDestroy.asReversed().forEach { runCatching { it.destroy() } }

    withFrameNanos { }
    withFrameNanos { }

    runCatching { engine.flushAndWait() }
}

private fun handleTap(
    motionEvent: MotionEvent,
    frame: Frame?,
    state: ScanState,
    engine: com.google.android.filament.Engine,
    materialLoader: io.github.sceneview.loaders.MaterialLoader,
    childNodes: MutableList<io.github.sceneview.node.Node>,
    onEvent: (ScanEvent) -> Unit,
    onPendingImageCapture: (String) -> Unit
) {
    Log.d(TAG, "Tap at: ${motionEvent.x}, ${motionEvent.y}")

    val currentFrame = frame
    if (currentFrame == null) {
        Log.d(TAG, "No frame available")
        return
    }

    if (state.trackingState != TrackingState.TRACKING) {
        Log.d(TAG, "Not tracking: ${state.trackingState}")
        return
    }

    if (state.isCapturingImage) {
        Log.d(TAG, "Already capturing image")
        return
    }

    // Perform hit test
    val hitResults = currentFrame.hitTest(motionEvent.x, motionEvent.y)
    Log.d(TAG, "Hit results: ${hitResults.size}")

    // Find best hit - prefer planes over instant placement
    val validHit = findBestHit(hitResults)

    if (validHit == null) {
        Log.d(TAG, "No valid hit found - tap on a detected surface")
        return
    }

    val trackable = validHit.trackable
    Log.d(TAG, "Valid hit on: ${trackable::class.simpleName}")

    // Determine surface type
    val surfaceType = when (trackable) {
        is Plane -> {
            Log.d(TAG, "Plane type: ${trackable.type}")
            when (trackable.type) {
                Plane.Type.HORIZONTAL_UPWARD_FACING -> {
                    // Check height to distinguish floor vs table
                    val hitY = validHit.hitPose.ty()
                    Log.d(TAG, "Horizontal plane at Y: $hitY")
                    if (hitY < -0.5f) SurfaceType.FLOOR else SurfaceType.TABLE
                }
                Plane.Type.HORIZONTAL_DOWNWARD_FACING -> SurfaceType.CEILING
                Plane.Type.VERTICAL -> SurfaceType.WALL
                else -> SurfaceType.UNKNOWN
            }
        }
        else -> SurfaceType.UNKNOWN
    }

    Log.d(TAG, "Surface type: $surfaceType")

    // Create anchor
    val anchor = validHit.createAnchorOrNull()
    if (anchor == null) {
        Log.e(TAG, "Failed to create anchor")
        return
    }

    val pose = anchor.pose
    val defectId = UUID.randomUUID().toString()

    Log.d(TAG, "Creating marker at: ${pose.tx()}, ${pose.ty()}, ${pose.tz()}")

    // Add visual marker - color based on surface type
    try {
        val markerColor = when (surfaceType) {
            SurfaceType.FLOOR -> Color(0xFF4CAF50)    // Green
            SurfaceType.WALL -> Color(0xFF2196F3)     // Blue
            SurfaceType.CEILING -> Color(0xFF9C27B0) // Purple
            SurfaceType.TABLE -> Color(0xFFFF9800)   // Orange
            SurfaceType.UNKNOWN -> Color.Red
        }

        val anchorNode = AnchorNode(engine = engine, anchor = anchor)
        val sphereNode = SphereNode(
            engine = engine,
            radius = 0.025f,
            materialInstance = materialLoader.createColorInstance(
                color = markerColor,
                metallic = 0.0f,
                roughness = 0.5f,
                reflectance = 0.5f
            )
        )
        anchorNode.addChildNode(sphereNode)
        childNodes.add(sphereNode)
        childNodes.add(anchorNode)
        Log.d(TAG, "Marker added to scene")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create marker", e)
        return
    }

    onEvent(ScanEvent.TapToPlace(
        defectId = defectId,
        worldX = pose.tx(),
        worldY = pose.ty(),
        worldZ = pose.tz(),
        surfaceType = surfaceType
    ))

    onPendingImageCapture(defectId)
}

private fun findBestHit(hitResults: List<HitResult>): HitResult? {
    // First, try to find a hit on a tracked plane
    val planeHit = hitResults.firstOrNull { hit ->
        val trackable = hit.trackable
        trackable is Plane &&
                trackable.trackingState == ARTrackingState.TRACKING &&
                trackable.isPoseInPolygon(hit.hitPose)
    }

    if (planeHit != null) {
        return planeHit
    }

    // Fall back to any tracked plane (even if not in polygon - edge case)
    return hitResults.firstOrNull { hit ->
        val trackable = hit.trackable
        trackable is Plane && trackable.trackingState == ARTrackingState.TRACKING
    }
}
/**
 * Acquire bitmap from AR frame's camera image
 */
private fun acquireFrameBitmap(frame: Frame): Bitmap? {
    return try {
        frame.acquireCameraImage().use { image ->
            imageToBitmap(image)
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert YUV camera image to Bitmap
 */
private fun imageToBitmap(image: Image): Bitmap? {
    return try {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            image.width,
            image.height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, image.width, image.height),
            90,
            out
        )

        val bytes = out.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ScanOverlay(
    state: ScanState,
    onEvent: (ScanEvent) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        TrackingStatusCard(state = state)

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.isPlanDetected) {
                InstructionCard(text = "Move your phone slowly to detect surfaces")
            } else if (state.trackingState == TrackingState.TRACKING) {
                InstructionCard(text = "Tap on a surface to mark a defect")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DefectCounter(count = state.savedDefectsCount)

                if (state.savedDefectsCount > 0) {
                    Button(onClick = { onEvent(ScanEvent.NavigateToReview) }) {
                        Text("Review (${state.savedDefectsCount})")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingStatusCard(state: ScanState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (state.trackingState) {
                TrackingState.TRACKING -> Color.Green.copy(alpha = 0.8f)
                TrackingState.LIMITED -> Color.Yellow.copy(alpha = 0.8f)
                TrackingState.INITIALIZING, TrackingState.STOPPED -> Color.Red.copy(alpha = 0.8f)
            }
        )
    ) {
        Text(
            text = when (state.trackingState) {
                TrackingState.INITIALIZING -> "Initializing..."
                TrackingState.TRACKING -> "Tracking"
                TrackingState.LIMITED -> "Limited tracking"
                TrackingState.STOPPED -> "Tracking lost"
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White
        )
    }
}

@Composable
private fun InstructionCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DefectCounter(count: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count defects marked",
                color = Color.White
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This app needs camera access to scan your space and mark defects in AR.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun DefectDescriptionDialog(
    description: String,
    onDescriptionChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Describe the defect") },
        text = {
            Column {
                Text(
                    "Add a short description of this defect",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Crack in wall, water damage...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = description.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel")
            }
        }
    )
}