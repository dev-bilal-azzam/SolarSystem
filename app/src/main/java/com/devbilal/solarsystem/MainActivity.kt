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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.devbilal.solarsystem.ui.theme.SolarSystemTheme
import com.devbilal.solarsystem.ui.theme.bgEnd1
import com.devbilal.solarsystem.ui.theme.bgEnd2
import com.devbilal.solarsystem.ui.theme.bgEnd3
import com.devbilal.solarsystem.ui.theme.bgStart1
import com.devbilal.solarsystem.ui.theme.bgStart2
import com.devbilal.solarsystem.ui.theme.bgStart3
import com.devbilal.solarsystem.ui.theme.planetBg
import com.devbilal.solarsystem.ui.theme.planetBorder
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.lerp as lerpColor

// --- Data Model for Planets Info ---
data class PlanetData(
    val name: String, val subtitle: String,
    val weight: String, val day: String,
    val temp: String, val tempInfo: String?,
    val info: String, val imageId: Int
)

val planetsList = listOf(
    PlanetData("Saturn", "The Ring Master", "70kg → 74kg", "10.7 Hours", "-178°C", "Bring 3 jacket", "Lighter than water", R.drawable.saturn),
    PlanetData("Mars", "The next colony", "70kg → 27kg", "24.6 Hours", "-65°C", "Bring a jacket", "Red Dust Storms", R.drawable.mars),
    PlanetData("Mercury", "The Fastest Planet", "70kg → 26kg", "1,408 Hours", "167°C", null, "Birthday every 88 day", R.drawable.mercury),
    PlanetData("Venus", "The Toxic Beauty", "70kg → 63kg", "243 Days", "465°C", null, "Sun rises from West", R.drawable.venus),
    PlanetData("Jupiter", "The Heavy Giant", "70kg → 177kg", "9.9 Hours", "-110°C", "Bring a jacket", "Has 95 moons", R.drawable.jupiter),
    PlanetData("Uranus", "The Lacy Iceberg", "70kg → 62kg", "17 Hours", "-224°C", "Bring 3 jacket", "diamond Shower", R.drawable.uranus),
    PlanetData("Neptune", "The Windy World", "70kg → 79kg", "16 Hours", "-214°C", "Bring 3 jacket", "Wind faster than Sound", R.drawable.neptune)
)

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
        var isDragUp = remember { false }

        // Swipe sensitivity determining how much pixel drag equals a full 0-1 transition
        val maxScrollDistancePx = screenHeightPx * 0.65f

        val gestureModifier = Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = {
                    // Snap smoothly to the closest state upon releasing the finger
                    val target = if (isDragUp) 1f else 0f
                    val animationSpec = spring<Float>(.6f, 12f)
                    coroutineScope.launch { scrollProgress.animateTo(target, animationSpec) }
                },
                onDragCancel = {
                    val target = if (isDragUp) 1f else 0f
                    val animationSpec = spring<Float>(.6f, 12f)
                    coroutineScope.launch { scrollProgress.animateTo(target, animationSpec) }
                },
                onVerticalDrag = { change, dragAmount ->
                    change.consume()
                    isDragUp = dragAmount < 0
                    println("MAINACTIVITY : drag amount = $dragAmount")
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

            AnimatedHeader(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )

            AnimatedFooter(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )

            // 🎯 تم ربط حركة القائمة هنا لتعمل بشكل موضعي نقي متزامن مع حركة الأرض
            AnimatedPlanetsList(
                progressProvider = { scrollProgress.value },
                screenHeightPx = screenHeightPx
            )
        }
    }
}

