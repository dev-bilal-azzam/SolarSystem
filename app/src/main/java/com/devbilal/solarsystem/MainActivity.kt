package com.devbilal.solarsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp as lerpColor


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SolarSystemScreen()
        }
    }
}

//region SolarSystemScreen
@Composable
fun SolarSystemScreen() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val coroutineScope = rememberCoroutineScope()

        val scrollProgress = remember { Animatable(0f) }
        var isDragUp = remember { false }
        val maxScrollDistancePx = screenHeightPx * 0.65f

        val gestureModifier = Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    val target = if (isDragUp) 1f else 0f
                    val animationSpec = spring<Float>(0.6f, 12f)
                    coroutineScope.launch { scrollProgress.animateTo(target, animationSpec) }
                },
                onDragCancel = {
                    val target = if (isDragUp) 1f else 0f
                    val animationSpec = spring<Float>(0.6f, 12f)
                    coroutineScope.launch { scrollProgress.animateTo(target, animationSpec) }
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    isDragUp = dragAmount < 0
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

            AnimatedHeader(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )

            AnimatedFooter(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )

            AnimatedPlanetsList(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )
        }
    }
}
//endregion




//region AnimatedPlanetsList
@Composable
fun AnimatedPlanetsList(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val coroutineScope = rememberCoroutineScope()

    // Constants for list and stack styling
    val spacingPx = with(density) { 16.dp.toPx() }
    val stackOffsetPx = with(density) { 16.dp.toPx() }
    val earthBaseSizePx = with(density) { (screenWidth * 0.55f).toPx() }
    val listSpacingPx = with(density) { 24.dp.toPx() }

    // Screen bound calculations
    val startEarthBottomPx = (screenHeightPx * 0.65f) + (earthBaseSizePx / 2f) + (earthBaseSizePx * 3.22f / 2f)
    val startY = startEarthBottomPx + listSpacingPx
    val endEarthBottomPx = (screenHeightPx * 0.12f) + earthBaseSizePx
    val endY = endEarthBottomPx + listSpacingPx
    val visibleHeightDp = with(density) { (screenHeightPx - endY).toDp() }

    // 🎯 التعديل الأول: استخدام mutableStateListOf ليتفاعل الـ UI فور حساب الأطوال الحقيقية
    val cardHeightsPx = remember { mutableStateListOf<Float>().apply { repeat(planetsList.size) { add(0f) } } }
    val scrollOffsetPx = remember { Animatable(0f) }
    var isDragUp = remember { false }

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (progressProvider() < 0.99f) return@detectVerticalDragGestures

                // Calculate dynamic snapping points based on exact card heights
                val snapPoints = FloatArray(planetsList.size)
                var currentY = 0f
                for (i in planetsList.indices) {
                    snapPoints[i] = maxOf(0f, currentY - (i * stackOffsetPx))
                    currentY += cardHeightsPx[i] + spacingPx
                }

                val currentScroll = scrollOffsetPx.value
                val target = if (isDragUp) {
                    snapPoints.firstOrNull { it >= currentScroll } ?: snapPoints.last()
                } else {
                    snapPoints.lastOrNull { it <= currentScroll } ?: snapPoints.first()
                }

                coroutineScope.launch {
                    scrollOffsetPx.animateTo(target, spring(0.75f, 15f))
                }
            },
            onDragCancel = {
                if (progressProvider() < 0.99f) return@detectVerticalDragGestures

                val snapPoints = FloatArray(planetsList.size)
                var currentY = 0f
                for (i in planetsList.indices) {
                    snapPoints[i] = maxOf(0f, currentY - (i * stackOffsetPx))
                    currentY += cardHeightsPx[i] + spacingPx
                }

                val currentScroll = scrollOffsetPx.value
                val target = snapPoints.minByOrNull { kotlin.math.abs(it - currentScroll) } ?: 0f

                coroutineScope.launch {
                    scrollOffsetPx.animateTo(target, spring(0.75f, 15f))
                }
            },
            onVerticalDrag = { change, dragAmount ->
                if (progressProvider() < 0.99f) return@detectVerticalDragGestures

                change.consume()
                isDragUp = dragAmount < 0

                // Calculate boundaries dynamically
                val lastIndex = planetsList.size - 1
                var lastDefaultY = 0f
                for (i in 0 until lastIndex) {
                    lastDefaultY += cardHeightsPx[i] + spacingPx
                }
                val maxScroll = maxOf(0f, lastDefaultY - (lastIndex * stackOffsetPx))

                coroutineScope.launch {
                    val newScroll = (scrollOffsetPx.value - dragAmount).coerceIn(0f, maxScroll)
                    scrollOffsetPx.snapTo(newScroll)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(visibleHeightDp)
            .graphicsLayer {
                val progress = progressProvider()
                translationY = lerp(startY, endY, progress)
            }
            .then(gestureModifier)
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp)
        ) {
            planetsList.forEachIndexed { index, planet ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { layoutCoordinates ->
                            val height = layoutCoordinates.size.height.toFloat()
                            if (cardHeightsPx[index] != height) {
                                cardHeightsPx[index] = height
                            }
                        }
                        .graphicsLayer {
                            alpha = if (cardHeightsPx[index] == 0f) 0f else 1f

                            val currentScroll = scrollOffsetPx.value

                            // Normal position calculation based on measured heights
                            var defaultY = 0f
                            for (i in 0 until index) {
                                defaultY += cardHeightsPx[i] + spacingPx
                            }

                            // 16dp stacked offset calculation
                            val stackedY = index * stackOffsetPx
                            val movingY = defaultY - currentScroll

                            // Stacking behavior happens dynamically here
                            translationY = maxOf(stackedY, movingY)
                        }
                ) {
                    PlanetCard(planet = planet)
                }
            }
        }
    }
}
//endregion

