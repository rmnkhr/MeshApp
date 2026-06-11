package com.rmnkhr.meshapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rmnkhr.meshapp.ui.theme.MashAppTheme
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val MIN_GRID = 2
private const val MAX_GRID = 5

/** Max normalized distance a point travels during the whole-mesh wobble. */
private const val MESH_WOBBLE_DISTANCE = 0.2f

/**
 * Color-harmony schemes expressed as hue offsets (in degrees) from a base hue.
 * Sampling colors from one of these keeps a mesh visually cohesive instead of
 * mixing clashing hues from across the whole spectrum.
 */
private val HARMONY_SCHEMES: List<List<Float>> = listOf(
    listOf(0f, 30f, -30f, 15f, -15f),   // analogous – neighbouring hues
    listOf(0f, 180f, 30f, 150f),        // complementary – base + opposite
    listOf(0f, 120f, 240f),             // triadic – evenly spaced thirds
    listOf(0f, 150f, 210f),             // split-complementary
    listOf(0f, 90f, 180f, 270f),        // tetradic – evenly spaced quarters
)

/**
 * Generates a fresh set of mesh colors, one per vertex (row-major).
 *
 * A single random base hue and harmony scheme are chosen per call, producing a
 * small cohesive palette. Each vertex is then assigned a color sampled from that
 * palette, so colors look good together and may repeat across vertices.
 */
private fun generateMeshColors(count: Int): List<Color> {
    val baseHue = Random.nextFloat() * 360f
    val scheme = HARMONY_SCHEMES[Random.nextInt(HARMONY_SCHEMES.size)]

    // Shared saturation/value range keeps the palette tonally consistent.
    val palette = scheme.map { offset ->
        Color.hsv(
            hue = (baseHue + offset + 360f) % 360f,
            saturation = 0.55f + Random.nextFloat() * 0.30f,
            value = 0.78f + Random.nextFloat() * 0.17f,
        )
    }

    return List(count) { palette[Random.nextInt(palette.size)] }
}

// The 4x4 example from https://nilcoalescing.com/blog/MeshGradientsInSwiftUI/,
// reproduced point-for-point. Colors approximate Apple's system colors.
private val ApplePurple = Color(0xFFAF52DE)
private val AppleIndigo = Color(0xFF5856D6)
private val ApplePink = Color(0xFFFF2D55)
private val AppleYellow = Color(0xFFFFCC00)
private val AppleOrange = Color(0xFFFF9500)

private const val EXAMPLE_GRID = 4

/** Control point positions for the example, row-major (matches SwiftUI `points`). */
private val EXAMPLE_POINTS: List<Offset> = listOf(
    Offset(0.0f, 0.0f), Offset(0.3f, 0.0f), Offset(0.7f, 0.0f), Offset(1.0f, 0.0f),
    Offset(0.0f, 0.3f), Offset(0.2f, 0.4f), Offset(0.7f, 0.2f), Offset(1.0f, 0.3f),
    Offset(0.0f, 0.7f), Offset(0.3f, 0.8f), Offset(0.7f, 0.6f), Offset(1.0f, 0.7f),
    Offset(0.0f, 1.0f), Offset(0.3f, 1.0f), Offset(0.7f, 1.0f), Offset(1.0f, 1.0f),
)

/** Vertex colors for the example, row-major (matches SwiftUI `colors`). */
private val EXAMPLE_COLORS: List<Color> = listOf(
    ApplePurple, AppleIndigo, ApplePurple, AppleYellow,
    ApplePink, ApplePurple, ApplePink, AppleYellow,
    AppleOrange, ApplePink, AppleYellow, AppleOrange,
    AppleYellow, AppleOrange, ApplePink, ApplePurple,
)