// 🎯 المكون المحدث: حركة موضعية نقية تتبع الأرض تماماً وتحافظ على الـ Padding ثابت في الحالتين
@Composable
fun AnimatedPlanetsList(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val earthBaseSizePx = with(density) { (screenWidth * 0.55f).toPx() }
    val spacingPx = with(density) { 24.dp.toPx() }

    // 1. حساب الإحداثيات أسفل الأرض في الـ Start والـ End (نفس كودك بالظبط)
    val startEarthBottomPx = (screenHeightPx * 0.65f) + (earthBaseSizePx / 2f) + (earthBaseSizePx * 3.22f / 2f)
    val startY = startEarthBottomPx + spacingPx

    val endEarthBottomPx = (screenHeightPx * 0.12f) + earthBaseSizePx
    val endY = endEarthBottomPx + spacingPx

    // 🎯 الحل السحري: حساب الارتفاع الفعلي المتاح للقائمة في الـ End State وتحويله لـ Dp
    val visibleHeightDp = with(density) { (screenHeightPx - endY).toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(visibleHeightDp) // 👈 هنا حددنا الارتفاع الحقيقي المتاح للقائمة بدلاً من fillMaxSize
            .graphicsLayer {
                val progress = progressProvider()
                // الحركة الموضعية النقية تتبع الأرض صعوداً وهبوطاً كتفاً بكتف
                translationY = lerp(startY, endY, progress)
            }
            .padding(start = 24.dp, end = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // 👈 الآن سيعمل الـ Scroll فوراً لأن المحتوى أكبر من الـ Viewport
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            planetsList.forEach { planet ->
                PlanetCard(planet = planet)
            }
        }
    }
}