// region PlanetCard
@Composable
fun PlanetCard(planet: PlanetData) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(planetBg.copy(alpha = 0.8f))
                .border(0.5.dp, planetBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .height(96.dp)
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier
                        .width(112.dp)
                        .padding(end = 16.dp))
                    Column {
                        Text(
                            text = planet.name,
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.rubik_bold)),
                            fontSize = 18.sp,
                            letterSpacing = 0.25.sp
                        )
                        Text(
                            text = planet.subtitle,
                            color = Color.LightGray,
                            fontFamily = FontFamily(Font(R.font.rubik_regular)),
                            fontSize = 14.sp,
                            letterSpacing = 0.25.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatItem(
                            title = "You Would Weigh",
                            value = planet.weight,
                            icon = ImageVector.vectorResource(R.drawable.ic_weight)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(0.5.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        StatItem(
                            title = "One Day",
                            value = planet.day,
                            icon = ImageVector.vectorResource(R.drawable.ic_sun)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.16f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatItem(
                            title = "Temperature",
                            value = planet.temp,
                            icon = ImageVector.vectorResource(R.drawable.ic_temperature),
                            subValue = planet.tempInfo
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(0.5.dp)
                            .height(32.dp)
                            .background(Color.White.copy(alpha = 0.16f))
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        StatItem(
                            title = "Additional info",
                            value = planet.info,
                            icon = ImageVector.vectorResource(R.drawable.ic_info)
                        )
                    }
                }
            }
        }

        Image(
            painter = painterResource(id = planet.imageId),
            contentDescription = planet.name,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(112.dp)
                .dropShadow(
                    shape = CircleShape,
                    shadow = Shadow(
                        radius = 100.dp,
                        color = planet.glowColor,
                        alpha = 0.5f,
                    )
                )
        )
    }
}
//endregion

// region StatItem
@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    subValue: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.rubik_regular)),
                letterSpacing = 0.25.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )

            val combinedValue = remember(value, subValue) {
                buildAnnotatedString {
                    append(value)
                    if (!subValue.isNullOrEmpty()) {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White.copy(alpha = 0.66f),
                                fontSize = 10.sp,
                                fontFamily = FontFamily(Font(R.font.rubik_regular))
                            )
                        ) {
                            append(", $subValue")
                        }
                    }
                }
            }

            Text(
                text = combinedValue,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.rubik_medium)),
                letterSpacing = 0.25.sp,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            )
        }
    }
}
//endregion

