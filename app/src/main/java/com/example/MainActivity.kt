package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.data.db.AppDatabase
import com.example.data.db.DiagnosticLogEntity
import com.example.data.db.StreamEntity
import com.example.data.pref.SettingsRepository
import com.example.data.repository.StreamRepository
import com.example.data.repository.PrayerTimesCalculator
import com.example.data.repository.PrayerTime
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.BarTrackBackground
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.WarningColor
import com.example.ui.theme.ErrorColor
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MainViewModelFactory
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val settingsRepo = SettingsRepository(this)
        val streamRepo = StreamRepository(database.streamDao(), database.diagnosticDao(), settingsRepo)
        val factory = MainViewModelFactory(this, streamRepo, settingsRepo)
        
        val viewModel: MainViewModel by viewModels { factory }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val savedTheme by viewModel.settingsRepository.themeMode.collectAsStateWithLifecycle()

            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSplash = false
            }

            MyApplicationTheme(themeMode = savedTheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            MizanRadioApp(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(tween(600))
                        ) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MizanRadioApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var devModeUnlocked by remember { mutableStateOf(false) }
    
    val navigationItems = listOf(
        Triple("الرئيسية", Icons.Default.Home, "tab_home"),
        Triple("الأذكار", Icons.Default.List, "tab_library"),
        Triple("مواقيت الصلاة", Icons.Default.Favorite, "tab_prayer"),
        Triple("الإعدادات", Icons.Default.Settings, "tab_settings")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Premium Brand Banner Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Crescent dome symbol
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(10.dp))
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(22.dp)) {
                            val path = Path()
                            path.moveTo(size.width * 0.4f, size.height * 0.1f)
                            path.cubicTo(
                                size.width * 0.9f, size.height * 0.2f,
                                size.width * 0.9f, size.height * 0.8f,
                                size.width * 0.4f, size.height * 0.9f
                            )
                            path.cubicTo(
                                size.width * 0.7f, size.height * 0.75f,
                                size.width * 0.7f, size.height * 0.25f,
                                size.width * 0.4f, size.height * 0.1f
                            )
                            drawPath(path = path, brush = GoldPaint(this.size.width))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = "إِذَاعَةُ القُرْآنِ الكَرِيمِ",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "من القاهرة - بث مباشر معتمد وآمن",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 10.sp
                        )
                    }
                }
                Divider(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    thickness = 0.8.dp
                )
            }
        }

        // Screen Content hosting
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> PlayerScreen(viewModel = viewModel)
                1 -> LibraryScreen(viewModel = viewModel) // Combined Quran & Adhkar Tab
                2 -> PrayerTimesScreen(viewModel = viewModel) // New Prayer times tab
                3 -> SettingsScreen(viewModel = viewModel, devModeUnlocked = devModeUnlocked, onDevModeToggle = { devModeUnlocked = it })
            }
        }

        Divider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            thickness = 0.8.dp
        )
        
        // Navigation bar
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
            modifier = Modifier.navigationBarsPadding()
        ) {
            navigationItems.forEachIndexed { index, (label, icon, tag) ->
                val isSelected = selectedTab == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { selectedTab = index },
                    icon = { 
                        Icon(
                            imageVector = icon, 
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    label = { 
                        Text(
                            text = label, 
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ) 
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.testTag(tag)
                )
            }
        }
    }
}

@Composable
fun LibraryScreen(viewModel: MainViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        AdhkarScreen(viewModel = viewModel)
    }
}