/** Resets the control points to an evenly spaced normalized grid. */
private fun SnapshotStateList<Offset>.resetToGrid(rows: Int, columns: Int) {
    clear()
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            add(Offset(column / (columns - 1f), row / (rows - 1f)))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MeshGradientScreen() {
    // Number of control points per side (a gridSize x gridSize mesh).
    // Starts on the 4x4 example gradient.
    var gridSize by remember { mutableIntStateOf(EXAMPLE_GRID) }
    val vertexCount = gridSize * gridSize

    // Vertex colors, one per mesh vertex (row-major).
    var meshColors by remember { mutableStateOf(EXAMPLE_COLORS) }

    // Movable control points: normalized (0..1) positions, one per vertex (row-major).
    val meshPoints = remember { mutableStateListOf<Offset>().apply { addAll(EXAMPLE_POINTS) } }

    // Resizing rebuilds an even grid and a fresh palette for the new vertex count.
    fun changeGridSize(size: Int) {
        gridSize = size
        meshPoints.resetToGrid(size, size)
        meshColors = generateMeshColors(size * size)
    }

    // Loads the SwiftUI 4x4 example verbatim.
    fun loadExample() {
        gridSize = EXAMPLE_GRID
        meshPoints.clear()
        meshPoints.addAll(EXAMPLE_POINTS)
        meshColors = EXAMPLE_COLORS
    }

    val meshAnimScope = rememberCoroutineScope()
    var meshAnimating by remember { mutableStateOf(false) }

    // Plays a one-shot "there and back" wobble across the whole mesh: every point
    // eases out to a small random offset, then springs back to where it started.
    // Edge points only move along their edge and corners stay pinned, so the
    // gradient keeps filling the card while the interior ripples.
    fun animateMesh() {
        if (meshAnimating) return
        meshAnimating = true
        meshAnimScope.launch {
            try {
                val bases = meshPoints.toList()
                val targets = bases.mapIndexed { index, base ->
                    val row = index / gridSize
                    val column = index % gridSize
                    val canMoveX = column != 0 && column != gridSize - 1
                    val canMoveY = row != 0 && row != gridSize - 1
                    val angle = Random.nextFloat() * (2f * PI.toFloat())
                    val magnitude = MESH_WOBBLE_DISTANCE * (0.6f + Random.nextFloat() * 0.4f)
                    Offset(
                        x = base.x + if (canMoveX) cos(angle) * magnitude else 0f,
                        y = base.y + if (canMoveY) sin(angle) * magnitude else 0f,
                    )
                }

                fun applyProgress(fraction: Float) {
                    // Guard against a grid resize that happened mid-animation.
                    if (meshPoints.size != bases.size) return
                    for (i in bases.indices) {
                        meshPoints[i] = lerp(bases[i], targets[i], fraction)
                    }
                }

                val progress = Animatable(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                ) { applyProgress(value) }
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) { applyProgress(value) }
                applyProgress(0f)
            } finally {
                meshAnimating = false
            }
        }
    }

    var showHandles by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    // Index of the control point whose color is being edited, or null when none.
    var editingColorIndex by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val colorSheetState = rememberModalBottomSheetState()

    // Rebuild the painter when a point moves, the colors change, or the grid resizes.
    val meshPainter = remember(meshPoints.toList(), meshColors, gridSize) {
        // The painter counts *patches*, not points: a gridSize x gridSize grid
        // of vertices is (gridSize - 1) patches per side, with (rows + 1) x
        // (columns + 1) vertices total. hasBicubicColor uses Catmull-Rom color
        // interpolation for the smooth, iOS-like blend instead of bilinear.
        MeshGradientPainter(
            rows = gridSize - 1,
            columns = gridSize - 1,
            hasBicubicColor = true,
        ) {
            val pointsPerRow = this.columns + 1
            for (row in 0..this.rows) {
                for (column in 0..this.columns) {
                    val index = row * pointsPerRow + column
                    setVertex(
                        row = row,
                        column = column,
                        // Positions are normalized (0..1); scaled to the draw size.
                        position = meshPoints[index],
                        color = meshColors[index],
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Inset by the status/navigation bars so edge control points stay
            // reachable instead of sitting under the system bars.
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                // Extra bottom padding so the mesh clears the floating toolbar.
                .padding(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 120.dp)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val handleSize = 28.dp

            // The mesh gradient itself, as a rounded-corner card.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(28.dp))
                    .paint(meshPainter)
            )

            // Handles live outside the clipped card so corner points stay
            // fully visible and grabbable. Each shows its vertex color.
            if (showHandles) {
                meshPoints.forEachIndexed { index, point ->
                    Box(
                        modifier = Modifier
                            .offset {
                                val half = handleSize.toPx() / 2f
                                IntOffset(
                                    x = (point.x * widthPx - half).roundToInt(),
                                    y = (point.y * heightPx - half).roundToInt(),
                                )
                            }
                            .size(handleSize)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.6f))
                            // Tap a point to edit its color; drag to move it.
                            .pointerInput(index) {
                                detectTapGestures(onTap = { editingColorIndex = index })
                            }
                            .pointerInput(index, gridSize, widthPx, heightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val row = index / gridSize
                                    val column = index % gridSize
                                    val current = meshPoints[index]
                                    // Keep this point between its axis neighbours so a
                                    // cell can't fold over itself — an inverted quad is
                                    // what the mesh renderer draws as a hard triangular
                                    // seam. This keeps every row x-monotonic and every
                                    // column y-monotonic.
                                    val minX = if (column > 0) meshPoints[index - 1].x else 0f
                                    val maxX = if (column < gridSize - 1) meshPoints[index + 1].x else 1f
                                    val minY = if (row > 0) meshPoints[index - gridSize].y else 0f
                                    val maxY = if (row < gridSize - 1) meshPoints[index + gridSize].y else 1f
                                    meshPoints[index] = Offset(
                                        x = (current.x + dragAmount.x / widthPx).coerceIn(minX, maxX),
                                        y = (current.y + dragAmount.y / heightPx).coerceIn(minY, maxY),
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // Inner dot showing this point's selected color.
                        Box(
                            modifier = Modifier
                                .size(handleSize - 10.dp)
                                .clip(CircleShape)
                                .background(meshColors[index])
                        )
                    }
                }
            }
        }

        // Material 3 floating toolbar docked at the bottom of the mesh.
        // Color and Reset are direct actions; Settings is the prominent FAB.
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            floatingActionButton = {
                FloatingToolbarDefaults.StandardFloatingActionButton(
                    onClick = { showSettings = true },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                    )
                }
            },
        ) {
            IconButton(onClick = { meshColors = generateMeshColors(vertexCount) }) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = "Change colors",
                )
            }
            IconButton(
                onClick = { animateMesh() },
                enabled = !meshAnimating,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Animate mesh",
                )
            }
            IconButton(onClick = { loadExample() }) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = "Reset to default layout",
                )
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
        ) {
            SettingsSheetContent(
                gridSize = gridSize,
                onGridSizeChange = { changeGridSize(it) },
                showHandles = showHandles,
                onShowHandlesChange = { showHandles = it },
                onLoadExample = {
                    loadExample()
                    showSettings = false
                },
                onClose = { showSettings = false },
            )
        }
    }

    val editIndex = editingColorIndex
    if (editIndex != null && editIndex < meshColors.size) {
        ModalBottomSheet(
            onDismissRequest = { editingColorIndex = null },
            sheetState = colorSheetState,
        ) {
            ColorEditSheetContent(
                pointNumber = editIndex + 1,
                color = meshColors[editIndex],
                onColorChange = { newColor ->
                    meshColors = meshColors.toMutableList().also { it[editIndex] = newColor }
                },
                onClose = { editingColorIndex = null },
            )
        }
    }
}

