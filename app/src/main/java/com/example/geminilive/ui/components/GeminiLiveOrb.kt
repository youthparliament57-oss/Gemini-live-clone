package com.example.geminilive.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class LiveOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

private class Particle(
    var angle: Float,
    var distanceFraction: Float,
    var speed: Float,
    var size: Float,
    var alpha: Float,
    var color: Color
)

@Composable
fun GeminiLiveOrb(
    state: LiveOrbState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    // Continuous rotation & wave oscillation animations
    val infiniteTransition = rememberInfiniteTransition(label = "orb_infinite")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == LiveOrbState.THINKING) 3000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Smooth amplitude response
    val animatedAmplitude by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "amplitude_anim"
    )

    // Cosmic background particles
    val particles = remember {
        val colors = listOf(
            Color(0xFF4285F4),
            Color(0xFF9B72CF),
            Color(0xFF24C1E0),
            Color(0xFFFA5D75),
            Color(0xFF8AB4F8)
        )
        List(38) {
            Particle(
                angle = Random.nextFloat() * 360f,
                distanceFraction = 0.55f + Random.nextFloat() * 0.42f,
                speed = 0.15f + Random.nextFloat() * 0.45f,
                size = 2.5f + Random.nextFloat() * 4.5f,
                alpha = 0.25f + Random.nextFloat() * 0.65f,
                color = colors.random()
            )
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (minOf(size.width, size.height) * 0.32f) * breathingScale

            // 1. Ambient Cosmic Background Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when (state) {
                            LiveOrbState.IDLE -> Color(0x334285F4)
                            LiveOrbState.LISTENING -> Color(0x4D24C1E0)
                            LiveOrbState.THINKING -> Color(0x559B72CF)
                            LiveOrbState.SPEAKING -> Color(0x59FA5D75)
                        },
                        Color(0x184285F4),
                        Color(0x00000000)
                    ),
                    center = center,
                    radius = baseRadius * 1.85f
                ),
                center = center,
                radius = baseRadius * 1.85f
            )

            // 2. Cosmic Floating Particles
            particles.forEach { p ->
                val dynamicAngle = (p.angle + rotationAngle * p.speed) * (PI / 180f).toFloat()
                val dist = baseRadius * p.distanceFraction * (1f + animatedAmplitude * 0.3f)
                val px = center.x + cos(dynamicAngle) * dist
                val py = center.y + sin(dynamicAngle) * dist
                drawCircle(
                    color = p.color.copy(alpha = p.alpha * (0.5f + animatedAmplitude * 0.5f)),
                    radius = p.size,
                    center = Offset(px, py)
                )
            }

            // 3. Render State Specific Waves / Orb
            when (state) {
                LiveOrbState.IDLE -> {
                    drawIdleOrb(center, baseRadius, rotationAngle, wavePhase)
                }
                LiveOrbState.LISTENING -> {
                    drawListeningFluidWaves(center, baseRadius, animatedAmplitude, wavePhase, rotationAngle)
                }
                LiveOrbState.THINKING -> {
                    drawThinkingPlanetaryRings(center, baseRadius, rotationAngle, wavePhase)
                }
                LiveOrbState.SPEAKING -> {
                    drawSpeakingFluidSoundwave(center, baseRadius, animatedAmplitude, wavePhase, rotationAngle)
                }
            }
        }
    }
}

// ---------------- IDLE STATE ----------------
private fun DrawScope.drawIdleOrb(
    center: Offset,
    radius: Float,
    rotation: Float,
    phase: Float
) {
    // Outer subtle orbital ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0x004285F4),
                Color(0x664285F4),
                Color(0x669B72CF),
                Color(0x0024C1E0),
                Color(0x664285F4)
            ),
            center = center
        ),
        center = center,
        radius = radius * 1.22f,
        style = Stroke(width = 2.5f)
    )

    // Dual layered fluid breathing core
    val path1 = createFluidBlobPath(center, radius, lobes = 4, phase = phase, amplitude = 0.08f)
    drawPath(
        path = path1,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF4285F4),
                Color(0xFF24C1E0),
                Color(0xFF1A3575)
            ),
            center = center,
            radius = radius
        )
    )

    val path2 = createFluidBlobPath(center, radius * 0.88f, lobes = 5, phase = -phase * 1.2f, amplitude = 0.09f)
    drawPath(
        path = path2,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9B72CF),
                Color(0xFF4285F4),
                Color(0xCC1E1B4B)
            ),
            center = center,
            radius = radius * 0.9f
        )
    )

    // Inner bright gem center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8F0FE),
                Color(0x99A8C7FA),
                Color(0x004285F4)
            ),
            center = center,
            radius = radius * 0.42f
        ),
        center = center,
        radius = radius * 0.42f
    )
}

