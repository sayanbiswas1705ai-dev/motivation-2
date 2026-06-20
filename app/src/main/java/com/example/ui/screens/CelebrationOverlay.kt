package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val size: Float,
    val color: Color,
    val speedX: Float,
    val speedY: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CelebrationOverlay(
    monthId: Int,
    onDismiss: () -> Unit
) {
    val random = remember { Random() }
    val colors = listOf(
        Color(0xFF3B82F6), // Accent Blue
        Color(0xFF10B981), // Green
        Color(0xFFFBBF24), // Gold
        Color(0xFFEF4444), // Crimson
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6)  // Purple
    )

    // Confetti positions state
    var particles by remember {
        mutableStateOf(
            List(120) {
                ConfettiParticle(
                    x = random.nextFloat() * 1000f,
                    y = -50f - random.nextFloat() * 400f,
                    size = 15f + random.nextFloat() * 20f,
                    color = colors[random.nextInt(colors.size)],
                    speedX = -3f + random.nextFloat() * 6f,
                    speedY = 4f + random.nextFloat() * 8f,
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = -5f + random.nextFloat() * 10f
                )
            }
        )
    }

    // Animation Loop
    LaunchedEffect(Unit) {
        val maxUpdates = 180 // End particle gravity after ~3 seconds
        var updates = 0
        while (updates < maxUpdates) {
            delay(16) // ~60fps
            particles = particles.map { p ->
                val newY = if (p.y > 2200f) -50f else p.y + p.speedY
                p.copy(
                    x = p.x + p.speedX,
                    y = newY,
                    rotation = p.rotation + p.rotationSpeed
                )
            }
            updates++
        }
    }

    // Background scrim and dialog
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw falling particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            particles.forEach { p ->
                // Apply wrapping or bound protection
                val px = p.x.coerceIn(0f, width)
                val py = p.y.coerceIn(-100f, height)

                rotate(degrees = p.rotation, pivot = androidx.compose.ui.geometry.Offset(px, py)) {
                    drawRect(
                        color = p.color,
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.6f),
                        topLeft = androidx.compose.ui.geometry.Offset(px - p.size / 2f, py - (p.size * 0.6f) / 2f)
                    )
                }
            }
        }

        // Concentrated celebration card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    shadowElevation = 16f
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Celebration Icon
                Icon(
                    imageVector = Icons.Default.Celebration,
                    contentDescription = "Celebration Star",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Congratulations!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Month $monthId Completed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You've successfully finished all 30 days of study for this milestone! The next month's challenges are now fully unlocked. Double down on your academic compound streak!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "Continue Study Journey",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