/** Bottom sheet to edit a single control point's color via HSV sliders. */
@Composable
private fun ColorEditSheetContent(
    pointNumber: Int,
    color: Color,
    onColorChange: (Color) -> Unit,
    onClose: () -> Unit,
) {
    // Seed the sliders from the current color (decomposed into HSV).
    val initialHsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    fun push() = onColorChange(Color.hsv(hue, saturation, value))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Point $pointNumber color",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                )
            }
        }

        // Live preview swatch.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.hsv(hue, saturation, value))
        )

        Text(text = "Hue", fontSize = 16.sp)
        Slider(
            value = hue,
            onValueChange = { hue = it; push() },
            valueRange = 0f..360f,
        )

        Text(text = "Saturation", fontSize = 16.sp)
        Slider(
            value = saturation,
            onValueChange = { saturation = it; push() },
            valueRange = 0f..1f,
        )

        Text(text = "Brightness", fontSize = 16.sp)
        Slider(
            value = value,
            onValueChange = { value = it; push() },
            valueRange = 0f..1f,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheetContent(
    gridSize: Int,
    onGridSizeChange: (Int) -> Unit,
    showHandles: Boolean,
    onShowHandlesChange: (Boolean) -> Unit,
    onLoadExample: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                )
            }
        }

        Text(
            text = "Points per side",
            fontSize = 16.sp,
        )
        val gridSizes = (MIN_GRID..MAX_GRID).toList()
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            gridSizes.forEachIndexed { index, size ->
                SegmentedButton(
                    selected = size == gridSize,
                    onClick = { onGridSizeChange(size) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = gridSizes.size,
                    ),
                ) {
                    Text(size.toString())
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Show control points",
                fontSize = 16.sp,
            )
            Switch(
                checked = showHandles,
                onCheckedChange = onShowHandlesChange,
            )
        }

        OutlinedButton(
            onClick = onLoadExample,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Load example gradient")
        }
    }
}

@Preview
@Composable
fun MeshGradientScreenPreview() {
    MashAppTheme {
        MeshGradientScreen()
    }
}