@Composable
fun PlanetCard(planet: PlanetData) {
    // استخدمنا Box خارجي كحاوية لتسمح للصورة بالخروج من حدود الكارت العلوي بدون اقتصاصها
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp) // 👈 مسافة علوية لإعطاء مساحة للجزء البارز من الكوكب
    ) {
        // جسم الكارت الأساسي
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp) // 👈 إزاحة جسم الكارت لأسفل ليظهر الكوكب بارزاً بـ 16dp
                .clip(RoundedCornerShape(24.dp))
                .background(planetBg.copy(alpha = .8f))
                .border(.5.dp, planetBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // الهيدر: تم استبدال الصورة بـ Spacer بنفس عرضها لترك مكان فارغ لها في الـ Row
                Row(
                    modifier = Modifier.height(96.dp).padding(bottom = 24.dp), // الارتفاع المتبقي من الكوكب داخل الكارت (112 - 16)
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(112.dp).padding(end = 16.dp)) // 👈 حجز مساحة لعرض الكوكب
                    Column {
                        Text(
                            text = planet.name,
                            color = Color.White,
                            fontFamily = FontFamily(Font(R.font.rubik_bold)),
                            fontSize = 18.sp,
                            letterSpacing = .25.sp
                        )
                        Text(
                            text = planet.subtitle,
                            color = Color.LightGray,
                            fontFamily = FontFamily(Font(R.font.rubik_regular)),
                            fontSize = 14.sp,
                            letterSpacing = .25.sp
                        )
                    }
                }

                // الصف الأول من البيانات (الوزن و اليوم) مع خط رأسي بالمنتصف
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
                    // الخط الفاصل الرأسي بـ 16dp padding أفقي من الطرفين
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

                // الخط الفاصل الأفقي بين الصفوف
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth()
                        .height(.5.dp)
                        .background(Color.White.copy(alpha = 0.16f))
                )

                // الصف الثاني من البيانات (الحرارة و معلومات إضافية)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatItem(
                            title = "Temperature",
                            value = planet.temp,
                            icon = ImageVector.vectorResource(R.drawable.ic_temperature),
                            subValue = planet.tempInfo // 👈 نمرر النص الإضافي هنا
                        )
                    }
                    // الخط الفاصل الرأسي بـ 16dp padding أفقي من الطرفين
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

        // كوكب الأرض / الكواكب البارزة يتم رسمها فوق الـ Box بشكل مستقل لعدم اقتصاصها
        Image(
            painter = painterResource(id = planet.imageId),
            contentDescription = planet.name,
            modifier = Modifier
                .padding(start = 16.dp) // نفس محاذاة البادينج الداخلي للكارت
                .size(112.dp)
        )
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    subValue: String? = null // معامل اختياري للنصوص الإضافية مثل ملحوظة الطقس
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
                color = Color.White.copy(alpha = .66f),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.rubik_regular)),
                letterSpacing = .25.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )

            // دمج الـ value والـ subValue داخل AnnotatedString لضمان انسيابية السطور وانثنائها معاً بدقة
            val combinedValue = remember(value, subValue) {
                buildAnnotatedString {
                    append(value)
                    if (!subValue.isNullOrEmpty()) {
                        // إضافة ستايل أخف للنص الفرعي ليطابق التصميم تماماً
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
                color = Color.White.copy(alpha = .88f),
                fontSize = 12.sp,
                fontFamily = FontFamily(Font(R.font.rubik_medium)),
                letterSpacing = .25.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}

// =========================================================================================
// ⬇️ الـ Components الأصلية بتاعتك بدون أي تعديل نهائي ⬇️
// =========================================================================================

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
fun BoxScope.AnimatedHeader(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val density = LocalDensity.current
    val screenWidth = LocalWindowInfo.current.containerDpSize.width

    // Exact same Earth size logic from AnimatedEarth
    val earthBaseSizePx = remember(density, screenWidth) { with(density) { (screenWidth * 0.55f).toPx() } }

    // Dynamic paddings required for the Start Header
    val startPaddingPx = remember(density) { with(density) { 56.dp.toPx() } }
    val endPaddingPx = remember(density) { with(density) { 98.dp.toPx() } }

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. END STATE HEADER (Perfect Center Alignment to Earth)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val progress = progressProvider()

                // 🎯 THE MAGIC FORMULA:
                // Earth Top in End State = screenHeightPx * 0.12f
                // Earth Center in End State = (screenHeightPx * 0.12f) + (earthBaseSizePx / 2)
                // We offset the header by this exact amount, then subtract half of the header's estimated height
                // (around 40dp converted to px) to achieve absolute vertical center matching.
                val earthCenterY = (screenHeightPx * 0.12f) + (earthBaseSizePx / 2f)
                val headerHalfHeightPx = with(density) { 40.dp.toPx() }
                val finalCenterY = earthCenterY - headerHalfHeightPx

                // Movement: Fly down from above the screen (-0.4f) to the exact finalCenterY
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

        // 2. START STATE HEADER (Follows standard padding rules)
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

@Composable
fun BoxScope.AnimatedEarth(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    val screenWidth = LocalWindowInfo.current.containerDpSize.width
    val earthBaseSize = screenWidth * 0.55f// Base size of the planet when at 1x scale (End state)

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
                val startScale = 3.22f
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

@Composable
fun BoxScope.AnimatedFooter(
    progressProvider: () -> Float,
    screenHeightPx: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 48.dp) // Base margin from bottom
            // graphicsLayer skips recomposition and handles the movement/fade
            .graphicsLayer {
                val progress = progressProvider()

                // Translate Downwards: Push the footer below the screen edge as progress increases
                val startTranslateY = 0f
                val endTranslateY =
                    screenHeightPx * 0.15f // 15% down is enough to hide it completely
                translationY = lerp(startTranslateY, endTranslateY, progress)

                // Fade Out: Progress from 1f (fully visible) to 0f (invisible)
                // We multiply by 1.5f to make it fade out slightly faster before reaching full scroll
                alpha = (1f - (progress * 1.5f)).coerceIn(0f, 1f)
            }
    ) {
        // 3 Arrows Indicator
        val arrowPainter = painterResource(id = R.drawable.ic_arrow_up)
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Negative spacing to overlap the 24dp boxes so the 13px arrows look connected
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

        // "Swipe Up To Explore" Text
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