// ---------------- LISTENING STATE ----------------
private fun DrawScope.drawListeningFluidWaves(
    center: Offset,
    baseRadius: Float,
    amplitude: Float,
    phase: Float,
    rotation: Float
) {
    // Expandable outer listening ripple rings
    val rippleRadii = listOf(
        baseRadius * (1.15f + amplitude * 0.65f),
        baseRadius * (1.35f + amplitude * 0.95f),
        baseRadius * (1.55f + amplitude * 1.25f)
    )

    rippleRadii.forEachIndexed { index, r ->
        val alpha = (0.5f - index * 0.15f) * (0.4f + amplitude * 0.6f)
        drawCircle(
            color = Color(0xFF24C1E0).copy(alpha = alpha.coerceIn(0.05f, 0.7f)),
            radius = r,
            center = center,
            style = Stroke(width = 2.5f + index * 1.2f)
        )
    }

    // Dynamic Multi-lobe listening liquid blob reacting strongly to mic amplitude
    val dynAmp = 0.12f + amplitude * 0.38f
    val pathOuter = createFluidBlobPath(center, baseRadius * (1.0f + amplitude * 0.35f), lobes = 6, phase = phase * 1.8f, amplitude = dynAmp)
    drawPath(
        path = pathOuter,
        brush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF00E5FF),
                Color(0xFF2979FF),
                Color(0xFF7C4DFF),
                Color(0xFF00E5FF)
            ),
            center = center
        )
    )

    val pathInner = createFluidBlobPath(center, baseRadius * (0.82f + amplitude * 0.25f), lobes = 5, phase = -phase * 2.2f, amplitude = dynAmp * 0.9f)
    drawPath(
        path = pathInner,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFF00E5FF),
                Color(0xFF1565C0)
            ),
            center = center,
            radius = baseRadius * 0.9f
        )
    )
}

// ---------------- THINKING STATE ----------------
private fun DrawScope.drawThinkingPlanetaryRings(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    phase: Float
) {
    // Fast spinning planetary aura rings
    val ringCount = 3
    for (i in 0 until ringCount) {
        val ringAngle = (rotation * (1.5f + i * 0.8f)) % 360f
        val ringRadius = baseRadius * (1.1f + i * 0.25f)
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0x00FFFFFF),
                    Color(0xFFFA5D75),
                    Color(0xFF9B72CF),
                    Color(0xFF4285F4),
                    Color(0x00FFFFFF)
                ),
                center = center
            ),
            center = center,
            radius = ringRadius,
            style = Stroke(width = 3.5f - i * 0.8f)
        )

        // Orbital comet heads
        val rad = (ringAngle * (PI / 180f)).toFloat()
        val cometPos = Offset(center.x + cos(rad) * ringRadius, center.y + sin(rad) * ringRadius)
        drawCircle(
            color = Color.White,
            radius = 4.5f - i * 0.5f,
            center = cometPos
        )
    }

    // Morphing inner nebula core
    val corePath = createFluidBlobPath(center, baseRadius * 0.85f, lobes = 6, phase = phase * 2.5f, amplitude = 0.14f)
    drawPath(
        path = corePath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFF9B72CF),
                Color(0xFF3F51B5),
                Color(0xFF1A1235)
            ),
            center = center,
            radius = baseRadius * 0.95f
        )
    )
}

// ---------------- SPEAKING STATE ----------------
private fun DrawScope.drawSpeakingFluidSoundwave(
    center: Offset,
    baseRadius: Float,
    amplitude: Float,
    phase: Float,
    rotation: Float
) {
    // Expansive pulse wave
    val pulseRadius = baseRadius * (1.25f + amplitude * 0.8f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x44FA5D75),
                Color(0x339B72CF),
                Color(0x00000000)
            ),
            center = center,
            radius = pulseRadius * 1.35f
        ),
        center = center,
        radius = pulseRadius * 1.35f
    )

    // Outer expressive wave blob
    val ampMult = 0.16f + amplitude * 0.42f
    val wavePathOuter = createFluidBlobPath(
        center = center,
        baseRadius = baseRadius * (1.05f + amplitude * 0.32f),
        lobes = 5,
        phase = phase * 2.6f,
        amplitude = ampMult
    )
    drawPath(
        path = wavePathOuter,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFA5D75), // Gemini coral
                Color(0xFF9B72CF), // Gemini violet
                Color(0xFF4285F4), // Gemini blue
                Color(0xFF24C1E0)  // Gemini cyan
            ),
            start = Offset(center.x - baseRadius, center.y - baseRadius),
            end = Offset(center.x + baseRadius, center.y + baseRadius)
        )
    )

    // Mid layer contrast blob
    val wavePathMid = createFluidBlobPath(
        center = center,
        baseRadius = baseRadius * (0.85f + amplitude * 0.28f),
        lobes = 7,
        phase = -phase * 3.1f,
        amplitude = ampMult * 0.85f
    )
    drawPath(
        path = wavePathMid,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD54F), // Warm gold highlight
                Color(0xFFFF4081), // Vivid pink
                Color(0xFF7C4DFF)  // Deep violet
            ),
            center = center,
            radius = baseRadius
        )
    )

    // Inner radiant glowing core
    val coreRadius = baseRadius * (0.55f + amplitude * 0.25f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xEEFFE082),
                Color(0x00FF80AB)
            ),
            center = center,
            radius = coreRadius
        ),
        center = center,
        radius = coreRadius
    )
}

// Procedural Organic Blob Path Generator using Bezier Curves
private fun createFluidBlobPath(
    center: Offset,
    baseRadius: Float,
    lobes: Int,
    phase: Float,
    amplitude: Float
): Path {
    val path = Path()
    val pointsCount = 48
    val angleStep = (2 * PI / pointsCount).toFloat()

    val points = mutableListOf<Offset>()
    for (i in 0 until pointsCount) {
        val angle = i * angleStep
        // Harmonic lobe displacement
        val modulation = 1f + amplitude * sin(lobes * angle + phase) + (amplitude * 0.4f) * cos(3 * angle - phase * 0.7f)
        val r = baseRadius * modulation
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        points.add(Offset(x, y))
    }

    if (points.isNotEmpty()) {
        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until pointsCount) {
            val p0 = points[i]
            val p1 = points[(i + 1) % pointsCount]
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            path.quadraticTo(p0.x, p0.y, midX, midY)
        }
        path.close()
    }
    return path
}
