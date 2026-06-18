package com.devbilal.solarsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.devbilal.solarsystem.ui.theme.SolarSystemTheme
import com.devbilal.solarsystem.ui.theme.bgEnd1
import com.devbilal.solarsystem.ui.theme.bgEnd2
import com.devbilal.solarsystem.ui.theme.bgEnd3
import com.devbilal.solarsystem.ui.theme.bgStart1
import com.devbilal.solarsystem.ui.theme.bgStart2
import com.devbilal.solarsystem.ui.theme.bgStart3
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp as lerpColor


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarSystemTheme {
                SolarSystemScreen()
            }
        }
    }
}

@Composable
fun SolarSystemScreen() {
    // BoxWithConstraints is used to accurately measure screen height/width for proportional scaling
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()

        // Progress tracks the scroll state: 0f = Start, 1f = End.
        val scrollProgress = remember { Animatable(0f) }

        // Swipe sensitivity determining how much pixel drag equals a full 0-1 transition
        val maxScrollDistancePx = screenHeightPx * 0.65f

        val gestureModifier = Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    // Snap smoothly to the closest state upon releasing the finger
                    coroutineScope.launch {
                        val target = if (scrollProgress.value > 0.5f) 1f else 0f
                        scrollProgress.animateTo(
                            targetValue = target,
                            animationSpec = tween(
                                durationMillis = 350,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        val target = if (scrollProgress.value > 0.5f) 1f else 0f
                        scrollProgress.animateTo(target, tween(350, easing = FastOutSlowInEasing))
                    }
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    // Dragging up (negative dragAmount) increases progress towards 1f (End State)
                    val progressDelta = -dragAmount / maxScrollDistancePx
                    coroutineScope.launch {
                        val newProgress = (scrollProgress.value + progressDelta).coerceIn(0f, 1f)
                        scrollProgress.snapTo(newProgress)
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            AnimatedBackground(progressProvider = { scrollProgress.value })

            AnimatedEarth(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )
        }
    }
}

@Composable
fun AnimatedBackground(progressProvider: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // drawBehind is used to prevent layout recomposition during animation updates
            .drawBehind {
                val progress = progressProvider()
                val color1 = lerpColor(bgStart1, bgEnd1, progress)
                val color2 = lerpColor(bgStart2, bgEnd2, progress)
                val color3 = lerpColor(bgStart3, bgEnd3, progress)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color1, color2, color3)
                    )
                )
            }
    ) {
        // Overlay Stars Image
        Image(
            painter = painterResource(id = R.drawable.stars_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun BoxScope.AnimatedEarth(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val earthBaseSize = 240.dp // Base size of the planet when at 1x scale (End state)

    Image(
        painter = painterResource(id = R.drawable.earth),
        contentDescription = "Earth",
        modifier = Modifier
            .size(earthBaseSize)
            .align(Alignment.TopCenter) // Start at TopCenter to easily calculate Y offsets
            // graphicsLayer modifies transformation properties skipping recomposition
            .graphicsLayer {
                val progress = progressProvider()

                // "Start" State Settings (Giant Earth bottom anchored)
                val startScale = 2.8f
                val startTranslateY = screenHeightPx * 0.65f

                // "End" State Settings (Normal Earth top anchored)
                val endScale = 1.0f
                val endTranslateY = screenHeightPx * 0.12f

                // Interpolate scale and placement proportionally based on finger scroll
                val currentScale = lerp(startScale, endScale, progress)
                scaleX = currentScale
                scaleY = currentScale
                translationY = lerp(startTranslateY, endTranslateY, progress)
            }
    )
}

@Preview(showBackground = true)
@Composable
fun SolarSystemScreenPreview() {
    SolarSystemTheme {
        SolarSystemScreen()
    }
}