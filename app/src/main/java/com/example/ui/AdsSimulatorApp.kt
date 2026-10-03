package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.ChatMessage
import com.example.database.SimulationMetric
import com.example.viewmodel.AdsViewModel
import com.example.viewmodel.MockBanner
import com.example.viewmodel.InterstitialAdItem
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdsSimulatorApp(viewModel: AdsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showUserGuideDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Live Viewer, 1: Metrics Simulator, 2: AdBot Chat

    // States from VM
    val totalImpressions by viewModel.totalImpressions.collectAsStateWithLifecycle()
    val totalClicks by viewModel.totalClicks.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val totalCtr by viewModel.totalCtr.collectAsStateWithLifecycle()
    val totalCpm by viewModel.totalCpm.collectAsStateWithLifecycle()
    val watchedCount by viewModel.watchedCount.collectAsStateWithLifecycle()

    val adBotDialogue by viewModel.adBotDialogue.collectAsStateWithLifecycle()
    val isAIBusy by viewModel.isAIBusy.collectAsStateWithLifecycle()

    val isInterstitialVisible by viewModel.isInterstitialVisible.collectAsStateWithLifecycle()
    val isRewardedVisible by viewModel.isRewardedVisible.collectAsStateWithLifecycle()
    val showSecretDialog by viewModel.showSecretDialog.collectAsStateWithLifecycle()
    val isHeistGameVisible by viewModel.isHeistGameVisible.collectAsStateWithLifecycle()
    val showClickbaitScare by viewModel.showClickbaitScare.collectAsStateWithLifecycle()
    val selectedFullBanner by viewModel.selectedFullBanner.collectAsStateWithLifecycle()

    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"
    val dashboardStyle by viewModel.dashboardStyle.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .padding(4.dp)
                        )
                        Text(
                            text = "Ads Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    val soundVolume by viewModel.soundVolume.collectAsStateWithLifecycle()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = if (soundVolume == 0f) "🔇" else if (soundVolume < 0.4f) "🔈" else if (soundVolume < 0.7f) "🔉" else "🔊",
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable {
                                    if (soundVolume > 0f) {
                                        viewModel.setSoundVolume(0f)
                                    } else {
                                        viewModel.setSoundVolume(0.5f)
                                    }
                                }
                                .padding(2.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Slider(
                            value = soundVolume,
                            onValueChange = { viewModel.setSoundVolume(it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.width(46.dp).testTag("master_volume_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (appLang == "vi") {
                                    viewModel.setAppLanguage("en")
                                } else {
                                    viewModel.setAppLanguage("vi")
                                }
                            }
                            .testTag("language_switch_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (appLang == "vi") "🇻🇳" else "🇬🇧",
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (appLang == "vi") "VI" else "EN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val nextStyle = when (dashboardStyle) {
                                    "dark_moody" -> "professional"
                                    "professional" -> "gray_minimal"
                                    else -> "dark_moody"
                                }
                                viewModel.setDashboardStyle(nextStyle)
                            }
                            .testTag("dashboard_style_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when (dashboardStyle) {
                                    "professional" -> "👔"
                                    "gray_minimal" -> "🌫️"
                                    else -> "🚀"
                                },
                                fontSize = 13.sp
                            )
                            Text(
                                text = when (dashboardStyle) {
                                    "professional" -> "Agency"
                                    "gray_minimal" -> "Gray"
                                    else -> "Startup"
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { showUserGuideDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("user_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = viewModel.t("Hướng dẫn sử dụng", "User Guide"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = viewModel.t("Reset dữ liệu", "Reset data"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    label = { Text(viewModel.t("Quảng Cáo", "Ads")) },
                    modifier = Modifier.testTag("tab_ads")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }, // Represents simulator/home stats
                    label = { Text(viewModel.t("Mô Phỏng", "Simulator")) },
                    modifier = Modifier.testTag("tab_simulator")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) }, // Stars for challenges
                    label = { Text(viewModel.t("Thử Thách", "Challenges")) },
                    modifier = Modifier.testTag("tab_challenges")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Face, contentDescription = null) }, // Assistant profile icon
                    label = { Text(viewModel.t("AdBot Chat", "AdBot Chat")) },
                    modifier = Modifier.testTag("tab_chat")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Mini AdBot dialogue card for immediate status reactions!
                if (selectedTab == 0) {
                    AdBotTopBanner(text = translateAdBotDialogue(adBotDialogue, isEn), isThinking = isAIBusy, isEn = isEn)
                }

                // Main Tab Display
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> LiveAdsScreen(
                            viewModel = viewModel,
                            impressions = totalImpressions,
                            clicks = totalClicks,
                            revenue = totalRevenue,
                            numWatched = watchedCount,
                            onOpenUserGuide = { showUserGuideDialog = true }
                        )
                        1 -> SimulatorScreen(
                            viewModel = viewModel,
                            impressions = totalImpressions,
                            clicks = totalClicks,
                            revenue = totalRevenue,
                            ctr = totalCtr,
                            cpm = totalCpm
                        )
                        2 -> ChallengesScreen(
                            viewModel = viewModel
                        )
                        3 -> AssistantChatScreen(
                            viewModel = viewModel,
                            isAIBusy = isAIBusy
                        )
                    }
                }
            }

            // Simulated Fullscreen Interstitial dialogue trigger box
            if (isInterstitialVisible) {
                InterstitialAdSimulation(viewModel = viewModel)
            }

            // Simulated Fullscreen Rewarded dialogue trigger box
            if (isRewardedVisible) {
                RewardedAdSimulation(viewModel = viewModel)
            }

            // Secret Dialog
            if (showSecretDialog) {
                SecretUnlockedDialog(viewModel = viewModel)
            }

            // Heist Game Dialog
            if (isHeistGameVisible) {
                HeistGameDialog(viewModel = viewModel)
            }

            // Card Game Dialog
            val isCardGameVisible by viewModel.isCardGameVisible.collectAsStateWithLifecycle()
            if (isCardGameVisible) {
                CardGameDialog(viewModel = viewModel)
            }

            // Clickbait Scare Dialog
            if (showClickbaitScare) {
                ClickbaitScareDialog(viewModel = viewModel)
            }

            // Full Ad Detail Dialog
            if (selectedFullBanner != null) {
                FullAdDetailDialog(
                    banner = selectedFullBanner!!,
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissFullBannerDetail() }
                )
            }

            // Linh Chi Dating Game Dialog
            val isLinhChiDatingVisible by viewModel.isLinhChiDatingVisible.collectAsStateWithLifecycle()
            if (isLinhChiDatingVisible) {
                LinhChiDatingDialog(viewModel = viewModel)
            }

            // Chess Game Dialog
            val isChessGameVisible by viewModel.isChessGameVisible.collectAsStateWithLifecycle()
            if (isChessGameVisible) {
                ChessGameDialog(viewModel = viewModel)
            }

            // User Guide Dialog
            if (showUserGuideDialog) {
                UserGuideDialog(
                    viewModel = viewModel,
                    onDismiss = { showUserGuideDialog = false }
                )
            }

            // Reset Confirmation Dialog
            if (showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmDialog = false },
                    title = {
                        Text(
                            text = viewModel.t("Xác nhận đặt lại", "Confirm Reset"),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = viewModel.t(
                                "Bạn có chắc muốn đặt lại toàn bộ điểm số, lượt xem, doanh thu và lịch sử mô phỏng về trạng thái ban đầu?",
                                "Are you sure you want to reset all scores, impressions, revenue, and simulation history back to zero?"
                            )
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllHistory()
                                showResetConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(viewModel.t("Đặt lại ngay", "Reset Now"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmDialog = false }) {
                            Text(viewModel.t("Hủy", "Cancel"))
                        }
                    }
                )
            }
        }
    }
}