@Composable
fun WaveformVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    val barCount = 19
    val heightMultipliers = listOf(
        0.15f, 0.35f, 0.55f, 0.75f, 0.88f, 0.96f, 1.0f, 1.0f, 1.0f,
        1.0f, 0.96f, 0.88f, 0.75f, 0.55f, 0.35f, 0.15f, 0.08f, 0.05f, 0.02f
    )
    
    Row(
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until barCount) {
            val baseMultiplier = heightMultipliers[index]
            val duration = 280 + (index % 5) * 105
            val delay = (index % 4) * 65
            
            val animatedHeight = if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.08f,
                    targetValue = baseMultiplier,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = duration,
                            delayMillis = delay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
            } else {
                infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.12f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 2000,
                            delayMillis = index * 40,
                            easing = LinearOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "idle_$index"
                )
            }
            
            val barHeight = 40.dp * animatedHeight.value
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.5.dp)
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun PlayerScreen(viewModel: MainViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentPlayingUrl.collectAsStateWithLifecycle()
    val streams by viewModel.allStreams.collectAsStateWithLifecycle()

    val currentStream = streams.find { it.url == currentUrl } ?: streams.firstOrNull()
    
    val calendar = remember { Calendar.getInstance() }
    
    val locationMode by viewModel.settingsRepository.locationMode.collectAsStateWithLifecycle()
    val manualCity by viewModel.settingsRepository.manualCity.collectAsStateWithLifecycle()
    val manualLatitude by viewModel.settingsRepository.manualLatitude.collectAsStateWithLifecycle()
    val manualLongitude by viewModel.settingsRepository.manualLongitude.collectAsStateWithLifecycle()
    val autoLatitude by viewModel.settingsRepository.autoLatitude.collectAsStateWithLifecycle()
    val autoLongitude by viewModel.settingsRepository.autoLongitude.collectAsStateWithLifecycle()
    val autoCity by viewModel.settingsRepository.autoCity.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.settingsRepository.hijriAdjustment.collectAsStateWithLifecycle()

    val hijriDateStr = remember(hijriAdjustment) { getHijriDate(hijriAdjustment) }
    val gregDateStr = remember {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date())
    }
    
    val activeLatitude = if (locationMode == "AUTO") autoLatitude.toDouble() else manualLatitude.toDouble()
    val activeLongitude = if (locationMode == "AUTO") autoLongitude.toDouble() else manualLongitude.toDouble()
    val activeCityName = if (locationMode == "AUTO") autoCity else manualCity

    val prayerTimes = remember(calendar, activeLatitude, activeLongitude) {
        val tz = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
        PrayerTimesCalculator.getPrayerTimesForDate(calendar, activeLatitude, activeLongitude, tz)
    }
    val (activePrayer, nextPrayer) = remember(prayerTimes) { PrayerTimesCalculator.getCurrentAndNextPrayer(prayerTimes) }
    
    var showPrayersList by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Upper Scrollable Cards View
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                
                // Location & Dates Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                                            .border(1.2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "إِذَاعَةُ القُرْآنِ الكَرِيمِ",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "مِنَ القَاهِرَةِ",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                // Dynamic location badge
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(text = "📍 $activeCityName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = hijriDateStr,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.displayLarge
                                    )
                                    Text(
                                        text = gregDateStr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // Player connection status pill
                                val connectedText = if (isPlaying) "البث متصل" else if (playbackState == Player.STATE_BUFFERING) "جاري التحميل..." else "البث جاهز"
                                val connectedColor = if (isPlaying) SuccessColor else if (playbackState == Player.STATE_BUFFERING) WarningColor else MaterialTheme.colorScheme.primary
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(connectedColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .border(1.dp, connectedColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(connectedColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = connectedText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = connectedColor,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Next Prayer Highlights Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (activePrayer != null) {
                                        Text(
                                            text = "دخل وقت صلاة " + activePrayer.nameAr,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "توقيت الأذان: " + activePrayer.timeString,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                // Countdown ticker
                                if (nextPrayer != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = getPrayerCountdown(nextPrayer),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPrayersList = !showPrayersList }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showPrayersList) "إخفاء جدول صلوات اليوم" else "عرض مواقيت الصلاة بالكامل بمصر",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (showPrayersList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = showPrayersList,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PrayerItemCard(time = prayerTimes[0], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                        PrayerItemCard(time = prayerTimes[1], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                        PrayerItemCard(time = prayerTimes[2], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PrayerItemCard(time = prayerTimes[3], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                        PrayerItemCard(time = prayerTimes[4], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                        PrayerItemCard(time = prayerTimes[5], activePrayerName = activePrayer?.nameAr ?: "", modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 2. STICKY Pinned Bottom Player (Always visible immediately on startup without scroll)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Premium Waveform Animation inside Player card
                    WaveformVisualizer(isPlaying = isPlaying)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الإذاعة الرسمية للقرآن الكريم",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (isPlaying) {
                                    "تبث الآن: ${currentStream?.displayNameAr ?: "المصدر الرئيسي"}"
                                } else if (playbackState == Player.STATE_BUFFERING) {
                                    "جاري الاتصال بقناة الصوت..."
                                } else {
                                    "جاهز للاستماع الآن"
                                },
                                fontSize = 11.sp,
                                color = if (isPlaying) SuccessColor else if (playbackState == Player.STATE_BUFFERING) WarningColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Large 72dp premium play button
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.togglePlayback() }
                                .testTag("play_pause_button"),
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                if (playbackState == Player.STATE_BUFFERING) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.secondary, 
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else if (isPlaying) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(width = 6.dp, height = 22.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                        Box(modifier = Modifier.size(width = 6.dp, height = 22.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "بدء التشغيل",
                                        modifier = Modifier.size(38.dp).offset(x = (-2).dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionStatusBadge(streams: List<StreamEntity>) {
    val healthyCount = streams.count { it.isHealthy }
    val badgeColor = if (healthyCount >= 3) SuccessColor else WarningColor
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "تم فحص ($healthyCount من ${streams.size}) بنجاح",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun PrayerItemCard(time: PrayerTime, activePrayerName: String, modifier: Modifier = Modifier) {
    val isHighlighted = time.nameAr == activePrayerName
    val borderStroke = if (isHighlighted) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    }
    
    val bg = if (isHighlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = time.nameAr,
                fontSize = 13.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = time.timeString,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class QuranTrack(
    val surahName: String,
    val reciterName: String,
    val audioUrl: String,
    val explanation: String
)

@Composable
fun QuranScreen(viewModel: MainViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentPlayingUrl.collectAsStateWithLifecycle()
    
    // Arabic names sanitized (removed diacritics and kashida) to resolve font engine distortions
    val quranTracks = remember {
        listOf(
            QuranTrack("سورة يس", "الشيخ عبد الباسط عبد الصمد", "https://server11.mp3quran.net/basit/036.mp3", "تلاوة مجودة خاشعة تحبس الأنفاس"),
            QuranTrack("سورة الرحمن", "الشيخ محمد صديق المنشاوي", "https://server10.mp3quran.net/minsh/055.mp3", "تلاوة مرتلة مفعمة بالسكينة والوقار"),
            QuranTrack("سورة الكهف", "الشيخ محمود خليل الحصري", "https://server14.mp3quran.net/lhusr/018.mp3", "نور ما بين الجمعتين مرتلة بالترتيل التاريخي"),
            QuranTrack("سورة الحجرات", "الشيخ مصطفى إسماعيل", "https://server14.mp3quran.net/mustafa/049.mp3", "تلاوة بديعة متميزة بمقاماتها الرائعة"),
            QuranTrack("سورة الملك", "الشيخ محمود علي البنا", "https://server8.mp3quran.net/banna/067.mp3", "المورد العذب المنجية من عذاب القبر"),
            QuranTrack("قصار السور", "الشيخ محمد رفعت", "https://server14.mp3quran.net/mustafa/112.mp3", "تسجيل تاريخي من كنوز الإذاعة المصرية")
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "المصحف الإلكتروني المرتل",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "انتقاء روائع التلاوات التاريخية لكبار قراء مصر برواية حفص عن عاصم المباشرة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quranTracks) { track ->
                    val isTrackPlayingNow = isPlaying && currentUrl == track.audioUrl
                    val cardStyleBorder = if (isTrackPlayingNow) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                    } else {
                        BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    }
                    val cardBgColor = if (isTrackPlayingNow) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val dummyStream = StreamEntity(
                                    id = 999,
                                    url = track.audioUrl,
                                    name = "QuranRecitation",
                                    displayNameAr = "${track.surahName} - ${track.reciterName}",
                                    isPreferred = false,
                                    isHealthy = true
                                )
                                viewModel.playStream(dummyStream)
                             },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        border = cardStyleBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (isTrackPlayingNow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isTrackPlayingNow) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 14.dp)
                                                .background(Color.White, RoundedCornerShape(1.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(width = 4.dp, height = 14.dp)
                                                .background(Color.White, RoundedCornerShape(1.dp))
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.surahName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = track.reciterName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = track.explanation,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            if (isTrackPlayingNow) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(14.dp)
                                ) {
                                    Box(modifier = Modifier.width(3.dp).height(12.dp).background(MaterialTheme.colorScheme.secondary))
                                    Box(modifier = Modifier.width(3.dp).height(8.dp).background(MaterialTheme.colorScheme.secondary))
                                    Box(modifier = Modifier.width(3.dp).height(14.dp).background(MaterialTheme.colorScheme.secondary))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ThikrItem(
    val id: String,
    val text: String,
    val countTarget: Int,
    val explanation: String
)

@Composable
fun AdhkarScreen(viewModel: MainViewModel) {
    var selectedTabGroup by remember { mutableStateOf(0) } // 0: المسبحة, 1: صباح, 2: مساء, 3: بعد الصلاة, 4: نوم, 5: استيقاظ
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val morningAdhkar = remember {
        listOf(
            ThikrItem("morning_1", "آية الكرسي: اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ…", 1, "تحمي من الجن والشياطين حتى يمسي."),
            ThikrItem("morning_2", "سورة الإخلاص، الفلق، الناس (٣ مرات)", 3, "تكفيه من كل شيء."),
            ThikrItem("morning_3", "أصبحنا وأصبح الملك لله والحمد لله، لا إله إلا الله وحده لا شريك له...", 1, "ذكر التوحيد والحمد في الصباح."),
            ThikrItem("morning_4", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم (٣ مرات)", 3, "لم يضره شيء."),
            ThikrItem("morning_5", "رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً (٣ مرات)", 3, "كان حقاً على الله أن يرضيه.")
        )
    }

    val eveningAdhkar = remember {
        listOf(
            ThikrItem("evening_1", "آية الكرسي: اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...", 1, "تحمي من الجن والشياطين حتى يصبح."),
            ThikrItem("evening_2", "سورة الإخلاص، الفلق، الناس (٣ مرات)", 3, "تكفيه من كل شيء."),
            ThikrItem("evening_3", "أمسينا وأمسى الملك لله والحمد لله، لا إله إلا الله وحده لا شريك له...", 1, "ذكر التوحيد والحمد في المساء."),
            ThikrItem("evening_4", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم (٣ مرات)", 3, "لم يضره شيء."),
            ThikrItem("evening_5", "أعوذ بكلمات الله التامات من شر ما خلق (٣ مرات)", 3, "لم تضره حمة لو لدغته.")
        )
    }

    val sleepAdhkar = remember {
        listOf(
            ThikrItem("sleep_1", "باسمك ربي وضعت جنبي وبك أرفعه، فإن أمسكت نفسي فارحمها...", 1, "يقوله إذا أوى إلى فراشه."),
            ThikrItem("sleep_2", "اللهم قني عذابك يوم تبعث عبادك (٣ مرات)", 3, "يقوله وهو واضع يده تحت خده."),
            ThikrItem("sleep_3", "سورة الإخلاص، الفلق، الناس (تجمع الكفين وتنفث فيهما وتقرأ ثم تمسح ما استطعت من الجسد - ٣ مرات)", 3, "سنة نبوية قبل النوم.")
        )
    }

    val wakeupAdhkar = remember {
        listOf(
            ThikrItem("wakeup_1", "الحمد لله الذي أحيانا بعد ما أماتنا وإليه النشور.", 1, "يقال فور الاستيقاظ."),
            ThikrItem("wakeup_2", "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير، الحمد لله وسبحان الله...", 1, "من تعارّ من الليل فقالها غفر له.")
        )
    }

    val afterPrayerAdhkar = remember {
        listOf(
            ThikrItem("after_prayer_1", "أستغفر الله (ثلاثاً)، اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام.", 1, "يقال فور السلام من الصلاة المكتوبة."),
            ThikrItem("after_prayer_2", "سبحان الله (٣٣ مرة)، الحمد لله (٣٣ مرة)، الله أكبر (٣٣ مرة)، ثم تمام المئة: لا إله إلا الله وحده لا شريك له...", 1, "غفرت خطاياه وإن كانت مثل زبد البحر."),
            ThikrItem("after_prayer_3", "آية الكرسي: اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...", 1, "لم يمنعه من دخول الجنة إلا أن يموت.")
        )
    }

    var subhahThikrSelected by remember { mutableStateOf("سبحان الله") }
    var subhahCounter by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val favorites by viewModel.settingsRepository.adhkarFavorites.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabGroup,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 8.dp,
                divider = {}
            ) {
                val tabTitles = listOf("المسبحة", "أذكار الصباح", "أذكار المساء", "أذكار بعد الصلاة", "أذكار النوم", "أذكار الاستيقاظ")
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabGroup == index,
                        onClick = { selectedTabGroup = index },
                        text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTabGroup == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "المسبحة الإلكترونية المطورة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "تلاوة الذكر وتسجيل الأوراد اليومية بلمسة واحدة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("سبحان الله", "الحمد لله", "أستغفر الله", "الله أكبر").forEach { text ->
                            val isSelected = subhahThikrSelected == text
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { subhahThikrSelected = text },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = text,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .clickable { subhahCounter++ }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                        )
                        Surface(
                            modifier = Modifier.size(130.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            shadowElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = subhahThikrSelected, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$subhahCounter", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "اضغـط للتسبيح", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Button(
                        onClick = { subhahCounter = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "إعادة تصفير المسبحة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                val activeList = when (selectedTabGroup) {
                    1 -> morningAdhkar
                    2 -> eveningAdhkar
                    3 -> afterPrayerAdhkar
                    4 -> sleepAdhkar
                    else -> wakeupAdhkar
                }

                val filteredList = if (showOnlyFavorites) {
                    activeList.filter { favorites.contains(it.id) }
                } else {
                    activeList
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (selectedTabGroup) {
                                1 -> "أذكار الصباح"
                                2 -> "أذكار المساء"
                                3 -> "أذكار بعد الصلاة"
                                4 -> "أذكار النوم"
                                5 -> "أذكار الاستيقاظ"
                                else -> ""
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showOnlyFavorites = !showOnlyFavorites }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (showOnlyFavorites) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "المفضلة فقط",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showOnlyFavorites) "لا توجد أذكار في المفضلة حالياً." else "لا توجد أذكار متوفرة.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredList) { item ->
                                val isFav = favorites.contains(item.id)
                                var localTapCount by remember(item) {
                                    mutableStateOf(viewModel.settingsRepository.getThikrProgress(item.id))
                                }
                                val isDone = localTapCount >= item.countTarget

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (localTapCount < item.countTarget) {
                                                localTapCount = viewModel.settingsRepository.incrementThikrProgress(item.id, item.countTarget)
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDone) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDone) SuccessColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = item.text,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 22.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (item.explanation.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.explanation,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.settingsRepository.toggleFavorite(item.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "تفضيل",
                                                        tint = if (isFav) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(item.text))
                                                        Toast.makeText(context, "تم نسخ الذكر للحافظة", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.List,
                                                        contentDescription = "نسخ",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val sendIntent = Intent().apply {
                                                            action = Intent.ACTION_SEND
                                                            putExtra(Intent.EXTRA_TEXT, item.text + " - شارك من تطبيق إذاعة القرآن الكريم من القاهرة")
                                                            type = "text/plain"
                                                        }
                                                        context.startActivity(Intent.createChooser(sendIntent, null))
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "مشاركة",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                if (localTapCount > 0) {
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.settingsRepository.resetThikrProgress(item.id)
                                                            localTapCount = 0
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "تصفير",
                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isDone) SuccessColor else MaterialTheme.colorScheme.primary
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (isDone) "تم ولله الحمد" else "$localTapCount من ${item.countTarget}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerTimesScreen(viewModel: MainViewModel) {
    val calendar = remember { Calendar.getInstance() }
    
    val locationMode by viewModel.settingsRepository.locationMode.collectAsStateWithLifecycle()
    val manualCity by viewModel.settingsRepository.manualCity.collectAsStateWithLifecycle()
    val manualLatitude by viewModel.settingsRepository.manualLatitude.collectAsStateWithLifecycle()
    val manualLongitude by viewModel.settingsRepository.manualLongitude.collectAsStateWithLifecycle()
    val autoLatitude by viewModel.settingsRepository.autoLatitude.collectAsStateWithLifecycle()
    val autoLongitude by viewModel.settingsRepository.autoLongitude.collectAsStateWithLifecycle()
    val autoCity by viewModel.settingsRepository.autoCity.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.settingsRepository.hijriAdjustment.collectAsStateWithLifecycle()
    val isRetrievingLocation by viewModel.isRetrievingLocation.collectAsStateWithLifecycle()

    val hijriDateStr = remember(hijriAdjustment) { getHijriDate(hijriAdjustment) }
    val gregDateStr = remember {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
        sdf.format(Date())
    }
    
    val activeLatitude = if (locationMode == "AUTO") autoLatitude.toDouble() else manualLatitude.toDouble()
    val activeLongitude = if (locationMode == "AUTO") autoLongitude.toDouble() else manualLongitude.toDouble()
    val activeCityName = if (locationMode == "AUTO") autoCity else manualCity

    val prayerTimes = remember(calendar, activeLatitude, activeLongitude) {
        val tz = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 3600000.0
        PrayerTimesCalculator.getPrayerTimesForDate(calendar, activeLatitude, activeLongitude, tz)
    }
    val (activePrayer, nextPrayer) = remember(prayerTimes) { PrayerTimesCalculator.getCurrentAndNextPrayer(prayerTimes) }

    val context = LocalContext.current
    var showCityDropdown by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.settingsRepository.setLocationMode("AUTO")
            viewModel.retrieveGPSLocation()
        } else {
            Toast.makeText(context, "تم رفض صلاحية الموقع، تم التحويل للوضع اليدوي", Toast.LENGTH_LONG).show()
            viewModel.settingsRepository.setLocationMode("MANUAL")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "مواقيت الصلاة اليوم بمصر",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = "حسابات دقيقة بناءً على الموقع الجغرافي النشط والمنطقة الزمنية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Location card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📍 $activeCityName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$hijriDateStr | $gregDateStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Location Mode Controls & Cities Dropdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (locationMode == "AUTO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = stringResource(id = R.string.location_auto_btn),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (locationMode == "AUTO") Color.White else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { showCityDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (locationMode == "MANUAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.location_manual_btn),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (locationMode == "MANUAL") Color.White else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                DropdownMenu(
                                    expanded = showCityDropdown,
                                    onDismissRequest = { showCityDropdown = false }
                                ) {
                                    viewModel.citiesList.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city.nameAr, fontSize = 12.sp, style = MaterialTheme.typography.bodyLarge) },
                                            onClick = {
                                                viewModel.settingsRepository.setLocationMode("MANUAL")
                                                viewModel.settingsRepository.setManualCity(city.nameAr, city.latitude.toFloat(), city.longitude.toFloat())
                                                showCityDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (isRetrievingLocation) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.fillMaxWidth().height(4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.location_status_loading),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Next Prayer Highlights Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (activePrayer != null) {
                            Text(
                                text = "الصلاة الحالية: ${activePrayer.nameAr}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "أذان الصلاة: ${activePrayer.timeString}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (nextPrayer != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "الصلاة القادمة: ${nextPrayer.nameAr} (في تمام ${nextPrayer.timeString})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getPrayerCountdown(nextPrayer),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Prayer timetable schedule list
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        prayerTimes.forEach { time ->
                            val isCurrent = time.nameAr == activePrayer?.nameAr
                            val bg = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
                            val border = if (isCurrent) BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary) else BorderStroke(0.8.dp, Color.Transparent)
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp)),
                                color = bg,
                                border = border
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = time.nameAr,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(الآن)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = time.timeString,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel, 
    devModeUnlocked: Boolean,
    onDevModeToggle: (Boolean) -> Unit
) {
    val streams by viewModel.allStreams.collectAsStateWithLifecycle()
    val savedTheme by viewModel.settingsRepository.themeMode.collectAsStateWithLifecycle()
    val autoReconnect by viewModel.settingsRepository.autoReconnect.collectAsStateWithLifecycle()
    val backgroundPlayback by viewModel.settingsRepository.backgroundPlayback.collectAsStateWithLifecycle()
    val preferredUrl by viewModel.settingsRepository.preferredStreamUrl.collectAsStateWithLifecycle()
    val logs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
    
    val hijriAdjustment by viewModel.settingsRepository.hijriAdjustment.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.settingsRepository.notificationsEnabled.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentPlayingUrl.collectAsStateWithLifecycle()

    var showStreamMenu by remember { mutableStateOf(false) }
    var showDevConsoleDialog by remember { mutableStateOf(false) }
    var developerTapCount by remember { mutableStateOf(0) }

    // Dialog sheets states for release assets
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val preferredStream = streams.find { it.url == preferredUrl }
    val currentStream = streams.find { it.url == currentUrl } ?: streams.firstOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reconnect Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                  Text(
                                      text = stringResource(id = R.string.auto_reconnect_title),
                                      fontSize = 14.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      style = MaterialTheme.typography.bodyLarge
                                  )
                                  Text(
                                      text = stringResource(id = R.string.auto_reconnect_desc),
                                      fontSize = 11.sp,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                      lineHeight = 16.sp,
                                      style = MaterialTheme.typography.bodyMedium
                                  )
                            }
                            Switch(
                                checked = autoReconnect,
                                onCheckedChange = { viewModel.toggleAutoReconnect(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))

                        // Background playback switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                  Text(
                                      text = stringResource(id = R.string.background_playback_title),
                                      fontSize = 14.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      style = MaterialTheme.typography.bodyLarge
                                  )
                                  Text(
                                      text = stringResource(id = R.string.background_playback_desc),
                                      fontSize = 11.sp,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                      lineHeight = 16.sp,
                                      style = MaterialTheme.typography.bodyMedium
                                  )
                            }
                            Switch(
                                checked = backgroundPlayback,
                                onCheckedChange = { viewModel.toggleBackgroundPlayback(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))

                        // Default Preferred Stream Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.preferred_stream_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.preferred_stream_desc),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Box {
                                val streamText = preferredStream?.displayNameAr ?: "المصدر الرئيسي"
                                Button(
                                    onClick = { showStreamMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = streamText, 
                                        color = MaterialTheme.colorScheme.primary, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1, 
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showStreamMenu,
                                    onDismissRequest = { showStreamMenu = false }
                                ) {
                                    streams.forEach { stream ->
                                        DropdownMenuItem(
                                            text = { Text(stream.displayNameAr, fontSize = 12.sp, style = MaterialTheme.typography.bodyLarge) },
                                            onClick = {
                                                viewModel.changePreferredStream(stream)
                                                showStreamMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Appearance Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(id = R.string.appearance_section_title),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(id = R.string.theme_mode_desc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val themeOptions = listOf(
                            Pair("LIGHT", stringResource(id = R.string.theme_light)),
                            Pair("DARK", stringResource(id = R.string.theme_dark)),
                            Pair("SYSTEM", stringResource(id = R.string.theme_system))
                        )

                        themeOptions.forEachIndexed { index, (mode, label) ->
                            val isSelected = savedTheme == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.changeThemeMode(mode) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.changeThemeMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                )
                            }
                            if (index < themeOptions.size - 1) {
                                Divider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Hijri Calendar adjustment section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(id = R.string.hijri_adjust_title),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(id = R.string.hijri_adjust_desc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Pair(stringResource(id = R.string.hijri_adjust_sub_1), -1),
                                Pair(stringResource(id = R.string.hijri_adjust_0), 0),
                                Pair(stringResource(id = R.string.hijri_adjust_add_1), 1)
                            ).forEach { (label, adjust) ->
                                val isSelected = hijriAdjustment == adjust
                                Button(
                                    onClick = { viewModel.settingsRepository.setHijriAdjustment(adjust) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notification Switch Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.notification_section_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.notification_toggle_all_desc),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.settingsRepository.setNotificationsEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }

                        if (notificationsEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val prayers = listOf(
                                Pair(stringResource(id = R.string.notify_fajr_label), SettingsRepository.KEY_NOTIFY_FAJR),
                                Pair(stringResource(id = R.string.notify_sunrise_label), SettingsRepository.KEY_NOTIFY_SUNRISE),
                                Pair(stringResource(id = R.string.notify_dhuhr_label), SettingsRepository.KEY_NOTIFY_DHUHR),
                                Pair(stringResource(id = R.string.notify_asr_label), SettingsRepository.KEY_NOTIFY_ASR),
                                Pair(stringResource(id = R.string.notify_maghrib_label), SettingsRepository.KEY_NOTIFY_MAGHRIB),
                                Pair(stringResource(id = R.string.notify_isha_label), SettingsRepository.KEY_NOTIFY_ISHA)
                            )

                            prayers.forEach { (label, key) ->
                                var enabled by remember(key) {
                                    mutableStateOf(viewModel.settingsRepository.isNotificationForPrayerEnabled(key))
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Checkbox(
                                        checked = enabled,
                                        onCheckedChange = {
                                            viewModel.settingsRepository.setNotificationForPrayerEnabled(key, it)
                                            enabled = it
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Legal & Info Documents Card (Release readiness)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(id = R.string.about_app_title), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(id = R.string.privacy_policy_title), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = { showTermsDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            Icon(imageVector = Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = stringResource(id = R.string.terms_of_use_title), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Hidden Developer Mode section inside settings
            if (devModeUnlocked) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "وضع المطورين نشط 🛠️",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سجل التشخيصات وفحص نبض خوادم البث الصوتي متاح الآن.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showDevConsoleDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "فتح لوحة التشخيص الفنية", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // About application card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "إذاعة القرآن الكريم من القاهرة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "بث آمن ومستمر لإذاعة القرآن الكريم المصرية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.clickable {
                                if (!devModeUnlocked) {
                                    developerTapCount++
                                    if (developerTapCount >= 7) {
                                        onDevModeToggle(true)
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(id = R.string.about_version),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Full Screen Developer Dialog Console Sheet
        if (showDevConsoleDialog) {
            Dialog(onDismissRequest = { showDevConsoleDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "لوحة التدقيق والتشخيص الحية 🛠️",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                            IconButton(onClick = { showDevConsoleDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerSpeedTest() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(text = "فحص الاستجابة", fontSize = 11.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetDiagnostics() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(text = "إعادة ضبط", fontSize = 11.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "قنوات ومسارات البث الاحتياطية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                PositionStatusBadge(streams = streams)
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                streams.forEach { stream ->
                                    val isSelected = currentStream?.id == stream.id
                                    val cardBg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    val cardTextColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    val borderAccentColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    
                                    val nameArShort = when(stream.id) {
                                        1 -> "الرئيسي"
                                        2 -> "المرآة"
                                        3 -> "الطارئ"
                                        else -> "بث بديل"
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clickable { viewModel.changePreferredStream(stream) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBg),
                                        border = BorderStroke(1.dp, borderAccentColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = nameArShort,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cardTextColor,
                                                style = MaterialTheme.typography.labelMedium,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (stream.isHealthy && stream.latencyMs < 9999L) "${stream.latencyMs}ms" else "تالف",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary,
                                                style = MaterialTheme.typography.labelSmall,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "سجل التشخيصات (آخر 15 حدث):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (logs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد أحداث مسجلة.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(logs.take(15)) { log ->
                                        val sdf = SimpleDateFormat("HH:mm:ss", Locale("ar"))
                                        val timeStr = sdf.format(Date(log.timestamp))
                                        val tint = when (log.eventType) {
                                            "ERROR" -> MaterialTheme.colorScheme.error
                                            "SUCCESS" -> SuccessColor
                                            "FAILOVER" -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.04f), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(tint, CircleShape))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = log.message,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 15.sp,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                                Text(
                                                    text = "$timeStr | ${log.eventType}",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // About app document dialog sheet
        if (showAboutDialog) {
            Dialog(onDismissRequest = { showAboutDialog = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(id = R.string.about_app_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { showAboutDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(id = R.string.about_app_desc),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Privacy Policy document dialog sheet
        if (showPrivacyDialog) {
            Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(id = R.string.privacy_policy_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { showPrivacyDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(id = R.string.privacy_policy_content),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Terms of Use document dialog sheet
        if (showTermsDialog) {
            Dialog(onDismissRequest = { showTermsDialog = false }) {
                Surface(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = stringResource(id = R.string.terms_of_use_title), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { showTermsDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stringResource(id = R.string.terms_of_use_content),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        IslamicOrnamentBackground(modifier = Modifier.fillMaxSize())
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "splash")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp))
                    .border(2.dp, MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    val path = Path()
                    path.moveTo(size.width * 0.4f, size.height * 0.1f)
                    path.cubicTo(
                        size.width * 0.9f, size.height * 0.2f,
                        size.width * 0.9f, size.height * 0.8f,
                        size.width * 0.4f, size.height * 0.9f
                    )
                    path.cubicTo(
                        size.width * 0.7f, size.height * 0.75f,
                        size.width * 0.7f, size.height * 0.25f,
                        size.width * 0.4f, size.height * 0.1f
                    )
                    drawPath(path = path, brush = GoldPaint(this.size.width))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "إذاعة القرآن الكريم من القاهرة",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "البث الرسمي المباشر والتعبد اليومي المعتمد",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun IslamicOrnamentBackground(modifier: Modifier = Modifier) {
    val patternColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.025f)
    Canvas(modifier = modifier) {
        val stepPx = 48.dp.toPx()
        val halfStepPx = stepPx / 2f
        val radiusPx = 2.dp.toPx()
        
        val cols = (size.width / stepPx).toInt() + 2
        val rows = (size.height / stepPx).toInt() + 2
        
        for (col in -1..cols) {
            for (row in -1..rows) {
                val x = col * stepPx
                val y = row * stepPx
                
                val path = Path()
                path.moveTo(x + halfStepPx, y)
                path.lineTo(x + stepPx, y + halfStepPx)
                path.lineTo(x + halfStepPx, y + stepPx)
                path.lineTo(x, y + halfStepPx)
                path.close()
                
                drawPath(
                    path = path,
                    color = patternColor,
                    style = Stroke(width = 0.8f)
                )
                
                drawCircle(
                    color = patternColor,
                    radius = radiusPx,
                    center = androidx.compose.ui.geometry.Offset(x + halfStepPx, y + halfStepPx)
                )
            }
        }
    }
}

fun GoldPaint(width: Float): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFFEBC154),
            Color(0xFFC49F3D),
            Color(0xFFFDEBB3)
        )
    )
}

fun getHijriDate(adjustment: Int): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val localDate = java.time.LocalDate.now()
            val adjustedLocalDate = if (adjustment != 0) {
                localDate.plusDays(adjustment.toLong())
            } else {
                localDate
            }
            val hijri = java.time.chrono.HijrahDate.from(adjustedLocalDate)
            
            val day = hijri.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
            val month = hijri.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val year = hijri.get(java.time.temporal.ChronoField.YEAR)
            
            val monthNamesAr = listOf(
                "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
                "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
            )
            val monthName = monthNamesAr.getOrNull(month - 1) ?: "ذو الحجة"
            
            return "$day $monthName $year هـ"
        } catch (e: Exception) {
            // Fallback inside catch
        }
    }
    return "28 ذو الحجة 1447 هـ"
}

fun getPrayerCountdown(nextPrayer: PrayerTime?): String {
    if (nextPrayer == null) return ""
    val now = Calendar.getInstance()
    val parts = nextPrayer.dateString.split(":")
    val targetHour = parts[0].toIntOrNull() ?: 12
    val targetMin = parts[1].toIntOrNull() ?: 0
    
    val targetCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, targetHour)
        set(Calendar.MINUTE, targetMin)
        set(Calendar.SECOND, 0)
    }
    
    if (targetCal.before(now)) {
        targetCal.add(Calendar.DAY_OF_YEAR, 1)
    }
    
    val diffMillis = targetCal.timeInMillis - now.timeInMillis
    val diffHours = diffMillis / (3600 * 1000)
    val diffMins = (diffMillis % (3600 * 1000)) / (60 * 1000)
    
    fun formatHoursAr(hours: Long): String {
        return when (hours) {
            1L -> "ساعة"
            2L -> "ساعتين"
            in 3L..10L -> "$hours ساعات"
            else -> "$hours ساعة"
        }
    }

    fun formatMinutesAr(minutes: Long): String {
        return when (minutes) {
            1L -> "دقيقة واحدة"
            2L -> "دقيقتين"
            in 3L..10L -> "$minutes دقائق"
            else -> "$minutes دقيقة"
        }
    }

    return when {
        diffHours > 0L && diffMins > 0L -> {
            "باقي على أذان ${nextPrayer.nameAr}: ${formatHoursAr(diffHours)} و ${formatMinutesAr(diffMins)}"
        }
        diffHours > 0L -> {
            "باقي على أذان ${nextPrayer.nameAr}: ${formatHoursAr(diffHours)}"
        }
        diffMins > 0L -> {
            "باقي على أذان ${nextPrayer.nameAr}: ${formatMinutesAr(diffMins)}"
        }
        else -> {
            "دخل وقت صلاة ${nextPrayer.nameAr}"
        }
    }
}