// region AnimatedBackground
@Composable
fun AnimatedBackground(progressProvider: () -> Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val progress = progressProvider()
                val color1 = lerpColor(bgStart1, bgEnd1, progress)
                val color2 = lerpColor(bgStart2, bgEnd2, progress)
                val color3 = lerpColor(bgStart3, bgEnd3, progress)

                drawRect(brush = Brush.verticalGradient(colors = listOf(color1, color2, color3)))
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.stars_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
//endregion

// region AnimatedHeader
@Composable
fun BoxScope.AnimatedHeader(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val earthBaseSizePx = remember(density, screenWidth) { with(density) { (screenWidth * 0.55f).toPx() } }
    val startPaddingPx = remember(density) { with(density) { 56.dp.toPx() } }
    val endPaddingPx = remember(density) { with(density) { 98.dp.toPx() } }

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // End State Header Configurations
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val progress = progressProvider()
                val earthCenterY = (screenHeightPx * 0.12f) + (earthBaseSizePx / 2f)
                val headerHalfHeightPx = with(density) { 40.dp.toPx() }
                val finalCenterY = earthCenterY - headerHalfHeightPx
                val startTranslateY = -screenHeightPx * 0.4f

                translationY = lerp(startTranslateY, finalCenterY, progress)
                alpha = 1f
            },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Our Solar System",
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.rubik_bold)),
                fontWeight = FontWeight(700),
                fontSize = 24.sp
            )
            Text(
                text = "Earth is only one small part of a much larger story.",
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.lily_regular)),
                fontWeight = FontWeight(400),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }

        // Start State Header Configurations
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val progress = progressProvider()
                val currentPadding = lerp(startPaddingPx, endPaddingPx, progress)
                val startTranslateY = 0f
                val endTranslateY = -screenHeightPx * 0.4f

                translationY = lerp(startTranslateY, endTranslateY, progress) + currentPadding
                alpha = 1f
            },
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Earth",
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.rubik_bold)),
                fontWeight = FontWeight(700),
                fontSize = 64.sp
            )
            Text(
                text = "A tiny blue world drifting\nthrough the endless dark.",
                color = Color.White,
                fontFamily = FontFamily(Font(R.font.lily_regular)),
                fontWeight = FontWeight(400),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
//endregion

// region AnimatedEarth
@Composable
fun BoxScope.AnimatedEarth(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val earthBaseSize = screenWidth * 0.55f

    Image(
        painter = painterResource(id = R.drawable.earth),
        contentDescription = "Earth",
        modifier = Modifier
            .size(earthBaseSize)
            .align(Alignment.TopCenter)
            .graphicsLayer {
                val progress = progressProvider()
                val startScale = 3.22f
                val startTranslateY = screenHeightPx * 0.65f
                val endScale = 1.0f
                val endTranslateY = screenHeightPx * 0.12f

                val currentScale = lerp(startScale, endScale, progress)
                scaleX = currentScale
                scaleY = currentScale
                translationY = lerp(startTranslateY, endTranslateY, progress)
            }
            .dropShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 50.dp,
                    color = earthShadowColor,
                    alpha = 0.25f,
                    offset = DpOffset(0.dp, (-12).dp)
                )
            )
    )
}
//endregion

// region AnimatedFooter
@Composable
fun BoxScope.AnimatedFooter(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 48.dp)
            .graphicsLayer {
                val progress = progressProvider()
                val startTranslateY = 0f
                val endTranslateY = screenHeightPx * 0.15f
                translationY = lerp(startTranslateY, endTranslateY, progress)
                alpha = (1f - (progress * 1.5f)).coerceIn(0f, 1f)
            }
    ) {
        val arrowPainter = painterResource(id = R.drawable.ic_arrow_up)
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-12).dp)
        ) {
            repeat(3) {
                Icon(
                    painter = arrowPainter,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Text(
            text = "Swipe Up To Explore",
            color = Color.White,
            fontFamily = FontFamily(Font(R.font.rubik_medium)),
            fontWeight = FontWeight(500),
            fontSize = 16.sp,
            letterSpacing = 0.25.sp
        )
    }
}
//endregion



//region Data Models
data class PlanetData(
    val name: String,
    val subtitle: String,
    val weight: String,
    val day: String,
    val temp: String,
    val tempInfo: String?,
    val info: String,
    val imageId: Int,
    val glowColor: Color
)
//endregion

//region Static Dataset
val planetsList = listOf(
    PlanetData("Saturn", "The Ring Master", "70kg → 74kg", "10.7 Hours", "-178°C", "Bring 3 jacket", "Lighter than water", R.drawable.saturn, Color(0xFFE2BF7D)),
    PlanetData("Mars", "The next colony", "70kg → 27kg", "24.6 Hours", "-65°C", "Bring a jacket", "Red Dust Storms", R.drawable.mars, Color(0xFFFF6B4A)),
    PlanetData("Mercury", "The Fastest Planet", "70kg → 26kg", "1,408 Hours", "167°C", null, "Birthday every 88 day", R.drawable.mercury, Color(0xFFD5D5D5)),
    PlanetData("Venus", "The Toxic Beauty", "70kg → 63kg", "243 Days", "465°C", null, "Sun rises from West", R.drawable.venus, Color(0xFFE3973B)),
    PlanetData("Jupiter", "The Heavy Giant", "70kg → 177kg", "9.9 Hours", "-110°C", "Bring a jacket", "Has 95 moons", R.drawable.jupiter, Color(0xFFD8A070)),
    PlanetData("Uranus", "The Lacy Iceberg", "70kg → 62kg", "17 Hours", "-224°C", "Bring 3 jacket", "diamond Shower", R.drawable.uranus, Color(0xFF70CFFF)),
    PlanetData("Neptune", "The Windy World", "70kg → 79kg", "16 Hours", "-214°C", "Bring 3 jacket", "Wind faster than Sound", R.drawable.neptune, Color(0xFF4B70DD))
)
//endregion

//region Color Constants
val bgStart3 = Color(0xFF020D3C)
val bgStart2 = Color(0xFF0F172A)
val bgStart1 = Color(0xFF060816)
val bgEnd3 = Color(0xFF030712)
val bgEnd2 = Color(0xFF0F172A)
val bgEnd1 = Color(0xFF1E1B4B)
val planetBg = Color(0xFF0B1223)
val planetBorder = Color(0xFF2F2E2E)
val earthShadowColor = Color(0xFF4197E7)
//endregion