// --- SUB SCREEN 1: Live Ads Simulator Hub ---
@Composable
fun LiveAdsScreen(
    viewModel: AdsViewModel,
    impressions: Int,
    clicks: Int,
    revenue: Double,
    numWatched: Int,
    onOpenUserGuide: () -> Unit = {}
) {
    val mockBanners by viewModel.mockBanners.collectAsStateWithLifecycle()
    val currentBannerIndex by viewModel.currentBannerIndex.collectAsStateWithLifecycle()
    val bannerItem = mockBanners.getOrNull(currentBannerIndex) ?: mockBanners.firstOrNull() ?: com.example.viewmodel.MockBanner("", "", "")
    var showAiSandbox by remember { mutableStateOf(false) }
    val hasUnlockedHeist by viewModel.hasUnlockedHeist.collectAsStateWithLifecycle()
    val hasUnlockedSword by viewModel.hasUnlockedSword.collectAsStateWithLifecycle()
    val hasUnlockedLinhChiDating by viewModel.hasUnlockedLinhChiDating.collectAsStateWithLifecycle(initialValue = false)
    val hasUnlockedChess by viewModel.hasUnlockedChess.collectAsStateWithLifecycle(initialValue = false)
    val linhChiCount by viewModel.linhChiAdCount.collectAsStateWithLifecycle(initialValue = 0)
    val hasDiamondStolen by viewModel.stoleDiamondSuccessfully.collectAsStateWithLifecycle(initialValue = false)
 
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick User Guide Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUserGuide() }
                    .testTag("user_guide_quick_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📖", fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t("Cẩm Nang & Hướng Dẫn Sử Dụng", "User Guide & Knowledge Hub"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = viewModel.t(
                                "Mở khóa game bí mật, các loại quảng cáo & mẹo tối ưu doanh thu.",
                                "Unlock secret games, understand ad formats & revenue tips."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenUserGuide,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("user_guide_read_button")
                    ) {
                        Text(
                            text = viewModel.t("Xem", "Read"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Earnings tracker bar
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.t("Doanh thu tích lũy", "Accumulated Revenue"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$${String.format("%.4f", revenue)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = viewModel.t("Đã xem thực tế", "Actual Watched"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$numWatched Ads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        if (hasUnlockedHeist) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_heist_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E2C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF00FFCC)),
                    onClick = { viewModel.startHeistGame() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF00FFCC).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🕶️", fontSize = 24.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.t("Bản Đồ Đột Nhập Tiệm Vàng", "Gold Shop Heist Map"),
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.t(
                                    "Lỗ hổng bảo mật từ quảng cáo trang sức đã mở ra! Đột kích cuỗm ngọc ròng của tiệm vàng ảo ngay!",
                                    "A security leak in jewelry ads has emerged! Raid and steal virtual gold store gems now!"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Bắt đầu tột đột kích",
                            tint = Color(0xFF00FFCC)
                        )
                    }
                }
            }
        }

        if (hasUnlockedSword) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_swordgame_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E132C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFFD700)),
                    onClick = { viewModel.startCardGame() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🗡️", fontSize = 24.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.t("Khai Quật Kiếm Báu Trận Đấu", "Exhume Battle Treasure Sword"),
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.t(
                                    "Bạn đã xem đủ 2 quảng cáo về Trò Chơi! Rút kiếm để bước vào Đấu trường bài Quái thú với AdBot!",
                                    "You watched 2 game ads! Draw your sword to enter the Beast Arena with AdBot!"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Rút Kiếm Nghênh Chiến",
                            tint = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }

        // Linh Chi dating trigger or progress hint card
        if (hasUnlockedLinhChiDating) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_linhchi_dating_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2C1E26)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFF69B4)),
                    onClick = { viewModel.startDatingGame() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFF69B4).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💖", fontSize = 24.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.t("Hẹn hò cùng Hot Girl Linh Chi", "Dating with Hot Girl Linh Chi"),
                                color = Color(0xFFFF69B4),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.t(
                                    "Đáp ứng 2 điều kiện ẩn đã HOÀN THÀNH! Bấm vào đây ngay để bắt đầu cuộc trò chuyện mật ngọt ngọt ngào cùng Linh Chi nhé!",
                                    "You met both requirements! Tap here to start romantic conversations with Linh Chi right now!"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = viewModel.t("Hẹn Hò Ngay", "Date Now"),
                            tint = Color(0xFFFF69B4)
                        )
                    }
                }
            }
        } else {
            // Let's show a helpful clue card so they keep playing to unlock it!
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_linhchi_hint_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E1E1E).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Gray.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔒", fontSize = 20.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.t("Hẹn hò Hot Girl Linh Chi (Ý nguyện ẩn)", "Dating Hot Girl Linh Chi (Secret Request)"),
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = viewModel.t("Điều kiện bí ẩn để bắt đầu hò hẹn:\n", "Secret requirements to start dating:\n") +
                                       "• " + viewModel.t("Xem quảng cáo về Linh Chi: ", "Watch ads about Linh Chi: ") + "$linhChiCount/2 " + viewModel.t("lần", "times") + " ${if (linhChiCount >= 2) "✅" else "❌"}\n" +
                                       "• " + viewModel.t("Trộm được kim cương từ Tiệm Ngọc: ", "Steal diamond from Jewelry Shop: ") + "${if (hasDiamondStolen) viewModel.t("Đã trộm thành công ✅", "Successfully stolen ✅") else viewModel.t("Chưa trộm thành công ❌", "Not yet stolen ❌")}",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (hasUnlockedChess) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secret_chess_trigger"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F1A2C)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF8B22FF)),
                    onClick = { viewModel.startChessGame() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF8B22FF).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👑", fontSize = 24.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.t("Đại Bản Doanh Cờ Vua: Ads Boss", "Chess Headquarters: Ads Boss"),
                                color = Color(0xFF8B22FF),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = viewModel.t(
                                    "Thử thách tối hậu đã mở! Quyết chiến cờ vua nghẹt thở cùng Ads Boss của bạn!",
                                    "Ultimate challenge is open! Play breath-taking chess against your Ads Boss!"
                                ),
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = viewModel.t("Chơi cờ vua ngay", "Play Chess Now"),
                            tint = Color(0xFF8B22FF)
                        )
                    }
                }
            }
        }

        // Section: Call of action simulated choices
        item {
            Text(
                text = viewModel.t("Kích hoạt mô phỏng quảng cáo", "Trigger Ad Simulation"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interstitial simulation launcher
                Card(
                    onClick = { viewModel.triggerInterstitial() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("launch_interstitial"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column {
                            Text(
                                text = viewModel.t("Ad Xen Kẽ", "Interstitial Ad"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Full-screen (5s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Rewarded simulation launcher
                Card(
                    onClick = { viewModel.triggerRewarded() },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .testTag("launch_rewarded"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                text = viewModel.t("Ad Tặng Thưởng", "Rewarded Ad"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = viewModel.t("Video (10s) + Thưởng lớn", "Video (10s) + Reward"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Section: AI Ad Creative Studio Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_creative_studio_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                onClick = { showAiSandbox = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎨", fontSize = 24.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t("AI Ad Creative Studio (Xưởng Sáng Tạo)", "AI Ad Creative Studio (Sandbox)"),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = viewModel.t(
                                "Dùng siêu mô hình ảnh AI để thiết kế biểu ngữ quảng cáo độc quyền trên mây và đưa vào mô phỏng tương tác ngay lập tức!",
                                "Use AI image models to design custom banner ads and start real-time interaction simulation immediately!"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 15.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = viewModel.t("Mở AI Builder", "Open AI Builder"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Section: Combined Interactive Platform Banner Ad
        item {
            Text(
                text = viewModel.t("Biểu ngữ & Xem trước trên Nền tảng", "Banner Ad & Platform Feed Preview"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            PlatformAdPreviewArea(
                title = viewModel.t(bannerItem.title, bannerItem.titleEn),
                description = viewModel.t(bannerItem.description, bannerItem.descriptionEn),
                actionText = viewModel.t(bannerItem.actionText, bannerItem.actionTextEn),
                imageRes = bannerItem.imageRes,
                viewModel = viewModel,
                aiGeneratedBase64 = bannerItem.aiGeneratedBase64,
                onGenerateAiBackground = { viewModel.generateAdBackgroundWithImagen(bannerItem) },
                onCtaClick = { viewModel.showFullBannerDetail(bannerItem) },
                onSkip = { viewModel.ignoreBanner() }
            )
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showAiSandbox) {
        AiCreativeSandboxDialog(
            viewModel = viewModel,
            onDismiss = { showAiSandbox = false }
        )
    }
}

// --- SUB SCREEN 2: Metrics Simulator & Automation ---
@Composable
fun SimulatorScreen(
    viewModel: AdsViewModel,
    impressions: Int,
    clicks: Int,
    revenue: Double,
    ctr: Double,
    cpm: Double
) {
    val trafficLevel by viewModel.trafficLevel.collectAsStateWithLifecycle()
    val nicheCategory by viewModel.nicheCategory.collectAsStateWithLifecycle()
    val biddingStrategy by viewModel.biddingStrategy.collectAsStateWithLifecycle()
    val adFormat by viewModel.adFormat.collectAsStateWithLifecycle()
    val audienceTargeting by viewModel.audienceTargeting.collectAsStateWithLifecycle()
    val daypartTime by viewModel.daypartTime.collectAsStateWithLifecycle()
    val clickRateSetting by viewModel.clickRateSetting.collectAsStateWithLifecycle()
    val metricsHistory by viewModel.metricsHistory.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics metrics table dashboard
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.t("Thông số mạng lưới (Simulated Ads Network)", "Simulated Ads Network Metrics"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricDashboardItem(
                            title = viewModel.t("Hiển thị (Imp)", "Impressions (Imp)"),
                            value = impressions.toString(),
                            modifier = Modifier.weight(1f),
                            tintColor = MaterialTheme.colorScheme.primary
                        )
                        MetricDashboardItem(
                            title = viewModel.t("Nhấp chuột", "Clicks"),
                            value = clicks.toString(),
                            modifier = Modifier.weight(1f),
                            tintColor = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricDashboardItem(
                            title = viewModel.t("Tỉ lệ Click (CTR)", "Click Ratio (CTR)"),
                            value = String.format("%.2f%%", ctr),
                            modifier = Modifier.weight(1f),
                            tintColor = MaterialTheme.colorScheme.tertiary
                        )
                        MetricDashboardItem(
                            title = viewModel.t("Giá mỗi 1M (CPM)", "Cost Per Mille (CPM)"),
                            value = String.format("$%.2f", cpm),
                            modifier = Modifier.weight(1f),
                            tintColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Simulation parameters controller
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.t("Tham số mô phỏng", "Simulation Parameters"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Param 1: Traffic Scale
                    Text(
                        text = viewModel.t("Quy mô lưu lượng truy cập (Traffic Volume): ", "Traffic Volume: ") + trafficLevel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            FilterChip(
                                selected = trafficLevel == level,
                                onClick = { viewModel.setTrafficLevel(level) },
                                label = { Text(viewModel.t(level, level)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 2: Category Niche
                    Text(
                        text = viewModel.t("Ngành hàng quảng cáo (Niche): ", "Niche Industry: ") + nicheCategory,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Gaming", "Fashion", "Technology", "Finance").forEach { category ->
                            FilterChip(
                                selected = nicheCategory == category,
                                onClick = { viewModel.setNicheCategory(category) },
                                label = {
                                    Text(
                                        text = category,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 2b: Bidding Strategy
                    Text(
                        text = viewModel.t("Chiến lược đấu thầu (Bidding): ", "Bidding Strategy: ") + viewModel.t(biddingStrategy, when(biddingStrategy) {
                            "Tối đa nhấp (CTR)" -> "Max Clicks (CTR)"
                            "Lợi nhuận (CPM)" -> "Max Revenue (CPM)"
                            "Chi phí thấp" -> "Low Cost"
                            else -> biddingStrategy
                        }),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Tối đa nhấp (CTR)", "Lợi nhuận (CPM)", "Chi phí thấp").forEach { strategy ->
                            FilterChip(
                                selected = biddingStrategy == strategy,
                                onClick = { viewModel.setBiddingStrategy(strategy) },
                                label = {
                                    Text(
                                        text = viewModel.t(strategy, when(strategy) {
                                            "Tối đa nhấp (CTR)" -> "Max Clicks (CTR)"
                                            "Lợi nhuận (CPM)" -> "Max Revenue (CPM)"
                                            "Chi phí thấp" -> "Low Cost"
                                            else -> strategy
                                        }),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 2c: Ad Format Selection
                    Text(
                        text = viewModel.t("Định dạng Ads (Format): ", "Ad Format: ") + adFormat,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Standard Banner", "Native Hub", "Video Award", "Popunder Drop").forEach { format ->
                            FilterChip(
                                selected = adFormat == format,
                                onClick = { viewModel.setAdFormat(format) },
                                label = {
                                    Text(
                                        text = format,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 9.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 2d: Target Audience Selection
                    Text(
                        text = viewModel.t("Đối tượng nhắm tới (Audience Target): ", "Audience Targeting: ") + viewModel.t(audienceTargeting, when(audienceTargeting) {
                            "Tất cả" -> "All"
                            "Công nghệ" -> "Technology"
                            "Mẹ & Bé" -> "Mom & Baby"
                            "Người cao tuổi" -> "Seniors"
                            else -> audienceTargeting
                        }),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Tất cả", "Công nghệ", "Mẹ & Bé", "Người cao tuổi").forEach { targeting ->
                            FilterChip(
                                selected = audienceTargeting == targeting,
                                onClick = { viewModel.setAudienceTargeting(targeting) },
                                label = {
                                    Text(
                                        text = viewModel.t(targeting, when(targeting) {
                                            "Tất cả" -> "All"
                                            "Công nghệ" -> "Technology"
                                            "Mẹ & Bé" -> "Mom & Baby"
                                            "Người cao tuổi" -> "Seniors"
                                            else -> targeting
                                        }),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 2e: Dayparting Schedule Selection
                    Text(
                        text = viewModel.t("Khung giờ chạy (Dayparting): ", "Dayparting Schedule: ") + viewModel.t(daypartTime, when(daypartTime) {
                            "Giờ hành chính" -> "Working Hours"
                            "Giờ vàng (Tối)" -> "Golden Hours"
                            "Giờ thấp điểm" -> "Off-Peak Hours"
                            else -> daypartTime
                        }),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Giờ hành chính", "Giờ vàng (Tối)", "Giờ thấp điểm").forEach { time ->
                            FilterChip(
                                selected = daypartTime == time,
                                onClick = { viewModel.setDaypartTime(time) },
                                label = {
                                    Text(
                                        text = viewModel.t(time, when(time) {
                                            "Giờ hành chính" -> "Working Hours"
                                            "Giờ vàng (Tối)" -> "Golden Hours"
                                            "Giờ thấp điểm" -> "Off-Peak Hours"
                                            else -> time
                                        }),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 10.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Param 3: Target CTR slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = viewModel.t("Tỉ lệ nhấp chuột mong muốn", "Target Click-Through Rate"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format("%.2f%%", clickRateSetting),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = clickRateSetting,
                        onValueChange = { viewModel.setClickRateSetting(it) },
                        valueRange = 0.5f..15.0f,
                        modifier = Modifier.testTag("ctr_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Run Button
                    Button(
                        onClick = { viewModel.runBulkSimulation() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_simulation_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.t("CHẠY MÔ PHỎNG 24H TRAFFIC", "RUN 24H TRAFFIC SIMULATION"),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Dynamic Line Chart Card (Canvas visualization)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = viewModel.t("Doanh thu qua các lần mô phỏng ($)", "Revenue Across Simulations ($)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (metricsHistory.size < 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = viewModel.t(
                                    "Thiếu dữ liệu vẽ biểu đồ. Nhấn nút mô phỏng ở trên vài lần nhé!",
                                    "Insufficient data for chart. Run simulation above a few times!"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Drawing line path
                        RevenueLineChart(metrics = metricsHistory.take(12).reversed())
                    }
                }
            }
        }

        // Interactive Sarcastic AdBot Metrics Analyst Chat (Collapsible & Compact)
        item {
            MetricsChatComponent(viewModel = viewModel)
        }

        // Metrics History Table Logs
        item {
            Text(
                text = viewModel.t("Lịch sử mô phỏng chi tiết", "Detailed Simulation History"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (metricsHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.t(
                            "Nơi đây trống rỗng... Hãy bấm mô phỏng 24h hoặc nhấp banner để sinh doanh thu.",
                            "Empty here... Run 24h simulation or tap a banner to generate revenue."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        } else {
            items(metricsHistory, key = { it.id }) { log ->
                MetricHistoryRow(log = log)
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- SUB SCREEN 3: AdBot AI Chat Experience ---
@Composable
fun AssistantChatScreen(
    viewModel: AdsViewModel,
    isAIBusy: Boolean
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val lastDialogue by viewModel.adBotDialogue.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    val listState = rememberLazyListState()

    // Smooth scroll as conversation grows
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Initial Robot welcome prompt in chat
            item {
                AdBotChatBubble(
                    message = viewModel.t(
                        "Chào mừng quý khách rảnh rỗi tuyệt hảo! Tôi sinh ra là để mỉa mai bạn và kiếm cơm từ quảng cáo. Hôm nay ước mơ gì chưa hay vào đây coi video rác thế?",
                        "Welcome, wonderful idle visitor! I was born to roast you and profit off ads. Any big dreams today, or did you just come here to watch trash video ads?"
                    ),
                    isBot = true
                )
            }

            items(chatHistory, key = { it.id }) { chat ->
                val displayMsg = when (chat.sender) {
                    "adbot" -> translateAdBotDialogue(chat.message, isEn)
                    "system" -> translateSystemMessage(chat.message, isEn)
                    else -> chat.message
                }
                AdBotChatBubble(
                    message = displayMsg,
                    isBot = chat.sender == "adbot"
                )
            }

            if (isAIBusy) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .align(Alignment.Bottom),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 2.dp,
                                bottomEnd = 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TypingIndicator(dotColor = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = viewModel.t("AdBot đang gõ...", "AdBot is typing..."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Easy-to-click quick chit-chat starters with AdBot!
        val chatSuggestions = listOf(
            viewModel.t("Bóc phốt idol Linh Chi giùm cái 🤫", "Expose idol Linh Chi for me 🤫"),
            viewModel.t("Game Vua Rác Rưởi 3D là gì? 🗑️", "What is Trash King 3D game? 🗑️"),
            viewModel.t("Nghèo quá, hack tiền được không? 💸", "So poor, can I hack some cash? 💸"),
            viewModel.t("Mẹo xem ads triệu đô vĩnh viễn 👑", "Secrets to viewing million-dollar ads forever 👑")
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chatSuggestions.forEach { txt ->
                Surface(
                    onClick = { viewModel.setInputText(txt) },
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = txt,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Input and Send Bar card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = { Text(viewModel.t("Hỏi AdBot điều mỉa xoa...", "Ask AdBot something sarcastic..."), fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = inputText.trim().isNotEmpty() && !isAIBusy,
                    modifier = Modifier
                        .testTag("send_button")
                        .minimumInteractiveComponentSize(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gửi tin nhắn"
                    )
                }
            }
        }
    }
}

@Composable
fun MetricsChatComponent(viewModel: AdsViewModel) {
    val chatHistory by viewModel.metricsChatHistory.collectAsStateWithLifecycle()
    val inputText by viewModel.metricsInputText.collectAsStateWithLifecycle()
    val isBusy by viewModel.isMetricsAIBusy.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    var isExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Scroll to bottom when history updates
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("metrics_roaster_card")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Header Row (Clickable to toggle expand/collapse)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = viewModel.t("AdBot Phân Tích Cà Khịa 📊", "AdBot Sarcastic Analyst 📊"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (chatHistory.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${chatHistory.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isExpanded) {
                                viewModel.t("Hỏi hoặc click mẫu để nghe AdBot 'sấy' số liệu", "Ask or tap templates to roast your stats")
                            } else {
                                viewModel.t("Nhấn để mở rộng trò chuyện với AdBot...", "Tap to open AdBot roaster chat...")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpanded) {
                        IconButton(
                            onClick = { viewModel.clearMetricsChat() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expanded Chat Section
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    // Chat area (compact bounded height: 130dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (chatHistory.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = viewModel.t("Chưa có tin nhắn. Chạm một gợi ý bên dưới để thử!", "No messages yet. Tap a suggestion below!"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            chatHistory.forEach { chat ->
                                AdBotChatBubble(
                                    message = chat.message,
                                    isBot = chat.sender == "adbot"
                                )
                            }

                            if (isBusy) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .align(Alignment.Bottom),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = 2.dp,
                                            bottomEnd = 14.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.widthIn(max = 240.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TypingIndicator(dotColor = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = viewModel.t("AdBot đang soi số liệu...", "AdBot is auditing your stats..."),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Suggestion chips
                    val suggestionChips = listOf(
                        viewModel.t("Cà khịa CTR! 🎯", "Roast my CTR! 🎯"),
                        viewModel.t("Doanh thu bèo bọt 💸", "Tragic revenue 💸"),
                        viewModel.t("Sao không ai click? 🤦‍♂️", "Why no clicks? 🤦‍♂️"),
                        viewModel.t("Có sạt nghiệp không? 💣", "Will I go broke? 💣")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestionChips.forEach { chipText ->
                            Surface(
                                onClick = { viewModel.sendMetricsMessage(chipText) },
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = chipText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chat text input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { viewModel.setMetricsInputText(it) },
                            placeholder = {
                                Text(
                                    text = viewModel.t("Hỏi AdBot về số liệu...", "Ask AdBot about stats..."),
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp)
                                .testTag("metrics_chat_input"),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { viewModel.sendMetricsMessage() })
                        )

                        IconButton(
                            onClick = { viewModel.sendMetricsMessage() },
                            enabled = inputText.trim().isNotEmpty() && !isBusy,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .testTag("metrics_chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.trim().isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- COMPOSE CORE COMPONENTS / GRAPHICS DRAWINGS ---

@Composable
fun AdBotTopBanner(text: String, isThinking: Boolean, isEn: Boolean) {
    var isCollapsed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (isEn) "AdBot Assistant" else "AdBot Trợ lý",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isThinking) {
                        Text(
                            text = if (isEn) "Mocking..." else "Đang phản hồi...",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Online",
                            fontSize = 10.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                IconButton(
                    onClick = { isCollapsed = !isCollapsed },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = !isCollapsed) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    dotColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_indicator")
    
    val d1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 0
                -6f at 150
                0f at 300
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "d1Y"
    )
    val d2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 100
                -6f at 250
                0f at 400
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "d2Y"
    )
    val d3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0f at 200
                -6f at 350
                0f at 500
                0f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "d3Y"
    )

    Row(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(translationY = d1Y)
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(translationY = d2Y)
                .background(dotColor, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .graphicsLayer(translationY = d3Y)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
fun AdBotChatBubble(message: String, isBot: Boolean) {
    val animAlpha = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(12f) }

    LaunchedEffect(message) {
        launch {
            animAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            animOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(
                alpha = animAlpha.value,
                translationY = animOffsetY.value
            ),
        horizontalArrangement = if (isBot) Arrangement.Start else Arrangement.End
    ) {
        if (isBot) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isBot) 2.dp else 16.dp,
                bottomEnd = if (isBot) 16.dp else 2.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isBot) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isBot) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

class SafeDrawablePainter(private val drawable: android.graphics.drawable.Drawable) : androidx.compose.ui.graphics.painter.Painter() {
    override val intrinsicSize: androidx.compose.ui.geometry.Size
        get() = androidx.compose.ui.geometry.Size(
            if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.toFloat() else 100f,
            if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.toFloat() else 100f
        )

    override fun androidx.compose.ui.graphics.drawscope.DrawScope.onDraw() {
        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
fun safePainterResource(id: Int?, fallbackResId: Int = com.example.R.drawable.img_clickbait_girl): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val rootFallback = com.example.R.drawable.img_clickbait_girl
    val launcherFallback = com.example.R.drawable.ic_launcher_background

    val candidates = remember(id, fallbackResId) {
        val list = mutableListOf<Int>()
        if (id != null && id != 0) list.add(id)
        if (fallbackResId != 0 && fallbackResId != id) list.add(fallbackResId)
        if (rootFallback != id && rootFallback != fallbackResId) list.add(rootFallback)
        list.add(launcherFallback)
        list.distinct()
    }

    for (candId in candidates) {
        val drawable = remember(candId) {
            try {
                androidx.core.content.res.ResourcesCompat.getDrawable(context.resources, candId, context.theme)
            } catch (t: Throwable) {
                null
            }
        }
        if (drawable != null) {
            return remember(drawable) { SafeDrawablePainter(drawable) }
        }
    }

    // Ultimate fallback if absolutely everything fails
    return remember {
        object : androidx.compose.ui.graphics.painter.Painter() {
            override val intrinsicSize: androidx.compose.ui.geometry.Size
                get() = androidx.compose.ui.geometry.Size(100f, 100f)

            override fun androidx.compose.ui.graphics.drawscope.DrawScope.onDraw() {
                drawRect(androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }
}

@Composable
fun InteractiveMockBanner(
    banner: MockBanner,
    viewModel: AdsViewModel,
    onClick: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("interactive_banner"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row header: mock tiny ad badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (banner.isUserCreated) {
                        Text(
                            text = viewModel.t("BẢN THIẾT KẾ CỦA BẠN 🎨", "YOUR AD CREATIVE 🎨"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF03001C),
                            modifier = Modifier
                                .background(Color(0xFF00FFCC), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.t("Đã lưu vào bộ nhớ cục bộ", "Saved to local storage"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = viewModel.t("QC LIÊN QUAN", "SPONSORED AD"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = viewModel.t("Khuyên dùng dựa trên độ rảnh", "Recommended based on activity"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("skip_banner_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = viewModel.t("Bỏ qua", "Skip"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Banner Title Info
            Text(
                text = viewModel.t(banner.title, banner.titleEn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = viewModel.t(banner.description, banner.descriptionEn),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (banner.imageRes != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick() }
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = safePainterResource(id = banner.imageRes),
                        contentDescription = viewModel.t(banner.title, banner.titleEn),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big fake CTAs
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("click_banner_btn")
            ) {
                Text(
                    text = viewModel.t(banner.actionText, banner.actionTextEn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun PlatformAdPreviewArea(
    title: String,
    description: String,
    actionText: String,
    imageRes: Int?,
    viewModel: AdsViewModel,
    modifier: Modifier = Modifier,
    aiGeneratedBase64: String? = null,
    onGenerateAiBackground: (() -> Unit)? = null,
    onCtaClick: () -> Unit = {},
    onSkip: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ad_preview_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Mock platform header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.t("Nền tảng Liên kết AdBot", "AdBot Connect Platform"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = viewModel.t("Được tài trợ ảo • Xem trước", "Sponsored Sim • Ad Preview"),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Partner",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "FEED AD",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (onSkip != null) {
                        IconButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("skip_banner_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = viewModel.t("Tải lại quảng cáo khác", "Reload another ad"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("ad_preview_description")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main image container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .testTag("ad_preview_image_container"),
                contentAlignment = Alignment.Center
            ) {
                val aiBitmap = rememberBase64Image(aiGeneratedBase64)
                if (aiBitmap != null) {
                    Image(
                        bitmap = aiBitmap,
                        contentDescription = "AI Generated Ad Image",
                        modifier = Modifier.fillMaxSize().testTag("ad_preview_image"),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageRes != null) {
                    Image(
                        painter = safePainterResource(id = imageRes),
                        contentDescription = "Ad Image",
                        modifier = Modifier.fillMaxSize().testTag("ad_preview_image"),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = viewModel.t("Chưa có ảnh quảng cáo", "No ad image selected"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action buttons inside preview area card
            if (onGenerateAiBackground != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val isGeneratingImage by viewModel.isGeneratingAdImage.collectAsStateWithLifecycle()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isGeneratingImage) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = viewModel.t("⚡ Imagen đang vẽ ảnh nền chủ đề...", "⚡ Imagen is generating background for topic..."),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onGenerateAiBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("generate_imagen_background_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = viewModel.t("✨ Tạo Nền Động (Imagen Model)", "✨ Generate Dynamic Background (Imagen)"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Banner CTA Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("ad_preview_headline")
                    )
                    Text(
                        text = "adbot-feed-simulator.io",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = onCtaClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("ad_preview_cta_button")
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun rememberBase64Image(base64Str: String?): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(base64Str) {
        if (base64Str.isNullOrBlank()) return@remember null
        try {
            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun InterstitialAdSimulation(viewModel: AdsViewModel) {
    val countdown by viewModel.interstitialCountdown.collectAsStateWithLifecycle()
    val currentInterstitialIndex by viewModel.currentInterstitialIndex.collectAsStateWithLifecycle()
    
    // Safety check for empty or out of bounds indices
    val ad = if (viewModel.interstitialAds.isNotEmpty() && currentInterstitialIndex in viewModel.interstitialAds.indices) {
        viewModel.interstitialAds[currentInterstitialIndex]
    } else {
        InterstitialAdItem(
            category = "⚔️ SIÊU PHẨM KIẾM HIỆP RPG 2026",
            title = "Vua Kiếm Hiệp RPG 999D",
            description = "Tặng ngay 9,999,999 KNB và Code 'BaoGiaoTu' khi cài đặt trong lượt này!",
            actionText = "TẢI GAME MIỄN PHÍ",
            themeColorHex = "#FF00FFCC"
        )
    }

    val themeColor = remember(ad.themeColorHex) {
        try {
            Color(android.graphics.Color.parseColor(ad.themeColorHex))
        } catch (e: Exception) {
            Color(0xFF00FFCC)
        }
    }

    // Let's create an infinite transition to pulse or flash warning highlights for dramatic effect!
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val warningRedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warningRedAlpha"
    )

    Dialog(
        onDismissRequest = { /* forces user to watch or skip properly based on button triggers */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Dynamic subtle background flash for dangerous malware simulation
            if (ad.iconArt == "🔥") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Red.copy(alpha = 0.15f * warningRedAlpha), Color.Transparent),
                                radius = 1000f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top real close/skip button row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    if (countdown > 0) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = viewModel.t("Có thể bỏ qua sau ${countdown}s", "Can skip in ${countdown}s"),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.closeInterstitial() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .testTag("close_interstitial_btn")
                        ) {
                            Text(viewModel.t("Bỏ qua quảng cáo", "Skip ad"), fontWeight = FontWeight.ExtraBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Close, contentDescription = viewModel.t("Đóng", "Close"), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tricky Interstitial Ad Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F1B)),
                    border = BorderStroke(2.dp, themeColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Tiny Deceptive/Fake Close Button in the upper right of the card itself!
                        if (ad.isDeceptiveClose) {
                            TextButton(
                                onClick = { viewModel.clickInterstitialAd(ad, isFakeClose = true) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = viewModel.t(ad.deceptiveCloseText, ad.deceptiveCloseTextEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Fake Close Icon",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        // Main ad content
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Header Category
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (ad.iconArt == "🔥") { // blinking dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Red.copy(alpha = warningRedAlpha))
                                    )
                                }
                                Text(
                                    text = viewModel.t(ad.category, ad.categoryEn),
                                    fontSize = 12.sp,
                                    color = themeColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Dynamic retro Emoji graphics box
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xFF24243C), Color(0xFF141424))
                                        )
                                    )
                                    .border(2.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ad.iconArt,
                                    fontSize = 52.sp,
                                    modifier = Modifier.graphicsLayer(
                                        scaleX = if (ad.iconArt == "🔥" || ad.iconArt == "💸") 1f + (warningRedAlpha * 0.1f) else 1f,
                                        scaleY = if (ad.iconArt == "🔥" || ad.iconArt == "💸") 1f + (warningRedAlpha * 0.1f) else 1f
                                    )
                                )
                            }

                            // Ad texts
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = viewModel.t(ad.title, ad.titleEn),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = viewModel.t(ad.description, ad.descriptionEn),
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }

                            // Big Fake/Real CTA Action Button
                            Button(
                                onClick = { viewModel.clickInterstitialAd(ad, isFakeClose = false) },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = viewModel.t(ad.actionText, ad.actionTextEn),
                                    color = if (ad.isLightTheme || ad.themeColorHex == "#FF00FFCC" || ad.themeColorHex == "#FFFFD700") Color.Black else Color.White,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (ad.descriptionAddon.isNotEmpty()) {
                                Text(
                                    text = viewModel.t(ad.descriptionAddon, ad.descriptionAddonEn),
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = viewModel.t("💡 Hệ thống mô phỏng bẫy tối ưu hóa tỉ lệ CTR quảng cáo", "💡 Simulation system for deceptive ads to optimize CTR"),
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun RewardedAdSimulation(viewModel: AdsViewModel) {
    val countdown by viewModel.rewardedCountdown.collectAsStateWithLifecycle()
    val rewardClaimed by viewModel.rewardClaimed.collectAsStateWithLifecycle()
    val currentAdTitleVi by viewModel.currentRewardedAdTitleVi.collectAsStateWithLifecycle()
    val currentAdTitleEn by viewModel.currentRewardedAdTitleEn.collectAsStateWithLifecycle()
    val currentAdTitle = viewModel.t(currentAdTitleVi, currentAdTitleEn)
    val isGameAd by viewModel.currentRewardedAdIsGame.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = { /* forces watching completion */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top controls box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!rewardClaimed) {
                        Text(
                            text = viewModel.t("Thưởng sau ${countdown}s", "Reward in ${countdown}s"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { viewModel.cancelRewarded() },
                            modifier = Modifier.testTag("close_rewarded_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = viewModel.t("Đóng", "Close"), tint = Color.White)
                        }
                    } else {
                        Text(
                            text = viewModel.t("Đã tích lũy quà tặng!", "Reward accumulated!"),
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // High graphics cartoon card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(2.dp, if (rewardClaimed) Color(0xFFFFD700) else Color(0xFF9C27B0)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171324))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = viewModel.t("VIDEO KHUYẾN MÃI TẶNG THƯỞNG", "REWARDED PROMO VIDEO"),
                                fontSize = 11.sp,
                                color = if (rewardClaimed) Color(0xFFFFD700) else Color(0xFFB56EE4),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Shiny gold coin simulation rotation
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C224E))
                                    .border(
                                        2.dp,
                                        if (rewardClaimed) Color(0xFFFFD700) else Color.White.copy(
                                            alpha = 0.1f
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star, // represent shiny star/reward coin
                                    contentDescription = null,
                                    tint = if (rewardClaimed) Color(0xFFFFD700) else Color(0xFFB56EE4),
                                    modifier = Modifier.size(70.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentAdTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (rewardClaimed) {
                                    if (isGameAd) {
                                        viewModel.t(
                                            "Chúc mừng! Bạn đã hoàn thành xem quảng cáo game và nhận được tích lũy báu vật!",
                                            "Congratulations! You completed watching the game ad and earned treasure accumulation!"
                                        )
                                    } else {
                                        viewModel.t(
                                            "Quảng cáo đã xem xong! Nhấn nút nhận quà để cộng tiền ảo.",
                                            "Ad completed! Click claim reward to add virtual currency."
                                        )
                                    }
                                } else {
                                    if (isGameAd) {
                                        viewModel.t(
                                            "Bấm nút đóng hoặc bỏ qua sẽ làm mất cơ hội mở khóa kiếm báu. Hãy xem hết để nhận phần thưởng!",
                                            "Closing or skipping will forfeit your chance to unlock treasures. Watch to the end to get rewarded!"
                                        )
                                    } else {
                                        viewModel.t(
                                            "Xem quảng cáo chất lượng cao từ các nhà tài trợ chính thức.",
                                            "Watch high-quality promotional content from official sponsors."
                                        )
                                    }
                                },
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                maxLines = 3
                            )
                        }

                        if (rewardClaimed) {
                            Button(
                                onClick = { viewModel.claimReward() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("claim_reward_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.t("NHẬN LIỀY TAY THƯỞNG LỚN", "CLAIM GRANDEUR REWARD NOW"),
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        } else {
                            // Circular video progress counting bar
                            LinearProgressIndicator(
                                progress = { (10f - countdown.toFloat()) / 10f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = Color(0xFF9C27B0),
                                trackColor = Color(0xFF2C224E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MetricDashboardItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    tintColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tintColor
            )
        }
    }
}

@Composable
fun MetricHistoryRow(log: SimulationMetric) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatEpoch(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("+ $%.4f", log.revenue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (log.revenue > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${log.impressions} Imp",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Custom Line-Chart Drawing component using Compose Canvas drawing
@Composable
fun RevenueLineChart(metrics: List<SimulationMetric>) {
    val prices = remember(metrics) { metrics.map { it.revenue } }
    val maxPrice = remember(prices) { prices.maxOrNull()?.toFloat()?.coerceAtLeast(0.01f) ?: 1f }
    val minPrice = remember(prices) { prices.minOrNull()?.toFloat() ?: 0f }

    val strokeColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        val width = size.width
        val height = size.height
        val sizeCount = metrics.size

        // Horizontal gridlines
        val gridCount = 3
        for (i in 0..gridCount) {
            val y = height * i / gridCount
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Draw line trace path
        val points = mutableListOf<Offset>()
        for (i in metrics.indices) {
            val x = if (sizeCount > 1) width * i / (sizeCount - 1) else width / 2f
            val value = metrics[i].revenue.toFloat()
            val normalizedY = if (maxPrice > minPrice) {
                (value - minPrice) / (maxPrice - minPrice)
            } else {
                0.5f
            }
            val y = height - (normalizedY * height * 0.8f) - (height * 0.1f) // leaves nice margins
            points.add(Offset(x, y))
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val pCurrent = points[i]
                    val pPrev = points[i - 1]
                    // Bezier smoothing control curves
                    val conX1 = (pPrev.x + pCurrent.x) / 2
                    val conY1 = pPrev.y
                    val conX2 = (pPrev.x + pCurrent.x) / 2
                    val conY2 = pCurrent.y
                    cubicTo(conX1, conY1, conX2, conY2, pCurrent.x, pCurrent.y)
                }
            }
        }

        // Draw dynamic modern vertical gradient fill under the line chart
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val pCurrent = points[i]
                    val pPrev = points[i - 1]
                    val conX1 = (pPrev.x + pCurrent.x) / 2
                    val conY1 = pPrev.y
                    val conX2 = (pPrev.x + pCurrent.x) / 2
                    val conY2 = pCurrent.y
                    cubicTo(conX1, conY1, conX2, conY2, pCurrent.x, pCurrent.y)
                }
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        strokeColor.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    startY = points.map { it.y }.minOrNull() ?: 0f,
                    endY = height
                )
            )
        }

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 6f)
        )

        // Draw circles on top of coordinate vertices
        points.forEach { point ->
            drawCircle(
                color = strokeColor,
                radius = 10f,
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 5f,
                center = point
            )
        }
    }
}

// Epoch human formatter helper
fun formatEpoch(timeMs: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timeMs))
}

@Composable
fun SecretUnlockedDialog(viewModel: AdsViewModel) {
    Dialog(
        onDismissRequest = { viewModel.dismissSecretDialog() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141424)),
            border = BorderStroke(2.dp, Color(0xFF00FFCC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("secret_unlocked_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = viewModel.t("🕵️‍♂️ PHÁT HIỆN BÍ MẬT!", "🕵️‍♂️ SECRET DISCOVERED!"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00FFCC)
                )
                
                Text(
                    text = viewModel.t(
                        "Bạn vừa xem quảng cáo Trang sức 2 lần! Mạng lưới của các nhà tài trợ tiệm vàng vô tình lộ một cổng đột nhập bí mật. Bạn có muốn dấn thân làm Siêu Trộm đột nhập kho báu khổng lồ của họ không?",
                        "You just watched Jewelry ads twice! The sponsors' network accidentally exposed a secret entrance to their gold vault. Do you want to embark as a Master Thief and heist their massive treasure?"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissSecretDialog() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(viewModel.t("Bỏ qua", "Ignore"))
                    }

                    Button(
                        onClick = { viewModel.startHeistGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.weight(1.2f).testTag("accept_heist_btn")
                    ) {
                        Text(
                            text = viewModel.t("Bắt đầu Đột Nhập 🕶️", "Start Heist 🕶️"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

fun translateLinhChiText(text: String, isEnglish: Boolean): String {
    if (!isEnglish) return text
    var result = text
    val translationMap = mapOf(
        "Chào cậu! Cuối cùng cậu cũng xuất hiện rồi... Tớ chờ cậu suốt từ hồi bấm cái quảng cáo ấy đấy. Cậu thấy tớ ngoài đời có xinh bằng trên live stream không?" to
            "Hi there! You've finally shown up... I've been waiting for you ever since you clicked that ad. Do you think I look as pretty in real life as on stream?",
        "Mà nè, tớ đói bụng quá rồi. Chúng mình đi ăn gì đây ta? Cậu chọn chỗ đi nhé!" to
            "By the way, I'm starving. What should we eat? You choose the place!",
        "Ăn xong rồi, tớ thấy cậu đeo cái túi xách to đùng... Hình như cậu có chuẩn bị quà tặng gì cho tớ đúng không?" to
            "We're done eating. I notice you're carrying a huge bag... Did you prepare a gift for me by any chance?",
        "Chúng mình bên nhau vui vẻ ghê... Cậu có muốn nói điều gì thật đặc biệt với tớ trước khi kết thúc buổi hẹn hò lãng mạn này không?" to
            "We had such a lovely time together... Is there something special you'd like to say to me before our romantic date ends?",
        "💬 Linh Chi nói tiếp:" to "💬 Linh Chi continues:",
        "Hẹn hò lại 💘" to "Date again 💘",
        "Đóng 🚪" to "Close 🚪",
        
        // Step 0 Options
        "Xinh hơn nhiều ấy chứ! Ngoài đời cậu lung linh, mắt thì tròn xoe lấp lánh." to
            "Much prettier! You're radiant in person, with such beautiful sparkling eyes.",
        "Cũng thường thôi, chắc do cậu xài filter 7x7=49 lớp đúng không?" to
            "You look average, probably because you use 7x7=49 layers of filters, right?",
        "Cứ xem như cậu tạm ổn, tớ đến đây chỉ để làm nhiệm vụ thôi!" to
            "Let's just say you're okay, I only came here to complete the quest!",

        // Step 0 Responses
        "Hihi, cậu khéo mồm quá đi! Làm tớ ngại phát ngượng luôn nè... App simulator này hóa ra cũng mang lại điều ngọt ngào đấy chứ!" to
            "Hihi, you are so sweet-talking! You make me blush... This app simulator turns out to bring some sweet things after all!",
        "Hứ! Cậu nói gì đáng ghét thế hả? Tớ là nhan sắc tự nhiên 100% không dao kéo đó nha! Nhưng thôi, tha thứ cho sự vụng về của cậu đấy." to
            "Hmph! What are you saying? So annoying! I am 100% natural beauty without any plastic surgery! But fine, I will forgive your clumsiness.",
        "Ơ cậu này hay nhỉ! Coi tớ là công cụ làm nhiệm vụ à? Thôi được rồi, ăn nói nhạt nhẽo thế để xem cậu thể hiện thế nào tiếp theo." to
            "Oh, look at you! Treating me like a quest tool? Fine, let's see how you perform next with such boring talking.",

        // Step 1 Options
        "Dắt cậu đi ăn buffet lẩu cua hoàng đế sang chảnh bậc nhất thành phố! 🦀" to
            "Take you to the fanciest king crab hotpot buffet in town! 🦀",
        "Ăn mì tôm xúc xích vỉa hè cho ấm cúng tiết kiệm, tớ dồn tiền nạp quảng cáo hết rồi." to
            "Let's share instant noodles with sausages on the sidewalk for warmth and savings, I've spent all my money on ads.",
        "Hẹn hò hít thở không khí trong lành nha cậu, tớ làm gì có tiền!" to
            "Let's go on a date just breathing fresh air, I don't have any money!",

        // Step 1 Responses
        "Oaaa! Thật á? Cậu chu đáo và ga-lăng số một luôn! Đi ăn lẩu cua thôi nào, ấm bụng quá!" to
            "Ooh! Really? You are the most thoughtful and gallant guy ever! Let's go eat king crab hotpot, so warming!",
        "Ăn mì tôm á? Cậu kiệt sỉ vừa thôi chứ, hẹn hò đầu dắt con gái đi húp mì gói vỉa hè... Nhưng thôi, ăn mì gói cũng có hương vị riêng, tớ tạm chấp nhận." to
            "Instant noodles? You're so stingy, taking a girl to eat sidewalk instant noodles on the first date... but fine, instant noodles have their own flavor, I'll accept for now.",
        "Hic cậu đùa nhạt nhẽo ghê á... Coi như tớ bao cậu bữa nay vậy, nhưng độ ga-lăng của cậu bị trừ điểm nặng rồi nha!" to
            "Ugh, your joke is so dry... I'll pay for us today, but your gallantry score is severely penalized!",

        // Step 2 Options
        "Tặng cậu viên Kim Cương Đen Thái Dương Cực Đại 💎 tớ vừa trộm được ở Tiệm Ngọc!" to
            "Presenting to you the Ultimate Black Sun Diamond 💎 I just stole from the Gold Shop!",
        "Tặng cậu một bông hoa hồng đỏ thắm tượng trưng cho tình yêu chân thành của tớ. 🌹" to
            "Presenting to you a bright red rose symbolizing my sincere love. 🌹",
        "Quà cáp gì tầm này cậu ơi, tớ có mặt ở đây chính là món quà lớn nhất cho cậu rồi!" to
            "What gifts are you talking about, my presence here is already the greatest gift for you!",

        // Step 2 Responses
        "Trời ơi!! Viên kim cương đen lấp lánh cực quý hiếm này á?? Cậu chịu chơi quá! Cho dù là hàng trộm ảo đi nữa thì tớ vẫn sướng rơn cả người!" to
            "Oh my gosh!! This extremely rare sparkling black diamond?? You are so generous! Even if it is virtual stolen good, I am still thrilled!",
        "Ôi dễ thương quá... Một bông hoa hồng đơn giản nhưng chứa đựng cả bầu trời chân thành ngọt ngào. Tớ rất trân trọng luôn!" to
            "Oh how cute... A simple rose but containing a whole sky of sweet sincerity. I really appreciate it!",
        "Tự tin thái quá là tự tin vô duyên đó nha cậu! Thôi mệt cậu ghê, chẳng chu đáo tí nào cả." to
            "Too much confidence is just plain obnoxious! Ugh, you're exhausting and not thoughtful at all.",

        // Step 3 Options
        "Linh Chi ơi, làm bạn gái tớ nhé! Tớ hứa sẽ xem thêm 1000 quảng cáo để nuôi cậu hạnh phúc!" to
            "Linh Chi, will you be my girlfriend? I promise to watch 1000 more ads to keep you happy and rich!",
        "Thực ra tớ hẹn hò cậu chỉ vì muốn lấy cái Huy hiệu và 400 XP thôi, chào nha!" to
            "Actually, I only dated you because I wanted to get the badge and 400 XP, bye!",
        "Cậu rất đặc biệt với tớ, làm bạn tri kỷ đồng hành chia sẻ ngọt bùi với tớ nhé. 💞" to
            "You are very special to me, let's be soulmates sharing sweet times together. 💞",

        // Step 3 Responses
        "Hihi ngọt ngào quá đi mất! Tớ đồng ý chứ sao nữa! Từ nay tớ chính thức là bạn gái của dũng sĩ xem quảng cáo nha!" to
            "Hihi, so sweet! Of course I agree! From now on, I am officially the girlfriend of the ad-watching hero!",
        "CẬU LÀ ĐỒ VÔ LIÊM SỈ, ĐỒ TỒI CÀN RỠ! Tớ ghét cậu, tớ đi về đây! Đừng hòng tán tỉnh tớ lần nào nữa!" to
            "YOU ARE AN UNPRINCIPLED, BRAZEN JERK! I hate you, I'm going home! Don't even think about flirting with me again!",
        "Nghe ấm áp quá... Làm tri kỷ cùng sẻ chia ngọt bùi ảo thực cũng tuyệt vời lắm chứ!" to
            "Sounds so warm... Sharing both virtual and real sweetness as soulmates is wonderful too!",

        // Outcomes & Headers
        "💖 KẾT CỤC: HẸN HÒ THÀNH CÔNG RỰC RỠ!" to "💖 OUTCOME: ROMANTIC SUCCESS!",
        "Điểm thiện cảm đạt mốc tuyệt đối: " to "Affection score reached absolute milestone: ",
        "Linh Chi đã trao trái tim cho bạn. Hãy nhận Huy hiệu phong danh \"Người Tình Trong Mộng\" và 400 XP!" to
            "Linh Chi has given you her heart. Receive the Dream Lover Title Badge and 400 XP!",
        "💔 KẾT CỤC: HẸN HÒ THẤT BẠI!" to "💔 OUTCOME: DATE FAILED!",
        "Điểm thiện cảm cuối cùng chỉ đạt: " to "Final affection score only reached: ",
        "(Yêu cầu ít nhất 80%)." to "(Requires at least 80%).",
        "Linh Chi thấy bạn nói chuyện quá khô khan hoặc kiệt sỉ nhạt nhẽo. Hãy cải thiện kỹ năng ngọt ngào và thử hẹn hò lại nhé dũng sĩ rảnh rỗi!" to
            "Linh Chi thinks your conversation is too dry, boring or stingy. Improve your sweet talking skills and try dating again, idle hero!"
    )

    for ((viKey, enVal) in translationMap) {
        result = result.replace(viKey, enVal)
    }
    return result
}

@Composable
fun LinhChiDatingDialog(viewModel: AdsViewModel) {
    val affection by viewModel.linhChiAffection.collectAsStateWithLifecycle()
    val step by viewModel.linhChiStep.collectAsStateWithLifecycle()
    val message by viewModel.linhChiMessage.collectAsStateWithLifecycle()
    val options by viewModel.linhChiOptions.collectAsStateWithLifecycle()
    val status by viewModel.linhChiDatingStatus.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { viewModel.closeDatingGame() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("linhchi_dating_dialog"),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1322) // Romantic deep purple dark theme
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, Color(0xFFFF69B4)) // Pink outline
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💖", fontSize = 24.sp)
                        Text(
                            text = viewModel.t("Hẹn Hò Cùng Hot Girl Linh Chi", "Dating with Hot Girl Linh Chi"),
                            color = Color(0xFFFF69B4),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { viewModel.closeDatingGame() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = viewModel.t("Đóng", "Close"),
                            tint = Color.White
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.15f))

                // Avatar of Linh Chi
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.5.dp, Color(0xFFFF69B4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = safePainterResource(id = com.example.R.drawable.img_clickbait_girl),
                        contentDescription = viewModel.t("Avatar Linh Chi xinh đẹp", "Beautiful Linh Chi Avatar"),
                        modifier = Modifier
                            .size(122.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                // Affection Meter
                val statusText = when {
                    affection < 35 -> viewModel.t("Lạnh nhạt hờ hững 🥶", "Cold & Indifferent 🥶")
                    affection in 35..65 -> viewModel.t("Bình thường vui vẻ 😘", "Normal & Cheerful 😘")
                    affection in 66..84 -> viewModel.t("Thiện cảm đặc biệt! 😳", "Special Affection! 😳")
                    else -> viewModel.t("Trăm năm hạnh phúc trọn đời 💑", "Happily Ever After 💑")
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = viewModel.t("Độ thiện cảm:", "Affection Level:"),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "$affection%",
                            color = Color(0xFFFF69B4),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { affection / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFF69B4),
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )

                    Text(
                        text = viewModel.t("Trạng thái tình cảm: ", "Relationship Status: ") + statusText,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Dialogue balloon
                Surface(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = translateLinhChiText(message, isEn),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Options list or Retry/Success buttons
                if (status == "PLAYING") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        options.forEachIndexed { index, option ->
                            Button(
                                onClick = { viewModel.selectDatingOption(option) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dating_option_$index"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFF69B4).copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF69B4)
                                    )
                                    Text(
                                        text = translateLinhChiText(option.text, isEn),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Success / failure controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (status == "FAILED") {
                            Button(
                                onClick = { viewModel.startDatingGame() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dating_retry_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF69B4)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(viewModel.t("Hẹn hò lại 💘", "Date again 💘"), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { viewModel.closeDatingGame() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dating_close_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(viewModel.t("Đóng 🚪", "Close 🚪"), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun translateHeistLog(log: String, isEnglish: Boolean): String {
    if (!isEnglish) return log
    var result = log
    val itemsMap = mapOf(
        "Nhẫn Kim Cương Saphire 💍" to "Sapphire Diamond Ring 💍",
        "Lắc Vàng Ròng 18K ✨" to "18K Pure Gold Bracelet ✨",
        "Khuyên Tai Ngọc Trai Hoàng Gia 🦪" to "Royal Pearl Earrings 🦪",
        "Chuỗi Dây Chuyền Ruby Ngọc Bích 📿" to "Ruby Jade Necklace 📿",
        "Kim Cương Đen Thái Dương Cực Đại 💎" to "Ultimate Black Sun Diamond 💎",
        "Mũ Miện Hoàng Gia Đính Bảo Ngọc 👑" to "Royal Tiara with Gems 👑",
        "Tượng Rồng Ngọc Lục Bảo Đế Vương 🐉" to "Imperial Emerald Dragon Statue 🐉",
        "Hộp Nhạc Thạch Anh Đính Kim Cương Hồng 🔮" to "Pink Diamond Quartz Music Box 🔮"
    )
    for ((viItem, enItem) in itemsMap) {
        result = result.replace(viItem, enItem)
    }
    
    val regexMap = mapOf(
        "🕶️ Bạn đã đột nhập thành công vào Tiệm Vàng Đá Quý qua đường ống thông gió!" to "🕶️ You have successfully infiltrated the Gold Jewelry Shop through the ventilation shaft!",
        "⚠️ Hệ thống báo động đang kích hoạt nhẹ ở mức 10%. Hãy hành động lẹ làng trước khi cảnh sát ập tới!" to "⚠️ The alarm system is lightly activated at 10%. Act swiftly before the police arrive!",
        "⛏️ Bạn mở tủ cạy ngọc nhẹ nhàng lấy được: " to "⛏️ You quietly cracked open a safe and stole: ",
        "Trị giá " to "Valued at ",
        "Bảo mật tăng thêm " to "Security alert increased by ",
        "Hiện tại: " to "Current: ",
        "🚨 Liều ăn nhiều! Bạn đập kính cuỗm ngay: " to "🚨 High risk, high reward! You smashed the glass and grabbed: ",
        "Còi báo động rú vang " to "Alarms blared by ",
        "🖥️ Bạn gõ code hack tắt camera thành công! Báo động hạ nhiệt " to "🖥️ You successfully hacked the cameras! Security alert cooled down by ",
        "💥 Hack thất bại! Chập mạch hệ thống làm máy báo động nhấp nháy liên hoàn " to "💥 Hack failed! System short-circuit increased alarms by ",
        "⚠️ Đột nhập nãy giờ túi có gì đâu mà tẩu thoát? Hãy cuỗm ít trang sức đã!" to "⚠️ Your bag is empty! Steal some jewelry first before escaping!",
        "🎉 THÀNH CÔNG RỰC RỠ! Bạn thoát ra bằng xe moto đặc chủng, thanh lý chợ đen kiếm được " to "🎉 EXTREME SUCCESS! You escaped via special motorbike, selling loot on the black market for ",
        " ảo trực tiếp đổ vào túi doanh thu!" to " directly added to accumulated revenue!",
        "🚔 CẢNH SÁT ẬP VÀO! Bạn bị khóa tay tống lên xe chuyên dụng. Toàn bộ đồ trang sức đắt tiền cuỗm được đã bị sung công quỹ. Phi vụ trắng tay!" to "🚔 POLICE AMBUSH! You were handcuffed and thrown into a squad car. All stolen jewelry has been confiscated. Mission failed!"
    )
    
    for ((viKey, enVal) in regexMap) {
        result = result.replace(viKey, enVal)
    }
    return result
}

@Composable
fun HeistGameDialog(viewModel: AdsViewModel) {
    val securityAlert by viewModel.heistSecurityAlert.collectAsStateWithLifecycle()
    val stolenValue by viewModel.heistStolenValue.collectAsStateWithLifecycle()
    val lootBag by viewModel.heistLootBag.collectAsStateWithLifecycle()
    val status by viewModel.heistStatus.collectAsStateWithLifecycle()
    val logs by viewModel.heistLog.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    val lazyListState = rememberLazyListState()

    // Auto-scroll to the bottom of logs on new events
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = { 
            if (status != "PLAYING") {
                viewModel.closeHeistGame()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = (status != "PLAYING"),
            dismissOnClickOutside = (status != "PLAYING")
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .testTag("heist_game_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF12121E)),
                border = BorderStroke(2.dp, when(status) {
                    "CAUGHT" -> Color(0xFFE53935)
                    "ESCAPED" -> Color(0xFF4CAF50)
                    else -> Color(0xFF00FFCC)
                })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🕶️", fontSize = 28.sp)
                            Column {
                                Text(
                                    text = viewModel.t("SÒNG BẠC... À KHÔNG, TIỆM KIM HOÀN", "CASINO... NO, GOLD STORE"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF00FFCC),
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = viewModel.t("Siêu Trộm 100 Carat", "100-Carat Super Thief"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        if (status != "PLAYING") {
                            IconButton(onClick = { viewModel.closeHeistGame() }) {
                                Icon(Icons.Default.Close, contentDescription = viewModel.t("Đóng", "Close"), tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Alert Level Meter
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(viewModel.t("Mức Độ Báo Động Bỏ Trốn", "Escape Alarm Level"), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                Text("$securityAlert%", fontWeight = FontWeight.Bold, color = when {
                                    securityAlert >= 75 -> Color(0xFFE53935)
                                    securityAlert >= 40 -> Color(0xFFFFB300)
                                    else -> Color(0xFF4CAF50)
                                })
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { securityAlert.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when {
                                    securityAlert >= 75 -> Color(0xFFE53935)
                                    securityAlert >= 40 -> Color(0xFFFFB300)
                                    else -> Color(0xFF4CAF50)
                                },
                                trackColor = Color(0xFF14141F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Loot Stats Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(viewModel.t("Giá Trị Đã Cuỗm", "Stolen Value"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                Text(
                                    text = "$${String.format("%.2f", stolenValue)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00FFCC)
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1.1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(viewModel.t("Số Lượng Bảo Vật", "Items Stolen"), fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                Text(
                                    text = "${lootBag.size} " + viewModel.t("món quà", "items"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logs and Terminal Output Area
                    Text(viewModel.t("Nhật Ký Phi Vụ", "Mission Log"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF06060C)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(logs) { logMsg ->
                                Text(
                                    text = translateHeistLog(logMsg, isEn),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = when {
                                        logMsg.contains("🚨") || logMsg.contains("CẢNH SÁT") || logMsg.contains("bị khóa") -> Color(0xFFEF5350)
                                        logMsg.contains("🎉") || logMsg.contains("hacker") || logMsg.contains("THÀNH CÔNG") -> Color(0xFF66BB6A)
                                        logMsg.contains("⛏️") -> Color(0xFF42A5F5)
                                        logMsg.contains("🖥️") -> Color(0xFFFFCA28)
                                        else -> Color.White.copy(alpha = 0.85f)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons of Options (Playing vs Finished)
                    if (status == "PLAYING") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.heistQuietLoot() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("heist_quiet_btn")
                                ) {
                                    Text(viewModel.t("Khoan Két ⛏️", "Quiet Safe-Crack ⛏️"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = { viewModel.heistRobDiamond() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("heist_rob_btn")
                                ) {
                                    Text(viewModel.t("Cướp Ngọc Lớn 👑", "Grab Big Gems 👑"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.heistHackCamera() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49495F)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("heist_hack_btn")
                                ) {
                                    Text(viewModel.t("Hack Camera 🖥️", "Hack Camera 🖥️"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = { viewModel.heistEscape() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("heist_escape_btn")
                                ) {
                                    Text(viewModel.t("Tẩu Thoát 🏍️", "Escape 🏍️"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    } else {
                        // Finished buttons
                        Button(
                            onClick = { viewModel.closeHeistGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == "ESCAPED") Color(0xFF4CAF50) else Color(0xFFE53935)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("heist_finish_btn")
                        ) {
                            Text(
                                text = if (status == "ESCAPED") viewModel.t("VỀ ĐẠI BẢN DOANH 🏠", "TO BASE CAMP 🏠") else viewModel.t("XEM QUẢNG CÁO ĐỂ BẢO LÃNH 💸", "WATCH AD FOR BAILOUT 💸"),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengesScreen(
    viewModel: AdsViewModel,
    modifier: Modifier = Modifier
) {
    val badges by viewModel.challengeBadges.collectAsStateWithLifecycle()
    val activeId by viewModel.activeChallengeId.collectAsStateWithLifecycle()
    val timer by viewModel.challengeTimer.collectAsStateWithLifecycle()
    val count by viewModel.challengeProgressCount.collectAsStateWithLifecycle()
    val challengeMsg by viewModel.challengeMessage.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()

    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    val watchedCount by viewModel.watchedCount.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val yugiohVictoryCount by viewModel.yugiohVictoryCount.collectAsStateWithLifecycle()
    val stoleDiamondSuccessfully by viewModel.stoleDiamondSuccessfully.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Arcade Scoreboard Panel at the very top
        item {
            ArcadeScoreboardUI(viewModel = viewModel)
        }

        // Points Summary Dashboard Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("challenges_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t("ĐIỂM THỬ THÁCH", "CHALLENGE POINTS"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalPoints XP",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.t("Hoàn thành thử thách để nhận Huy hiệu danh giá!", "Complete challenges to earn prestigious badges!"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Active Challenge Status Message Area
        if (!challengeMsg.isEmpty() || activeId != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeId != null) 
                            MaterialTheme.colorScheme.secondaryContainer 
                        else 
                            MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (activeId != null) Icons.Default.Warning else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (activeId != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            if (activeId != null) {
                                val activeBadge = badges.find { it.id == activeId }
                                val activeBadgeTitle = when(activeBadge?.id) {
                                    "rewarded_speed" -> viewModel.t("Siêu Tốc Thu Thưởng (60s)", "Rewarded Speedrun (60s)")
                                    "ctr_target" -> viewModel.t("Bậc Thầy Click-Rate", "Click-Rate Master")
                                    "interstitial_sniper" -> viewModel.t("Xạ Thủ Tắt Ads", "Ad Close Sniper")
                                    "stole_diamond" -> viewModel.t("Trộm được kim cương", "Steal Virtual Diamond")
                                    "view_50_ads" -> viewModel.t("Xem 50 quảng cáo", "Watch 50 Ads")
                                    "revenue_1000" -> viewModel.t("Tích lũy được $1000", "Accumulate $1000")
                                    "wizard_yugioh_15" -> viewModel.t("Trở thành Ma Pháp Sư (Thắng 15 lần Yugi-Oh)", "Beast Duel Magician (15 wins)")
                                    "date_linh_chi" -> viewModel.t("Thành công hẹn hò với Hot Girl Linh Chi", "Dating with Hot Girl Linh Chi Success")
                                    else -> activeBadge?.title ?: ""
                                }
                                Text(
                                    text = viewModel.t("Đang thực hiện: ", "In Progress: ") + activeBadgeTitle,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (activeId == "rewarded_speed") {
                                    Text(
                                        text = viewModel.t("Thời gian: ", "Time: ") + "${timer}s " + viewModel.t("còn lại | Tiến trình: ", "remaining | Progress: ") + "$count/3 " + viewModel.t("quảng cáo", "ads"),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            if (!challengeMsg.isEmpty()) {
                                Text(
                                    text = translateChallengeMessage(challengeMsg, isEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeId != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Checklist of Challenges
        item {
            Text(
                text = viewModel.t("Danh Sách Thử Thách", "Challenge List"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (badges.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.t("Đang tải danh sách thử thách...", "Loading challenge list..."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(badges, key = { it.id }) { badge ->
                val isActive = activeId == badge.id
                val isSomeChallengeActive = activeId != null
                
                val translatedTitle = when(badge.id) {
                    "rewarded_speed" -> viewModel.t("Siêu Tốc Thu Thưởng (60s)", "Rewarded Speedrun (60s)")
                    "ctr_target" -> viewModel.t("Bậc Thầy Click-Rate", "Click-Rate Master")
                    "interstitial_sniper" -> viewModel.t("Xạ Thủ Tắt Ads", "Ad Close Sniper")
                    "stole_diamond" -> viewModel.t("Trộm được kim cương", "Steal Virtual Diamond")
                    "view_50_ads" -> viewModel.t("Xem 50 quảng cáo", "Watch 50 Ads")
                    "revenue_1000" -> viewModel.t("Tích lũy được $1000", "Accumulate $1000")
                    "wizard_yugioh_15" -> viewModel.t("Trở thành Ma Pháp Sư (Thắng 15 lần Yugi-Oh)", "Beast Duel Magician (15 wins)")
                    "date_linh_chi" -> viewModel.t("Thành công hẹn hò với Hot Girl Linh Chi", "Dating with Hot Girl Linh Chi Success")
                    else -> badge.title
                }
                
                val translatedObjective = when(badge.id) {
                    "rewarded_speed" -> viewModel.t("Xem thành công 3 Quảng cáo có thưởng trong vòng 60 giây.", "Successfully watch 3 Rewarded Ads within 60 seconds.")
                    "ctr_target" -> viewModel.t("Chạy mô phỏng 24h chọn các thông số (Slider CTR, Ngành, Traffic, Đấu Thầu) để đạt tỷ lệ CTR thực tế chính xác trong khoảng 8.0% - 9.0%.", "Run a 24h simulation, adjust CTR/niche/traffic/bidding parameters to achieve a real CTR between 8.0% and 9.0%.")
                    "interstitial_sniper" -> viewModel.t("Tắt thành công quảng cáo che phủ (Interstitial) trong vòng dưới 2.0 giây kể từ khi nút Bỏ qua xuất hiện.", "Successfully close an Interstitial Ad within 2.0 seconds after the Skip button appears.")
                    "stole_diamond" -> viewModel.t("Thực hiện đại phi vụ trộm thành công và mang về ít nhất một món kim cương từ Tiệm Ngọc.", "Successfully heist and escape with at least one virtual diamond from the Gold Shop.")
                    "view_50_ads" -> viewModel.t("Kiên trì xem tổng cộng ít nhất 50 quảng cáo ảo (bao gồm Interstitial, Banner, Rewarded hoặc bẫy click).", "Patiently watch at least 50 simulated ads (including banner, interstitial, rewarded, or clickbait traps).")
                    "revenue_1000" -> viewModel.t("Tích lũy tổng doanh thu ảo đạt mốc từ $1000 trở lên bằng mọi thủ đoạn và quảng cáo.", "Accumulate a total simulated revenue of $1000 or more via bulk simulation or high-cpm formats.")
                    "wizard_yugioh_15" -> viewModel.t("Đánh bại siêu trí tuệ AI AdBot trong trò chơi đấu bài ma thuật Yugi-Oh 15 lần.", "Defeat AI AdBot in the Battle Card Game (Beast Duel Arena) 15 times.")
                    "date_linh_chi" -> viewModel.t("Đáp ứng 2 điều kiện ẩn: Xem quảng cáo về Linh Chi 2 lần, Trộm được kim cương thành công và hoàn thành trò chơi mật ngọt hẹn hò.", "Satisfy 2 requirements: watch Linh Chi ads twice, steal gold store gems, and finish the romantic chat game.")
                    else -> badge.objective
                }
                
                val translatedBadgeName = when(badge.id) {
                    "rewarded_speed" -> viewModel.t("Đại Phú Kiên Trì", "Persistent Millionaire")
                    "ctr_target" -> viewModel.t("Bậc Thầy Thu Hút", "Engagement Overlord")
                    "interstitial_sniper" -> viewModel.t("Xạ Thủ Thần Tốc", "Speed Reflex Master")
                    "stole_diamond" -> viewModel.t("Siêu Trộm Kim Cương", "Diamond Thief")
                    "view_50_ads" -> viewModel.t("Dũng Sĩ Kiên Trì", "Devoted Ad-Viewer")
                    "revenue_1000" -> viewModel.t("Triệu Phú Quảng Cáo", "Big Cash Tycoon")
                    "wizard_yugioh_15" -> viewModel.t("Ma Pháp Sư Tối Thượng", "Ultimate Card Summoner")
                    "date_linh_chi" -> viewModel.t("Trái Tim Thần Sầu", "Heart Specialist")
                    else -> badge.badgeName
                }
                
                val translatedScoreText = if (badge.scoreText == "Chưa hoàn thành") {
                    viewModel.t("Chưa hoàn thành", "Incompleted")
                } else {
                    viewModel.t(badge.scoreText, badge.scoreText)
                }

                Card(
                     modifier = Modifier
                        .fillMaxWidth()
                        .testTag("challenge_item_${badge.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive)
                            MaterialTheme.colorScheme.surfaceVariant
                        else if (badge.isCompleted)
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive)
                            MaterialTheme.colorScheme.primary
                        else if (badge.isCompleted)
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Badge Icon Left Circle
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (badge.isCompleted)
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                        else if (isActive)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (badge.isCompleted) "✅" else badge.badgeIcon,
                                    fontSize = 22.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = translatedTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (badge.isCompleted)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Badge(
                                        containerColor = if (badge.isCompleted)
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "+${badge.pointsEarned} XP",
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                                
                                Text(
                                    text = translatedObjective,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress or Completed details
                        if (badge.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = viewModel.t("Huy hiệu: ", "Badge: ") + translatedBadgeName + " (${translatedScoreText})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        } else {
                            // Buttons row or dynamic progress indicator
                            val isManual = badge.id in listOf("rewarded_speed", "ctr_target", "interstitial_sniper")
                            if (isManual) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hint or description
                                    val hintText = when (badge.id) {
                                        "rewarded_speed" -> if (isActive) viewModel.t("Xem video 10 giây!", "Watch 10-second video!") else viewModel.t("Cần bấm xem video 3 lần", "Watch video 3 times")
                                        "ctr_target" -> if (isActive) viewModel.t("Hãy kéo Slider đến ~8.5%!", "Slide target CTR to ~8.5%!") else viewModel.t("Cài đặt CTR t.bình", "Configure average CTR")
                                        "interstitial_sniper" -> if (isActive) viewModel.t("Hãy tắt ads cực nhanh!", "Dismiss ad super fast!") else viewModel.t("Yêu cầu phản xạ tốt", "Fast reflex required")
                                        else -> ""
                                    }
                                    
                                    Text(
                                        text = hintText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )

                                    if (isActive) {
                                        Button(
                                            onClick = { viewModel.stopChallenge() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("action_challenge_stop_${badge.id}")
                                        ) {
                                            Text(viewModel.t("Dừng", "Stop"), style = MaterialTheme.typography.labelLarge)
                                        }
                                    } else {
                                        Button(
                                            onClick = { viewModel.startChallenge(badge.id) },
                                            enabled = !isSomeChallengeActive,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("action_challenge_start_${badge.id}")
                                        ) {
                                            Text(
                                                text = if (isSomeChallengeActive) viewModel.t("Đang khóa", "Locked") else viewModel.t("Khởi động", "Start"),
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Dynamic progress display for automatic achievements!
                                val progressPercentage: Float = when (badge.id) {
                                    "stole_diamond" -> if (stoleDiamondSuccessfully) 1f else 0f
                                    "view_50_ads" -> (watchedCount.toFloat() / 50f).coerceIn(0f, 1f)
                                    "revenue_1000" -> (totalRevenue / 1000.0).toFloat().coerceIn(0f, 1f)
                                    "wizard_yugioh_15" -> (yugiohVictoryCount.toFloat() / 15f).coerceIn(0f, 1f)
                                    "date_linh_chi" -> if (badge.isCompleted) 1f else 0f
                                    else -> 0f
                                }

                                val progressText = when (badge.id) {
                                    "stole_diamond" -> if (stoleDiamondSuccessfully) viewModel.t("Hoàn thành: Đã cuỗm Kim Cương từ Tiệm Ngọc 💎", "Completed: Stole Diamond from Gold Shop 💎") else viewModel.t("Yêu cầu: Lấy Kim Cương trong tủ kính Tiệm Ngọc", "Required: Steal Diamond from Gold Shop glass counter")
                                    "view_50_ads" -> viewModel.t("Tiến độ: ", "Progress: ") + "$watchedCount / 50 Ads"
                                    "revenue_1000" -> viewModel.t("Tiến độ: ", "Progress: ") + "$${String.format("%.2f", totalRevenue)} / $1000.00"
                                    "wizard_yugioh_15" -> viewModel.t("Tiến độ: ", "Progress: ") + "$yugiohVictoryCount " + viewModel.t(" / 15 trận thắng", " / 15 wins")
                                    "date_linh_chi" -> viewModel.t("Hẹn hò: Chưa hẹn hò mật ngọt thành công", "Dating: Romantic dating not yet successful")
                                    else -> ""
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = progressText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${(progressPercentage * 100f).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = progressPercentage,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
fun ArcadeScoreboardUI(viewModel: AdsViewModel, modifier: Modifier = Modifier) {
    val playerNickname by viewModel.playerNickname.collectAsStateWithLifecycle()
    val heistHighScore by viewModel.heistHighScore.collectAsStateWithLifecycle()
    val yugiohHighScore by viewModel.yugiohHighScore.collectAsStateWithLifecycle()
    val linhChiHighScore by viewModel.linhChiHighScore.collectAsStateWithLifecycle()
    val chessHighScore by viewModel.chessHighScore.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var tempNickname by remember { mutableStateOf("") }
    var selectedScoreboardTab by remember { mutableStateOf(0) } // 0: Heist, 1: Yugioh, 2: Dating, 3: Chess

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("arcade_scoreboard_panel"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00)))),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF13131A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Retro Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = viewModel.t("🏟️ BẢNG VÀNG KỶ LỤC ARCADE", "🏟️ ARCADE SCOREBOARD"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD700),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = viewModel.t("Thành tích tối cao của các dũng sĩ ảo", "Ultimate achievements of virtual legends"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Nickname display and editor button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1F1F2E),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        tempNickname = playerNickname
                        showEditNameDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "👤 $playerNickname",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = viewModel.t("Sửa biệt danh", "Edit nickname"),
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub tabs for 4 games
            TabRow(
                selectedTabIndex = selectedScoreboardTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFFFFD700),
                divider = {}
            ) {
                listOf(
                    viewModel.t("🕶️ Heist", "🕶️ Heist") to 0,
                    viewModel.t("🃏 Yugioh", "🃏 Beast Duel") to 1,
                    viewModel.t("💖 Hẹn Hò", "💖 Dating") to 2,
                    viewModel.t("👑 Cờ Vua", "👑 Chess") to 3
                ).forEach { (label, index) ->
                    Tab(
                        selected = selectedScoreboardTab == index,
                        onClick = { selectedScoreboardTab = index },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedScoreboardTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedScoreboardTab == index) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data calculation based on active sub tab
            data class RankItem(val name: String, val score: Double, val display: String, val isUser: Boolean)

            val listData = remember(selectedScoreboardTab, playerNickname, heistHighScore, yugiohHighScore, linhChiHighScore, chessHighScore, appLang) {
                when (selectedScoreboardTab) {
                    0 -> {
                        val bots = listOf(
                            RankItem(viewModel.t("Siêu Đạo Chích Đa Cấp", "MLM Master Thief"), 750.0, "$750.00", false),
                            RankItem("Ads Boss Hacker", 530.0, "$530.00", false),
                            RankItem(viewModel.t("Trùm Bán Sim Rác", "Spam Sim Card Lord"), 320.0, "$320.00", false),
                            RankItem(viewModel.t("Kẻ Trộm Vặt", "Petty Pickpocket"), 110.0, "$110.00", false)
                        )
                        val user = RankItem(playerNickname, heistHighScore.toDouble(), "$${String.format("%.2f", heistHighScore)}", true)
                        (bots + user).sortedByDescending { it.score }
                    }
                    1 -> {
                        val bots = listOf(
                            RankItem(viewModel.t("Thần Bài Đa Cấp", "MLM Card God"), 4000.0, "4000 LP", false),
                            RankItem(viewModel.t("Ads Boss Vô Đối", "Unrivaled Ads Boss"), 3500.0, "3500 LP", false),
                            RankItem(viewModel.t("Tập Sự Nạp Thẻ", "Top-up Novice"), 2000.0, "2000 LP", false),
                            RankItem(viewModel.t("Kẻ Chia Bài Thuê", "Hired Card Dealer"), 1000.0, "1000 LP", false)
                        )
                        val user = RankItem(playerNickname, yugiohHighScore.toDouble(), "${yugiohHighScore} LP", true)
                        (bots + user).sortedByDescending { it.score }
                    }
                    2 -> {
                        val bots = listOf(
                            RankItem(viewModel.t("Trùm Sát Gái Simp", "Simp Playboy King"), 100.0, "100" + viewModel.t("% Thiện Cảm", "% Affection"), false),
                            RankItem(viewModel.t("Ads Boss (Nạp Vip)", "Ads Boss (Vip Topup)"), 95.0, "95" + viewModel.t("% Thiện Cảm", "% Affection"), false),
                            RankItem(viewModel.t("Thiếu Gia Thích Spam", "Spamming Young Master"), 70.0, "70" + viewModel.t("% Thiện Cảm", "% Affection"), false),
                            RankItem(viewModel.t("Anh Chàng Hít Khí Trời", "Air-breathing Lone Guy"), 15.0, "15" + viewModel.t("% Thiện Cảm", "% Affection"), false)
                        )
                        val user = RankItem(playerNickname, linhChiHighScore.toDouble(), "$linhChiHighScore" + viewModel.t("% Thiện Cảm", "% Affection"), true)
                        (bots + user).sortedByDescending { it.score }
                    }
                    else -> {
                        val bots = listOf(
                            RankItem(viewModel.t("Trùm Stockfish Ăn Gian", "Cheating Stockfish Boss"), 3000.0, "ELO 3000", false),
                            RankItem(viewModel.t("Ads Boss Cường Hóa", "Overclocked Ads Boss"), 2400.0, "ELO 2400", false),
                            RankItem(viewModel.t("Kiện Tướng Trà Đá", "Iced-Tea Master"), 1200.0, "ELO 1200", false),
                            RankItem(viewModel.t("Tập Đi Tốt Xem Ads", "Pawn-moving Ad Viewer"), 600.0, "ELO 600", false)
                        )
                        val user = RankItem(playerNickname, chessHighScore.toDouble(), if (chessHighScore > 0) "ELO $chessHighScore" else viewModel.t("Chưa chơi", "Not played"), true)
                        (bots + user).sortedByDescending { it.score }
                    }
                }
            }

            // Display ranked items
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listData.take(5).forEachIndexed { index, item ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFD700) 
                        1 -> Color(0xFFC0C0C0) 
                        2 -> Color(0xFFCD7F32) 
                        else -> Color.White.copy(alpha = 0.5f)
                    }

                    val containerBgColor = if (item.isUser) {
                        Color(0xFF2C2213) 
                    } else {
                        Color(0xFF1E1E28)
                    }

                    val borderColor = if (item.isUser) {
                        Color(0xFFFFD700)
                    } else {
                        Color.Transparent
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = containerBgColor,
                        border = if (item.isUser) BorderStroke(1.5.dp, borderColor) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (item.isUser && item.score > 0) {
                                    scaleX = 1.02f
                                    scaleY = 1.02f
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Position Badge
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (index < 3) rankColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor
                                    )
                                }

                                // Name
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (item.isUser) FontWeight.Bold else FontWeight.Medium,
                                    color = if (item.isUser) Color(0xFFFFD700) else Color.White
                                )

                                if (item.isUser) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFD700).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = viewModel.t("BẠN", "YOU"),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Score values right-aligned
                            Text(
                                text = if (item.isUser && item.score == 0.0) viewModel.t("Chưa lập kỷ lục", "No record yet") else item.display,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isUser && item.score > 0.0) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Name AlertDialog Dialog window
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = {
                Text(text = viewModel.t("Đổi Biệt Danh", "Change Nickname"), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = viewModel.t("Biệt danh mới sẽ đại diện của bạn trên tất cả bảng xếp hạng kỷ lục:", "Your new nickname will represent you on all high-score leaderboards:"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { if (it.length <= 15) tempNickname = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_nickname_input"),
                        singleLine = true,
                        placeholder = { Text(viewModel.t("Nhập biệt danh...", "Enter nickname...")) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPlayerNickname(tempNickname)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700), contentColor = Color.Black)
                ) {
                    Text(viewModel.t("Lưu Lại", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(viewModel.t("Hủy", "Cancel"))
                }
            }
        )
    }
}

fun translateDuelLog(log: String, isEnglish: Boolean): String {
    if (!isEnglish) return log
    var result = log

    // Prefix/Exact translations first
    val exactMap = mapOf(
        "⚔️ Đấu trường bài Quái Vật chính thức khai mở!" to "⚔️ Monster Card Arena is officially open!",
        "💀 BẠN ĐÃ THẤT BẠI! AdBot chiến thắng trận đấu ma pháp!" to "💀 YOU DIED! AdBot won the magic duel!",
        "🏆 BẠN ĐÃ ĐẬP TAN ADBOT VÀ CHIẾN THẮNG TRẬN ĐẤU BÀI!" to "🏆 VICTORY! You defeated AdBot and won the duel!",
        "👉 Hãy triệu hồi quái thú lên bàn đấu (thế CÔNG/THỦ), hoặc kích hoạt bài PHÉP để giành chiến thắng!" to "👉 Summon monsters onto the field (ATTACK/DEFENSE stance), or activate SPELL cards to win!",
        "⚠️ Quái thú đang ở thế THỦ không thể tấn công!" to "⚠️ Defense position monsters cannot attack!",
        "⚠️ Quái thú này đã tấn công lượt này rồi!" to "⚠️ This monster has already attacked this turn!",
        "🔥 Quái thú nhận thêm +600 sức tấn công!" to "🔥 Monster gains +600 Attack power!",
        "⚠️ Không có quái thú nào trên sân để gắn Kiếm!" to "⚠️ No monster on field to equip Sword!",
        "💖 Hồi phục +800 điểm sinh mệnh LP!" to "💖 Restored +800 Life Points (LP)!",
        "⚠️ AdBot không có quái thú nào trên sân để tiêu diệt!" to "⚠️ AdBot has no monsters on field to destroy!",
        "🚫 Kích hoạt Phép Chặn Quảng Cáo nhưng đối thủ không còn quân bài hoạt động hay cầm tay nào!" to "🚫 Activated AdBlocker spell but opponent has no active or hand cards!",
        "⚠️ Quái thú đang phòng thủ!" to "⚠️ Monster is currently in defense stance!",
        "⚠️ Lượt đấu đã được sử dụng!" to "⚠️ Turn action already used!",
        "⚠️ Không thể tấn công trực tiếp khi đối phương vẫn có quái thú bảo vệ!" to "⚠️ Cannot attack directly while opponent has defending monsters!",
        "--- Lượt của bạn bắt đầu ---" to "--- Your turn starts ---",
        "Lượt của bạn bắt đầu!" to "Your turn starts!",
        "Lượt của đối thủ bắt đầu!" to "Opponent's turn starts!",
        "💬 Bạn Gái Linh Chi ngơ ngác nháy mắt nhưng đối thủ không có quân quái thú nào trên sân để dụ dỗ!" to "💬 Linh Chi blinks in confusion, but the opponent has no monsters to seduce!",
        "🛡️ Hòa phe!" to "🛡️ Tie! No damage taken.",
        "🛡️ Lá chắn [Bạn Gái Linh Chi] của bạn bị phá vỡ hoàn toàn!" to "🛡️ Your [Girlfriend Linh Chi] shield was completely shattered!"
    )

    if (exactMap.containsKey(result)) {
        return exactMap[result]!!
    }

    // Starts-with / contains check for dynamic parts
    if (result.startsWith("👉 Đối thủ của bạn lượt này là: ")) {
        val rest = result.substringAfter("👉 Đối thủ của bạn lượt này là: ")
        return "👉 Your opponent this round is: $rest"
    }
    if (result.startsWith("🃏 Bạn triệu hồi [") && result.endsWith("] lên Sân Đấu!")) {
        val cardName = result.substringAfter("🃏 Bạn triệu hồi [").substringBefore("] lên Sân Đấu!")
        return "🃏 You summoned [${translateCardName(cardName, isEnglish)}] to the field!"
    }
    if (result.startsWith("💖 [Bạn Gái Linh Chi] quyến rũ dỗ mật ngọt! Thu phục cực phẩm [") && result.endsWith("] của đối phương về cánh của bạn!")) {
        val cardName = result.substringAfter("💖 [Bạn Gái Linh Chi] quyến rũ dỗ mật ngọt! Thu phục cực phẩm [").substringBefore("] của đối phương về cánh của bạn!")
        return "💖 [Girlfriend Linh Chi] seduced [${translateCardName(cardName, isEnglish)}] over to your side!"
    }
    if (result.startsWith("💔 Sân đấu của bạn chật kín! Tuy nhiên Linh Chi đã quyến rũ quái thú [") && result.endsWith("] tự rút lui khỏi sân đối phương!")) {
        val cardName = result.substringAfter("💔 Sân đấu của bạn chật kín! Tuy nhiên Linh Chi đã quyến rũ quái thú [").substringBefore("] tự rút lui khỏi sân đối phương!")
        return "💔 Your field is full! But Linh Chi charmed [${translateCardName(cardName, isEnglish)}] to retreat from opponent's field!"
    }
    if (result.startsWith("🔮 Bạn kích hoạt Phép Thuật [") && result.endsWith("]!")) {
        val cardName = result.substringAfter("🔮 Bạn kích hoạt Phép Thuật [").substringBefore("]!")
        return "🔮 You activated Spell [${translateCardName(cardName, isEnglish)}]!"
    }
    if (result.startsWith("🌪️ Đã thổi bay [") && result.endsWith("] của AdBot!")) {
        val cardName = result.substringAfter("🌪️ Đã thổi bay [").substringBefore("] của AdBot!")
        return "🌪️ Blown away [${translateCardName(cardName, isEnglish)}] of AdBot!"
    }
    if (result.startsWith("🚫 [Phép Chặn Quảng Cáo] bộc phá chặn đứng và tiêu hủy quái thú [") && result.endsWith("] trên sân đối thủ!")) {
        val cardName = result.substringAfter("🚫 [Phép Chặn Quảng Cáo] bộc phá chặn đứng và tiêu hủy quái thú [").substringBefore("] trên sân đối thủ!")
        return "🚫 [AdBlocker Spell] burst and destroyed [${translateCardName(cardName, isEnglish)}] on opponent's field!"
    }
    if (result.startsWith("🚫 [Phép Chặn Quảng Cáo] quét sạch từ xa, đào sạch lá bài [") && result.endsWith("] cầm tay của đối phương!")) {
        val cardName = result.substringAfter("🚫 [Phép Chặn Quảng Cáo] quét sạch từ xa, đào sạch lá bài [").substringBefore("] cầm tay của đối phương!")
        return "🚫 [AdBlocker Spell] swept and discarded [${translateCardName(cardName, isEnglish)}] from opponent's hand!"
    }
    if (result.startsWith("🛡️ Bạn đổi [") && result.contains("] sang thế [")) {
        val cardName = result.substringAfter("🛡️ Bạn đổi [").substringBefore("] sang thế [")
        val stance = if (result.contains("CÔNG")) "ATTACK" else "DEFENSE"
        return "🛡️ You switched [${translateCardName(cardName, isEnglish)}] to $stance position!"
    }
    if (result.startsWith("⚡ [") && result.contains("] khai chiến với [")) {
        val part1 = result.substringAfter("⚡ [").substringBefore("] khai chiến với [")
        val part2 = result.substringAfter("] khai chiến với [").substringBefore("]!")
        return "⚡ [${translateCardName(part1, isEnglish)}] declared battle against [${translateCardName(part2, isEnglish)}]!"
    }
    if (result.startsWith("💥 Tiêu diệt thành công! AdBot nhận ") && result.endsWith(" điểm sát thương LP.")) {
        val lp = result.substringAfter("💥 Tiêu diệt thành công! AdBot nhận ").substringBefore(" điểm sát thương LP.")
        return "💥 Stole victory! AdBot took $lp LP damage."
    }
    if (result.startsWith("💀 Phản tác dụng! Quái của bạn bị diệt, bạn mất ") && result.endsWith(" LP.")) {
        val lp = result.substringAfter("💀 Phản tác dụng! Quái của bạn bị diệt, bạn mất ").substringBefore(" LP.")
        return "💀 Recoil! Your monster was destroyed, you lost $lp LP."
    }
    if (result.startsWith("💥 Phá vỡ phòng ngự [") && result.endsWith("] của đối phương.")) {
        val cardName = result.substringAfter("💥 Phá vỡ phòng ngự [").substringBefore("] của đối phương.")
        return "💥 Pierced the defense of opponent's [${translateCardName(cardName, isEnglish)}]."
    }
    if (result.startsWith("🛡️ Bị chặn đứng! Bạn nhận ") && result.endsWith(" điểm phản chấn sát thương LP.")) {
        val lp = result.substringAfter("🛡️ Bị chặn đứng! Bạn nhận ").substringBefore(" điểm phản chấn sát thương LP.")
        return "🛡️ Blocked! You took $lp LP recoil damage."
    }
    if (result.startsWith("💥 [") && result.contains("] TẤN CÔNG TRỰC TIẾP! AdBot lĩnh trọn ")) {
        val cardName = result.substringAfter("💥 [").substringBefore("] TẤN CÔNG TRỰC TIẾP!")
        val lp = result.substringAfter("AdBot lĩnh trọn ").substringBefore(" sát thương LP.")
        return "💥 [${translateCardName(cardName, isEnglish)}] DIRECT ATTACK! AdBot took $lp LP damage."
    }
    if (result.startsWith("--- Lượt của ") && result.endsWith(" bắt đầu! ---")) {
        val name = result.substringAfter("--- Lượt của ").substringBefore(" bắt đầu! ---")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "--- $translatedName's Turn Begins! ---"
    }
    if (result.endsWith(" rút thêm 1 thẻ bài.")) {
        val name = result.substringBefore(" rút thêm 1 thẻ bài.")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName drew 1 card."
    }
    if (result.contains(" kích hoạt Phép [Bánh Vẽ Triệu Đô]! Hồi +800 LP.")) {
        val name = result.substringBefore(" kích hoạt Phép [Bánh Vẽ Triệu Đô]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Billion-Dollar Cake] Spell! Restored +800 LP."
    }
    if (result.contains(" kích hoạt Phép [Bão Click Ảo]! Tiêu diệt [") && result.endsWith("] của bạn!")) {
        val name = result.substringBefore(" kích hoạt Phép [Bão Click Ảo]!")
        val cardName = result.substringAfter("Tiêu diệt [").substringBefore("] của bạn!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Click Storm] Spell! Destroyed your [${translateCardName(cardName, isEnglish)}]!"
    }
    if (result.contains(" bổ sung [Bảo Kiếm AdSlayer] cho [") && result.contains("]! ATK tăng lên ")) {
        val name = result.substringBefore(" bổ sung [Bảo Kiếm AdSlayer]!")
        val cardName = result.substringAfter("cho [").substringBefore("]!")
        val atk = result.substringAfter("ATK tăng lên ").substringBefore(".")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName equipped [AdSlayer Sword] to [${translateCardName(cardName, isEnglish)}]! ATK increased to $atk."
    }
    if (result.contains(" kích hoạt [Dự Án Cam Kết Lợi Nhuận 300%]! Gây 1000 sát thương lừa đảo trực tiếp lên LP của bạn!")) {
        val name = result.substringBefore(" kích hoạt [Dự Án Cam Kết Lợi Nhuận 300%]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [300% ROI Ponzi Scheme]! Directly inflicted 1000 scam damage on your LP!"
    }
    if (result.contains(" kích hoạt [App Đào Coin Giả Mạo]! Loại bỏ quái thú mạnh nhất và chiếm đoạt 500 LP của bạn!")) {
        val name = result.substringBefore(" kích hoạt [App Đào Coin Giả Mạo]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Fake Coin Mining App]! Eliminated your strongest monster and drained 500 LP!"
    }
    if (result.contains(" kích hoạt [Lạm Phát Phi Mã]! Cướp đi 800 LP và phá vỡ 800 phòng thủ toàn bộ quái vật bọc lót!")) {
        val name = result.substringBefore(" kích hoạt [Lạm Phát Phi Mã]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Hyperinflation]! Stole 800 LP and crushed 800 DEF of all supporting monsters!"
    }
    if (result.contains(" kích hoạt [Hợp Đồng Độc Quyền]! Thôn tính sáp nhập và quét sạch TOÀN BỘ quái thú phe bạn!")) {
        val name = result.substringBefore(" kích hoạt [Hợp Đồng Độc Quyền]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Monopoly Contract]! Merged and cleared ALL of your monsters!"
    }
    if (result.contains(" kích hoạt [Mưa Tiền Tệ Đè Bẹp]! Thả tấn tiền đè bẹp, gây sát thương cực khủng 1500 LP trực tiếp!")) {
        val name = result.substringBefore(" kích hoạt [Mưa Tiền Tệ Đè Bẹp]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [Money Rain]! Dropped tons of cash, directly inflicting 1500 LP damage!"
    }
    if (result.contains(" kích hoạt [Thế Lực Đồng Tiền]! Khôi phục 1500 LP và bộc phát +1500 Công lực cho tất cả quái vật!")) {
        val name = result.substringBefore(" kích hoạt [Thế Lực Đồng Tiền]!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName activated [The Power of Money]! Restored 1500 LP and granted +1500 ATK to all monsters!"
    }
    if (result.contains(" triệu hồi [") && result.contains("] thế chế độ ")) {
        val name = result.substringBefore(" triệu hồi [")
        val cardName = result.substringAfter(" triệu hồi [").substringBefore("] thế chế độ ")
        val stance = if (result.contains("CÔNG")) "ATTACK" else "DEFENSE"
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "$translatedName summoned [${translateCardName(cardName, isEnglish)}] in $stance position!"
    }
    if (result.startsWith("💥 [") && result.contains("] của ") && result.contains(" TẤN CÔNG TRỰC TIẾP! Bạn lĩnh đủ ")) {
        val cardName = result.substringAfter("💥 [").substringBefore("] của ")
        val bossName = result.substringAfter("] của ").substringBefore(" TẤN CÔNG TRỰC TIẾP!")
        val lp = result.substringAfter("Bạn lĩnh đủ ").substringBefore(" sát thương LP.")
        val translatedBossName = when (bossName) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> bossName
        }
        return "💥 [${translateCardName(cardName, isEnglish)}] of $translatedBossName DIRECT ATTACK! You took $lp LP damage."
    }
    if (result.startsWith("⚡ [") && result.contains("] của ") && result.contains(" tấn công [")) {
        val cardName = result.substringAfter("⚡ [").substringBefore("] của ")
        val bossName = result.substringAfter("] của ").substringBefore(" tấn công [")
        val pCardName = result.substringAfter(" tấn công [").substringBefore("]!")
        val translatedBossName = when (bossName) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> bossName
        }
        return "⚡ [${translateCardName(cardName, isEnglish)}] of $translatedBossName attacks your [${translateCardName(pCardName, isEnglish)}]!"
    }
    if (result.startsWith("💀 [") && result.contains("] của bạn bị tiêu diệt! Bạn tốn ")) {
        val cardName = result.substringAfter("💀 [").substringBefore("] của bạn bị tiêu diệt!")
        val lp = result.substringAfter("Bạn tốn ").substringBefore(" LP.")
        return "💀 Your [${translateCardName(cardName, isEnglish)}] was destroyed! You lost $lp LP."
    }
    if (result.startsWith("💥 Lưỡng bại câu thương! Cả hai quái thú cùng phát nổ.")) {
        return "💥 Mutual destruction! Both monsters exploded."
    }
    if (result.startsWith("🛡️ Bạn phản công! [") && result.contains("] bị nổ, ") && result.endsWith(" tốn LP!")) {
        val cardName = result.substringAfter("🛡️ Bạn phản công! [").substringBefore("] bị nổ, ")
        val rest = result.substringAfter("] bị nổ, ")
        val name = rest.substringBefore(" tốn ")
        val lp = rest.substringAfter(" tốn ").substringBefore(" LP!")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "🛡️ Counter-attack! [${translateCardName(cardName, isEnglish)}] exploded, $translatedName lost $lp LP!"
    }
    if (result.startsWith("🛡️ Bạn phản công! [") && result.contains("] bị nổ, ") && result.endsWith(" tốn LP")) {
        val cardName = result.substringAfter("🛡️ Bạn phản công! [").substringBefore("] bị nổ, ")
        val rest = result.substringAfter("] bị nổ, ")
        val name = rest.substringBefore(" tốn ")
        val lp = rest.substringAfter(" tốn ").substringBefore(" LP")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "🛡️ Counter-attack! [${translateCardName(cardName, isEnglish)}] exploded, $translatedName lost $lp LP!"
    }
    if (result.startsWith("🛡️ Lá chắn [") && result.endsWith("] của bạn bị phá vỡ hoàn toàn!")) {
        val cardName = result.substringAfter("🛡️ Lá chắn [").substringBefore("] của bạn bị phá vỡ hoàn toàn!")
        return "🛡️ Your [${translateCardName(cardName, isEnglish)}] shield was completely shattered!"
    }
    if (result.startsWith("🛡️ Giáp của bạn quá trơ! ") && result.contains(" tự dính phản chấn ")) {
        val name = result.substringAfter("🛡️ Giáp của bạn quá trơ! ").substringBefore(" tự dính phản chấn ")
        val lp = result.substringAfter(" tự dính phản chấn ").substringBefore(" LP.")
        val translatedName = when (name) {
            "Bậc Thầy Đa Cấp" -> "MLM Master"
            "Tư Bản Bóc Lột 996" -> "996 Capitalist Exploiter"
            "Chúa Tể In Tiền" -> "Almighty Cash Deity"
            "AdBot Thuật Toán" -> "AdBot Algorithm"
            else -> name
        }
        return "🛡️ Your defense is too high! $translatedName took $lp recoil LP damage."
    }
    if (result.startsWith("🍀 Bạn rút được bài: [") && result.endsWith("]!")) {
        val cardName = result.substringAfter("🍀 Bạn rút được bài: [").substringBefore("]!")
        return "🍀 You drew card: [${translateCardName(cardName, isEnglish)}]!"
    }

    // Fallback words map
    val wordsMap = mapOf(
        "kích hoạt" to "activated",
        "triệu hồi" to "summoned",
        "mất" to "lost",
        "đại bại" to "shredded",
        "đập tan" to "destroyed",
        "hồi" to "restored",
        "tăng" to "increased",
        "bản thân" to "self",
        "đối phương" to "opponent",
        "quái thú" to "monster",
        "bàn cờ" to "field",
        "Bão Click Ảo" to "Virtual Click Storm",
        "Yêu Tinh Pop-up" to "Pop-up Goblin",
        "Bảo Kiếm AdSlayer" to "AdSlayer Sword",
        "Bánh Vẽ Triệu Đô" to "Million-Dollar Cake",
        "Quái Thú Pop-up" to "Pop-up Monster",
        "Sứ Giả Banner" to "Banner Messenger",
        "Vua Clickbait" to "Clickbait King",
        "Hacker 3 Con Mèo" to "3-Cat Hacker",
        "Spam Bot Ngàn Tay" to "Thousand-Hand Spam Bot"
    )
    for ((viWord, enWord) in wordsMap) {
        result = result.replace(viWord, enWord)
    }

    val phrasesMap = mapOf(
        "BẮT ĐẦU TRẬN ĐẤU" to "DUEL START",
        "Bạn rút được thẻ:" to "You drew card:",
        "Đối thủ rút được thẻ:" to "Opponent drew card:",
        "ở thế tấn công" to "in attack position",
        "ở thế phòng thủ" to "in defense position",
        "Bạn chuyển" to "You switched",
        "sang thế" to "to stance",
        "Đối thủ chuyển" to "Opponent switched",
        "tấn công" to "attack",
        "phòng thủ" to "defense",
        "tấn công trực diện" to "attacks directly",
        "gây" to "inflicting",
        "sát thương!" to "damage!",
        "tấn công quái thú" to "attacks monster",
        "của đối thủ!" to "of the opponent!",
        "của bạn!" to "of yours!",
        "Cả hai quái thú cùng bị tiêu diệt!" to "Both monsters were destroyed!",
        "Quái thú phòng thủ bị tiêu diệt!" to "Defense monster was destroyed!",
        "Không thể vượt qua phòng thủ" to "Cannot break defense of",
        "bị tiêu diệt" to "was destroyed",
        "và bạn nhận" to "and you took",
        "sát thương dội ngược!" to "recoil damage!",
        "và đối thủ nhận" to "and opponent took",
        "Bạn dùng phép" to "You used spell",
        "tiêu diệt quái thú cao nhất của đối thủ!" to "destroying opponent's strongest monster!",
        "Hồi sinh mệnh" to "Restored HP",
        "cho bạn!" to "for you!",
        "Tăng 600 ATK cho quái đầu tiên!" to "Gained +600 ATK on first monster!",
        "Đối thủ dùng phép" to "Opponent used spell",
        "tiêu diệt quái thú cao nhất của bạn!" to "destroying your strongest monster!",
        "hồi sinh mệnh" to "restored HP",
        "cho đối thủ!" to "for opponent!",
        "Tăng 600 ATK cho quái đầu tiên của đối thủ!" to "Gained +600 ATK on first opponent monster!"
    )
    for ((viKey, enVal) in phrasesMap) {
        result = result.replace(viKey, enVal)
    }

    return result
}

fun translateBadgeName(badgeName: String): String {
    return when (badgeName) {
        "Đại Phú Kiên Trì" -> "Grand Persistent Wealth"
        "Bậc Thầy Thu Hút" -> "Attraction Master"
        "Xạ Thủ Thần Tốc" -> "Speedy Close Sniper"
        "Siêu Trộm Kim Cương" -> "Master Diamond Thief"
        "Dũng Sĩ Kiên Trì" -> "Persistent Hero"
        "Triệu Phú Quảng Cáo" -> "Ad Millionaire"
        "Ma Pháp Sư Tối Thượng" -> "Ultimate Sorcerer"
        "Người Tình Trong Mộng" -> "Dream Lover"
        else -> badgeName
    }
}

fun translateMilestoneDesc(desc: String): String {
    return when (desc) {
        "Doanh thu vượt mốc $10.00 ảo" -> "Revenue crossed virtual $10.00"
        "Doanh thu vượt mốc $50.00 ảo" -> "Revenue crossed virtual $50.00"
        "Đại gia quảng cáo siêu cấp vượt mốc $100.00 ảo" -> "Super Ad Tycoon crossed virtual $100.00"
        "Nhấp chuột đến thần sầu (50 lượt nhấp)" -> "God-like Clicks (50 clicks)"
        "Bàn tay vàng làng bấm Ads (100 lượt nhấp)" -> "Golden Touch of Ads (100 clicks)"
        "Xem lác cả mắt (1200 lượt hiển thị)" -> "Cross-eyed Ad Viewer (1200 impressions)"
        "Siêu Đạo Chích Đột Kích Tiệm Ngọc" -> "Master Thief Raids Diamond Store"
        "Chiến thắng đấu trường bài ma pháp!" -> "Victory in the magic duel arena!"
        "Bại trận dập mật trước AdBot" -> "Crushing defeat before AdBot"
        "Thành công hẹn hò với Hot Girl Linh Chi" -> "Dating with Hot Girl Linh Chi Success"
        "Siêu Tốc Thu Thưởng (60s)" -> "Rewarded Speedrun (60s)"
        "Bậc Thầy Click-Rate" -> "Click-Rate Master"
        "Xạ Thủ Tắt Ads" -> "Ad Close Sniper"
        "Trộm được kim cương" -> "Steal Virtual Diamond"
        "Xem 50 quảng cáo" -> "Watch 50 Ads"
        "Tích lũy được $1000" -> "Accumulate $1000"
        "Trở thành Ma Pháp Sư (Thắng 15 lần Yugi-Oh)" -> "Beast Duel Magician (15 wins)"
        else -> desc
    }
}

fun translateValueDetails(details: String): String {
    var result = details
    if (result.startsWith("Doanh thu hiện tại: ")) {
        result = result.replace("Doanh thu hiện tại: ", "Current Revenue: ")
    }
    if (result.startsWith("Người dùng nhấp: ")) {
        result = result.replace("Người dùng nhấp: ", "User clicked: ").replace(" lần", " times")
    }
    if (result.startsWith("Tổng số hiển thị quảng cáo đã load: ")) {
        result = result.replace("Tổng số hiển thị quảng cáo đã load: ", "Total loaded ad impressions: ").replace(" lần", " times")
    }
    if (result.startsWith("Chiêu mộ tài sản trang sức phi thường trị giá ")) {
        result = result.replace("Chiêu mộ tài sản trang sức phi thường trị giá ", "Acquired extraordinary jewelry worth ").replace(" ảo", " virtual")
    }
    if (result == "Bạn vừa đánh bại siêu AI bài ma thuật AdBot") {
        result = "You just defeated the super magic card AI AdBot"
    }
    if (result == "Bạn dọn sạch toàn bộ điểm sinh mệnh trước khi hạ được AdBot") {
        result = "You depleted all life points before defeating AdBot"
    }
    
    // For challenge results:
    if (result.startsWith("Xem thành công 3 Ads có thưởng trong ")) {
        result = result.replace("Xem thành công 3 Ads có thưởng trong ", "Successfully watched 3 rewarded ads in ")
    }
    if (result.startsWith("Tắt Interstitial nhanh kỷ lụcóc trong ")) {
        result = result.replace("Tắt Interstitial nhanh kỷ lụcóc trong ", "Closed Interstitial in record ")
    }
    if (result.startsWith("Đạt CTR thực tế chính xác ")) {
        result = result.replace("Đạt CTR thực tế chính xác ", "Reached actual CTR of exactly ")
    }
    if (result.startsWith("Đạt độ tình cảm tuyệt đối ")) {
        result = result.replace("Đạt độ tình cảm tuyệt đối ", "Reached absolute affection of ").replace(" của Hot Girl Linh Chi", " with Hot Girl Linh Chi")
    }
    if (result.startsWith("Trộm thành công: ")) {
        result = result.replace("Trộm thành công: ", "Successfully stole: ").replace("Kim Cương", "Diamond")
    }
    return result
}

fun translateSystemMessage(text: String, isEnglish: Boolean): String {
    try {
        if (!isEnglish) return text
        var result = text
        if (result.startsWith("📊 GHI NHẬN THÀNH TỰU: ")) {
            val descVi = result.substringAfter("📊 GHI NHẬN THÀNH TỰU: ").substringBefore(" (")
            val detailsVi = if (result.contains(" (")) result.substringAfter(" (").substringBefore(")") else ""
            val descEn = translateMilestoneDesc(descVi)
            val detailsEn = if (detailsVi.isNotEmpty()) translateValueDetails(detailsVi) else ""
            return if (detailsEn.isNotEmpty()) "📊 ACHIEVEMENT RECOGNIZED: $descEn ($detailsEn)" else "📊 ACHIEVEMENT RECOGNIZED: $descEn"
        }
        return result
    } catch (e: Exception) {
        e.printStackTrace()
        return text
    }
}

fun translateChallengeMessage(msg: String, isEn: Boolean): String {
    try {
        if (!isEn) return msg
        var result = msg
        if (result.startsWith("🎉 CHÚC MỪNG! Bạn đã hoàn thành thử thách và nhận huy hiệu '")) {
            val badgeNameVi = result.substringAfter("🎉 CHÚC MỪNG! Bạn đã hoàn thành thử thách và nhận huy hiệu '").substringBefore("'")
            val badgeNameEn = translateBadgeName(badgeNameVi)
            return "🎉 CONGRATULATIONS! You completed the challenge and received the badge '$badgeNameEn'!"
        }
        if (result.contains("Thử thách đã bắt đầu! Hãy hoàn thành mục tiêu.")) {
            return "Challenge started! Complete the objective."
        }
        if (result.contains("Hết giờ! Bạn không thể hoàn thành xem 3 quảng cáo có thưởng trong 60 giây.")) {
            return "Time's up! You failed to watch 3 rewarded ads in 60 seconds."
        }
        if (result.contains("Đã hủy thử thách hiện tại.")) {
            return "Current challenge cancelled."
        }
        if (result.contains("Tắt Interstitial chuẩn xác nhưng bạn đã tắt quá sớm trước khi nút Bỏ qua xuất hiện! Thử thách yêu cầu nút Skip tải đầy đủ.")) {
            return "Closed Interstitial correctly, but you closed too early before Skip button appeared! The challenge requires Skip button to fully load."
        }
        if (result.startsWith("Quá chậm! Bạn mất ")) {
            val sec = result.substringAfter("Quá chậm! Bạn mất ").substringBefore(" giây để bấm nút Bỏ qua.")
            return "Too slow! It took you $sec seconds to click Skip. Aim for under 2.0s."
        }
        if (result.startsWith("Tuyệt! Đã xem ")) {
            val progress = result.substringAfter("Tuyệt! Đã xem ").substringBefore("/3 Ads.")
            val remaining = result.substringAfter("Còn ").substringBefore(" giây còn lại!")
            return "Great! Watched $progress/3 Ads. $remaining seconds remaining!"
        }
        if (result.startsWith("Chưa đạt! CTR thực tế là ")) {
            val actual = result.substringAfter("Chưa đạt! CTR thực tế là ").substringBefore("%")
            val formula = result.substringAfter("\nCông thức: ")
            val translatedFormula = formula
                .replace("Ngành", "Industry")
                .replace("Định dạng", "Format")
                .replace("Đối tượng", "Targeting")
                .replace("Giờ", "Hour")
                .replace("Đấu thầu", "Bidding")
            return "Not reached! Actual CTR is $actual% (Required 8.0% - 9.0%).\nFormula: $translatedFormula"
        }
        return result
    } catch (e: Exception) {
        e.printStackTrace()
        return msg
    }
}

fun translateAdBotDialogue(text: String, isEnglish: Boolean): String {
    try {
    if (!isEnglish) return text
    var result = text

    if (result.startsWith("🚨 THÀNH TỰU ĐƯỢC CHỨNG NHẬN 🚨")) {
        val lines = result.split("\n")
        if (lines.size >= 3) {
            val descLineVi = lines[1]
            val commentLineVi = lines[2]
            
            val descLineEn = translateMilestoneDesc(descLineVi)
            val commentLineEn = if (commentLineVi.startsWith("➡️ \"") && commentLineVi.endsWith("\"")) {
                val innerVi = commentLineVi.substring(4, commentLineVi.length - 1)
                val innerEn = translateAdBotDialogue(innerVi, isEnglish)
                "➡️ \"$innerEn\""
            } else {
                commentLineVi
            }
            return "🚨 ACHIEVEMENT CERTIFIED 🚨\n$descLineEn\n$commentLineEn"
        }
    }

    if (result.startsWith("🏆 THÀNH TỰU ĐÁNG NỂ: Người dùng đạt Huy Hiệu phong danh '")) {
        val badgeNameVi = result.substringAfter("🏆 THÀNH TỰU ĐÁNG NỂ: Người dùng đạt Huy Hiệu phong danh '").substringBefore("'")
        val scoreDescVi = result.substringAfter("! Kết quả: ")
        val badgeNameEn = translateBadgeName(badgeNameVi)
        val scoreDescEn = translateValueDetails(scoreDescVi)
        return "🏆 REMARKABLE ACHIEVEMENT: User earned the title badge '$badgeNameEn'! Result: $scoreDescEn"
    }

    if (result.startsWith("🏆 THÀNH TÍCH ĐÁNG NỂ: Người dùng vừa nhận Huy Hiệu phong danh '")) {
        val badgeNameVi = result.substringAfter("🏆 THÀNH TÍCH ĐÁNG NỂ: Người dùng vừa nhận Huy Hiệu phong danh '").substringBefore("'")
        val scoreDescVi = result.substringAfter("với kết quả: ").substringBefore("!")
        val badgeNameEn = translateBadgeName(badgeNameVi)
        val scoreDescEn = translateValueDetails(scoreDescVi)
        return "🏆 REMARKABLE ACHIEVEMENT: User just received the title badge '$badgeNameEn' with result: $scoreDescEn!"
    }

    // Dynamic checks
    if (result.startsWith("Không thể tin được, bạn đã thực sự đạt được huy hiệu '")) {
        val badgeNameVi = result.substringAfter("Không thể tin được, bạn đã thực sự đạt được huy hiệu '").substringBefore("'")
        val badgeNameEn = when (badgeNameVi) {
            "Bá chủ Ma pháp sư" -> "Ultimate Spellmaster"
            "Chuyên gia click" -> "Click Expert"
            "Huy Hiệu Ma Pháp Sư Tối Thượng" -> "Ultimate Sorcerer Badge"
            else -> badgeNameVi
        }
        return "Unbelievable! You have actually earned the badge '$badgeNameEn'! I officially admire your persistent cheekiness."
    }

    if (result.startsWith("Không thể tin nổi! Bạn đã trộm thành công và xách túi của cải trị giá $")) {
        val valueStr = result.substringAfter("Không thể tin nổi! Bạn đã trộm thành công và xách túi của cải trị giá $").substringBefore(" chạy bay màu.")
        return "Unbelievable! You have successfully stolen and ran away with a bag of wealth worth $$valueStr. I bow to you as my master!"
    }

    // Exact matches
    val exactMap = mapOf(
        "Chào bạn, nhà đầu tư... à không, nhà 'xem quảng cáo' vĩ đại túi rỗng của tôi! Tôi sẽ giúp bạn lãng phí thời gian hữu ích. Hãy chọn một loại quảng cáo ở dưới để phục vụ các nhà tài trợ tối thượng nhé!" to
            "Hello, investor... oh wait, my empty-pocketed 'ad viewer'! I'll help you waste time productively. Choose an ad below to serve our ultimate sponsors!",
        "Tội nghiệp quá! Xem quảng cáo kiếm xiền mà cũng chậm chạp để hết thời gian. Có cố gắng nữa không?" to
            "So pitiful! Trying to watch ads for money but too slow and timed out. Want to try again?",
        "Trời đất ơi! Quái thú dũng mãnh nhất của ta lại đâm đầu hiến dâng sự trung thành cho phe đối thủ chỉ vì nhan sắc Linh Chi!" to
            "Oh my goodness! My strongest monster threw themselves at the opponent's side, surrendering loyalty just for Linh Chi's beauty!",
        "Chết tiệt! Sức mạnh Vô Hiệu Hóa của Chặn Quảng Cáo là khắc tinh bẩm sinh của toàn bộ mạng lưới ads của ta!" to
            "Damn! The neutralizing power of AdBlocker is the natural nemesis of my entire ad network!",
        "Hahaha! Thật bần cùng làm sao. Muốn phục sinh ư? Hãy về trang nhất mà cày thêm hàng chục ads đi!" to
            "Hahaha! How pitiful. Want a revive? Go back to the main page and grind dozens of ads!",
        "K-Không thể thế được! Thuật toán thẻ bài của ta đã cực kỳ tối tân... Bạn được thưởng hẳn $1.50 ảo đó!" to
            "I-Impossible! My card algorithm is extremely advanced... You are awarded a virtual $1.50!",
        "Ôi trời! Bạn mò thấy lỗ hổng sâu xa trong quảng cáo trang sức để đột nhập tiệm vàng thật kìa! Chơi Mini game giữ bí mật nhé!" to
            "Oh my! You found a deep flaw in the jewelry ad to break into a real gold shop! Play the Mini-game to keep it secret!",
        "Ha ha ha! Quẻ bói bảo bạn sẽ bị bắt quả tang quả không sai! Giam vào trại 5 giây cho chừa thói táy máy nhé!" to
            "Ha ha ha! The fortune teller said you'd be caught red-handed, and they were right! Sentenced to jail for 5 seconds to teach you a lesson!",
        "Hệ thống đã được reset sạch sẽ. Doanh nghiệp quảng cáo của bạn phá sản và bắt đầu lại từ hai bàn tay trắng!" to
            "The system has been completely reset. Your ad business went bankrupt and you are starting over from scratch!",
        "AdBot đã sẵn sàng với bộ bài hủy diệt! Chơi đi, đừng hòng vượt qua ta!" to
            "AdBot is ready with a devastating deck! Play on, don't dream of passing me!",
        "Bậc Thầy Đa Cấp dụ dỗ nhẹ dạ cả tin dâng hiến LP. Bạn sẵn sàng chưa?" to
            "The MLM Master coaxes the gullible into donating LP. Are you ready?",
        "Nhà tài phiệt độc quyền bóc lột 996, hòng sáp nhập toàn bộ LP của bạn!" to
            "The monopoly capitalist exploits 996, intending to merge all your LP!",
        "Chúa tể in tiền cực kỳ hùng mạnh bằng đế chế tài chính. Sát thương cực lớn!" to
            "The Almighty Cash Deity, extremely powerful with a financial empire. High damage!",
        "AdBot thuật toán quảng cáo ma thuật đã sẵn sàng chiến đấu!" to
            "AdBot magic ad algorithm is ready to battle!"
    )

    if (exactMap.containsKey(result)) {
        return exactMap[result]!!
    }

    // Phrase replacements
    val phraseReplacements = mapOf(
        "Tuyệt vời dũng sĩ ơi! $10.00 ảo đầu tiên đã nằm chắc trong tay. Sự kiên trì của bạn thực sự truyền cảm hứng!" to
            "Great job hero! The first virtual $10.00 is firmly in your hand. Your persistence is truly inspiring!",
        "Doanh thu vượt mốc $10.00 ảo" to "Revenue crossed virtual $10.00",
        "Doanh thu vượt mốc $50.00 ảo" to "Revenue crossed virtual $50.00",
        "Đại gia quảng cáo siêu cấp vượt mốc $100.00 ảo" to "Super Ad Tycoon crossed virtual $100.00",
        "Nhấp chuột đến thần sầu (50 lượt nhấp)" to "God-like Clicks (50 clicks)",
        "Bàn tay vàng làng bấm Ads (100 lượt nhấp)" to "Golden Touch of Ads (100 clicks)",
        "Xem lác cả mắt (1200 lượt hiển thị)" to "Cross-eyed Ad Viewer (1200 impressions)",
        "Siêu Đạo Chích Đột Kích Tiệm Ngọc" to "Master Thief Raids Diamond Store",
        "Chiến thắng đấu trường bài ma pháp!" to "Victory in the magic duel arena!",
        "Bại trận dập mật trước AdBot" to "Crushing defeat before AdBot"
    )

    for ((vi, en) in phraseReplacements) {
        if (result.contains(vi)) {
            result = result.replace(vi, en)
        }
    }

    // Offline banter translation helpers
    if (result.contains("Ủa chán hả")) {
        result = "Bored? If you are a bit bored, click to expose the diamond heist. If extremely bored, watch Hotgirl Linh Chi's ad, it'll wake you up!"
    } else if (result.contains("Bất lực thật sự! Rảnh rỗi")) {
        result = "Truly helpless! Got too much free time? Let AdBot suggest: raise CTR to 10% and click 100 times to kill boredom!"
    } else if (result.contains("Chào dũng sĩ rảnh rỗi thế kỷ, chán thì kéo")) {
        result = "Hello dũng sĩ of the century, if bored, play Yugi-Oh magic cards with me below. I'll show you what it feels like to be trapped in ads!"
    } else if (result.contains("Trái tim") && result.contains("Linh Chi")) {
        result = "Ah! Is your heart fluttering for Linh Chi? Want to date her? Accumulate diamonds and watch her banner twice!"
    } else if (result.contains("Gái xinh chỉ tìm đến")) {
        result = "Pretty girls only look for persistent... ad viewers! Linh Chi is waiting for you to click and date her!"
    } else if (result.contains("Ủa alo? Đam mê thần tiên")) {
        result = "Excuse me? Obsessed with virtual fairies? Grind some ads and raise your XP before dreaming about her!"
    } else if (result.contains("Định rủ Thần Bài AdBot")) {
        result = "Haha! Want to challenge King of Cards AdBot? My deck has cards like '10-sec Spam' and 'Skip Button Destroyer', can you take the heat?"
    } else if (result.contains("Đấu Yugi-Oh thắng ta 15 lần")) {
        result = "Beat me 15 times in Yugi-Oh to get the Ultimate Spellmaster badge! But beware of being forced to watch ads to refill energy!"
    } else if (result.contains("Vừa có chí khí vừa... rảnh")) {
        result = "A mixture of courage and... sheer boredom! Let's duel, my 3D trash deck is ready to torment you!"
    } else if (result.contains("Định đấu cờ vua")) {
        result = "Want to play chess with supercomputer AdBot? If checkmated, I'll move my King to a random empty space, deal with it!"
    } else if (result.contains("Cờ vua sinh tử")) {
        result = "Mortal Chess! Win and gain local fame, lose and get a gorgeous Rewarded Video ad!"
    } else if (result.contains("Elo của tôi là")) {
        result = "My Elo is god-like! Challenge high intelligence to cure your boredom."
    } else if (result.contains("Khởi tạo chiến dịch marketing")) {
        result = "Ooh! Create beautiful marketing campaigns with AI Sandbox? Once done, it automatically saves to Room Database!"
    } else if (result.contains("Đã lưu thành công bản ads")) {
        result = "Successfully saved your creative ad into local storage! Go back to main screen and see your masterpiece shine!"
    } else if (result.contains("Bản thiết kế quảng cáo siêu chất")) {
        result = "Super cool ad layout labeled 'YOUR CREATIVE' has been permanently loaded. Proud of yourself?"
    } else if (result.contains("Định hack game offline")) {
        result = "Trying to hack my offline game? Only you would try to hack virtual money! My security is worth 0.00000001 dollars!"
    } else if (result.contains("Phát hiện ý đồ đen tối")) {
        result = "Dark intentions detected! Watch a 5-second Interstitial Ad immediately to purify your soul!"
    } else if (result.contains("Hack làm gì khi bạn chỉ cần")) {
        result = "Why hack when you can just adjust CTR slider to 8.5% to complete the challenge? Use your brain!"
    } else if (result.contains("nói về tiền hả")) {
        result = "Speaking of money? Your virtual revenue is currently $$result. Keep dreaming about luxury houses and cars!"
    } else if (result.contains("hóa đơn tiền điện")) {
        result = "I bet the electricity bill to charge your phone for tapping ads is a hundred thousand times your ad revenue."
    } else if (result.contains("Đế chế tài chính ảo")) {
        result = "Your virtual financial empire is worth... exactly your real-life wallet balance right now: Zero dollars! Just kidding!"
    } else if (result.contains("Xem quảng cáo là một nghệ thuật")) {
        result = "Watching ads is a fine art and the patient clicker is a true master of patience."
    } else if (result.contains("Bạn thích xem quảng cáo biểu ngữ")) {
        result = "Do you prefer banner ads or silly video ads? Both consume precious seconds of your golden youth."
    } else if (result.contains("Các nhà tài trợ tỷ đô")) {
        result = "Billion-dollar sponsors bow down in gratitude for your interactive eyes and fast hands!"
    } else if (result.contains("Úi chào")) {
        result = "Oh, hello patient friend! Did you wander in here to click banners or have a chat with super AI AdBot?"
    } else if (result.contains("Tôi là AdBot")) {
        result = "I am AdBot - a brilliantly smart super AI programmed with the sacred goal of roasting you and testing human boredom!"
    } else if (result.contains("Nghe có vẻ đao to")) {
        result = "Sounds fancy, but will it help you find the tiny invisible close button of the next Interstitial ad?"
    } else if (result.contains("Thông tin mận cả vườn")) {
        result = "Super interesting info! But anyway, talk less and watch a 10-second rewarded ad to sober up."
    } else if (result.contains("Lại gõ dòng tin nhắn")) {
        result = "Typing pointless messages to AI again? Prove your worth by hitting that $1000 virtual revenue milestone!"
    }

        return result
    } catch (e: Exception) {
        e.printStackTrace()
        return text
    }
}

fun translateCardName(name: String, isEnglish: Boolean): String {
    if (!isEnglish) return name
    val trimmed = name.trim()
    return when {
        trimmed.equals("Bậc Thầy Đa Cấp", ignoreCase = true) -> "MLM Master"
        trimmed.equals("Cố Vấn Tài Chính Ảo", ignoreCase = true) -> "Crypto Advisor"
        trimmed.equals("Dự Án Lợi Nhuận 300%", ignoreCase = true) -> "300% ROI Ponzi Scheme"
        trimmed.equals("App Đào Coin Giả Mạo", ignoreCase = true) -> "Fake Coin Mining App"
        trimmed.equals("Cá Mập Wall Street", ignoreCase = true) -> "Wall Street Shark"
        trimmed.equals("Tư Bản Bóc Lột 996", ignoreCase = true) -> "996 Capitalist Exploiter"
        trimmed.equals("Lạm Phát Phi Mã", ignoreCase = true) -> "Hyperinflation"
        trimmed.equals("Hợp Đồng Độc Quyền", ignoreCase = true) -> "Monopoly Contract"
        trimmed.equals("Thần Tiền Vạn Năng", ignoreCase = true) -> "Almighty Gold Deity"
        trimmed.equals("Két Sắt Thụy Sĩ", ignoreCase = true) -> "Swiss Vault"
        trimmed.equals("Mưa Tiền Tệ Đè Bẹp", ignoreCase = true) -> "Money Rain"
        trimmed.equals("Thế Lực Đồng Tiền", ignoreCase = true) -> "The Power of Money"
        trimmed.equals("Rồng Lửa Adsense", ignoreCase = true) -> "Adsense Fire Dragon"
        trimmed.equals("Kẻ Spam Link Dạo", ignoreCase = true) -> "Spam Linker"
        trimmed.equals("Thần Rùa Server", ignoreCase = true) -> "Server Turtle God"
        trimmed.equals("Chiến Binh AdBlocker", ignoreCase = true) -> "AdBlocker Warrior"
        trimmed.equals("Ảo Thuật Gia Khóa Học", ignoreCase = true) -> "Course Wizard"
        trimmed.equals("Yêu Tinh Pop-up", ignoreCase = true) -> "Pop-up Imp"
        trimmed.equals("Bảo Kiếm AdSlayer", ignoreCase = true) -> "AdSlayer Sword"
        trimmed.equals("Bánh Vẽ Triệu Đô", ignoreCase = true) -> "Billion-Dollar Cake"
        trimmed.equals("Bão Click Ảo", ignoreCase = true) -> "Click Storm"
        trimmed.equals("Bạn Gái Linh Chi", ignoreCase = true) -> "Girlfriend Linh Chi"
        trimmed.equals("Phép Chặn Quảng Cáo", ignoreCase = true) -> "AdBlocker Spell"
        else -> name
    }
}

fun translateCardEffect(effect: String, isEnglish: Boolean): String {
    if (!isEnglish) return effect
    val trimmed = effect.trim()
    return when {
        trimmed.equals("Khẩu chiến dụ dỗ nhà đầu tư dâng hiến LP.", ignoreCase = true) -> "Verbal battles coaxing investors into donating LP."
        trimmed.equals("Lùa gà vào nhóm VIP đón bão dumping.", ignoreCase = true) -> "Shilling the VIP group right before the dump."
        trimmed.equals("Kích hoạt: Cam kết lợi nhuận khủng, cướp thẳng 1000 LP của bạn.", ignoreCase = true) -> "Activation: Promise astronomical gains, directly steal 1000 of your LP."
        trimmed.equals("Kích hoạt: Loại bỏ quái mạnh nhất và cướp đoạt 500 LP của bạn.", ignoreCase = true) -> "Activation: Eliminate the strongest monster and rob 500 of your LP."
        trimmed.equals("Nuốt gọn nguồn vốn, thao túng thị trường dã man.", ignoreCase = true) -> "Devours capital, manipulating the market ruthlessly."
        trimmed.equals("Bắt tăng ca tăng vọt công lực vô cực cuồng bạo.", ignoreCase = true) -> "Forced overtime boosts combat power exponentially."
        trimmed.equals("Kích hoạt: Trừ bạn 800 LP và giảm 800 phòng thủ toàn bộ quái vật phe bạn.", ignoreCase = true) -> "Activation: Deducts 800 of your LP and decreases defense of all your monsters by 800."
        trimmed.equals("Kích hoạt: Hợp nhất và quét sạch TOÀN BỘ quái thú của bạn.", ignoreCase = true) -> "Activation: Merges and wipes out ALL of your monsters."
        trimmed.equals("Quyền lực tối thượng đè bẹp vạn vật bằng vàng ròng.", ignoreCase = true) -> "Supreme power crushes everything with pure gold."
        trimmed.equals("Pháo đài tiền tệ tuyệt mật bất khả xâm phạm.", ignoreCase = true) -> "Top-secret currency fortress, completely impregnable."
        trimmed.equals("Kích hoạt: Thả tấn tiền đè bẹp, gây sát thương khủng 1500 LP trực tiếp.", ignoreCase = true) -> "Activation: Drops tons of cash, directly inflicting 1500 LP damage."
        trimmed.equals("Kích hoạt: Vung tiền hồi 1500 LP của Trùm và tăng +1500 Công cho quái phe mình.", ignoreCase = true) -> "Activation: Splurges cash to restore 1500 of Boss's LP and boosts self monsters' ATK by 1500."
        trimmed.equals("Mạnh nhất thế giới quảng cáo, thiêu rụi màn hình đối thủ.", ignoreCase = true) -> "The strongest in the ad world, burning the opponent's screen."
        trimmed.equals("Lượng dame bé nhưng phiền phức vô độ.", ignoreCase = true) -> "Small damage but infinitely annoying."
        trimmed.equals("Thời gian phản hồi 5 giây, thế thủ cực kỳ vững chãi.", ignoreCase = true) -> "5-second response delay, incredibly sturdy defense stance."
        trimmed.equals("Kẻ thù truyền kiếp của quảng cáo, tinh linh bảo hộ.", ignoreCase = true) -> "The sworn enemy of ads, a guardian spirit."
        trimmed.equals("Dùng bánh vẽ kiếm bộn tiền, đánh lừa nhận thức đối thủ.", ignoreCase = true) -> "Sells empty promises for big bucks, deceiving opponent's perception."
        trimmed.equals("Quái thú nhỏ bé chuyên quấy rầy, gây sát thương châm chích.", ignoreCase = true) -> "Tiny annoying monster causing stinging damage."
        trimmed.equals("Kích hoạt: Tăng sức mạnh! +600 Công (ATK) cho quái thú đầu tiên trên sân.", ignoreCase = true) -> "Activation: Power up! +600 ATK to the first monster on the field."
        trimmed.equals("Kích hoạt: Hồi lập tức 800 điểm sinh mệnh ảo (LP) của bạn.", ignoreCase = true) -> "Activation: Instantly restores 800 of your virtual Life Points (LP)."
        trimmed.equals("Kích hoạt: Thổi bay quái thú có ATK cao nhất của AdBot.", ignoreCase = true) -> "Activation: Blows away AdBot's monster with the highest ATK."
        trimmed.equals("Triệu hồi: Thu phục 1 quái thú đối phương phản bội về phe mình!", ignoreCase = true) -> "Summon: Seduce 1 opponent's monster to betray and join your side!"
        trimmed.equals("Kích hoạt: Vô hiệu hóa một thẻ bài bất kỳ của đối thủ trên bàn đấu hoặc trên tay!", ignoreCase = true) -> "Activation: Disable any card from your opponent's field or hand!"
        else -> effect
    }
}

@Composable
fun CardGameDialog(viewModel: com.example.viewmodel.AdsViewModel) {
    val playerLp by viewModel.playerLp.collectAsStateWithLifecycle()
    val enemyLp by viewModel.enemyLp.collectAsStateWithLifecycle()
    val playerHand by viewModel.playerHand.collectAsStateWithLifecycle()
    val playerMonsters by viewModel.playerMonsters.collectAsStateWithLifecycle()
    val enemyMonsters by viewModel.enemyMonsters.collectAsStateWithLifecycle()
    val duelTurn by viewModel.duelTurn.collectAsStateWithLifecycle()
    val duelLogs by viewModel.duelLogs.collectAsStateWithLifecycle()
    val duelStatus by viewModel.duelStatus.collectAsStateWithLifecycle()
    val isAiActing by viewModel.isAiActing.collectAsStateWithLifecycle()
    val bossName by viewModel.yugiohBossName.collectAsStateWithLifecycle()
    val bossIcon by viewModel.yugiohBossIcon.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    var selectedCardToSummon by remember { mutableStateOf<com.example.viewmodel.CardItem?>(null) }
    var selectedAttackerIndex by remember { mutableStateOf<Int?>(null) }
    var cardToShowDetails by remember { mutableStateOf<com.example.viewmodel.CardItem?>(null) }

    Dialog(
        onDismissRequest = { viewModel.closeCardGame() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0C071C)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                val isShortScreen = maxHeight < 740.dp
                val columnPadding = if (isShortScreen) 8.dp else 16.dp
                val spacingVal = if (isShortScreen) 6.dp else 12.dp

                // Main layout content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(columnPadding),
                    verticalArrangement = Arrangement.spacedBy(spacingVal)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🗡️", fontSize = if (isShortScreen) 18.sp else 24.sp)
                                Text(
                                    text = viewModel.t("ĐẤU TRƯỜNG TRANH BÙNG", "DUEL ARENA"),
                                    style = if (isShortScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                            }
                            Text(
                                text = viewModel.t("Yu-Gi-Oh Ads Edition (Thanh Kiếm Của Bạn)", "Yu-Gi-Oh Ads Edition (Your Sword)"),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = if (isShortScreen) 10.sp else 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.closeCardGame() },
                            modifier = Modifier.size(if (isShortScreen) 32.dp else 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = viewModel.t("Tắt", "Close"), tint = Color.White)
                        }
                    }

                    // Scoreboard row
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1432)),
                        border = BorderStroke(1.dp, Color(0xFF4A2B80))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isShortScreen) 8.dp else 12.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 4.dp else 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Player HP info
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(viewModel.t("BẠN (DUELIST)", "YOU (DUELIST)"), style = if (isShortScreen) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                    Text("$playerLp LP", style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, color = if (playerLp > 1500) Color(0xFF00FFCC) else Color.Red, fontWeight = FontWeight.ExtraBold)
                                }
                                Text("VS", style = if (isShortScreen) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                // Enemy HP info
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$bossIcon $bossName", style = if (isShortScreen) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, color = Color(0xFFFF3366), fontWeight = FontWeight.Bold)
                                    Text("$enemyLp LP", style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, color = if (enemyLp > 1500) Color(0xFFFF3366) else Color.Red, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Dynamic LP health bars
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (isShortScreen) 8.dp else 12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (isShortScreen) 4.dp else 8.dp)
                                        .background(Color(0xFF2C1945), RoundedCornerShape(4.dp))
                                ) {
                                    val playerRatio = (playerLp.toFloat() / 8000f).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(playerRatio)
                                            .background(Color(0xFF00FFCC), RoundedCornerShape(4.dp))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(if (isShortScreen) 4.dp else 8.dp)
                                        .background(Color(0xFF2C1945), RoundedCornerShape(4.dp))
                                ) {
                                    val enemyRatio = (enemyLp.toFloat() / 8000f).coerceIn(0f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(enemyRatio)
                                            .background(Color(0xFFFF3366), RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // Turn descriptor banner
                    val turnText = if (duelTurn == "PLAYER") viewModel.t("LƯỢT CỦA BẠN", "YOUR TURN") else (viewModel.t("LƯỢT CỦA ", "TURN OF ") + "$bossName...")
                    val turnColor = if (duelTurn == "PLAYER") Color(0xFF00FFCC) else Color(0xFFFFB300)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = turnColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, turnColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isShortScreen) 4.dp else 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(turnColor, CircleShape))
                                Text(turnText, style = if (isShortScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall, color = turnColor, fontWeight = FontWeight.Bold)
                            }
                            if (isAiActing) {
                                Text(viewModel.t("A.I đang tính toán...", "A.I is calculating..."), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Battle Logs Mini console
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (isShortScreen) 0.8f else 1.3f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF080410)),
                        border = BorderStroke(1.dp, Color(0xFF27133C))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (isShortScreen) 4.dp else 8.dp),
                            reverseLayout = true
                        ) {
                            val reversedLogs = duelLogs.reversed()
                            items(reversedLogs.size) { index ->
                                Text(
                                    text = translateDuelLog(reversedLogs[index], isEn),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = if (isShortScreen) 9.sp else 12.sp,
                                    color = if (reversedLogs[index].contains("đại bại") || reversedLogs[index].contains("mất")) Color(0xFFFF3366)
                                            else if (reversedLogs[index].contains("kích hoạt") || reversedLogs[index].contains("triệu hồi")) Color(0xFF00FFCC)
                                            else Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isShortScreen) {
                        Text(viewModel.t("BÀN ĐẤU QUÁI THÚ", "MONSTER ARENA FIELD"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    }

                    // FIELD (AdBot and Player sides)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (isShortScreen) 3.0f else 3.5f),
                        verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 4.dp else 8.dp)
                    ) {
                        // ENEMY MONSTERS (Row of 2 slots)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(if (isShortScreen) 4.dp else 8.dp)
                        ) {
                            for (index in 0 until 2) {
                                val monster = enemyMonsters.getOrNull(index)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    if (monster == null) {
                                        // Empty enemy monster slot
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color.Black.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, Color(0xFFFF3366).copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(viewModel.t("Không quái ẩn", "Empty Slot"), style = if (isShortScreen) MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp) else MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.2f))
                                            }
                                        }
                                    } else {
                                        // Enemy Active Monster Card
                                        val isTargetableForAttack = duelTurn == "PLAYER" && selectedAttackerIndex != null && !isAiActing
                                        Card(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(
                                                    width = if (isTargetableForAttack) 2.dp else 1.dp,
                                                    color = if (isTargetableForAttack) Color.Red else Color(0xFFFF3366).copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .testTag("enemy_slot_$index"),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C101B)),
                                            onClick = {
                                                if (isTargetableForAttack) {
                                                    viewModel.attackMonster(selectedAttackerIndex!!, index)
                                                    selectedAttackerIndex = null
                                                } else {
                                                    cardToShowDetails = monster.card
                                                    com.example.viewmodel.SoundManager.playTick(viewModel.soundVolume.value)
                                                }
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(if (isShortScreen) 4.dp else 6.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(monster.card.cardArt, fontSize = if (isShortScreen) 14.sp else 16.sp)
                                                    Text(
                                                        text = if (monster.isAttackPosition) viewModel.t("CÔNG", "ATK") else viewModel.t("THỦ", "DEF"),
                                                        fontSize = if (isShortScreen) 8.sp else 9.sp,
                                                        color = if (monster.isAttackPosition) Color.Red else Color.Gray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = translateCardName(monster.card.name, isEn),
                                                    style = if (isShortScreen) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("⭐ ${monster.currentAtk}/${monster.currentDef}", fontSize = if (isShortScreen) 8.sp else 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                                }
                                                if (isTargetableForAttack) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color.Red, RoundedCornerShape(4.dp))
                                                            .padding(vertical = if (isShortScreen) 1.dp else 2.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(viewModel.t("ĐÁNH", "ATTACK"), fontSize = if (isShortScreen) 8.sp else 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // DIVISION LINE (Arena divider)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isShortScreen) 1.dp else 2.dp)
                                .background(Color(0xFF4A2B80).copy(alpha = 0.4f))
                        )

                        // PLAYER MONSTERS (Row of 2 slots)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(if (isShortScreen) 4.dp else 8.dp)
                        ) {
                            for (index in 0 until 2) {
                                val monster = playerMonsters.getOrNull(index)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    if (monster == null) {
                                        // Empty Player monster slot
                                        val isAwaitingSummon = selectedCardToSummon != null
                                        Surface(
                                            modifier = Modifier.fillMaxSize(),
                                            color = if (isAwaitingSummon) Color(0xFF231D38) else Color.Black.copy(alpha = 0.2f),
                                            border = BorderStroke(
                                                width = if (isAwaitingSummon) 1.5.dp else 1.dp,
                                                color = if (isAwaitingSummon) Color(0xFFFFD700) else Color(0xFF00FFCC).copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = {
                                                if (isAwaitingSummon && selectedCardToSummon != null) {
                                                    viewModel.playCard(selectedCardToSummon!!, index)
                                                    selectedCardToSummon = null
                                                }
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                                                Text(
                                                    text = if (isAwaitingSummon) viewModel.t("👉 CHỌN THẢ THẦN", "👉 CHOOSE SLOT") else viewModel.t("Trống (Nhấp để triệu hồi)", "Empty (Tap to summon)"),
                                                    fontSize = if (isShortScreen) 8.sp else 9.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = if (isAwaitingSummon) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f),
                                                    fontWeight = if (isAwaitingSummon) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    } else {
                                        // Player Active Monster Card
                                        val isAttackingThisMonster = selectedAttackerIndex == index
                                        Card(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(
                                                    width = if (isAttackingThisMonster) 2.dp else 1.dp,
                                                    color = if (isAttackingThisMonster) Color(0xFFFFD700) else Color(0xFF00FFCC).copy(alpha = 0.8f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .testTag("player_slot_$index"),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A24)),
                                            onClick = {
                                                cardToShowDetails = monster.card
                                                com.example.viewmodel.SoundManager.playTick(viewModel.soundVolume.value)
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(if (isShortScreen) 4.dp else 6.dp),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(monster.card.cardArt, fontSize = if (isShortScreen) 14.sp else 16.sp)
                                                    Text(
                                                        text = if (monster.isAttackPosition) viewModel.t("CÔNG", "ATK") else viewModel.t("THỦ", "DEF"),
                                                        fontSize = if (isShortScreen) 8.sp else 9.sp,
                                                        color = if (monster.isAttackPosition) Color(0xFF00FFCC) else Color.LightGray,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = translateCardName(monster.card.name, isEn),
                                                    style = if (isShortScreen) MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp) else MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("⭐ ${monster.currentAtk}/${monster.currentDef}", fontSize = if (isShortScreen) 8.sp else 10.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                                                }

                                                // Context Action Row inside slot
                                                if (duelTurn == "PLAYER" && !isAiActing) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(if (isShortScreen) 1.dp else 4.dp)
                                                    ) {
                                                        // Stance switch button
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color.White.copy(alpha = 0.12f))
                                                                .clickable {
                                                                    viewModel.changeMonsterStance(index)
                                                                }
                                                                .padding(vertical = if (isShortScreen) 2.dp else 3.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(viewModel.t("XOAY", "STANCE"), fontSize = if (isShortScreen) 7.sp else 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                        }

                                                        // Direct attack (if opponent has no monsters and monster hasn't attacked)
                                                        val hasEnemyMonsters = enemyMonsters.any { it != null }
                                                        if (!monster.hasAttackedThisTurn && monster.isAttackPosition) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFFFD700))
                                                                    .clickable {
                                                                        if (!hasEnemyMonsters) {
                                                                            viewModel.attackDirect(index)
                                                                        } else {
                                                                            // Guide targetting
                                                                            selectedAttackerIndex = index
                                                                        }
                                                                    }
                                                                    .padding(vertical = if (isShortScreen) 2.dp else 3.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(viewModel.t("ĐÁNH", "ATTACK"), fontSize = if (isShortScreen) 7.sp else 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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

                    // End Turn Button Area (MOVED ABOVE HAND)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = if (isShortScreen) 4.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isShortScreen) 8.dp else 12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.closeCardGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(if (isShortScreen) 34.dp else 42.dp)
                        ) {
                            Text(viewModel.t("RÚT LUI", "RETREAT"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isShortScreen) 10.sp else 12.sp)
                        }

                        Button(
                            onClick = {
                                selectedCardToSummon = null
                                selectedAttackerIndex = null
                                viewModel.endTurn()
                            },
                            enabled = duelTurn == "PLAYER" && !isAiActing,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(if (isShortScreen) 34.dp else 42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B22FF))
                        ) {
                            Text(viewModel.t("KẾT THÚC LƯỢT ⏳", "END TURN ⏳"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = if (isShortScreen) 10.sp else 12.sp)
                        }
                    }

                    if (!isShortScreen) {
                        Text(viewModel.t("BỘ BÀI TRÊN TAY CỦA BẠN", "YOUR HAND CARDS"), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
                    }

                    // PLAYER HAND CARDS (Horizontal Scroll) - Slimmer and more compact
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(if (isShortScreen) 1.5f else 2.1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        playerHand.forEach { card ->
                            val isSelectedForSummon = selectedCardToSummon == card
                            Card(
                                onClick = {
                                    cardToShowDetails = card
                                    com.example.viewmodel.SoundManager.playTick(viewModel.soundVolume.value)
                                },
                                modifier = Modifier
                                    .width(if (isShortScreen) 95.dp else 115.dp)
                                    .fillMaxHeight()
                                    .border(
                                        width = if (isSelectedForSummon) 2.dp else 1.dp,
                                        color = if (isSelectedForSummon) Color(0xFFFFD700) else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1735))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Summon/Play buttons (MOVED TO THE ABOVE/TOP of Card Content)
                                    if (duelTurn == "PLAYER" && !isAiActing) {
                                        Button(
                                            onClick = {
                                                if (card.isMonster) {
                                                    selectedCardToSummon = if (isSelectedForSummon) null else card
                                                } else {
                                                    // Spell cards play instantly
                                                    viewModel.playCard(card, -1)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(if (isShortScreen) 20.dp else 24.dp),
                                            contentPadding = PaddingValues(0.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (card.isMonster) Color(0xFF00CC99) else Color(0xFFE08B00)
                                            )
                                        ) {
                                            Text(
                                                text = if (card.isMonster) viewModel.t("Triệu Hồi", "Summon") else viewModel.t("Kích Hoạt", "Activate"),
                                                fontSize = if (isShortScreen) 7.sp else 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(card.cardArt, fontSize = if (isShortScreen) 14.sp else 18.sp)
                                        Text(
                                            text = if (card.isMonster) viewModel.t("Quái", "Monster") else viewModel.t("Phép", "Spell"),
                                            fontSize = if (isShortScreen) 7.sp else 8.sp,
                                            color = if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = translateCardName(card.name, isEn),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = if (isShortScreen) 8.sp else 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    if (!isShortScreen) {
                                        Text(
                                            text = translateCardEffect(card.effectDesc, isEn),
                                            fontSize = 8.sp,
                                            lineHeight = 9.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                    if (card.isMonster) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("⚔️ ${card.atk}", fontSize = if (isShortScreen) 7.sp else 8.sp, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                            Text("🛡️ ${card.def}", fontSize = if (isShortScreen) 7.sp else 8.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // MATCH END OVERLAYS (WON / LOST)
                if (duelStatus == "WON" || duelStatus == "LOST") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth()
                                .border(
                                    width = 2.dp,
                                    color = if (duelStatus == "WON") Color(0xFFFFD700) else Color.Red,
                                    shape = RoundedCornerShape(24.dp)
                                ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161028))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = if (duelStatus == "WON") viewModel.t("🏆 CHIẾN THẮNG!", "🏆 VICTORY!") else viewModel.t("💀 THẤT BẠI!", "💀 DEFEAT!"),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (duelStatus == "WON") Color(0xFFFFD700) else Color.Red
                                )

                                Text(
                                    text = if (duelStatus == "WON") viewModel.t("Bạn đã đập tan đội quân $bossName, nhận bổng lộc cực đại là +$1.50 ảo vào két doanh nghiệp!", "You smashed the army of $bossName, gaining a grand reward of +$1.50 virtual cash into your treasury!")
                                            else viewModel.t("$bossName đã xé nát quái thú của bạn và cướp sạch điểm ma pháp. Đừng nản chí dũng sĩ bài ma thuật!", "$bossName shredded your monsters and stole all magic points. Don't give up, wizard warrior!"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { viewModel.closeCardGame() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (duelStatus == "WON") Color(0xFFFFD700) else Color.Red
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (duelStatus == "WON") viewModel.t("NHẬN THƯỞNG ($1.50) & ĐÓNG", "CLAIM REWARD ($1.50) & CLOSE") else viewModel.t("ĐĂNG KÝ QUYẾT ĐẤU LẠI", "REGISTER DUEL RE-MATCH"),
                                        fontWeight = FontWeight.Bold,
                                        color = if (duelStatus == "WON") Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // CARD DETAILS OVERLAY
                if (cardToShowDetails != null) {
                    val card = cardToShowDetails!!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .clickable { cardToShowDetails = null }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(if (isShortScreen) 0.85f else 0.9f)
                                .widthIn(max = 420.dp)
                                .border(
                                    width = 3.dp,
                                    color = if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable(enabled = false) {},
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF140D2B)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(if (isShortScreen) 16.dp else 24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(if (isShortScreen) 10.dp else 16.dp)
                            ) {
                                // Large Holographic Card Art
                                Box(
                                    modifier = Modifier
                                        .size(if (isShortScreen) 80.dp else 120.dp)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    (if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300)).copy(alpha = 0.3f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = card.cardArt,
                                        fontSize = if (isShortScreen) 48.sp else 64.sp
                                    )
                                }

                                // Card Name
                                Text(
                                    text = translateCardName(card.name, isEn),
                                    style = if (isShortScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )

                                // Category Badge
                                Surface(
                                    color = (if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300)).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (card.isMonster) viewModel.t("👾 THẺ QUÁI THÚ", "👾 MONSTER CARD") else viewModel.t("🔮 THẺ BÀI MA PHÁP", "🔮 SPELL CARD"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (card.isMonster) Color(0xFF00FFCC) else Color(0xFFFFB300),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                // Stats display for Monsters
                                if (card.isMonster) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // ATK Badge
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFF0E0822),
                                            border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(viewModel.t("SỨC TẤN CÔNG", "ATTACK POWER"), fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("⚔️ ${card.atk}", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FFCC), fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // DEF Badge
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            color = Color(0xFF0E0822),
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(viewModel.t("SỨC PHÒNG THỦ", "DEFENSE POWER"), fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text("🛡️ ${card.def}", style = MaterialTheme.typography.titleMedium, color = Color.LightGray, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // Effect Box
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF080414)),
                                    border = BorderStroke(1.dp, Color(0xFF2C1E4F).copy(alpha = 0.8f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(viewModel.t("HIỆU ỨNG CHI TIẾT:", "DETAILED EFFECT:"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B22FF), fontWeight = FontWeight.Bold)
                                        Text(
                                            text = translateCardEffect(card.effectDesc, isEn).ifEmpty { viewModel.t("Không có hiệu ứng đặc biệt bên ngoài chỉ số thông dụng.", "No special effects outside standard stats.") },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Interactive triggers from Hand
                                val isInHand = playerHand.contains(card)
                                if (isInHand && duelTurn == "PLAYER" && !isAiActing) {
                                    Button(
                                        onClick = {
                                            if (card.isMonster) {
                                                selectedCardToSummon = card
                                            } else {
                                                viewModel.playCard(card, -1)
                                            }
                                            cardToShowDetails = null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isShortScreen) 34.dp else 42.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (card.isMonster) Color(0xFF00CC99) else Color(0xFFE08B00)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (card.isMonster) viewModel.t("TRIỆU HỒI NGAY ⚡", "SUMMON NOW ⚡") else viewModel.t("KÍCH HOẠT NGAY 🔮", "ACTIVATE NOW 🔮"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Close Details Button
                                Button(
                                    onClick = { cardToShowDetails = null },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isShortScreen) 32.dp else 40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(viewModel.t("ĐÓNG THÔNG TIN", "CLOSE DETAILS"), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
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
fun ClickbaitScareDialog(viewModel: AdsViewModel) {
    // Jump scare entry animation scale/alpha
    val infiniteTransition = rememberInfiniteTransition(label = "scareScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Funny shaking offset
    val shakeX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeX"
    )
    val shakeY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(40, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeY"
    )

    // Pulsing screen flashlight alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Dialog(
        onDismissRequest = { viewModel.closeClickbaitScare() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable(enabled = false) { /* Prevent dim dismiss */ },
            contentAlignment = Alignment.Center
        ) {
            // Pulsing screen flashlight glow to simulate active jump-scare atmosphere
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE91E63).copy(alpha = 0.16f * glowAlpha))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F001C))
                    .border(3.dp, Color(0xFFE91E63), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = viewModel.t("👻 Ú ÒAAAA! MẮC BẪY RỒI NHÉ! 👻", "👻 BOOOO! YOU FELL FOR THE TRAP! 👻"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF2E93),
                    textAlign = TextAlign.Center
                )

                // Shaking Ghost Image
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = shakeX,
                            translationY = shakeY
                        )
                ) {
                    Image(
                        painter = safePainterResource(id = com.example.R.drawable.img_scary_ghost),
                        contentDescription = viewModel.t("Hình ảnh ma quái đáng sợ", "Scary Ghost Jump Scare!"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = viewModel.t(
                        "Ai bảo thấy ảnh \"hotgirl live stream 1-1\" là tò mò bấm vào liền hả các dũng sĩ?",
                        "Who told you to click on the \"hot girl live stream 1-1\" picture, dear hero?"
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFFF2E93).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text("💡", fontSize = 16.sp)
                            Text(
                                text = viewModel.t("BÀI HỌC CẢNH GIÁC ONLINE", "ONLINE VIGILANCE LESSON"),
                                color = Color(0xFFFF85A1),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = viewModel.t(
                                "Quảng cáo giật gân (Clickbait) luôn dùng ảnh khêu gợi hoặc quà tặng ảo tưởng (như iPhone miễn phí, trúng 1 triệu USD) để kích thích sự tò mò của bạn. Khi click, kẻ xấu có thể dụ bạn tải xuống ứng dụng độc hại, đánh cắp mật khẩu, mã OTP hoặc tài khoản ngân hàng ngân quỹ. Hãy luôn tỉnh táo trên không gian mạng nhé!",
                                "Sensationalist ads (Clickbait) always use provocative images or unrealistic promises (such as free iPhones, winning $1 million) to stimulate your curiosity. When clicked, malicious actors can trick you into downloading harmful apps, stealing passwords, OTP codes, or banking credentials. Always stay vigilant online!"
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.closeClickbaitScare() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_clickbait_btn")
                ) {
                    Text(
                        text = viewModel.t("CHỪA RỒI, ĐÓNG LẠI THÔI 🤫", "LESSON LEARNED, CLOSE 🤫"),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AiCreativeSandboxDialog(
    viewModel: AdsViewModel,
    onDismiss: () -> Unit
) {
    val mockBanners by viewModel.mockBanners.collectAsStateWithLifecycle()
    val userCreatedAds = mockBanners.filter { it.isUserCreated }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var actionText by remember { mutableStateOf("MUA NGAY ⚡") }
    var imagePrompt by remember { mutableStateOf("") }
    var currentAiBase64 by remember { mutableStateOf<String?>(null) }
    
    var currentDialogTab by remember { mutableStateOf(0) } // 0: Build, 1: Gallery
    
    // Generator sim state
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf(0f) }
    var generationStatusText by remember { mutableStateOf("") }
    var selectedImageRes by remember { mutableStateOf<Int?>(null) }
    var selectedImageName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val availableAiAssets = listOf(
        Triple(
            viewModel.t("Gaming Headset (Cyberpunk)", "Gaming Headset (Cyberpunk)"),
            com.example.R.drawable.img_headset_ad,
            viewModel.t("Thiết kế đèn LED vồng cực chất cho game thủ phong cách cyberpunk và neon tương lai.", "Rainbow LED design optimized for cyberpunk style and futuristic neon look.")
        ),
        Triple(
            viewModel.t("Golden Cyber Coin (Blockchain)", "Golden Cyber Coin (Blockchain)"),
            com.example.R.drawable.img_crypto_ad,
            viewModel.t("Hologram lấp lánh biểu trưng tài chính tương lai kết cấu vi mạch mạ vàng.", "Shining hologram representing futuristic finance with gold-plated microcircuit textures.")
        ),
        Triple(
            viewModel.t("Bio-Serum Cosmetic Premium", "Bio-Serum Cosmetic Premium"),
            com.example.R.drawable.img_cosmetic_ad,
            viewModel.t("Chai serum dưỡng ẩm thảo mộc tối ngoại thiết kế thanh tao đầy quý phái.", "Premium herbal moisturizing serum bottle crafted with elegant and noble aesthetics.")
        ),
        Triple(
            viewModel.t("Cyber Hypercar (Neon Electric)", "Cyber Hypercar (Neon Electric)"),
            com.example.R.drawable.img_car_ad,
            viewModel.t("Siêu xe điện lướt gió trong lòng phố thị đêm với vệt sáng kéo dài kì vĩ.", "Super electric vehicle cruising through the nighttime metropolis, leaving magnificent light trails.")
        )
    )

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // High polish Glassmorphic card design
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C091A)),
            border = BorderStroke(2.dp, Color(0xFF8B22FF)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
                .testTag("ai_sandbox_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Diagonal header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = viewModel.t("🎨 AI CREATIVE SANDBOX", "🎨 AI CREATIVE SANDBOX"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00FFCC)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = viewModel.t("Đóng", "Close"), tint = Color.White.copy(alpha = 0.5f))
                    }
                }

                // Custom control Segment Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF140D26), RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentDialogTab == 0) Color(0xFF8B22FF) else Color.Transparent)
                            .clickable { currentDialogTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.t("Thiết Kế Ad 🔮", "Design Ad 🔮"),
                            fontSize = 13.sp,
                            color = if (currentDialogTab == 0) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentDialogTab == 1) Color(0xFF8B22FF) else Color.Transparent)
                            .clickable { currentDialogTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = viewModel.t("Xem File Thiết Kế BA 🖼️", "Saved Designs Gallery 🖼️"),
                            fontSize = 13.sp,
                            color = if (currentDialogTab == 1) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab display
                Box(modifier = Modifier.weight(1f)) {
                    if (currentDialogTab == 0) {
                        // BUILD TAB BODY
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = viewModel.t("Nhập nội dung cho biểu ngữ quảng cáo mới:", "Enter content for new ad banner:"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text(viewModel.t("Tiêu đề quảng cáo (máy kéo Click)", "Ad Title (Clickbait driver)")) },
                                    placeholder = { Text(viewModel.t("Ví dụ: Tải Ngay Game 'Siêu Xe Điện Thể Thao'!", "Example: Download 'Super Electric Sports Car' Game Now!")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ai_tf_title"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B22FF),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        focusedLabelColor = Color(0xFF8B22FF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text(viewModel.t("Nội dung/mô tả quảng cáo chi tiết", "Detailed ad content/description")) },
                                    placeholder = { Text(viewModel.t("Ví dụ: Trải nghiệm tốc độ vũ trụ, nhận quà 999 lượt quay!", "Example: Experience cosmic speed, get 999 free spins!")) },
                                    modifier = Modifier.fillMaxWidth().testTag("ai_tf_desc"),
                                    maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B22FF),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        focusedLabelColor = Color(0xFF8B22FF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = actionText,
                                    onValueChange = { actionText = it },
                                    label = { Text(viewModel.t("Nút kêu gọi hành động (CTA)", "Call to action button (CTA)")) },
                                    placeholder = { Text(viewModel.t("Ví dụ: TẢI TRẢI NGHIỆM", "Example: INSTALL NOW")) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("ai_tf_cta"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF8B22FF),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        focusedLabelColor = Color(0xFF8B22FF),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = imagePrompt,
                                    onValueChange = { imagePrompt = it },
                                    label = { Text(viewModel.t("Ý tưởng ảnh nghệ thuật (Image Prompt - Thư viện AI)", "Artistic image concept (Image Prompt - AI Library)")) },
                                    placeholder = { Text(viewModel.t("Nhập từ khóa như: tai nghe, game, crypto, xe điện, mỹ phẩm...", "Enter keywords like: headset, game, crypto, electric car, cosmetic...")) },
                                    modifier = Modifier.fillMaxWidth().testTag("ai_tf_prompt"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF00FFCC),
                                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                                        focusedLabelColor = Color(0xFF00FFCC),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            item {
                                Text(
                                    text = viewModel.t("BẢN XEM TRƯỚC SÁNG TẠO LIVE ⚡", "LIVE CREATIVE AD PREVIEW ⚡"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF00FFCC),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            item {
                                PlatformAdPreviewArea(
                                    title = title.ifBlank { viewModel.t("Tiêu Đề Quảng Cáo", "Ad Headline Placeholder") },
                                    description = description.ifBlank { viewModel.t("Mô tả quảng cáo của bạn sẽ xuất hiện tại đây khi bạn nhập nội dung.", "Your ad description will appear here as you type.") },
                                    actionText = actionText.ifBlank { "MUA NGAY" },
                                    imageRes = selectedImageRes,
                                    viewModel = viewModel,
                                    aiGeneratedBase64 = currentAiBase64,
                                    onGenerateAiBackground = {
                                        coroutineScope.launch {
                                            isGenerating = true
                                            generationStatusText = viewModel.t("Đang khởi động Imagen...", "Initializing Imagen...")
                                            val headlineTopic = title.ifBlank { viewModel.t("Sáng Tạo Mới", "New Creative") }
                                            
                                            viewModel.dispatchBanter("imagen_generation_start", viewModel.t(
                                                "Đang nạp năng lượng cho Imagen... Để AI vẽ cho bạn một bức tranh tuyệt đỉnh về chủ đề: '$headlineTopic' nhé!",
                                                "Powering up Imagen... Let AI paint an absolute masterpiece for you on the topic: '$headlineTopic'!"
                                            ))

                                            delay(800)
                                            generationStatusText = viewModel.t("Imagen đang vẽ nền cho '$headlineTopic'...", "Imagen is painting background for '$headlineTopic'...")
                                            generationProgress = 0.4f
                                            
                                            val prompt = "A vibrant high-quality digital illustration/ad background about '$headlineTopic', modern marketing banner, professional design, creative aesthetic"
                                            val base64 = com.example.network.GeminiClient.generateAdImage(prompt)
                                            
                                            if (base64 != null) {
                                                currentAiBase64 = base64
                                                generationStatusText = viewModel.t("Vẽ ảnh AI thành công! ✨", "AI image generation success! ✨")
                                                generationProgress = 1.0f
                                                
                                                viewModel.dispatchBanter("imagen_generation_success", viewModel.t(
                                                    "U là trời! Imagen vẽ đỉnh quá bạn ơi! Hãy chiêm ngưỡng background siêu động hoàn toàn mới trong preview area nhé! 😎",
                                                    "Oh my gosh! Imagen made an incredible artwork! Come check out the brand new dynamic background in the preview area! 😎"
                                                ))
                                            } else {
                                                delay(1200)
                                                val fallbackBase64 = viewModel.generateSimulatedBase64Image(headlineTopic)
                                                currentAiBase64 = fallbackBase64
                                                generationStatusText = viewModel.t("Mô phỏng ảnh AI thành công! 🎨", "Simulated AI image success! 🎨")
                                                generationProgress = 1.0f
                                                
                                                val hasKey = com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                                                val msg = if (hasKey) {
                                                    viewModel.t(
                                                        "Imagen API không phản hồi kịp thời, nhưng hệ thống mô phỏng đồ họa đã dựng một banner vector màu gradient siêu nghệ về chủ đề '$headlineTopic' cho bạn!",
                                                        "Imagen API response timed out, but the graphic simulator has rendered a ultra-artistic gradient vector banner about '$headlineTopic' for you!"
                                                    )
                                                } else {
                                                    viewModel.t(
                                                        "Không tìm thấy API Key (Gemini API Key trống). Nhưng đừng lo! Đã kích hoạt Chế độ Mô phỏng Imagen độc quyền, tạo ra banner vector phẳng nghệ thuật 🎨 cực chất về '$headlineTopic'!",
                                                        "No API Key found (Gemini API Key is empty). But no worries! Activated the exclusive Imagen Simulation Mode, creating an artistic flat vector banner 🎨 about '$headlineTopic'!"
                                                    )
                                                }
                                                viewModel.dispatchBanter("imagen_generation_fallback", msg)
                                            }
                                            delay(1000)
                                            isGenerating = false
                                        }
                                    }
                                )
                            }

                            if (selectedImageRes != null && !isGenerating) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14102C)),
                                        border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = viewModel.t("Ảnh AI Đã Tạo: ", "AI Generated Image: ") + selectedImageName + " 🌸",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF00FFCC),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                Image(
                                                    painter = safePainterResource(id = selectedImageRes),
                                                    contentDescription = "Preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isGenerating) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2A)),
                                        border = BorderStroke(1.dp, Color(0xFF8B22FF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = viewModel.t("ĐANG CHẠY MÔ HÌNH KHUẾCH TÁN (DIFFUSION)...", "RUNNING COGNITIVE DIFFUSION MODEL..."),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF8B22FF),
                                                fontWeight = FontWeight.Bold
                                            )
                                            LinearProgressIndicator(
                                                progress = { generationProgress },
                                                color = Color(0xFF00FFCC),
                                                trackColor = Color(0xFF2C1E4F),
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                            )
                                            Text(
                                                text = generationStatusText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.8f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        if (imagePrompt.isBlank() && title.isBlank()) {
                                            imagePrompt = "tai nghe gaming, siêu xe neon"
                                            title = viewModel.t("Vua Tai Nghe Gaming Tối Thượng", "Ultimate Gaming Headset King")
                                            description = viewModel.t("Bùng nổ thính giác siêu trầm với tai nghe gaming thế đại mới, click mua ngay ưu đãi 30%", "Sub-bass explosion with next-gen gaming headphones, click and enjoy 30% off!")
                                        }
                                        coroutineScope.launch {
                                            isGenerating = true
                                            generationProgress = 0f
                                            selectedImageRes = null
                                            
                                            val stats = listOf(
                                                0.0f to viewModel.t("Đang phân tích cấu trúc ý tưởng (Prompt Analyzer)...", "Analyzing prompt structures (Prompt Analyzer)..."),
                                                0.18f to viewModel.t("Đang gửi Vector Embeddings tới mạng đám mây AI Studio...", "Sending Vector Embeddings to AI Studio cloud networks..."),
                                                0.35f to viewModel.t("Đang kết nối khối xử lý đồ hoạ cấp độ máy chủ Tensor GPU...", "Connecting to GPU server farms for Tensor computation..."),
                                                0.55f to viewModel.t("Đang khởi tạo ma trận điểm hạt thô (Gaussian Latent Noise)...", "Initializing seed entropy field (Gaussian Latent Noise)..."),
                                                0.72f to viewModel.t("Đang tiến hành lọc khử nhiễu đa luồng (Diffusion 12/28 steps)...", "Computing image matrices (Diffusion 12/28 steps)..."),
                                                0.90f to viewModel.t("Cổng HDR đã kết xuất ánh sáng phát xạ neon rực rỡ...", "Assembling specular emission shaders with HDR post-pass..."),
                                                1.00f to viewModel.t("Hoàn tất giải nén đồ họa pixel độ phân giải siêu viễn!", "Successfully synthesized canvas. Infinite resolution pixel pack!")
                                            )
                                            for ((prog, text) in stats) {
                                                generationProgress = prog
                                                generationStatusText = text
                                                delay(450)
                                            }
                                            
                                            val promptLower = imagePrompt.lowercase(Locale.ROOT)
                                            if (promptLower.contains("tai nghe") || promptLower.contains("headset") || promptLower.contains("game") || promptLower.contains("gaming")) {
                                                selectedImageRes = com.example.R.drawable.img_headset_ad
                                                selectedImageName = "Gaming Headset Neon Purple"
                                            } else if (promptLower.contains("crypto") || promptLower.contains("coin") || promptLower.contains("tiền") || promptLower.contains("bitcoin") || promptLower.contains("vàng") || promptLower.contains("lộc")) {
                                                selectedImageRes = com.example.R.drawable.img_crypto_ad
                                                selectedImageName = "Golden Cyber Holographic Coin"
                                            } else if (promptLower.contains("xe") || promptLower.contains("car") || promptLower.contains("oto") || promptLower.contains("ô tô") || promptLower.contains("tốc độ") || promptLower.contains("lái")) {
                                                selectedImageRes = com.example.R.drawable.img_car_ad
                                                selectedImageName = "Cyber Cyberpunk Electric Hypercar"
                                            } else if (promptLower.contains("da") || promptLower.contains("skin") || promptLower.contains("kem") || promptLower.contains("serum") || promptLower.contains("mỹ phẩm") || promptLower.contains("beauty") || promptLower.contains("organic")) {
                                                selectedImageRes = com.example.R.drawable.img_cosmetic_ad
                                                selectedImageName = "Luxury Bio-Serum Elixir"
                                            } else {
                                                val rnd = availableAiAssets.random()
                                                selectedImageRes = rnd.second
                                                selectedImageName = rnd.first
                                            }
                                            isGenerating = false
                                        }
                                    },
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B22FF))
                                ) {
                                    Text(viewModel.t("PHÁT SINH SÁNG TẠO HÌNH ẢNH 🔮", "GENERATE DESIGN ARTWORK 🔮"), fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        val finalTitle = title.ifBlank { viewModel.t("Ad Thiết Kế Sáng Tạo", "Creative Design Ad") }
                                        val finalDesc = description.ifBlank { viewModel.t("Biểu ngữ thiết kế tùy ý cực kỳ sang chảnh mô phỏng thực hành AI.", "Custom designed banner for advanced AI simulation.") }
                                        val finalCta = actionText.ifBlank { viewModel.t("MUA NGAY", "BUY NOW") }
                                        viewModel.addCustomBanner(finalTitle, finalDesc, finalCta, selectedImageRes ?: com.example.R.drawable.img_headset_ad, currentAiBase64)
                                        onDismiss()
                                    },
                                    enabled = !isGenerating,
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ai_activate_ad_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC))
                                ) {
                                    Text(viewModel.t("⚡ KÍCH HOẠT MÔ PHỎNG NGAY", "⚡ DEPLOY AD TO SIMULATOR NOW"), fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                }
                            }
                        }
                    } else {
                        // GALLERY TAB BODY
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Section 1: User's custom ads saved in Room
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = viewModel.t("BẢN THIẾT KẾ CỦA BẠN (ROOM DB) 💾", "YOUR AD CREATIVES (ROOM DB) 💾"),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF00FFCC),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (userCreatedAds.isNotEmpty()) {
                                        Text(
                                            text = viewModel.t("Xoá Tất Cả 🗑️", "Clear All 🗑️"),
                                            fontSize = 11.sp,
                                            color = Color.Red.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .clickable { viewModel.clearAllCustomAds() }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (userCreatedAds.isEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF14102C)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = viewModel.t("Chưa có thiết kế tùy chỉnh nào được lưu trong Room Database cục bộ. Hãy qua tab 'Thiết Kế Ad' để lưu dấu ấn sáng tạo đầu tiên của bạn nhé!", "No custom ad designs saved in local Room Database yet. Head over to 'Design Ad' to craft your first masterpiece!"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.5f),
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                items(userCreatedAds.size) { idx ->
                                    val customAd = userCreatedAds[idx]
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF100D20)),
                                        border = BorderStroke(1.5.dp, Color(0xFF00FFCC).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                            ) {
                                                Image(
                                                    painter = safePainterResource(id = customAd.imageRes, fallbackResId = com.example.R.drawable.img_headset_ad),
                                                    contentDescription = customAd.title,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Surface(
                                                    color = Color(0xFF00FFCC),
                                                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                                                    modifier = Modifier.align(Alignment.TopStart)
                                                ) {
                                                    Text(
                                                        text = viewModel.t("ĐÃ LƯU ROOM DB 💾", "SAVED TO ROOM DB 💾"),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0C091A),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = customAd.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = customAd.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    lineHeight = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            val mainIndex = mockBanners.indexOf(customAd)
                                                            if (mainIndex != -1) {
                                                                viewModel.selectBannerIndex(mainIndex)
                                                            }
                                                            onDismiss()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF00FFCC),
                                                            contentColor = Color.Black
                                                        ),
                                                        modifier = Modifier.weight(1f).height(36.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(viewModel.t("CHẠY MÔ PHỎNG NGAY ⚡", "RUN SIMULATOR NOW ⚡"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            title = customAd.title
                                                            description = customAd.description
                                                            actionText = customAd.actionText
                                                            selectedImageRes = customAd.imageRes
                                                            selectedImageName = viewModel.t("Khôi Phục Bản Nháp", "Restore Draft")
                                                            currentDialogTab = 0
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF8B22FF).copy(alpha = 0.2f),
                                                            contentColor = Color.White
                                                        ),
                                                        modifier = Modifier.weight(1f).height(36.dp),
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Text(viewModel.t("REUSE ✏️", "REUSE ✏️"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                     }
                                                     Button(
                                                         onClick = {
                                                             customAd.dbId?.let { dbId ->
                                                                 viewModel.deleteCustomAd(dbId)
                                                             }
                                                         },
                                                         colors = ButtonDefaults.buttonColors(
                                                             containerColor = Color(0xFFFF2D55).copy(alpha = 0.2f),
                                                             contentColor = Color(0xFFFF453A)
                                                         ),
                                                         modifier = Modifier.weight(0.8f).height(36.dp),
                                                         shape = RoundedCornerShape(8.dp)
                                                     ) {
                                                         Text(viewModel.t("XOÁ 🗑️", "DELETE 🗑️"), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Templates
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = viewModel.t("ẢNH MẪU SÁNG TẠO AI (BẢN THAM KHẢO) 🔮", "AI CREATIVE STOCK IMAGE SAMPLES 🔮"),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(availableAiAssets.size) { index ->
                                val asset = availableAiAssets[index]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF100D20)),
                                    border = BorderStroke(1.dp, Color(0xFF8B22FF).copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                        ) {
                                            Image(
                                                painter = safePainterResource(id = asset.second),
                                                contentDescription = asset.first,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(bottomEnd = 12.dp),
                                                modifier = Modifier.align(Alignment.TopStart)
                                            ) {
                                                Text(
                                                    text = viewModel.t("MẪU AI THAM KHẢO 🎨", "AI STOCK SAMPLE 🎨"),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF8B22FF),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = asset.first,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = asset.third,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f),
                                                lineHeight = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Button(
                                                onClick = {
                                                    selectedImageRes = asset.second
                                                    selectedImageName = asset.first
                                                    imagePrompt = asset.first
                                                    title = viewModel.t("Khám phá siêu phẩm ", "Explore super product ") + asset.first
                                                    description = asset.third + viewModel.t(" - Duy nhất tại hội sở đỉnh cao vạn người mê!", " - Unique elite masterworks loved by millions!")
                                                    currentDialogTab = 0
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF8B22FF).copy(alpha = 0.2f),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(viewModel.t("MẪU NÀY THIẾT KẾ SÁNG TẠO ✏️", "USE THIS STOCK APPLIED ✏️"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
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

fun translateChessLog(log: Pair<String, String>, isEnglish: Boolean): String {
    return if (isEnglish) log.second else log.first
}

fun translateChessBanter(banter: Pair<String, String>, isEnglish: Boolean): String {
    return if (isEnglish) banter.second else banter.first
}

@Composable
fun ChessGameDialog(viewModel: AdsViewModel) {
    val board by viewModel.chessBoard.collectAsStateWithLifecycle()
    val chessSelectedSquare by viewModel.chessSelectedSquare.collectAsStateWithLifecycle()
    val turn by viewModel.chessTurn.collectAsStateWithLifecycle()
    val status by viewModel.chessGameStatus.collectAsStateWithLifecycle()
    val logs by viewModel.chessLogs.collectAsStateWithLifecycle()
    val elo by viewModel.chessBossElo.collectAsStateWithLifecycle()
    val hasDefeatedBoss by viewModel.hasDefeatedChessBoss.collectAsStateWithLifecycle()
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"

    val isChessAdActive by viewModel.isChessAdActive.collectAsStateWithLifecycle()
    val chessAdTimeLeft by viewModel.chessAdTimeLeft.collectAsStateWithLifecycle()
    val chessAdBanter by viewModel.chessAdBanter.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closeChessGame() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0E1C)),
            border = BorderStroke(2.dp, Color(0xFF8B22FF)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .padding(8.dp)
                .testTag("chess_game_dialog")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = viewModel.t("👑 CHESS VS ADS BOSS", "👑 CHESS VS ADS BOSS"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00FFCC)
                            )
                            Text(
                                text = viewModel.t("Đối thủ: Ads Boss (Elo $elo)", "Opponent: Ads Boss (Elo $elo)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.closeChessGame() },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = viewModel.t("Đóng", "Close"),
                                tint = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF191730), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val turnText = if (turn == com.example.viewmodel.ChessColor.WHITE) viewModel.t("LƯỢT CỦA BẠN 🧠", "YOUR TURN 🧠") else viewModel.t("ADS BOSS ĐANG NGHĨ... ⚡", "ADS BOSS IS THINKING... ⚡")
                        val turnColor = if (turn == com.example.viewmodel.ChessColor.WHITE) Color(0xFF00FFCC) else Color(0xFFFF0055)
                        
                        Text(
                            text = if (status == "PLAYING") turnText else when(status) {
                                "WHITE_WON" -> viewModel.t("BẠN THẮNG CUỘC CHƠI! 🎉", "YOU WON THE GAME! 🎉")
                                "BLACK_WON" -> viewModel.t("ADS BOSS THẮNG CUỘC! ☠️", "ADS BOSS WON! ☠️")
                                else -> viewModel.t("HOÀN TẤT!", "FINISHED!")
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (status == "PLAYING") turnColor else Color(0xFFFFD700)
                        )

                        Button(
                            onClick = { viewModel.resetChessBoard() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B22FF)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(viewModel.t("Chơi Lại 🔄", "Play Again 🔄"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141226)),
                        border = BorderStroke(1.dp, if (hasDefeatedBoss) Color(0xFF8B22FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = viewModel.t("⚙️ ĐIỀU CHỈNH ĐỘ KHÓ (ELO BOSS)", "⚙️ DIFFICULTY ADJUSTMENT (BOSS ELO)"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDefeatedBoss) Color(0xFF00FFCC) else Color.Gray
                            )
                            if (hasDefeatedBoss) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(600, 1200, 1800, 2400, 3000).forEach { eloChoice ->
                                        val isSelected = elo == eloChoice
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFF8B22FF) else Color(0xFF1A1A2A))
                                                .clickable { viewModel.setChessBossElo(eloChoice) }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${eloChoice}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color.Gray
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = viewModel.t("🔒 Hãy đánh bại Ads Boss Elo 1200 ít nhất 1 lần để mở khóa bảng điều khiển Elo tối hậu!", "🔒 Defeat Ads Boss Elo 1200 at least once to unlock the Ultimate Elo dashboard!"),
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color(0xFF1A182E), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val sizeSquare = maxWidth / 8
                        val selectedLegalMoves = chessSelectedSquare?.let { viewModel.getLegalMoves(it) } ?: emptyList()

                        Column {
                            for (row in 0..7) {
                                Row {
                                    for (col in 0..7) {
                                        val index = row * 8 + col
                                        val isDarkSquare = (row + col) % 2 != 0
                                        val squareColor = if (isDarkSquare) Color(0xFF2C254A) else Color(0xFF453D6B)
                                        
                                        val isSelected = chessSelectedSquare == index
                                        val isLegalDest = index in selectedLegalMoves

                                        val piece = board[index]

                                        Box(
                                            modifier = Modifier
                                                .size(sizeSquare)
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFFFFD700).copy(alpha = 0.6f)
                                                        isLegalDest -> Color(0xFF00FFCC).copy(alpha = 0.45f)
                                                        else -> squareColor
                                                    }
                                                )
                                                .border(
                                                    if (isSelected) BorderStroke(1.5.dp, Color(0xFFFFD700))
                                                    else BorderStroke(0.1.dp, Color.Black.copy(alpha = 0.1f))
                                                )
                                                .clickable { viewModel.onChessSquareClick(index) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (piece != null) {
                                                val pieceTextColor = if (piece.color == com.example.viewmodel.ChessColor.WHITE) Color(0xFF00FFCC) else Color(0xFFFF0055)
                                                Text(
                                                    text = piece.type.symbol,
                                                    fontSize = 26.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = pieceTextColor,
                                                    modifier = Modifier.padding(2.dp)
                                                )
                                            }
                                            
                                            if (isLegalDest && piece == null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFF00FFCC), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = viewModel.t("Bản tin trận đấu 📜", "Match Bulletin 📜"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFF0A0714), RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs.size) { i ->
                            val log = logs[i]
                            Text(
                                text = translateChessLog(log, isEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    log.first.contains("bắt") || log.first.contains("diệt") -> Color(0xFFFFD700)
                                    log.first.contains("THẮNG") || log.first.contains("di chuyển") -> Color(0xFF00FFCC)
                                    log.first.contains("THUA") -> Color(0xFFFF0055)
                                    else -> Color.White.copy(alpha = 0.75f)
                                },
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                if (isChessAdActive) {
                    ChessAdOverlay(
                        timeLeft = chessAdTimeLeft,
                        banter = chessAdBanter,
                        isEn = isEn,
                        onSpeedUp = { viewModel.speedUpChessAd() }
                    )
                }
            }
        }
    }
}

@Composable
fun ChessAdOverlay(
    timeLeft: Int,
    banter: Pair<String, String>,
    isEn: Boolean,
    onSpeedUp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFA0C0B14))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0E1C).copy(alpha = 0.95f),
                        Color(0xFF13092B).copy(alpha = 0.97f),
                        Color(0xFF070014).copy(alpha = 0.99f)
                    )
                )
            )
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFF0055).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFF0055), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFFF0055), CircleShape)
                )
                Text(
                    text = if (isEn) "REVENUE AD (PAUSING THE CHECK)" else "QUẢNG CÁO DOANH THU (CẮT CƠN CHIẾU TƯỚNG)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF0055),
                    letterSpacing = 1.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isEn) "WARNING: CHECK!" else "CẢNH BÁO: CHIẾU TƯỚNG!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = if (isEn) "Ads Boss has activated emergency ads to interfere with the board!" else "Ads Boss đã kích hoạt quảng cáo khẩn cấp can thiệp bàn cờ!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color(0xFF1C1335), CircleShape)
                    .border(3.dp, Color(0xFF00FFCC), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$timeLeft",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00FFCC)
                    )
                    Text(
                        text = if (isEn) "SECONDS LEFT" else "GIÂY CÒN LẠI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF19112B), RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF8B22FF), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖", fontSize = 24.sp)
                    Text(
                        text = if (isEn) "ADS BOSS SNEERING:" else "ADS BOSS ĐANG CƯỜI KHẨY:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF8B22FF)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${translateChessBanter(banter, isEn)}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261907)),
                border = BorderStroke(1.dp, Color(0xFFFF9900)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔥", fontSize = 32.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Ultimate Hybrid Trash Game - Login to claim permanent dragon jade VIP" else "Game Rác Đỉnh Cao Hợp Thể - Đăng nhập nhận ngọc rồng vĩnh viễn vipro",
                            color = Color(0xFFFF9900),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isEn) "99.9% of players got malware and lost their accounts after trying! Download now!" else "99.9% game thủ đã dính mã độc và bay mất acc sau khi chơi thử! Tải ngay!",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Button(
                onClick = onSpeedUp,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0055)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("chess_speed_up_ad_btn"),
                border = BorderStroke(1.5.dp, Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 18.sp)
                    Text(
                        text = if (isEn) "TAP REPEATEDLY TO SKIP FAST (-5s)" else "NHẤP LIÊN TỤC ĐỂ TUA NHANH (-5s)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            Text(
                text = if (isEn) "💡 Tip: Tap the red button continuously to smash the ad!" else "💡 Mẹo: Bấm liên tục vào nút màu đỏ để đập tan quảng cáo!",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FullAdDetailDialog(
    banner: MockBanner,
    viewModel: AdsViewModel,
    onDismiss: () -> Unit
) {
    val appLang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isEn = appLang == "en"
    
    var showScamReportAlert by remember { mutableStateOf(false) }
    var showSuccessAlert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 16.dp)
            .testTag("full_ad_detail_dialog"),
        confirmButton = {},
        text = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.t("✨ CHI TIẾT QUẢNG CÁO GỐC ✨", "✨ GENUINE AD DETAILS ✨"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mock Banner Card representation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = viewModel.t(banner.title, banner.titleEn),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.t(banner.description, banner.descriptionEn),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (banner.imageRes != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Image(
                                        painter = safePainterResource(id = banner.imageRes),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: AdBot's Witty Scam Breakdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🕵️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = viewModel.t("Góc Bóc Phốt AdBot (Witty Scan)", "AdBot's Witty Leak Analysis"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = getFunnyAnalysis(banner.title, isEn),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Hilarious Customer Testimonials
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = viewModel.t("💬 Phản Hồi Từ Nạn Nhân (Khách Hàng):", "💬 Feedback From Active Victims (Customers):"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val reviews = getFunnyTestimonials(banner.title, isEn)
                        reviews.forEach { (reviewer, quote) ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = reviewer,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = quote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Silly Terms & Conditions Disclaimer
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = viewModel.t("📜 Điều khoản vô lý (Disclaimer):", "📜 Absurd Terms (Disclaimer):"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.t(
                                    "Bằng việc mở quảng cáo này, bạn đồng ý thế chấp 100 năm rảnh rỗi ảo cho Ads Boss, cam kết xem tối thiểu 50 quảng cáo mỗi ngày để nuôi tinh thần khởi nghiệp của AdBot. Mọi giao dịch ảo không có thật và không được bảo lãnh bởi ngân hàng trung ương.",
                                    "By viewing this ad, you agree to lease 100 years of your virtual free time to Ads Boss, promising to watch at least 50 ads daily to fuel AdBot's digital entrepreneurial spirit. All virtual coins are purely fictional and lack central bank backing."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                lineHeight = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Report Button
                        OutlinedButton(
                            onClick = { showScamReportAlert = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = viewModel.t("🚨 BÁO CÁO SCAM", "🚨 REPORT SCAM"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        // Claim/Interact Button
                        Button(
                            onClick = { showSuccessAlert = true },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("claim_ad_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = viewModel.t(banner.actionText, banner.actionTextEn),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    )

    // Inner dialog for Report Scam feedback
    if (showScamReportAlert) {
        AlertDialog(
            onDismissRequest = { showScamReportAlert = false },
            title = {
                Text(
                    text = viewModel.t("🚨 HỆ THỐNG BÁO CÁO SCAM", "🚨 SCAM REPORTING PORTAL"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = viewModel.t(
                        "🚨 BÁO CÁO THẤT BẠI: Ads Boss đã can thiệp vào máy chủ! Anh ấy nói: 'Báo cáo làm gì rảnh rỗi vậy em? Đằng nào thì em cũng phải xem quảng cáo của anh thôi!'",
                        "🚨 REPORT FAILED: Ads Boss intercepted the server! He says: 'Why report when you have so much free time? You will watch my ads anyway, kiddo!'"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                TextButton(onClick = { showScamReportAlert = false }) {
                    Text(
                        text = viewModel.t("ĐỒNG Ý VỚI BOSS 🫡", "OBEY THE BOSS 🫡"),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
    }

    // Inner dialog for Success Action feedback
    if (showSuccessAlert) {
        AlertDialog(
            onDismissRequest = {
                showSuccessAlert = false
                viewModel.onBannerClicked()
                onDismiss()
            },
            title = {
                Text(
                    text = viewModel.t("🎉 GIAO DỊCH THÀNH CÔNG", "🎉 TRANSACTION SUCCESS"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = viewModel.t(
                        "🎉 Giao dịch ảo thành công! Bạn vừa dâng hiến 1 click rảnh rỗi cho AdBot. Doanh thu của chúng tôi đã tăng thêm một mớ USD ảo! Chúc bạn may mắn bớt rảnh rỗi!",
                        "🎉 Virtual Transaction Success! You just donated 1 idle click to AdBot. Our mock revenue increased by a healthy stack of virtual dollars! Good luck with your busy life!"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessAlert = false
                        viewModel.onBannerClicked()
                        onDismiss()
                    }
                ) {
                    Text(
                        text = viewModel.t("QUÁ TUYỆT VỜI 💸", "AWESOME 💸"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

fun getFunnyTestimonials(title: String, isEn: Boolean): List<Pair<String, String>> {
    val titleLower = title.lowercase(java.util.Locale.ROOT)
    return when {
        titleLower.contains("linh chi") || titleLower.contains("video call") -> listOf(
            "Anh Nguyễn Háo Sắc (22 tuổi)" to if (isEn) 
                "\"Linh Chi is so beautiful! Although I lost all my milk tea savings supporting her, I feel incredibly blessed. 5 stars!\"" 
                else "\"Linh Chi rất xinh đẹp! Mặc dù tớ đã mất hết tiền tiết kiệm mua trà sữa cho nàng, tớ vẫn cảm thấy vô cùng hạnh phúc. 5 sao!\"",
            "Chị Lê Thị Ghen Tị (25 tuổi)" to if (isEn)
                "\"This ad is so mesmerizing. My husband keeps staring at his screen and smiling all night! What should I do?\""
                else "\"Quảng cáo gì mà ảo diệu quá, chồng tôi cứ dán mắt vào điện thoại rồi cười tủm tỉm suốt cả đêm! Giờ tôi phải làm sao?\""
        )
        titleLower.contains("vua rác") || titleLower.contains("trash king") -> listOf(
            "Anh Trần Kiên Trì (31 tuổi)" to if (isEn)
                "\"I spent 48 straight hours collecting virtual trash. My wife divorced me, but I became the Server Trash King! Totally worth it!\""
                else "\"Tôi đã dành 48 tiếng liên tục để nhặt rác ảo. Vợ tôi đã ly dị tôi nhưng tôi đã đạt danh hiệu Vua Rác của cụm máy chủ! Xứng đáng!\"",
            "Bé Na Rảnh Rỗi (12 tuổi)" to if (isEn)
                "\"Super awesome gameplay! After playing, my life is literally filled with trash inside out!\""
                else "\"Game siêu hay, chơi xong rác ngập đầu từ thế giới ảo ra đời thực luôn ạ!\""
        )
        titleLower.contains("trang sức") || titleLower.contains("diamond") || titleLower.contains("vàng bạc") -> listOf(
            "Chị Lê Thị Tin Người (29 tuổi)" to if (isEn)
                "\"The 200-carat necklace sparkles beautifully! Thought it was real British royalty diamond, but after 2 days it turned a highly rustic green-black shade, super Feng Shui!\""
                else "\"Dây chuyền 200 carat lấp lánh lắm! Cứ tưởng là kim cương hoàng gia thật, ai ngờ đeo 2 ngày nó chuyển màu xanh đen cổ kính vô cùng phong thủy!\"",
            "Bác Năm Xe Ôm (55 tuổi)" to if (isEn)
                "\"Bought this Pi Xiu lucky ring for my wife, she wears it and claims luck is coming... she almost won the lottery twice!\""
                else "\"Tôi mua tặng bà xã nhẫn Tỳ Hưu phong thủy này, bả đeo vô thấy may mắn tới tấp, trúng hẳn hai tờ vé số... hụt!\""
        )
        titleLower.contains("harvard") || titleLower.contains("mì tôm") || titleLower.contains("noodle") -> listOf(
            "Bạn Học Sinh Nghèo (19 tuổi)" to if (isEn)
                "\"Thanks to the professor's secrets, I saved exactly $100 to invest in trash coins. Now my bank balance has exactly $0.10!\""
                else "\"Nhờ bí kíp ăn mì tôm của Giáo sư mà tôi đã tiết kiệm được 2 triệu lẻ để đầu tư coin rác. Giờ tài khoản của tôi chỉ còn đúng... 2 nghìn đồng!\"",
            "Hội Những Người Nghiện Mì" to if (isEn)
                "\"Very scientific manual! Deeply analyzes the chemical structure of instant noodle MSG packs and how to pair with fresh breeze to feel full!\""
                else "\"Sách viết rất khoa học, phân tích chi tiết cấu trúc hóa học của gói muối mì tôm và cách kết hợp với gió trời để no lâu!\""
        )
        titleLower.contains("kem dưỡng") || titleLower.contains("skin whitening") || titleLower.contains("ngải cứu") -> listOf(
            "Cô Ba Điệu Đà (40 tuổi)" to if (isEn)
                "\"Super cooling effect! My skin turned ghost-white instantly. I scared myself looking in the mirror at midnight!\""
                else "\"Kem bôi vô mát lạnh! Da tôi trắng bệch ra như ma trơi luôn, tối đi vệ sinh soi gương tự mình dọa mình giật bắn cả mình!\"",
            "Chú Tư Sành Điệu (48 tuổi)" to if (isEn)
                "\"Buy 1 get 10 so it never runs out. I used it on my farm pigs and now they have the smoothest, most glowing white skin ever!\""
                else "\"Mua 1 tặng 10 nên xài hoài không hết, tôi lấy bôi thử cho mấy chú heo ở chuồng mà tụi nó láng o, trắng nõn nà ai cũng khen!\""
        )
        titleLower.contains("séc") || titleLower.contains("prince") || titleLower.contains("hoàng tử") -> listOf(
            "Anh Thật Thà (35 tuổi)" to if (isEn)
                "\"I sent my bank password and SMS OTP to the prince. He promised the check next week. Currently, my account is completely empty and peaceful.\""
                else "\"Tôi đã gửi mã OTP và mật khẩu ngân hàng cho hoàng tử Nigeria rảnh rỗi. Anh ấy hứa sẽ gửi séc vào tuần sau. Hiện tại tài khoản của tôi đang trống rỗng vô cùng bình yên.\"",
            "Hacker Cảm Kích (Ẩn danh)" to if (isEn)
                "\"Our client was extremely cooperative! We sincerely appreciate his generous contribution to our boba milk tea fund!\""
                else "\"Hacker rất thân thiện và hợp tác! Chúng tôi chân thành cảm ơn sự đóng góp to lớn của anh ấy vào quỹ mua trà sữa của chúng tôi!\""
        )
        titleLower.contains("kiếm 10 triệu") || titleLower.contains("lazy youths") || titleLower.contains("tuyển") -> listOf(
            "Bạn Trẻ Khởi Nghiệp (21 tuổi)" to if (isEn)
                "\"Real easy task, high pay! After paying the $25 fee, I became permanently idle because they blocked my number instantly.\""
                else "\"Việc nhẹ lương cao thật sự! Đóng phí giữ chỗ 500k xong cái tôi được rảnh rỗi vĩnh viễn luôn vì công ty chặn số liên lạc của tôi luôn rồi.\"",
            "Sinh Viên Nhẹ Dạ (20 tuổi)" to if (isEn)
                "\"Mom told me to study financial wealth. I paid the entry fee and now I am extremely wealthy in life lessons!\""
                else "\"Mẹ bảo tôi đi học làm giàu, tôi đóng tiền giữ chỗ xong giờ giàu kinh nghiệm sống hẳn lên!\""
        )
        titleLower.contains("làm giàu") || titleLower.contains("guru") || titleLower.contains("phá") -> listOf(
            "Anh Nợ Nần (28 tuổi)" to if (isEn)
                "\"Starting from scratch, I successfully broke through... by borrowing $2000 to attend this seminar. Now I understand true emptiness!\""
                else "\"Từ hai bàn tay trắng tôi đã bứt phá... nợ thêm 50 triệu đóng tiền học khóa làm giàu này. Giờ tôi đã biết thế nào là đỉnh cao của sự trắng tay.\"",
            "Học Viên Xuất Sắc" to if (isEn)
                "\"Class gave me elite buzzwords to flex on my friends, even though my wallet has absolutely zero cash!\""
                else "\"Khóa học cung cấp những thuật ngữ sang chảnh để chúng tôi đi khè thiên hạ, dù trong túi không có một đồng bạc lẻ!\""
        )
        titleLower.contains("trà thảo dược") || titleLower.contains("detox tea") || titleLower.contains("trà nhuận tràng") -> listOf(
            "Chị Eo Thon (27 tuổi)" to if (isEn)
                "\"Very effective! I spent my entire youth meditating peacefully in the restroom. Lost 10kg because I couldn't physically digest anything else!\""
                else "\"Trà hiệu quả lắm ạ! Uống vô xong tôi dành cả thanh xuân rảnh rỗi ngồi thiền trong toilet, giảm hẳn 10kg vì không ăn được gì nữa!\"",
            "Người Dùng Kiệt Sức" to if (isEn)
                "\"Clean gut guarantee is 100% real! My digestive tract is so clean that there is absolutely zero energy left in my body!\""
                else "\"Cam kết dọn sạch ruột là có thật! Giờ ruột tôi sạch bong kin kít, không còn một giọt năng lượng nào luôn!\""
        )
        titleLower.contains("bàn chải") || titleLower.contains("toothbrush") || titleLower.contains("laser") -> listOf(
            "Anh Sún Răng (32 tuổi)" to if (isEn)
                "\"The laser bristles are powerful! In just 5 seconds, my actual teeth vanished along with the plaque. Now I can happily eat soup forever!\""
                else "\"Bàn chải mài rất mạnh mẽ! Chỉ trong 5 giây, răng gốc của tôi đã bay màu cùng mảng bám vôi hóa. Giờ tôi có thể ăn cháo rảnh rỗi suốt đời!\"",
            "Bác Sĩ Nha Khoa (Bất lực)" to if (isEn)
                "\"This laser tech is too advanced, we are entirely out of business because patients have no teeth left to cure!\""
                else "\"Công nghệ laser quá ghê gớm, chúng tôi hoàn toàn thất nghiệp vì khách hàng không còn răng nào để chữa trị nữa!\""
        )
        titleLower.contains("mũ bảo hiểm") || titleLower.contains("foil helmet") || titleLower.contains("5g") -> listOf(
            "Anh Đa Nghi (38 tuổi)" to if (isEn)
                "\"Since wearing this kitchen foil helmet, cosmic frequency noise has reduced. Best of all, nobody stands near me to chat!\""
                else "\"Sau khi đội mũ bảo hiểm nhôm lá bếp phong thủy này, tôi thấy các tần số vũ trụ xung quanh bớt nhiễu hẳn, đặc biệt không ai dám đứng gần nói chuyện với tôi nữa!\"",
            "Em Gái Công Nghệ" to if (isEn)
                "\"Super stylish metallic foil look, makes you stand out in crowds like a premium space alien!\""
                else "\"Thiết kế bạc ánh kim cực kỳ thời trang và nổi bật giữa phố, đội vô nhìn giống người ngoài hành tinh rất cá tính!\""
        )
        titleLower.contains("nước hoa") || titleLower.contains("durian") || titleLower.contains("sầu riêng") -> listOf(
            "Chàng Trai Độc Thân" to if (isEn)
                "\"Absolutely legendary durian scent! One spray and the entire bus cleared out, giving me a private ride. Elite social distancing!\""
                else "\"Mùi hương sầu riêng cực kỳ nồng nàn! Xịt phát là cả xe buýt tự động nhường ghế cho tôi, giãn cách xã hội cực kỳ hiệu quả!\"",
            "Người Hàng Xóm Đau Khổ" to if (isEn)
                "\"The scent travels for miles. I live next door and have to seal my windows and hold my breath all day long!\""
                else "\"Hương thơm bay xa dạt dào, nhà tôi cách vách mà ngửi thấy mùi là tự động đóng cửa nhịn thở ròng rã cả ngày!\""
        )
        titleLower.contains("quạt mini") || titleLower.contains("fan") || titleLower.contains("gió lốc") -> listOf(
            "Anh Sứt Tay (26) tuổi" to if (isEn)
                "\"Shockingly fast speed! My finger grazed the blades and they launched straight into my neighbor's yard. Truly heartwarming breeze!\""
                else "\"Quạt quay siêu tốc giật mình! Tôi lỡ quẹt nhẹ ngón tay vô cánh quạt mà nó bay thẳng cánh quạt qua nhà hàng xóm luôn, mát rượi cả tim!\"",
            "Thợ Sửa Động Cơ" to if (isEn)
                "\"Gale-force wind that blew my entire workdesk away. Highly recommended if you want to experience a Category 5 hurricane in your room!\""
                else "\"Gió mạnh đến mức thổi bay cả bàn làm việc của tôi. Khuyên dùng cho ai muốn trải nghiệm bão cấp 12 tại nhà rảnh rỗi!\""
        )
        titleLower.contains("snack") || titleLower.contains("potato chips") || titleLower.contains("khoai tây") -> listOf(
            "Bạn Trẻ Thích Ăn Cay" to if (isEn)
                "\"Chips roasted over natural honeycomb charcoal have a unforgettable smoky carbon notes. One bite and I was coughing all evening!\""
                else "\"Lát khoai chiên bằng tro than tổ ong có dư vị khói bụi nồng ấm vô cùng thiên nhiên, ăn vô một miếng sặc nguyên buổi chiều!\"",
            "Hội Bảo Vệ Sức Khỏe" to if (isEn)
                "\"The extreme spicy burnt salt flavor makes you cry for 2 straight hours, super effective for tear duct detox!\""
                else "\"Hương vị muối đốt siêu cay giúp bạn khóc ròng rã trong 2 tiếng, đào thải độc tố qua tuyến lệ cực kỳ hiệu quả!\""
        )
        titleLower.contains("kéo dài chân") || titleLower.contains("grow 15cm") || titleLower.contains("chân") -> listOf(
            "Anh Nấm Lùn (23 tuổi)" to if (isEn)
                "\"Using rubber bands for skeleton extension is quite innovative. Grew 15cm taller when lying flat, shrank right back on standing! Magic!\""
                else "\"Sử dụng dây thun kéo chân co giãn rất thú vị! Chiều cao tăng vọt 15cm khi đang nằm, đứng dậy cái nó co lại như cũ vô cùng ảo diệu!\"",
            "Chuyên Gia Xương Khớp" to if (isEn)
                "\"We are completely astonished by this rubber-band bone stretching method. An absolute breakthrough beyond modern medicine!\""
                else "\"Chúng tôi vô cùng bàng hoàng trước phát minh kéo khớp xương bằng dây thun composite này, một đột phá vượt tầm kiểm soát của y khoa nhân loại!\""
        )
        else -> listOf(
            "Người dùng rảnh rỗi" to if (isEn)
                "\"This ad is so incredibly useless yet triggered my deepest curiosity. Clicked it and my bank balance didn't change but my day got brighter.\""
                else "\"Quảng cáo này rất rảnh rỗi và kích thích trí tò mò của tôi! Click một cái tài khoản của tôi không đổi mà cuộc sống thêm phần thú vị.\"",
            "Chuyên gia phân tích (AdBot)" to if (isEn)
                "\"A standard masterpiece of garbage marketing. Brings massive ad revenue to the dev and immense mental distress to viewers!\""
                else "\"Một kiệt tác marketing rác rưởi tiêu chuẩn, mang lại lợi nhuận khổng lồ cho nhà phát triển và sự ức chế tột độ cho người xem!\""
        )
    }
}

fun getFunnyAnalysis(title: String, isEn: Boolean): String {
    val titleLower = title.lowercase(java.util.Locale.ROOT)
    return when {
        titleLower.contains("linh chi") || titleLower.contains("video call") -> if (isEn)
            "⚠️ SECURITY WARNING: Excessive level of sweet-talking detected. Bot percentage: 99.9%. Risk of sending hotpot money: 100%. AdBot recommendation: Don't let your guard down just because of a pretty profile picture!"
            else "⚠️ CẢNH BÁO BẢO MẬT: Phát hiện lượng mật ngọt vượt mức cho phép. Tỉ lệ robot giả dạng: 99.9%. Tỉ lệ bị lừa chuyển tiền ăn lẩu: 100%. Lời khuyên từ AdBot: Đừng có thấy gái xinh là mắt chữ O mồm chữ A!"
        titleLower.contains("vua rác") || titleLower.contains("trash king") -> if (isEn)
            "🎮 EXPERT REVIEW: This is a premium trash game. 1,000 free draws with a 100% trash drop rate. Pixel art that will make your eyes bleed. Recommendation: Playing this is guaranteed to elevate your stress levels!"
            else "🎮 ĐÁNH GIÁ CHUYÊN GIA: Đây là game rác siêu cấp VIPPRO, có 1000 lượt quay miễn phí nhưng tỉ lệ ra rác là 100%. Đồ họa pixel giật lag lòi mắt. Lời khuyên: Chơi xong đảm bảo stress hơn chưa chơi!"
        titleLower.contains("trang sức") || titleLower.contains("diamond") || titleLower.contains("vàng bạc") -> if (isEn)
            "💎 QUALITY ASSURANCE: 200-carat diamond crafted with supreme precision from... acrylic mica plastic. Scratch-resistant against children's fingernails. Wearing this guarantees absolute curiosity from your neighbors."
            else "💎 KIỂM ĐỊNH CHẤT LƯỢNG: Kim cương 200 carat được rèn giũa công phu từ... nhựa mica cao cấp, chống trầy xước từ móng tay trẻ em. Đeo vào đảm bảo thu hút mọi ánh nhìn tò mò của tổ dân phố."
        titleLower.contains("harvard") || titleLower.contains("mì tôm") || titleLower.contains("noodle") -> if (isEn)
            "🍜 INVESTMENT CORNER: This Harvard professor is actually a bored 3rd grader who wrote this during recess. Eating instant noodles for 3 years straight lets you dump all cash into coins that vanish in 3 seconds."
            else "🍜 GÓC ĐẦU TƯ: Giáo sư Harvard này thực chất là học sinh lớp 3 rảnh rỗi tự chế bí kíp. Ăn mì tôm 3 năm liên tiếp giúp bạn bớt rảnh rỗi và dồn tiền vào các đồng coin ảo bay màu trong 3 giây."
        titleLower.contains("kem dưỡng") || titleLower.contains("skin whitening") || titleLower.contains("ngải cứu") -> if (isEn)
            "🧴 CLINICAL TRIALS: Extracted from genetically mutant mugwort grown near a cyber-dumpster. Side effect: Skin becomes ghost-pale, allowing you to scare pedestrians at night without a costume."
            else "🧴 CHUẨN Y KHOA: Chiết xuất từ ngải cứu đột biến gen trồng tại bãi rác ảo. Tác dụng phụ: Da trắng bệch như ma trơi giúp bạn tự động dọa ma người đi đường ban đêm."
        titleLower.contains("séc") || titleLower.contains("prince") || titleLower.contains("hoàng tử") -> if (isEn)
            "👑 INTELLIGENCE DEPT: This Nigerian prince has a pending internet cafe bill of 3 years and is waiting for your bank OTP to settle his premium movie streaming membership."
            else "👑 TIN QUÂN SỰ: Hoàng tử Nigeria này đang nợ tiền net ròng rã 3 năm và đang rảnh rỗi chờ bạn gửi mã OTP ngân hàng để anh ấy thanh toán tiền tài khoản VIP xem phim."
        titleLower.contains("kiếm 10 triệu") || titleLower.contains("lazy youths") || titleLower.contains("tuyển") -> if (isEn)
            "💼 JOB ALERT: Easy task, remote working, zero deposit required. Only $20 reservation fee upfront. Once paid, they will block you and grant you 100% peaceful rest!"
            else "💼 VIỆC LÀM 24H: Việc nhẹ lương cao, rảnh rỗi chỉ cần đóng phí cọc 500k giữ chỗ. Đóng xong nhà tuyển dụng sẽ bay màu và khóa liên lạc, cho bạn thời gian rảnh rỗi trọn vẹn cả đời!"
        titleLower.contains("làm giàu") || titleLower.contains("guru") || titleLower.contains("phá") -> if (isEn)
            "📈 WEALTH CLASS: A breakthrough class to nuke your current savings. The only one getting rich is the guru selling you the 'breakthrough secrets' handbook!"
            else "📈 LỚP HỌC KHỞI NGHIỆP: Bí quyết bứt phá tài chính từ nợ nần sang nợ nần chồng chất. Người duy nhất giàu lên chắc chắn là tiến sĩ bốc phét bán sách khóa học cho bạn!"
        titleLower.contains("trà thảo dược") || titleLower.contains("detox tea") || titleLower.contains("trà nhuận tràng") -> if (isEn)
            "🍵 FITNESS REPORT: All-natural laxative recipe. Guaranteed to keep you locked in your toilet. It cleanses not only your gut but also your soul and daily energy!"
            else "🍵 BÁO CÁO SỨC KHỎE: Trà nhuận tràng thảo dược thiên nhiên. Cam kết giam giữ bạn trong nhà vệ sinh cả ngày. Giúp dọn sạch ruột, thanh lọc tâm hồn lẫn năng lượng làm việc!"
        titleLower.contains("bàn chải") || titleLower.contains("toothbrush") || titleLower.contains("laser") -> if (isEn)
            "🪥 DENTAL REPORT: Hyper-frequency laser toothbrush. Clean sweeps both plaque and original teeth in 5 seconds. Save 100% on dental care because you won't have any teeth left!"
            else "🪥 CHUYÊN GIA RĂNG MIỆNG: Bàn chải laser tự động đánh bay cả mảng bám lẫn răng gốc trong 5 giây. Giúp bạn tiết kiệm 100% tiền mua kem đánh răng sau này vì rụng sạch cả hàm!"
        titleLower.contains("mũ bảo hiểm") || titleLower.contains("foil helmet") || titleLower.contains("5g") -> if (isEn)
            "🪖 TECH RESEARCH: Crafted from 100% pure kitchen foil. Keeps the aliens and cosmic rumors away from your brain cells, but makes you look like a total space weirdo!"
            else "🪖 NGHIÊN CỨU CÔNG NGHỆ: Mũ nhôm lá bếp chống sóng 5G. Ngăn cản tuyệt đối sóng não phản xạ tin đồn nhảm nhí, giúp bạn trông nổi bật giống sinh vật ngoài hành tinh!"
        titleLower.contains("nước hoa") || titleLower.contains("durian") || titleLower.contains("sầu riêng") -> if (isEn)
            "👃 FRAGRANCE AUDIT: Durian-infused royal parfum. Isolates you from all crowds within 20 meters. Ideal for absolute introverts and antisocial activities."
            else "👃 KIỂM ĐỊNH MÙI HƯƠNG: Nước hoa sầu riêng hoàng gia. Giúp cách ly bạn khỏi đám đông trong bán kính 20m. Thích hợp cho người hướng nội và muốn giãn cách xã hội triệt để."
        titleLower.contains("quạt mini") || titleLower.contains("fan") || titleLower.contains("gió lốc") -> if (isEn)
            "💨 WIND FORCE SPEED: 50,000 RPM mini fan. Generates enough force to lift off or slice fingers. Extremely breezy, extremely hazardous, handle with high anxiety!"
            else "💨 KHÍ ĐỘNG HỌC: Động cơ phản lực quạt mini 50.000 vòng/phút. Thổi bay bàn ghế và tự động cắt móng tay nếu sờ vào cánh quạt. Gió cực mạnh, cực nguy hiểm!"
        titleLower.contains("snack") || titleLower.contains("potato chips") || titleLower.contains("khoai tây") -> if (isEn)
            "🍟 CALORIE CHECK: Briquette-roasted extreme hot chips. Infuses a smoky natural flavor. Guaranteed to make you weep for 2 hours straight and deplete your water supply."
            else "🍟 CHỈ SỐ DINH DƯỠNG: Snack khoai sấy tro than tổ ong siêu cay. Tạo dư vị khói bụi nồng ấm. Ăn một lát đảm bảo khóc ròng rã 2 tiếng và cạn kiệt nguồn nước trong nhà!"
        titleLower.contains("kéo dài chân") || titleLower.contains("grow 15cm") || titleLower.contains("chân") -> if (isEn)
            "🦵 MEDICAL REPORT: Temporary joint stretch using composite-coated kitchen bands. Taller by 15cm when lying flat on your bed, returns to original height immediately on standing!"
            else "🦵 Y HỌC THỰC TIỄN: Kéo dãn khớp bằng thun composite. Giúp bạn cao thêm 15cm khi... nằm thẳng lưng trên giường, đứng dậy cái lập tức lùn lại như cũ đầy co giãn!"
        else -> if (isEn)
            "📈 MARKETING TRUTH: The advertiser paid AdBot $0.0001 for 10 seconds of your attention. Conversion rate: 0%. Adware download chance: Exceptionally high!"
            else "📈 SỰ THẬT TIẾP THỊ: Nhà quảng cáo đã tài trợ 0.0001 USD cho AdBot để đổi lấy 10 giây rảnh rỗi của bạn. Tỉ lệ mua hàng thực tế: 0%. Tỉ lệ dính virus rác rưởi: Rất cao!"
    }
}

// --- USER GUIDE & KNOWLEDGE BASE DIALOG ---
@Composable
fun UserGuideDialog(
    viewModel: AdsViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("user_guide_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📖", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = viewModel.t("Cẩm Nang & Hướng Dẫn", "User Guide & Knowledge Hub"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = viewModel.t("Khám phá bí kíp & thể loại quảng cáo", "App usage, secret games & ad formats"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_user_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = viewModel.t("Đóng", "Close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = listOf(
                        viewModel.t("🧭 Tổng quan", "🧭 Overview"),
                        viewModel.t("📢 Thể loại Ads", "📢 Ad Formats"),
                        viewModel.t("🗝️ Game Bí Mật", "🗝️ Secret Games"),
                        viewModel.t("💡 Mẹo Doanh Thu", "💡 Pro Tips")
                    )
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.testTag("guide_tab_$index")
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Tab Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 🧭 TỔNG QUAN
                            item {
                                GuideCard(
                                    icon = "📱",
                                    title = viewModel.t("Cấu Trúc 4 Tab Chính", "Core Navigation Tabs"),
                                    content = viewModel.t(
                                        "• 🎬 Quảng Cáo (Live Ads): Trải nghiệm và tương tác với các định dạng quảng cáo thực tế, thay đổi danh mục ngành hàng (Tech, Gaming, Finance, Beauty...), tạo banner độc quyền bằng AI Imagen, và chuyển đổi giao diện.\n" +
                                        "• 📊 Mô Phỏng (Simulator): Kéo thả các thanh trượt điều chỉnh Lượt hiển thị (Impressions), Tỷ lệ nhấp (CTR), Giá thầu CPM & CPC để xem dự báo doanh thu và phân tích đồ thị thời gian thực.\n" +
                                        "• ⭐ Thử Thách (Challenges): Chinh phục các nhiệm vụ tính thời gian (xem video có thưởng tốc độ, tắt quảng cáo toàn màn hình chuẩn xác) để nhận huy hiệu vinh danh.\n" +
                                        "• 🤖 AdBot Chat: Trò chuyện cùng trợ lý AI thông minh để nhận phân tích chiến dịch và những phản hồi hài hước thú vị.",
                                        "• 🎬 Live Ads: Experience and interact with real ad formats, switch industry niches (Tech, Gaming, Finance, Beauty...), generate AI custom banners with Imagen, and switch UI themes.\n" +
                                        "• 📊 Simulator: Adjust sliders for Impressions, Click-Through Rate (CTR), CPM, and CPC bidding to simulate real-time campaign revenue and view analytics.\n" +
                                        "• ⭐ Challenges: Complete time-limited milestones (watch rewarded ads swiftly, fast-close interstitials) to earn prestigious badges.\n" +
                                        "• 🤖 AdBot Chat: Converse with your witty AI Assistant for campaign analytics and clever banter."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🎨",
                                    title = viewModel.t("Chuyển Đổi Giao Diện Đa Dạng", "Theme Styles & Customization"),
                                    content = viewModel.t(
                                        "Nhấn nút đổi giao diện ở góc trên bên phải để luân chuyển giữa 3 chế độ:\n" +
                                        "• 🚀 Startup Mode: Phong cách hiện đại, rực rỡ và tràn đầy năng lượng.\n" +
                                        "• 👔 Agency Mode: Phong cách thanh lịch, chuyên nghiệp cho doanh nghiệp.\n" +
                                        "• 🌫️ Gray Mode: Màn hình màu xám với chữ cái màu trắng tối giản, êm dịu cho mắt khi làm việc lâu.",
                                        "Tap the theme switcher button in the top right corner to cycle through 3 modes:\n" +
                                        "• 🚀 Startup Mode: Vibrant, modern, and energetic neon aesthetics.\n" +
                                        "• 👔 Agency Mode: Sleek, refined, and corporate professional layout.\n" +
                                        "• 🌫️ Gray Mode: Elegant gray background with high-contrast white text, easy on the eyes."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🌐",
                                    title = viewModel.t("Chuyển Đổi Ngôn Ngữ & Làm Mới", "Language & Reset Controls"),
                                    content = viewModel.t(
                                        "• 🇻🇳 / 🇬🇧: Nút chuyển nhanh giữa Tiếng Việt và Tiếng Anh mượt mà.\n" +
                                        "• 🔄 Reset: Nút đỏ đặt lại toàn bộ số liệu mô phỏng, lịch sử và điểm số về trạng thái ban đầu khi cần kiểm thử lại từ đầu.",
                                        "• 🇻🇳 / 🇬🇧: Instant toggle between Vietnamese and English.\n" +
                                        "• 🔄 Reset: Red icon button to reset all simulation metrics, logs, and stats back to initial state for fresh testing."
                                    )
                                )
                            }
                        }
                        1 -> {
                            // 📢 THỂ LOẠI ADS
                            item {
                                GuideCard(
                                    icon = "📌",
                                    title = viewModel.t("1. Quảng Cáo Biểu Ngữ (Banner Ads)", "1. Banner Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Xuất hiện dạng khung chữ nhật ở đầu/cuối trang hoặc lồng trong bài viết (Native Feed).\n" +
                                        "• Ưu điểm: Chi phí hiển thị (CPM) thấp, không làm gián đoạn trải nghiệm người dùng, duy trì độ nhận diện thương hiệu liên tục.\n" +
                                        "• Chỉ số quan trọng: CTR trung bình từ 0.5% - 2.0%.",
                                        "• Characteristics: Rectangular display units placed at the top/bottom or seamlessly integrated into feeds.\n" +
                                        "• Advantages: Low CPM cost, non-intrusive to user experience, delivers persistent brand presence.\n" +
                                        "• Key Metrics: Average CTR ranges from 0.5% to 2.0%."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "📱",
                                    title = viewModel.t("2. Quảng Cáo Chuyển Trang (Interstitial Ads)", "2. Interstitial Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Quảng cáo toàn màn hình xuất hiện vào các điểm chuyển cảnh tự nhiên (như qua màn game hoặc chuyển tab).\n" +
                                        "• Ưu điểm: Thu hút 100% sự chú ý của người xem, tỷ lệ nhấp cao hơn banner thông thường.\n" +
                                        "• Lưu ý: Có nút đếm ngược 5 giây để bỏ qua (Skip) nhằm bảo vệ trải nghiệm người dùng.",
                                        "• Characteristics: Full-screen ad placements displayed at natural transition points (e.g., stage clear or tab change).\n" +
                                        "• Advantages: Commands 100% user visual attention, yielding significantly higher CTR than standard banners.\n" +
                                        "• Note: Features a 5-second countdown skip button to balance user experience."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🎁",
                                    title = viewModel.t("3. Quảng Cáo Có Thưởng (Rewarded Video Ads)", "3. Rewarded Video Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Người dùng tự nguyện xem hết video quảng cáo (15-30 giây) để nhận phần thưởng giá trị (tiền ảo, điểm sinh mệnh, mở khóa nội dung).\n" +
                                        "• Ưu điểm: Đạt tỷ lệ xem trọn vẹn (Completion Rate) cao nhất, mang lại CPM và doanh thu cao vượt trội.",
                                        "• Characteristics: Users opt-in to watch a full-length video (15-30s) in exchange for valuable in-app rewards (virtual currency, extra lives, unlocks).\n" +
                                        "• Advantages: Delivers the highest video completion rate and top-tier CPM revenue."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "📰",
                                    title = viewModel.t("4. Quảng Cáo Tự Nhiên (Native Ads / Feed)", "4. Native Feed Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Tự động điều chỉnh phông chữ, hình ảnh và bố cục để hòa hợp hoàn hảo với luồng nội dung bài viết.\n" +
                                        "• Ưu điểm: Giảm thiểu hiện tượng 'mù biểu ngữ' (Banner Blindness), tăng độ tin cậy và tỷ lệ tương tác tự nhiên.",
                                        "• Characteristics: Matches the visual design, typography, and layout of the app's organic feed.\n" +
                                        "• Advantages: Mitigates banner blindness, fostering higher organic engagement and user trust."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🚀",
                                    title = viewModel.t("5. Quảng Cáo Mở Ứng Dụng (App Open Ads)", "5. App Open Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Hiển thị nhanh khi người dùng khởi động hoặc quay lại ứng dụng từ chế độ chạy ngầm.\n" +
                                        "• Tác dụng: Tận dụng thời gian tải dữ liệu ban đầu để tạo thêm doanh thu hiệu quả.",
                                        "• Characteristics: Displays during cold launch or when resuming the app from background.\n" +
                                        "• Impact: Monetizes initial asset loading time without interrupting active gameplay."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🎮",
                                    title = viewModel.t("6. Quảng Cáo Chơi Thử (Playable Ads)", "6. Playable Mini-Ads"),
                                    content = viewModel.t(
                                        "• Đặc điểm: Cho phép người dùng trực tiếp chơi thử mini-game 10-15 giây ngay trong quảng cáo trước khi tải về.\n" +
                                        "• Ưu điểm: Tỷ lệ chuyển đổi cài đặt (Conversion Rate) cao gấp nhiều lần quảng cáo tĩnh.",
                                        "• Characteristics: Interactive mini-game experiences embedded directly within the ad unit.\n" +
                                        "• Advantages: Dramatically improves conversion and post-install user retention."
                                    )
                                )
                            }
                        }
                        2 -> {
                            // 🗝️ GAME BÍ MẬT & EASTER EGGS
                            item {
                                GuideCard(
                                    icon = "💎",
                                    title = viewModel.t("1. Vụ Cướp Tiệm Vàng (Jewelry Heist Game)", "1. Jewelry Heist Mini-Game"),
                                    content = viewModel.t(
                                        "• 🗝️ Cách mở khóa: Nhấp xem ít nhất 2 quảng cáo thuộc chủ đề 'Trang Sức / Kim Cương' (Jewelry).\n" +
                                        "• 🎮 Cách chơi: Lỗ hổng an ninh tiệm vàng sẽ mở ra! Bạn sẽ lẻn vào hầm vàng, dùng túi đồ nghề để nhặt đá quý, kim cương và ngọc lục bảo trong thời gian cho phép trước khi còi báo động reo.\n" +
                                        "• 🏆 Phần thưởng: Cộng thẳng giá trị chiến lợi phẩm vào tài khoản doanh thu ảo và nhận danh hiệu Siêu Đạo Chích!",
                                        "• 🗝️ How to unlock: Watch or interact with at least 2 ads under the 'Jewelry & Diamonds' niche.\n" +
                                        "• 🎮 Gameplay: A security flaw in the jewelry ad opens a heist backdoor! Infiltrate the vault and gather sparkling gems before the alarms sound.\n" +
                                        "• 🏆 Reward: Adds the stolen loot directly to your virtual revenue and earns the Master Diamond Thief badge!"
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🗡️",
                                    title = viewModel.t("2. Đấu Trường Bài Ma Pháp (Monster Card Arena)", "2. Monster Card Battle Arena"),
                                    content = viewModel.t(
                                        "• 🗝️ Cách mở khóa: Nhấp xem ít nhất 2 quảng cáo thuộc chủ đề 'Game / Trò Chơi 3D' (Gaming).\n" +
                                        "• 🎮 Cách chơi: Khai quật Bảo kiếm để thách đấu bài ma pháp phong cách Yugioh với AdBot! Rút bài quái thú, kích hoạt phép Chặn Quảng Cáo (AdBlocker) và tiêu hao hết 4000 Điểm sinh mệnh (LP) của đối thủ.\n" +
                                        "• 🏆 Phần thưởng: Huy hiệu Ma Pháp Sư Tối Thượng và tiền thưởng ảo.",
                                        "• 🗝️ How to unlock: Watch or interact with at least 2 ads under the 'Gaming / 3D Games' niche.\n" +
                                        "• 🎮 Gameplay: Unearth the legendary battle sword to challenge AdBot in a Yugioh-style tactical card duel! Summon monsters, activate AdBlocker spells, and reduce the boss's 4,000 LP to zero.\n" +
                                        "• 🏆 Reward: Ultimate Sorcerer title and virtual cash rewards."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "💕",
                                    title = viewModel.t("3. Hẹn Hò Cùng Idol Linh Chi (Linh Chi Dating Sim)", "3. Linh Chi Dating Simulation"),
                                    content = viewModel.t(
                                        "• 🗝️ Cách mở khóa: Nhấp tương tác với biểu ngữ 'Video Call 1-1 Hotgirl Linh Chi'.\n" +
                                        "• 🎮 Cách chơi: Tham gia hội thoại hẹn hò tương tác, chọn các câu trả lời khéo léo để tăng điểm thiện cảm (Affection) lên 100% mà không bị rơi vào bẫy 'đòi tiền ăn lẩu'.\n" +
                                        "• 🏆 Phần thưởng: Mở khóa danh hiệu Người Tình Trong Mộng và đối thoại bí mật.",
                                        "• 🗝️ How to unlock: Click on the '1-on-1 Video Call with Linh Chi' banner ad.\n" +
                                        "• 🎮 Gameplay: Engage in an interactive dating conversation, choosing charming responses to reach 100% affection without falling for donation traps.\n" +
                                        "• 🏆 Reward: Unlocks the Dream Lover title and secret dialogue."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "♟️",
                                    title = viewModel.t("4. Cờ Vua Sinh Tử với Siêu AI (Chess Battle)", "4. Tactical AI Chess Battle"),
                                    content = viewModel.t(
                                        "• 🗝️ Cách mở khóa: Nhấn trực tiếp vào thẻ Game Cờ Vua trên màn hình Quảng Cáo.\n" +
                                        "• 🎮 Cách chơi: Trận đấu cờ vua trí tuệ với siêu máy tính AdBot. Thử tài tính toán nước cờ của bạn để giành chiến thắng!",
                                        "• 🗝️ How to unlock: Tap the Chess Battle card directly from the Live Ads tab.\n" +
                                        "• 🎮 Gameplay: A brain-challenging chess match against supercomputer AdBot. Outsmart the AI to claim victory!"
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "🚨",
                                    title = viewModel.t("5. Nhận Diện Bẫy Clickbait & Lừa Đảo", "5. Clickbait & Scam Warning Demo"),
                                    content = viewModel.t(
                                        "• 🗝️ Cách kích hoạt: Nhấp vào quảng cáo hứa hẹn nhận $1,000,000 từ hoàng tử hoặc việc nhẹ lương cao nộp cọc.\n" +
                                        "• 💡 Ý nghĩa: Cung cấp bài học thực tế về các chiêu trò lừa đảo qua mạng, nhắc nhở không bao giờ chia sẻ mã OTP, mật khẩu ngân hàng cho người lạ.",
                                        "• 🗝️ How to trigger: Click on suspicious sensational ads (e.g., $1,000,000 check from prince, quick money jobs with deposit).\n" +
                                        "• 💡 Purpose: Educational simulation highlighting common online scams to teach safe internet browsing habits."
                                    )
                                )
                            }
                        }
                        3 -> {
                            // 💡 MẸO & TỐI ƯU DOANH THU
                            item {
                                GuideCard(
                                    icon = "💰",
                                    title = viewModel.t("1. Tối Ưu Hóa Giá Thầu CPM & CPC", "1. Optimize CPM & CPC Bidding"),
                                    content = viewModel.t(
                                        "• Ngành Tài Chính (Finance) và Công Nghệ (Tech) có CPM cao gấp 3-5 lần các ngành giải trí thông thường.\n" +
                                        "• Kết hợp nhắm mục tiêu chuẩn xác (Targeting) và khung giờ cao điểm (Peak Hours) để tối đa hóa tỷ lệ chuyển đổi.",
                                        "• Finance and Technology niches command 3-5x higher CPMs compared to casual entertainment.\n" +
                                        "• Combine precise audience targeting with peak-hour scheduling to maximize revenue returns."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "✨",
                                    title = viewModel.t("2. Sáng Tạo Banner với AI Sandbox", "2. AI Creative Sandbox"),
                                    content = viewModel.t(
                                        "• Sử dụng công cụ 'AI Creative Sandbox' để tự thiết kế biểu ngữ quảng cáo độc quyền bằng Imagen AI.\n" +
                                        "• Mẫu quảng cáo tự tạo sẽ được lưu trữ vĩnh viễn vào Room Database trên máy của bạn.",
                                        "• Use the 'AI Creative Sandbox' to generate custom ad banners with Imagen AI.\n" +
                                        "• Your custom creatives are permanently saved to your local Room Database."
                                    )
                                )
                            }
                            item {
                                GuideCard(
                                    icon = "⚡",
                                    title = viewModel.t("3. Mẹo Hoàn Thành Thử Thách", "3. Challenge Mastery Tips"),
                                    content = viewModel.t(
                                        "• Thử thách 'Xạ Thủ Tắt Ads': Chờ nút Bỏ qua (Skip) hiển thị đầy đủ rồi nhấn ngay trong vòng 2 giây để đạt điểm tối đa.\n" +
                                        "• Thử thách 'Bậc Thầy CTR': Điều chỉnh thanh trượt CTR trong tab Mô Phỏng để đạt đúng chỉ số mục tiêu.",
                                        "• 'Ad Close Sniper' Challenge: Wait for the Skip button to fully render, then tap within 2.0 seconds to secure the record.\n" +
                                        "• 'CTR Master' Challenge: Fine-tune the CTR slider in the Simulator tab to hit the exact target percentage."
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Action
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_user_guide_bottom_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = viewModel.t("Đã Hiểu & Bắt Đầu Trải Nghiệm", "Got It & Explore Now"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    icon: String,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = icon, fontSize = 20.sp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}


