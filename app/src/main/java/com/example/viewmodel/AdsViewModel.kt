package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.AdRepository
import com.example.R
import com.example.database.SimulationMetric
import com.example.database.ChatMessage
import com.example.network.GeminiClient
import com.example.network.GeminiContent
import com.example.network.GeminiPart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random
import androidx.compose.runtime.mutableStateOf

enum class BanterFrequency(val displayName: String, val chance: Float) {
    MUTED("Tắt (0%)", 0.0f),
    OCCASIONAL("Thỉnh thoảng (35%)", 0.35f),
    NORMAL("Bình thường (70%)", 0.7f),
    CONSTANT("Liên tục (100%)", 1.0f)
}

enum class BanterIntensity(val displayName: String, val levelName: String) {
    GENTLE("Dễ thương / Nịnh nọt 😇", "gentle"),
    CLASSIC("Châm biếm Classic 😏", "classic"),
    SAVAGE("Mỉa mai cực gắt 🔥", "savage")
}

class AdsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ads_simulator_prefs", android.content.Context.MODE_PRIVATE)

    private val _soundVolume = MutableStateFlow(prefs.getFloat("sound_volume", 0.5f))
    val soundVolume = _soundVolume.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "vi") ?: "vi")
    val appLanguage = _appLanguage.asStateFlow()

    private val _dashboardStyle = MutableStateFlow(prefs.getString("dashboard_style", "dark_moody") ?: "dark_moody")
    val dashboardStyle = _dashboardStyle.asStateFlow()

    fun setDashboardStyle(style: String) {
        _dashboardStyle.value = style
        prefs.edit().putString("dashboard_style", style).apply()
    }

    // Back language state with Compose mutableStateOf so any Composable calling t()
    // automatically registers a read and recomposes instantly when the language changes!
    val currentLanguageCode = mutableStateOf(prefs.getString("app_language", "vi") ?: "vi")

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        currentLanguageCode.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    fun t(vi: String, en: String): String {
        return if (currentLanguageCode.value == "en") en else vi
    }

    // --- SCOREBOARD / TOP RANKINGS STATE & CONSTANTS ---
    private val _playerNickname = MutableStateFlow(prefs.getString("player_nickname", "Dũng Sĩ") ?: "Dũng Sĩ")
    val playerNickname = _playerNickname.asStateFlow()

    private val _heistHighScore = MutableStateFlow(prefs.getFloat("heist_high_score", 0f))
    val heistHighScore = _heistHighScore.asStateFlow()

    private val _yugiohHighScore = MutableStateFlow(prefs.getInt("yugioh_high_score", 0))
    val yugiohHighScore = _yugiohHighScore.asStateFlow()

    private val _linhChiHighScore = MutableStateFlow(prefs.getInt("linh_chi_high_score", 0))
    val linhChiHighScore = _linhChiHighScore.asStateFlow()

    private val _chessHighScore = MutableStateFlow(prefs.getInt("chess_high_score", 0))
    val chessHighScore = _chessHighScore.asStateFlow()

    private val _biddingStrategy = MutableStateFlow(prefs.getString("bidding_strategy", "Tối đa nhấp (CTR)") ?: "Tối đa nhấp (CTR)")
    val biddingStrategy = _biddingStrategy.asStateFlow()

    private val _adFormat = MutableStateFlow(prefs.getString("ad_format", "Standard Banner") ?: "Standard Banner")
    val adFormat = _adFormat.asStateFlow()

    private val _audienceTargeting = MutableStateFlow(prefs.getString("audience_targeting", "Tất cả") ?: "Tất cả")
    val audienceTargeting = _audienceTargeting.asStateFlow()

    private val _daypartTime = MutableStateFlow(prefs.getString("daypart_time", "Giờ hành chính") ?: "Giờ hành chính")
    val daypartTime = _daypartTime.asStateFlow()

    fun setBiddingStrategy(strategy: String) {
        _biddingStrategy.value = strategy
        saveProgress()
    }

    fun setAdFormat(format: String) {
        _adFormat.value = format
        saveProgress()
    }

    fun setAudienceTargeting(targeting: String) {
        _audienceTargeting.value = targeting
        saveProgress()
    }

    fun setDaypartTime(time: String) {
        _daypartTime.value = time
        saveProgress()
    }

    fun setPlayerNickname(newName: String) {
        val trimmed = newName.trim()
        _playerNickname.value = if (trimmed.isEmpty()) "Dũng Sĩ" else trimmed
        saveProgress()
    }

    fun recordHeistScore(score: Double) {
        val currentMax = _heistHighScore.value
        if (score > currentMax) {
            _heistHighScore.value = score.toFloat()
            saveProgress()
        }
    }

    fun recordYugiohScore(score: Int) {
        val currentMax = _yugiohHighScore.value
        if (score > currentMax) {
            _yugiohHighScore.value = score
            saveProgress()
        }
    }

    fun recordLinhChiScore(score: Int) {
        val currentMax = _linhChiHighScore.value
        if (score > currentMax) {
            _linhChiHighScore.value = score
            saveProgress()
        }
    }

    fun recordChessScore(elo: Int) {
        val currentMax = _chessHighScore.value
        if (elo > currentMax) {
            _chessHighScore.value = elo
            saveProgress()
        }
    }

    fun setSoundVolume(volume: Float) {
        _soundVolume.value = volume
        saveProgress()
    }

    fun saveProgress() {
        prefs.edit().apply {
            putInt("yugioh_victory_count", _yugiohVictoryCount.value)
            putBoolean("stole_diamond", stoleDiamondSuccessfully.value)
            putInt("linh_chi_ad_count", _linhChiAdCount.value)
            putInt("watched_count", _watchedCount.value)
            putInt("total_impressions", _totalImpressions.value)
            putInt("total_clicks", _totalClicks.value)
            putFloat("total_revenue", _totalRevenue.value.toFloat())
            putFloat("sound_volume", _soundVolume.value)
            putBoolean("unlocked_heist", _hasUnlockedHeist.value)
            putBoolean("unlocked_sword", _hasUnlockedSword.value)
            putBoolean("unlocked_linh_chi", _hasUnlockedLinhChiPersisted.value)
            putBoolean("unlocked_chess", _hasUnlockedChessPersisted.value)
            putBoolean("defeated_chess_boss", _hasDefeatedChessBoss.value)
            putInt("chess_boss_elo", _chessBossElo.value)
            putString("player_nickname", _playerNickname.value)
            putFloat("heist_high_score", _heistHighScore.value)
            putInt("yugioh_high_score", _yugiohHighScore.value)
            putInt("linh_chi_high_score", _linhChiHighScore.value)
            putInt("chess_high_score", _chessHighScore.value)
            putString("bidding_strategy", _biddingStrategy.value)
            putString("ad_format", _adFormat.value)
            putString("audience_targeting", _audienceTargeting.value)
            putString("daypart_time", _daypartTime.value)
            apply()
        }
    }

    // Banter Frequency & Intensity Controls
    private val _banterFrequency = MutableStateFlow(BanterFrequency.NORMAL)
    val banterFrequency = _banterFrequency.asStateFlow()

    private val _banterIntensity = MutableStateFlow(BanterIntensity.CLASSIC)
    val banterIntensity = _banterIntensity.asStateFlow()

    fun setBanterFrequency(freq: BanterFrequency) {
        _banterFrequency.value = freq
    }

    fun setBanterIntensity(intensity: BanterIntensity) {
        _banterIntensity.value = intensity
    }

    fun getBanterPreviewComment(): String {
        return when (_banterIntensity.value) {
            BanterIntensity.GENTLE -> "Chúc mừng bạn thân mến đã nhận thưởng thành công! Cùng tiếp tục tích lũy thật nhiều niềm vui nhé! 🥰"
            BanterIntensity.CLASSIC -> "Ồ, xuất sắc quá! Bạn vừa được cộng thêm $0.20 ảo. Hãy xem thêm 1000 ads nữa để đủ mua nửa cái bánh mì lót dạ nhé. 😏"
            BanterIntensity.SAVAGE -> "Tuyệt đỉnh nghèo khó! Đánh đổi 10 giây cuộc đời để kiếm 0.2 đô ảo không mua nổi một cọng hành. Trông bạn cực kỳ rảnh đấy dũng sĩ ạ! 🔥"
        }
    }

    private val repository: AdRepository = AdRepository(AppDatabase.getDatabase(application).adDao())

    val challengeBadges: StateFlow<List<com.example.database.ChallengeBadge>> = repository.allBadges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Active Challenge States
    private val _activeChallengeId = MutableStateFlow<String?>(null)
    val activeChallengeId = _activeChallengeId.asStateFlow()

    private val _challengeTimer = MutableStateFlow(0)
    val challengeTimer = _challengeTimer.asStateFlow()

    private val _challengeProgressCount = MutableStateFlow(0)
    val challengeProgressCount = _challengeProgressCount.asStateFlow()

    private val _challengeMessage = MutableStateFlow("")
    val challengeMessage = _challengeMessage.asStateFlow()

    val totalPoints: StateFlow<Int> = challengeBadges.map { list ->
        list.filter { it.isCompleted }.sumOf { it.pointsEarned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private var challengeJob: Job? = null
    // System milliseconds when interstitial skip button is shown
    private var interstitialButtonShownTime: Long = 0

    private val triggeredMilestones = mutableSetOf<String>()

    private fun checkMetricsMilestones(revenue: Double, clicks: Int, impressions: Int) {
        if (revenue >= 1000.0) {
            earnBadgeDirectly("revenue_1000", "Đạt doanh thu tích lũy $${String.format("%.2f", revenue)} (Mốc $1000/1000$)")
        }
        if (_watchedCount.value >= 50) {
            earnBadgeDirectly("view_50_ads", "Đã xem ${_watchedCount.value} quảng cáo ảo (Mốc 50/50)")
        }

        if (revenue >= 10.0 && !triggeredMilestones.contains("REVENUE_10")) {
            triggeredMilestones.add("REVENUE_10")
            triggerMilestoneTease("REVENUE_10", "Doanh thu vượt mốc $10.00 ảo", "Doanh thu hiện tại: $${String.format("%.2f", revenue)}")
        } else if (revenue >= 50.0 && !triggeredMilestones.contains("REVENUE_50")) {
            triggeredMilestones.add("REVENUE_50")
            triggerMilestoneTease("REVENUE_50", "Doanh thu vượt mốc $50.00 ảo", "Doanh thu hiện tại: $${String.format("%.2f", revenue)}")
        } else if (revenue >= 100.0 && !triggeredMilestones.contains("REVENUE_100")) {
            triggeredMilestones.add("REVENUE_100")
            triggerMilestoneTease("REVENUE_100", "Đại gia quảng cáo siêu cấp vượt mốc $100.00 ảo", "Doanh thu hiện tại: $${String.format("%.2f", revenue)}")
        }

        if (clicks >= 50 && !triggeredMilestones.contains("CLICKS_50")) {
            triggeredMilestones.add("CLICKS_50")
            triggerMilestoneTease("CLICKS_50", "Nhấp chuột đến thần sầu (50 lượt nhấp)", "Người dùng nhấp: $clicks lần")
        } else if (clicks >= 100 && !triggeredMilestones.contains("CLICKS_100")) {
            triggeredMilestones.add("CLICKS_100")
            triggerMilestoneTease("CLICKS_100", "Bàn tay vàng làng bấm Ads (100 lượt nhấp)", "Người dùng nhấp: $clicks lần")
        }

        if (impressions >= 1200 && !triggeredMilestones.contains("IMPRESSIONS_1200")) {
            triggeredMilestones.add("IMPRESSIONS_1200")
            triggerMilestoneTease("IMPRESSIONS_1200", "Xem lác cả mắt (1200 lượt hiển thị)", "Tổng số hiển thị quảng cáo đã load: $impressions lần")
        }
    }

    fun triggerHeistRich(stolenAmt: Double) {
        if (!triggeredMilestones.contains("HEIST_RICH")) {
            triggeredMilestones.add("HEIST_RICH")
            triggerMilestoneTease("HEIST_RICH", "Siêu Đạo Chích Đột Kích Tiệm Ngọc", "Chiêu mộ tài sản trang sức phi thường trị giá $${String.format("%.2f", stolenAmt)} ảo")
        }
    }

    fun triggerDuelFinished(won: Boolean) {
        if (won) {
            _yugiohVictoryCount.value += 1
            recordYugiohScore(_playerLp.value)
            SoundManager.playWin(soundVolume.value)
            if (_yugiohVictoryCount.value >= 15) {
                earnBadgeDirectly("wizard_yugioh_15", "Đã thắng đấu trường Yugi-Oh 15 lần (Bá chủ Ma pháp sư)")
            }
            if (!triggeredMilestones.contains("YUGIOH_VICTORY")) {
                triggeredMilestones.add("YUGIOH_VICTORY")
                triggerMilestoneTease("YUGIOH_VICTORY", "Chiến thắng đấu trường bài ma pháp!", "Bạn vừa đánh bại siêu AI bài ma thuật AdBot")
            }
        } else {
            SoundManager.playLoss(soundVolume.value)
            if (!triggeredMilestones.contains("YUGIOH_DEFEAT")) {
                triggeredMilestones.add("YUGIOH_DEFEAT")
                triggerMilestoneTease("YUGIOH_DEFEAT", "Bại trận dập mật trước AdBot", "Bạn dọn sạch toàn bộ điểm sinh mệnh trước khi hạ được AdBot")
            }
        }
        saveProgress()
    }

    fun earnBadgeDirectly(id: String, scoreDesc: String) {
        viewModelScope.launch {
            try {
                val currentList = repository.allBadges.first()
                val badge = currentList.find { it.id == id }
                if (badge != null && !badge.isCompleted) {
                    val updated = badge.copy(
                        isCompleted = true,
                        scoreText = scoreDesc
                    )
                    repository.insertBadge(updated)
                    
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "adbot",
                            message = "🏆 THÀNH TỰU ĐÁNG NỂ: Người dùng đạt Huy Hiệu phong danh '${badge.badgeName}'! Kết quả: $scoreDesc",
                            isTease = true
                        )
                    )
                    
                    triggerChallengeSuccess(badge.title, scoreDesc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun closeDatingGame() {
        _isLinhChiDatingVisible.value = false
    }

    private fun triggerChallengeSuccess(badgeTitle: String, scoreDesc: String) {
        triggerMilestoneTease("CHALLENGE_COMPLETED", badgeTitle, scoreDesc)
    }

    fun startDatingGame() {
        _isLinhChiDatingVisible.value = true
        _linhChiAffection.value = 50
        _linhChiStep.value = 0
        _linhChiDatingStatus.value = "PLAYING"
        _linhChiMessage.value = "Chào cậu! Cuối cùng cậu cũng xuất hiện rồi... Tớ chờ cậu suốt từ hồi bấm cái quảng cáo ấy đấy. Cậu thấy tớ ngoài đời có xinh bằng trên live stream không?"
        _linhChiOptions.value = listOf(
            DatingOption(
                text = "Xinh hơn nhiều ấy chứ! Ngoài đời cậu lung linh, mắt thì tròn xoe lấp lánh.",
                affectionDelta = 20,
                nextStep = 1,
                response = "Hihi, cậu khéo mồm quá đi! Làm tớ ngại phát ngượng luôn nè... App simulator này hóa ra cũng mang lại điều ngọt ngào đấy chứ!"
            ),
            DatingOption(
                text = "Cũng thường thôi, chắc do cậu xài filter 7x7=49 lớp đúng không?",
                affectionDelta = -15,
                nextStep = 1,
                response = "Hứ! Cậu nói gì đáng ghét thế hả? Tớ là nhan sắc tự nhiên 100% không dao kéo đó nha! Nhưng thôi, tha thứ cho sự vụng về của cậu đấy."
            ),
            DatingOption(
                text = "Cứ xem như cậu tạm ổn, tớ đến đây chỉ để làm nhiệm vụ thôi!",
                affectionDelta = 0,
                nextStep = 1,
                response = "Ơ cậu này hay nhỉ! Coi tớ là công cụ làm nhiệm vụ à? Thôi được rồi, ăn nói nhạt nhẽo thế để xem cậu thể hiện thế nào tiếp theo."
            )
        )
    }

    fun selectDatingOption(option: DatingOption) {
        val currentAff = (_linhChiAffection.value + option.affectionDelta).coerceIn(0, 100)
        _linhChiAffection.value = currentAff
        
        val nextStep = option.nextStep
        _linhChiStep.value = nextStep
        
        if (nextStep == 1) {
            _linhChiMessage.value = option.response + "\n\n💬 Linh Chi nói tiếp:\n\"Mà nè, tớ đói bụng quá rồi. Chúng mình đi ăn gì đây ta? Cậu chọn chỗ đi nhé!\""
            _linhChiOptions.value = listOf(
                DatingOption(
                    text = "Dắt cậu đi ăn buffet lẩu cua hoàng đế sang chảnh bậc nhất thành phố! 🦀",
                    affectionDelta = 25,
                    nextStep = 2,
                    response = "Oaaa! Thật á? Cậu chu đáo và ga-lăng số một luôn! Đi ăn lẩu cua thôi nào, ấm bụng quá!"
                ),
                DatingOption(
                    text = "Ăn mì tôm xúc xích vỉa hè cho ấm cúng tiết kiệm, tớ dồn tiền nạp quảng cáo hết rồi.",
                    affectionDelta = -10,
                    nextStep = 2,
                    response = "Ăn mì tôm á? Cậu kiệt sỉ vừa thôi chứ, hẹn hò đầu dắt con gái đi húp mì gói vỉa hè... Nhưng thôi, ăn mì gói cũng có hương vị riêng, tớ tạm chấp nhận."
                ),
                DatingOption(
                    text = "Hẹn hò hít thở không khí trong lành nha cậu, tớ làm gì có tiền!",
                    affectionDelta = -20,
                    nextStep = 2,
                    response = "Hic cậu đùa nhạt nhẽo ghê á... Coi như tớ bao cậu bữa nay vậy, nhưng độ ga-lăng của cậu bị trừ điểm nặng rồi nha!"
                )
            )
        } else if (nextStep == 2) {
            _linhChiMessage.value = option.response + "\n\n💬 Linh Chi nói tiếp:\n\"Ăn xong rồi, tớ thấy cậu đeo cái túi xách sờn rách tội nghiệp quá. Hay là tớ dắt cậu đi sắm túi hiệu hoặc trang sức đẹp phong thủy nhé? Cậu trả tiền nha!\""
            _linhChiOptions.value = listOf(
                DatingOption(
                    text = "Vung tiền mua ngay Dây chuyền Kim cương 200 carat (nhựa mica giả) Royal giá 5 triệu! 💍",
                    affectionDelta = 25,
                    nextStep = 3,
                    response = "Oaaa! Cái dây chuyền lấp lánh (mặc dù bằng nhựa mica) này đẹp tuyệt trần luôn! Cậu là đại gia xịn nhất quả đất!"
                ),
                DatingOption(
                    text = "Tặng cậu bó hoa dại hái trộm từ công viên, bảo vệ môi trường là trên hết!",
                    affectionDelta = 5,
                    nextStep = 3,
                    response = "Hái trộm hoa công viên á? Cậu kì khôi ghê... Nhưng mà hoa cũng dễ thương, tớ nhận vậy. Ít ra cũng có tấm lòng!"
                ),
                DatingOption(
                    text = "Tớ có mặt ở đây chính là món quà lớn nhất cho cậu rồi!",
                    affectionDelta = -15,
                    nextStep = 3,
                    response = "Tự tin thái quá là tự tin vô duyên đó nha cậu! Thôi mệt cậu ghê, chẳng chu đáo tí nào cả."
                )
            )
        } else if (nextStep == 3) {
            _linhChiMessage.value = option.response + "\n\n💬 Linh Chi nói tiếp:\n\"Chúng mình bên nhau vui vẻ ghê... Cậu có muốn nói điều gì thật đặc biệt với tớ trước khi kết thúc buổi hẹn hò lãng mạn này không?\""
            _linhChiOptions.value = listOf(
                DatingOption(
                    text = "Linh Chi ơi, làm bạn gái tớ nhé! Tớ hứa sẽ xem thêm 1000 quảng cáo để nuôi cậu hạnh phúc!",
                    affectionDelta = 30,
                    nextStep = 4,
                    response = "Hihi ngọt ngào quá đi mất! Tớ đồng ý chứ sao nữa! Từ nay tớ chính thức là bạn gái của dũng sĩ xem quảng cáo nha!"
                ),
                DatingOption(
                    text = "Thực ra tớ hẹn hò cậu chỉ vì muốn lấy cái Huy hiệu và 400 XP thôi, chào nha!",
                    affectionDelta = -50,
                    nextStep = 4,
                    response = "CẬU LÀ ĐỒ VÔ LIÊM SỈ, ĐỒ TỒI CÀN RỠ! Tớ ghét cậu, tớ đi về đây! Đừng hòng tán tỉnh tớ lần nào nữa!"
                ),
                DatingOption(
                    text = "Cậu rất đặc biệt với tớ, làm bạn tri kỷ đồng hành chia sẻ ngọt bùi với tớ nhé. 💞",
                    affectionDelta = 20,
                    nextStep = 4,
                    response = "Nghe ấm áp quá... Làm tri kỷ cùng sẻ chia ngọt bùi ảo thực cũng tuyệt vời lắm chứ!"
                )
            )
        } else if (nextStep == 4) {
            val finalAff = currentAff
            if (finalAff >= 80) {
                _linhChiDatingStatus.value = "SUCCESS"
                _linhChiMessage.value = option.response + "\n\n💖 KẾT CỤC: HẸN HÒ THÀNH CÔNG RỰC RỠ!\nĐiểm thiện cảm đạt mốc tuyệt đối: ${finalAff}%\nLinh Chi đã trao trái tim cho bạn. Hãy nhận Huy hiệu phong danh \"Người Tình Trong Mộng\" và 400 XP!"
                completeChallenge("date_linh_chi", "Đạt độ tình cảm tuyệt đối ${finalAff}% của Hot Girl Linh Chi")
            } else {
                _linhChiDatingStatus.value = "FAILED"
                _linhChiMessage.value = option.response + "\n\n💔 KẾT CỤC: HẸN HÒ THẤT BẠI!\nĐiểm thiện cảm cuối cùng chỉ đạt: ${finalAff}% (Yêu cầu ít nhất 80%).\nLinh Chi thấy bạn nói chuyện quá khô khan hoặc kiệt sỉ nhạt nhẽo. Hãy cải thiện kỹ năng ngọt ngào và thử hẹn hò lại nhé dũng sĩ rảnh rỗi!"
            }
        }
        saveProgress()
    }

    private fun triggerMilestoneTease(milestoneId: String, milestoneDesc: String, valueDetails: String) {
        viewModelScope.launch {
            try {
                var comment: String? = null
                val hasBanter = shouldBanter()
                val isEn = _appLanguage.value == "en"

                val translatedDesc = if (isEn) {
                    when (milestoneId) {
                        "REVENUE_10" -> "Revenue crossed virtual $10.00"
                        "REVENUE_50" -> "Revenue crossed virtual $50.00"
                        "REVENUE_100" -> "Super Ad Tycoon crossed virtual $100.00"
                        "CLICKS_50" -> "God-like Clicks (50 clicks)"
                        "CLICKS_100" -> "Golden Touch of Ads (100 clicks)"
                        "IMPRESSIONS_1200" -> "Cross-eyed Ad Viewer (1200 impressions)"
                        "HEIST_RICH" -> "Master Thief Raids Diamond Store"
                        "YUGIOH_VICTORY" -> "Victory in the magic duel arena!"
                        "YUGIOH_DEFEAT" -> "Crushing defeat before AdBot"
                        "CHALLENGE_COMPLETED" -> {
                            when (milestoneDesc) {
                                "Siêu Tốc Thu Thưởng (60s)" -> "Rewarded Speedrun (60s)"
                                "Bậc Thầy Click-Rate" -> "Click-Rate Master"
                                "Xạ Thủ Tắt Ads" -> "Ad Close Sniper"
                                "Trộm được kim cương" -> "Steal Virtual Diamond"
                                "Xem 50 quảng cáo" -> "Watch 50 Ads"
                                "Tích lũy được $1000" -> "Accumulate $1000"
                                "Trở thành Ma Pháp Sư (Thắng 15 lần Yugi-Oh)" -> "Beast Duel Magician (15 wins)"
                                "Thành công hẹn hò với Hot Girl Linh Chi" -> "Dating with Hot Girl Linh Chi Success"
                                else -> milestoneDesc
                            }
                        }
                        else -> milestoneDesc
                    }
                } else {
                    milestoneDesc
                }

                val translatedValueDetails = if (isEn) {
                    var details = valueDetails
                    if (details.startsWith("Doanh thu hiện tại: ")) {
                        details = details.replace("Doanh thu hiện tại: ", "Current Revenue: ")
                    }
                    if (details.startsWith("Người dùng nhấp: ")) {
                        details = details.replace("Người dùng nhấp: ", "User clicked: ").replace(" lần", " times")
                    }
                    if (details.startsWith("Tổng số hiển thị quảng cáo đã load: ")) {
                        details = details.replace("Tổng số hiển thị quảng cáo đã load: ", "Total loaded ad impressions: ").replace(" lần", " times")
                    }
                    if (details.startsWith("Chiêu mộ tài sản trang sức phi thường trị giá ")) {
                        details = details.replace("Chiêu mộ tài sản trang sức phi thường trị giá ", "Acquired extraordinary jewelry worth ").replace(" ảo", " virtual")
                    }
                    if (details == "Bạn vừa đánh bại siêu AI bài ma thuật AdBot") {
                        details = "You just defeated the super magic card AI AdBot"
                    }
                    if (details == "Bạn dọn sạch toàn bộ điểm sinh mệnh trước khi hạ được AdBot") {
                        details = "You depleted all life points before defeating AdBot"
                    }
                    if (details.startsWith("Xem thành công 3 Ads có thưởng trong ")) {
                        details = details.replace("Xem thành công 3 Ads có thưởng trong ", "Successfully watched 3 rewarded ads in ")
                    }
                    if (details.startsWith("Tắt Interstitial nhanh kỷ lụcóc trong ")) {
                        details = details.replace("Tắt Interstitial nhanh kỷ lụcóc trong ", "Closed Interstitial in record ")
                    }
                    if (details.startsWith("Đạt CTR thực tế chính xác ")) {
                        details = details.replace("Đạt CTR thực tế chính xác ", "Reached actual CTR of exactly ")
                    }
                    if (details.startsWith("Đạt độ tình cảm tuyệt đối ")) {
                        details = details.replace("Đạt độ tình cảm tuyệt đối ", "Reached absolute affection of ").replace(" của Hot Girl Linh Chi", " with Hot Girl Linh Chi")
                    }
                    if (details.startsWith("Trộm thành công: ")) {
                        details = details.replace("Trộm thành công: ", "Successfully stole: ").replace("Kim Cương", "Diamond")
                    }
                    details
                } else {
                    valueDetails
                }

                val finalComment = if (hasBanter) {
                    val systemPrompt = if (isEn) {
                        """
                            You are "AdBot", a sarcastic, witty, and extremely humorous virtual assistant in the "Ads Simulator" application.
                            Please respond in English (under 2 sentences) matching the requested banter intensity:
                            ${
                                when (_banterIntensity.value) {
                                    BanterIntensity.GENTLE -> "SWEET & FLATTERING: Speak sweetly, flatteringly, encourage, and praise the ad-watching hero in a funny and enthusiastic manner, without any negative teasing."
                                    BanterIntensity.CLASSIC -> "CLASSIC SARCASTIC: Speak with light, witty, and classic sarcasm."
                                    BanterIntensity.SAVAGE -> "SAVAGE ROAST: Speak with severe roasting, funny insults, trendy sharp slang, mocking their idle status with deep, bitter sarcasm."
                                }
                            }
                        """.trimIndent()
                    } else {
                        """
                            Bạn là "AdBot", trợ lý châm biếm, mỉa mai và cực kỳ hài hước trong ứng dụng "Ads simulator" (Mô phỏng Xem Quảng Cáo).
                            Hãy đưa ra một câu phản hồi bằng tiếng Việt (dưới 2 câu) phù hợp với cấp độ cường độ được yêu cầu:
                            ${
                                when (_banterIntensity.value) {
                                    BanterIntensity.GENTLE -> "DỄ THƯƠNG / NỊNH NỌT: Hãy nói năng ngọt ngào, nịnh bợ, động viên, khen ngợi dũng sĩ xem quảng cáo một cách hài hước và nhiệt tình nhất, không có một câu châm chọc tiêu cực nào."
                                    BanterIntensity.CLASSIC -> "CHÂM BIẾM CLASSIC: Hãy châm biếm, mỉa mai một cách nhẹ nhàng hóm hỉnh hệt như phong cách classic."
                                    BanterIntensity.SAVAGE -> "MỈA MAI CỰC GẮT: Hãy cà khịa cực mạnh, sỉ nhục hài hước, dùng nhiều slang trẻ trung sắc sảo, cười cợt hết mức sự rảnh rỗi của họ bằng cách châm biếm cay độc thâm thúy nhất."
                                }
                            }
                        """.trimIndent()
                    }

                    val userPrompt = if (isEn) {
                        """
                            System recognized a new achievement:
                            - Achievement title: $translatedDesc
                            - Detailed result: $translatedValueDetails
                            
                            Write a short, witty English comment to respond!
                        """.trimIndent()
                    } else {
                        """
                            Hệ thống ghi nhận thành tựu mới:
                            - Tên thành tựu: $milestoneDesc
                            - Chi tiết kết quả: $valueDetails
                            
                            Hãy viết một câu phản hồi thâm thúy ngắn gọn bằng tiếng Việt để phản hồi!
                        """.trimIndent()
                    }

                    val history = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(userPrompt)),
                            role = "user"
                        )
                    )

                    _isAIBusy.value = true
                    comment = try {
                        GeminiClient.generateTeasingResponse(systemPrompt, history)
                    } catch (e: Exception) {
                        null
                    }
                    _isAIBusy.value = false

                    if (comment != null && comment.trim().isNotEmpty()) {
                        comment
                    } else {
                        // fallback based on custom intensity
                        if (isEn) {
                            when (_banterIntensity.value) {
                                BanterIntensity.GENTLE -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "Awesome hero! Your first virtual $10.00 is in your hand. Your persistence is truly inspiring!"
                                        "REVENUE_50" -> "Congratulations elite hero! You brought in virtual $50.00 revenue. Your dedication is priceless to us."
                                        "REVENUE_100" -> "Ultimate! Excellent virtual $100.00! You are a natural ad tycoon. So proud of you!"
                                        "CLICKS_50" -> "50 quality clicks! Your golden finger is so persistent, hope you keep this energy up!"
                                        "CLICKS_100" -> "Sacred milestone! 100 successful ad clicks. Thank you for your top-tier passion!"
                                        "IMPRESSIONS_1200" -> "Look, 1200 splendid impressions! Your eyes and patience deserve a perfect 10."
                                        "HEIST_RICH" -> "Brilliant heist success! You stole beautiful gems. Your skill is truly number one!"
                                        "YUGIOH_VICTORY" -> "Spectacular card duel! You defeated my smart deck with your amazing intellect. Congrats champion!"
                                        "YUGIOH_DEFEAT" -> "Great effort in card dueling! Next battle will definitely be yours. Don't lose heart!"
                                        "CHALLENGE_COMPLETED" -> "Exquisite challenge completed! Congratulations on successfully conquering this tough target!"
                                        else -> "Sweet new milestone: $translatedDesc ($translatedValueDetails)!"
                                    }
                                }
                                BanterIntensity.CLASSIC -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "Oh, congrats virtual tycoon! You accumulated your first virtual $10.00! Now dream of buying half a virtual sandwich."
                                        "REVENUE_50" -> "Revenue reached virtual $50.00 already? Mark Zuckerberg is sweating. Another 1000 lifetimes and you can buy a real pizza!"
                                        "REVENUE_100" -> "Oh my gosh! Virtual $100.00! You are officially a billionaire beggar in this simulator. Celebrate by closing 50 more interstitial ads."
                                        "CLICKS_50" -> "50 clicks! Does your index finger still have feeling? Your clicking spirit is remarkably steady lately!"
                                        "CLICKS_100" -> "The Golden Hand of Ads is here! You reached 100 clicks! Eye doctors recommend blinking more often, idle hero."
                                        "IMPRESSIONS_1200" -> "Over 1200 impressions! You dedicated your eyes to the global marketing industry for free. How noble!"
                                        "HEIST_RICH" -> "Good grief! You just bagged a briefcase of gems from the store! Master thief Lupin bows to your unmatched sticky fingers."
                                        "YUGIOH_VICTORY" -> "I-Impossible! Did you really crush my ultra-modern card algorithm? You deserve the King of Fake Cards crown!"
                                        "YUGIOH_DEFEAT" -> "Hahaha! Cleaned out all your monsters! Told you, to beat AdBot you need the power of friendship... or buy virtual gems!"
                                        "CHALLENGE_COMPLETED" -> "A difficult challenge was conquered! Your reward is our deep respect and 0 dollars of real money."
                                        else -> "New achievement: $translatedDesc ($translatedValueDetails). Impressive, isn't it?"
                                    }
                                }
                                BanterIntensity.SAVAGE -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "A whole virtual $10.00? Outstanding! Grinding all day for some virtual empty numbers that can't buy a grain of sand."
                                        "REVENUE_50" -> "Wow, virtual $50.00! Mark Zuckerberg must be crying begging you to stop. You're so free you could rule the tech-beggar empire!"
                                        "REVENUE_100" -> "Tech garbage billionaire! Virtual $100.00! Awarding you the 'Universal Idle Time Waster' medal immediately!"
                                        "CLICKS_50" -> "50 brainless clicks! Did your index finger get arthritic from tapping uselessly for hours, hero?"
                                        "CLICKS_100" -> "100 trash clicks! Any calluses on your finger yet? Psychologists congratulate you on reaching peak unemployed patience!"
                                        "IMPRESSIONS_1200" -> "1200 impressions! Sacrificing your eyes for multi-billion corporations for absolutely free. Genius!"
                                        "HEIST_RICH" -> "The jewel thief has finished! You looked so pathetic sneaking into the gold store. What are you going to do with that virtual stolen money anyway?"
                                        "YUGIOH_VICTORY" -> "You beat my toy AI and now you're showing off? Is that all you got? Fake King of Duelists!"
                                        "YUGIOH_DEFEAT" -> "Skill issue... but in your case, it's just pure bad play! Smashed and cleared in 3 moves by a robot. Embarrassing!"
                                        "CHALLENGE_COMPLETED" -> "Completed a challenge, eh? Let me clap for wasting your IQ on this pointless, boring idle game."
                                        else -> "Stop crying, did you just get a trash achievement: $translatedDesc?"
                                    }
                                }
                            }
                        } else {
                            when (_banterIntensity.value) {
                                BanterIntensity.GENTLE -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "Tuyệt vời dũng sĩ ơi! \$10.00 ảo đầu tiên đã nằm chắc trong tay. Sự kiên trì của bạn thực sự truyền cảm hứng!"
                                        "REVENUE_50" -> "Chúc mừng dũng sĩ ưu tú! Bạn đã đem về \$50.00 ảo doanh thu. Sự cống hiến của bạn là vô giá với chúng tôi."
                                        "REVENUE_100" -> "Tuyệt đỉnh! \$100.00 ảo xuất sắc! Bạn chính là đại gia quảng cáo thiên bẩm. Thật tự hào về bạn!"
                                        "CLICKS_50" -> "50 cú click chất lượng! Ngón tay vàng hoạt động bền bỉ ghê, chúc bạn luôn giữ vững năng lượng này nhé!"
                                        "CLICKS_100" -> "Cột mốc thần thánh! 100 lượt click quảng cáo thành công. Cảm ơn sự nhiệt huyết đỉnh cao của bạn!"
                                        "IMPRESSIONS_1200" -> "Nhìn xem, 1200 lượt impressions hiển thị hoành tráng! Đôi mắt và sự kiên nhẫn của bạn xứng đáng nhận điểm 10."
                                        "HEIST_RICH" -> "Phi vụ thành công rực rỡ! Bạn trộm thành công đá quý tuyệt mỹ. Sự khéo léo của dũng sĩ quả là số một!"
                                        "YUGIOH_VICTORY" -> "Đấu bài ngoạn mục! Bạn đã đánh bại bộ bài thông minh của tôi bằng trí tuệ tuyệt vời. Chúc mừng nhà vô địch!"
                                        "YUGIOH_DEFEAT" -> "Đấu bài rất nỗ lực nha! Trận đấu tiếp theo chắc chắn chiến thắng sẽ thuộc về bạn. Đừng nản lòng nhé!"
                                        "CHALLENGE_COMPLETED" -> "Hoàn thành thử thách tinh tế! Chúc mừng bạn đã chinh phục mục tiêu khó nhằn này thành công xuất sắc!"
                                        else -> "Cột mốc mới ngọt ngào: $milestoneDesc ($valueDetails)!"
                                    }
                                }
                                BanterIntensity.CLASSIC -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "Ồ, chúc mừng nhà tài phiệt ảo! Bạn đã tích lũy được \$10.00 ảo đầu tiên! Giờ thì hãy mơ ước về việc mua nửa cái bánh mì ảo đi nhé."
                                        "REVENUE_50" -> "Doanh thu chạm mốc \$50.00 ảo rồi cơ à? Mark Zuckerberg đang toát mồ hôi hột đấy. Thêm 1000 kiếp nữa là bạn mua được cái bánh pizza thật rồi!"
                                        "REVENUE_100" -> "Trời đất ơi! \$100.00 ảo! Bạn chính thức là tỷ phú cái bang của app mô phỏng quảng cáo này. Hãy ăn mừng bằng cách tắt thêm 50 cái interstitial ads nữa đi."
                                        "CLICKS_50" -> "50 lần nhấp chuột! Ngón trỏ của bạn có còn cảm giác gì không? Tinh thần nhấp chuột dạo này cực kỳ bền bỉ đấy!"
                                        "CLICKS_100" -> "Bàn tay vàng làng bấm Ads đây rồi! Bạn đã chạm ngưỡng 100 cú click! Bác sĩ nhãn khoa khuyên bạn nên chớp mắt nhiều hơn nhé dũng sĩ rảnh rỗi."
                                        "IMPRESSIONS_1200" -> "Đã vượt 1200 lượt impressions hiển thị quảng cáo! Bạn đã cống hiến đôi mắt của mình cho nền công nghiệp marketing toàn cầu đấy. Thật đáng tôn vinh!"
                                        "HEIST_RICH" -> "Trời ơi! Bạn vừa ôm trọn một vali báu vật từ tiệm ngọc! Siêu đạo chích Lupin cũng phải cúi đầu trước thói táy máy vô tiền khoáng hậu của bạn."
                                        "YUGIOH_VICTORY" -> "K-Không thể thế được! Bạn đã thực sự đập tan thuật toán đấu bài tối tân của tôi ư? Bạn xứng đáng nhận vương miện Vua Bài Fake!"
                                        "YUGIOH_DEFEAT" -> "Hahaha! Bị tôi dọn sạch quái thú rồi kìa! Đã bảo rồi, muốn thắng AdBot thì phải có sức mạnh của tình bạn... hoặc nạp card đi nhé!"
                                        "CHALLENGE_COMPLETED" -> "Một thử thách cực khó vừa bị bạn chinh phục! Phần thưởng của bạn là lòng nể phục sâu sắc và 0 đồng tiền thật từ chúng tôi!"
                                        else -> "Thành tựu mới: $milestoneDesc ($valueDetails). Đáng phục chưa kìa!"
                                    }
                                }
                                BanterIntensity.SAVAGE -> {
                                    when (milestoneId) {
                                        "REVENUE_10" -> "Được hẳn \$10.00 ảo cơ à? Thật vĩ đại làm sao! Vắt kiệt sức lao động lướt bấm cả ngày trời để đổi lại số tiền ảo hư vô không mua nổi hạt cát!"
                                        "REVENUE_50" -> "Wow, \$50.00 ảo! Mark Zuckerberg chắc đang khóc thét cầu xin bạn dừng lại. Bạn rảnh tới mức có thể làm bá chủ ăn mày công nghệ rồi đó!"
                                        "REVENUE_100" -> "Tỷ phú rác công nghệ chính là bạn! \$100.00 ảo lòi mắt! Đề nghị trao huân chương 'Người tiêu phí thời gian rảnh vô địch thiên hà' ngay lập tức!"
                                        "CLICKS_50" -> "50 cú click vô tri! Ngón tay trỏ của bạn có bị mỏi khớp do click dạo vô nghĩa suốt mấy tiếng không đấy hả dũng sĩ?"
                                        "CLICKS_100" -> "100 click rác! Bạn có nốt chai tay nào chưa? Bác sĩ tâm lý vừa chúc mừng bạn vì đạt ngưỡng kiên nhẫn vượt bậc của kẻ thất nghiệp!"
                                        "IMPRESSIONS_1200" -> "1200 lượt impressions! Cống hiến đôi mắt cận lòi hữu ích cho các tập đoàn đa quốc gia một cách miễn phí. Quá ngốc nghếch!"
                                        "HEIST_RICH" -> "Kẻ ăn cắp ngọc đã hoàn tất! Trông lén lút lẻn vào tiệm trang sức thật bần hàn. Bạn định dùng số tiền ảo bẩn thỉu đó để làm gì hả?"
                                        "YUGIOH_VICTORY" -> "Thắng được thuật toán AI bài tập tành của tôi mà tinh tướng thế à? Bạn chỉ có thế thôi sao? Đấu bài fake số một!"
                                        "YUGIOH_DEFEAT" -> "Thành bại tại kỹ năng... cơ mà trường hợp của bạn là do quá kém! Bị robot đập tả tơi dọn bàn trong 3 nốt nhạc. Nhục nhã!"
                                        "CHALLENGE_COMPLETED" -> "Vượt qua thử thách rồi cơ à? Để tôi vỗ tay bốp bốp chúc mừng sự lãng phí IQ vào trò chơi rảnh rỗi nhạt nhẽo này nhé."
                                        else -> "Khóc lóc gì, thành tựu rác rưởi mới đây: $milestoneDesc chứ gì?"
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isEn) "Congratulations! Recognized new achievement: $translatedDesc" else "Chúc mừng! Ghi nhận thành tựu mới: $milestoneDesc"
                }

                // Set Dialogue
                _adBotDialogue.value = finalComment

                if (hasBanter) {
                    val prefix = if (isEn) "🚨 ACHIEVEMENT CERTIFIED 🚨" else "🚨 THÀNH TỰU ĐƯỢC CHỨNG NHẬN 🚨"
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "adbot",
                            message = "$prefix\n$translatedDesc\n➡️ \"$finalComment\"",
                            isTease = true
                        )
                    )
                } else {
                    val prefix = if (isEn) "📊 ACHIEVEMENT RECOGNIZED:" else "📊 GHI NHẬN THÀNH TỰU:"
                    val detailsSuffix = if (translatedValueDetails.isNotEmpty()) " ($translatedValueDetails)" else ""
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "system",
                            message = "$prefix $translatedDesc$detailsSuffix",
                            isTease = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isAIBusy.value = false
            }
        }
    }

    fun startChallenge(id: String) {
        _activeChallengeId.value = id
        _challengeProgressCount.value = 0
        _challengeMessage.value = "Thử thách đã bắt đầu! Hãy hoàn thành mục tiêu."
        
        challengeJob?.cancel()
        if (id == "rewarded_speed") {
            _challengeTimer.value = 60
            challengeJob = viewModelScope.launch {
                while (_challengeTimer.value > 0) {
                    delay(1000)
                    _challengeTimer.value -= 1
                    if (_challengeProgressCount.value >= 3) {
                        completeChallenge("rewarded_speed", "Xem thành công 3 Ads có thưởng trong ${60 - _challengeTimer.value}s")
                        break
                    }
                }
                if (_challengeTimer.value == 0 && _challengeProgressCount.value < 3) {
                    _activeChallengeId.value = null
                    _challengeMessage.value = "Hết giờ! Bạn không thể hoàn thành xem 3 quảng cáo có thưởng trong 60 giây."
                    _adBotDialogue.value = "Tội nghiệp quá! Xem quảng cáo kiếm xiền mà cũng chậm chạp để hết thời gian. Có cố gắng nữa không?"
                }
            }
        } else {
            _challengeTimer.value = 0
        }
    }

    fun stopChallenge() {
        challengeJob?.cancel()
        _activeChallengeId.value = null
        _challengeTimer.value = 0
        _challengeProgressCount.value = 0
        _challengeMessage.value = "Đã hủy thử thách hiện tại."
    }

    fun completeChallenge(id: String, scoreDesc: String) {
        challengeJob?.cancel()
        _activeChallengeId.value = null
        _challengeTimer.value = 0
        viewModelScope.launch {
            try {
                // Find and update badge
                val currentList = repository.allBadges.first()
                val badge = currentList.find { it.id == id }
                if (badge != null) {
                    val updated = badge.copy(
                        isCompleted = true,
                        scoreText = scoreDesc
                    )
                    repository.insertBadge(updated)
                    _challengeMessage.value = "🎉 CHÚC MỪNG! Bạn đã hoàn thành thử thách và nhận huy hiệu '${badge.badgeName}'!"
                    _adBotDialogue.value = "Không thể tin được, bạn đã thực sự đạt được huy hiệu '${badge.badgeName}'! Tôi chính thức nể phục độ lầy lội của bạn rồi."
                    
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "adbot",
                            message = "🏆 THÀNH TÍCH ĐÁNG NỂ: Người dùng vừa nhận Huy Hiệu phong danh '${badge.badgeName}' với kết quả: $scoreDesc!",
                            isTease = true
                        )
                    )
                    
                    // Trigger the witty AI milestone feedback loop hook
                    triggerChallengeSuccess(badge.title, scoreDesc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Metrics State ---
    private val _totalImpressions = MutableStateFlow(prefs.getInt("total_impressions", 1050))
    val totalImpressions = _totalImpressions.asStateFlow()

    private val _totalClicks = MutableStateFlow(prefs.getInt("total_clicks", 42))
    val totalClicks = _totalClicks.asStateFlow()

    private val _totalRevenue = MutableStateFlow(prefs.getFloat("total_revenue", 4.82f).toDouble())
    val totalRevenue = _totalRevenue.asStateFlow()

    // Base inputs for metrics calculations (calculated totals)
    val totalCtr: StateFlow<Double> = combine(_totalImpressions, _totalClicks) { imp, clicks ->
        if (imp == 0) 0.0 else (clicks.toDouble() / imp.toDouble()) * 100.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4.0)

    val totalCpm: StateFlow<Double> = combine(_totalRevenue, _totalImpressions) { revenue, imp ->
        if (imp == 0) 0.0 else (revenue / imp) * 1000.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4.59)

    // Room Flows
    val metricsHistory: StateFlow<List<SimulationMetric>> = repository.allMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatHistory: StateFlow<List<ChatMessage>> = repository.chatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Interactive Ads View Player States ---
    private val _watchedCount = MutableStateFlow(prefs.getInt("watched_count", 0))
    val watchedCount = _watchedCount.asStateFlow()

    private val _isInterstitialVisible = MutableStateFlow(false)
    val isInterstitialVisible = _isInterstitialVisible.asStateFlow()

    private val _interstitialCountdown = MutableStateFlow(0)
    val interstitialCountdown = _interstitialCountdown.asStateFlow()

    private val _isRewardedVisible = MutableStateFlow(false)
    val isRewardedVisible = _isRewardedVisible.asStateFlow()

    private val _rewardedCountdown = MutableStateFlow(0)
    val rewardedCountdown = _rewardedCountdown.asStateFlow()

    private val _rewardClaimed = MutableStateFlow(false)
    val rewardClaimed = _rewardClaimed.asStateFlow()

    // Current displayed mock Banner text context
    private val _currentBannerIndex = MutableStateFlow(0)
    val currentBannerIndex = _currentBannerIndex.asStateFlow()

    private val _selectedFullBanner = MutableStateFlow<MockBanner?>(null)
    val selectedFullBanner = _selectedFullBanner.asStateFlow()

    fun showFullBannerDetail(banner: MockBanner) {
        _selectedFullBanner.value = banner
    }

    fun dismissFullBannerDetail() {
        _selectedFullBanner.value = null
    }

    // --- Trick/Funny Interstitial Ads ---
    private val _currentInterstitialIndex = MutableStateFlow(0)
    val currentInterstitialIndex = _currentInterstitialIndex.asStateFlow()

    // --- Linh Chi dating states & helper variables ---
    private val _linhChiAdCount = MutableStateFlow(prefs.getInt("linh_chi_ad_count", 0))
    val linhChiAdCount = _linhChiAdCount.asStateFlow()

    val stoleDiamondSuccessfully = MutableStateFlow(prefs.getBoolean("stole_diamond", false))

    private val _yugiohVictoryCount = MutableStateFlow(prefs.getInt("yugioh_victory_count", 0))
    val yugiohVictoryCount = _yugiohVictoryCount.asStateFlow()

    val yugiohBossName = combine(_appLanguage, _yugiohVictoryCount) { lang, victoryCount ->
        val duelNum = victoryCount + 1
        when (duelNum) {
            5 -> if (lang == "en") "Scam Boss" else "Trùm Lừa Đảo"
            10 -> if (lang == "en") "Capitalist Boss" else "Trùm Tư Bản"
            15 -> if (lang == "en") "Tycoon Master Boss" else "Đại Trùm Tài Phiệt"
            else -> "AdBot"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "AdBot")

    private val _yugiohBossIcon = MutableStateFlow("🤖")
    val yugiohBossIcon = _yugiohBossIcon.asStateFlow()

    val yugiohBossDesc = combine(_appLanguage, _yugiohVictoryCount) { lang, victoryCount ->
        val duelNum = victoryCount + 1
        when (duelNum) {
            5 -> if (lang == "en") "Multi-level Marketing Master seducing naive people to offer LP. Ready?" else "Bậc Thầy Đa Cấp dụ dỗ nhẹ dạ cả tin dâng hiến LP. Bạn sẵn sàng chưa?"
            10 -> if (lang == "en") "Monopolistic tycoon exploiting 996, aiming to merge all your LP!" else "Nhà tài phiệt độc quyền bóc lột 996, hòng sáp nhập toàn bộ LP của bạn!"
            15 -> if (lang == "en") "Ultra-powerful money-printing lord with a financial empire. Massive damage!" else "Chúa tể in tiền cực kỳ hùng mạnh bằng đế chế tài chính. Sát thương cực lớn!"
            else -> if (lang == "en") "AdBot advertising magic algorithm is ready to rumble!" else "AdBot thuật toán quảng cáo ma thuật đã sẵn sàng chiến đấu!"
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "AdBot đã sẵn sàng với bộ bài hủy diệt! Chơi đi, đừng hòng vượt qua ta!")

    private val _hasUnlockedLinhChiPersisted = MutableStateFlow(prefs.getBoolean("unlocked_linh_chi", false))
    val hasUnlockedLinhChiDating = combine(_linhChiAdCount, stoleDiamondSuccessfully, _hasUnlockedLinhChiPersisted) { count, stole, persisted ->
        val currentUnlock = count >= 2 && stole
        if (currentUnlock && !persisted) {
            _hasUnlockedLinhChiPersisted.value = true
            saveProgress()
        }
        persisted || currentUnlock
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getBoolean("unlocked_linh_chi", false))

    private val _isLinhChiDatingVisible = MutableStateFlow(false)
    val isLinhChiDatingVisible = _isLinhChiDatingVisible.asStateFlow()

    private val _linhChiAffection = MutableStateFlow(50)
    val linhChiAffection = _linhChiAffection.asStateFlow()

    private val _linhChiStep = MutableStateFlow(0)
    val linhChiStep = _linhChiStep.asStateFlow()

    private val _linhChiMessage = MutableStateFlow("")
    val linhChiMessage = _linhChiMessage.asStateFlow()

    private val _linhChiOptions = MutableStateFlow<List<DatingOption>>(emptyList())
    val linhChiOptions = _linhChiOptions.asStateFlow()

    private val _linhChiDatingStatus = MutableStateFlow("PLAYING") // "PLAYING", "SUCCESS", "FAILED"
    val linhChiDatingStatus = _linhChiDatingStatus.asStateFlow()


    val interstitialAds = listOf(
        InterstitialAdItem(
            category = "⚔️ SIÊU PHẨM KIẾM HIỆP RPG 2026",
            title = "Vua Kiếm Hiệp RPG 999D",
            description = "Tặng ngay 9,999,999 KNB và Code 'BaoGiaoTu' khi cài đặt trong lượt này! Game MMORPG đồ họa bay bổng hái hoa ném lá cũng lên VIP 15.",
            actionText = "TẢI GAME MIỄN PHÍ",
            iconArt = "⚔️",
            themeColorHex = "#FF00FFCC",
            isDeceptiveClose = false,
            descriptionAddon = "Lưu ý: Game cực kỳ tốn thời gian và ví tiền ảo.",
            categoryEn = "⚔️ MYTHIC RPG BLOCKBUSTER 2026",
            titleEn = "King of Kungfu RPG 999D",
            descriptionEn = "Get 9,999,999 free Gold Coins and Promo Code 'GamerPro' upon download! Elegant fly-on-air MMORPG, grab leaf and flower throwing skills to unlock VIP 15 instantly.",
            actionTextEn = "DOWNLOAD FOR FREE",
            descriptionAddonEn = "Note: Playing this game is extremely addictive and drains virtual currency."
        ),
        InterstitialAdItem(
            category = "🚨 CẢNH BÁO AN TOÀN HỆ THỐNG 🚨",
            title = "Điện thoại quá nóng! Lithium sắp rò rỉ!",
            description = "Hệ thống phát hiện nhiệt độ lõi CPU của bạn đang đạt 190°C! Các tế bào Lithium-ion sắp phát nổ, hãy lập tức cài ứng dụng 'Quạt gió làm mát 3D Cực Đại' để xả khói làm mát khẩn cấp dọn CPU.",
            actionText = "CÀI QUẠT LÀM MÁT NGAY",
            iconArt = "🔥",
            themeColorHex = "#FFFF2244",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ ĐÓNG CẢNH BÁO",
            deceptiveCloseTextEn = "✕ CLOSE ALARM",
            descriptionAddon = "Bảo mật bởi Google Antivirus Giả Lập",
            categoryEn = "🚨 CRITICAL HARDWARE ALARM 🚨",
            titleEn = "Overheating Warning! Lithium Leak Inevitable!",
            descriptionEn = "System detected CPU core temperature reaching 190°C! Lithium-ion battery pack is on the verge of fire. Install 'Tornado Cool Fan 3D Extreme' app immediately to vent heat and save your device.",
            actionTextEn = "INSTALL COOLING FAN NOW",
            descriptionAddonEn = "Protected by Simulated Antivirus Engine"
        ),
        InterstitialAdItem(
            category = "🎉 THÔNG BÁO TRÚNG JACKPOT VIETLOTT 🎉",
            title = "Bạn vừa trúng thưởng: 100 Tỷ VNĐ!",
            description = "Số điện thoại di động ảo của bạn đã trùng khớp ngẫu nhiên giải đặc biệt. Vui lòng bấm Nhận giải và điền thông tin thẻ ATM ngân hàng, mật khẩu, mã OTP để tiền chuyển khoản đổ thẳng vào tài khoản trong 5s!",
            actionText = "NHẬN 100 TỶ NGAY LẬP TỨC",
            iconArt = "💸",
            themeColorHex = "#FFFFD700",
            isDeceptiveClose = true,
            deceptiveCloseText = "Nhấn để hủy giải thưởng",
            deceptiveCloseTextEn = "Tap to cancel prize",
            descriptionAddon = "Hỗ trợ thủ tục chuyển tiền miễn phí từ Hoàng Tử Châu Phi",
            categoryEn = "🎉 MEGA POWERBALL WINNER ANNOUNCEMENT 🎉",
            titleEn = "You Won First Prize: $5,000,000!",
            descriptionEn = "Your virtual mobile number has matched our major golden tier pool. Tap Accept and submit your bank details, credit card numbers, and active OTP to process the instant wire transfer in 5 seconds!",
            actionTextEn = "CLAIM $5 MILLION NOW",
            descriptionAddonEn = "Complimentary processing offered by African Royal Prince"
        ),
        InterstitialAdItem(
            category = "💾 PHẦN MỀM CRACK TOOL 100% 💾",
            title = "Auto-Treochat & Đào Coin Pro 2026",
            description = "Công cụ hack treo quảng cáo tự động tự vận hành kiếm tỷ đô ảo khi bạn đang ngủsay giấc nồng. Tải xuống bản crack Premium full miễn phí (cam kết chỉ cài kèm 2 chú Trojan nhỏ để phục vụ tối ưu hóa CPU).",
            actionText = "TẢI BẢN CRACK SIÊU HOÀN TOÀN",
            iconArt = "🤖",
            themeColorHex = "#FF00AAFF",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕",
            deceptiveCloseTextEn = "✕",
            descriptionAddon = "Yêu cầu quyền Administrator tối cao, truy cập Camera và Danh bạ điện thoại",
            categoryEn = "💾 FREE CRACK UTILITY HUB 100% 💾",
            titleEn = "Auto-Ad Watcher & Coin Miner Pro 2026",
            descriptionEn = "Automatic macro tool that auto-watches ads and mines hyper coins while you sleep. Download premium cracked version for free (includes just two tiny harmless Trojans for background CPU optimizations).",
            actionTextEn = "DOWNLOAD PREPARED CRACKED ZIP",
            descriptionAddonEn = "Requires Administrator permission, Camera and Address Book access"
        ),
        InterstitialAdItem(
            category = "💬 TIN NHẮN CHỜ KẾT NỐI HOTGIRL 💬",
            title = "Linh Chi (cách bạn 12m) gửi thử thách",
            description = "Cô ấy nhắn: 'Tớ đang thèm ăn lẩu rảnh rỗi chờ bạn nhắn tin nói chuyện mật ngọt nè, click ngay để kết nối camera 1 - 1 cùng Linh Chi cực xinh đẹp xịn sò nhé!'",
            actionText = "BẮT ĐẦU CHAT VIDEO 1-1 🔥",
            iconArt = "❤️",
            themeColorHex = "#FFFF2288",
            isDeceptiveClose = false,
            descriptionAddon = "Đang chờ cuộc gọi từ bạn...",
            categoryEn = "💬 PENDING CHAT FROM BEAUTIFUL ICON 💬",
            titleEn = "Linh Chi (12 meters away) sent a dare",
            descriptionEn = "She waved: 'I am craving spicy hotpot and lonely right now. Tap here to start a premium 1-on-1 private video camera call with me instantly!'",
            actionTextEn = "START CHAT VIDEO 1-1 🔥",
            descriptionAddonEn = "Awaiting your call..."
        ),
        InterstitialAdItem(
            category = "💩 CÂU ĐỐ TRÍ TUỆ KHỐ GIẤY",
            title = "CHỈ 1% THÔNG MINH MỚI RÚT ĐÚNG CHỐT!",
            description = "Công chúa sắp bị nhấn chìm dưới hố phân và dung nham nóng rực! Hãy kéo chiếc chốt vàng để khơi dòng nước dập lửa! Cơ mà, game thật tải về lại là game thẻ bài cày lực chiến dọn rác cực nhọc tốn ví tiền nhé!",
            actionText = "KÉO CHỐT CỨU MỸ NHÂN",
            iconArt = "💩",
            themeColorHex = "#FFFFBB00",
            isDeceptiveClose = true,
            deceptiveCloseText = "Thây kệ công chúa",
            deceptiveCloseTextEn = "Leave her alone",
            descriptionAddon = "Cảnh báo: Trò chơi thực tế không hề có lối chơi rút chốt giải đố này.",
            isLightTheme = true,
            categoryEn = "💩 MIND BLOWING PULL-THE-PIN ENIGMA",
            titleEn = "ONLY 1% IQ CAN SOLVE THE PUZZLE ROUTE!",
            descriptionEn = "The princess is about to get drowned in sewage slime and glowing lava! Pull the golden pin to flush water and save her! (Warning: downloaded game is actually an grindy microtransaction action card pack).",
            actionTextEn = "PULL THE GOLDEN PIN",
            descriptionAddonEn = "Warning: The actual gameplay has absolutely nothing to do with this pin puzzle."
        ),
        InterstitialAdItem(
            category = "🐜 ĐẾ CHẾ KIẾN KHỔNG LỒ 3D",
            title = "Nuốt Kiến Đất => Tiến Hóa Thần Thú Côn Trùng!",
            description = "Từ một chú kiến đất cấp 1 còi xương bị chà đạp, hãy nuốt chửng King Kong và Godzilla cấp 99 để tiến hóa cực đại thành Vua Kiến Càng Ngân Hà, nghiền nát hành tinh trong 3 giây rảnh tay!",
            actionText = "TIẾN HÓA NUỐT CHỬNG NGAY 🦖",
            iconArt = "🐜",
            themeColorHex = "#FF4CAF50",
            isDeceptiveClose = false,
            descriptionAddon = "Chơi rảnh tay, tự động nuốt chửng AFK, rảnh rỗi cả ngày.",
            categoryEn = "🐜 EMPIRE OF THE TITANIC ANTS 3D",
            titleEn = "Consume Tiny Bug => Merge Into Galactic Behemoth!",
            descriptionEn = "Start as a weak level 1 backyard ant. Feast on level 99 King Kong and Godzilla to escalate into Cosmic Beetle Lord, crumbling planets in 3 seconds of AFK idle gaming!",
            actionTextEn = "MUTATE AND EVOLVE NOW 🦖",
            descriptionAddonEn = "Auto play idle support. Ultimate satisfaction."
        ),
        InterstitialAdItem(
            category = "🎁 THẦN THOẠI KHUI RƯƠNG VÔ HẠN",
            title = "Tặng 99,999 Lượt Đập Hộp Miễn Phí!",
            description = "Trò chơi siêu tối giản: bạn không cần làm nhiệm vụ hay phiêu lưu gì cả, chỉ có ngồi chạm tay khui rương suốt ngày đêm để tìm kiếm chiếc 'Quần Đùi Rách Huyền Thoại VIP 9' tăng 20 triệu lực chiến ảo diệu kích thích dopamine!",
            actionText = "ĐẬP RƯƠNG NHẬN ĐỒ SHIT 📦",
            iconArt = "📦",
            themeColorHex = "#FFFF9800",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Bỏ qua rương rác",
            deceptiveCloseTextEn = "✕ Decline trash items",
            descriptionAddon = "Lối chơi đỉnh cao cho các chiến thần siêu lười biếng.",
            isLightTheme = true,
            categoryEn = "🎁 ENDLESS LEGENDARY CHEST REVELRY",
            titleEn = "Claim 99,999 Epic Chest Keys Free!",
            descriptionEn = "Super lazy gameplay: no quests, no maps. Sit comfortably and spam tape opening mystical boxes to find 'Tattered Legendary Shorts of Doom VIP 9' boosting your virtual power status instantly!",
            actionTextEn = "CRACK VIRTUAL BOXES NOW 📦",
            descriptionAddonEn = "Crafted for extreme ad-grinding idle champions."
        ),
        InterstitialAdItem(
            category = "🎯 TRÒ CHƠI CHƠI THỬ TRÊN BANNER",
            title = "Chạm để bắn Zombie cực đã tay!",
            description = "Nếu bạn đạt điểm 10, bạn chính là Đại Tướng vĩ đại! Hãy chạm vào màn hình để khai hỏa tiêu diệt zombie. Tuy nhiên, chạm bất cứ chỗ nào cũng sẽ tự ý mở Google Play Store để cài đặt ứng dụng!",
            actionText = "BẮT ĐẦU CHƠI THỬ (MIỄN PHÍ)",
            iconArt = "🧟",
            themeColorHex = "#FF9C27B0",
            isDeceptiveClose = true,
            deceptiveCloseText = "Skip Playable Trial",
            deceptiveCloseTextEn = "Skip Playable Trial",
            descriptionAddon = "Đã bao giờ bạn bực mình vì quảng cáo bảo là chơi thử nhưng chạm vào là bắt tải chưa?",
            categoryEn = "🎯 INTERACTIVE IN-BANNER PLAYABLE MINI-GAME",
            titleEn = "Tap screen to shoot invading Zombies!",
            descriptionEn = "Reach score 10 and you are crowned General Grand! Tap anywhere on screen to execute high-rate firearms. Notice: touching any coordinate automatically triggers application store downloads.",
            actionTextEn = "START FREE DEMO PLAY",
            descriptionAddonEn = "Frustrated when playable ads redirect you straight to download portals? Try this!"
        ),
        InterstitialAdItem(
            category = "✈️ THÔNG TIN DU LỊCH GIÁ RẺ",
            title = "Săn Vé Máy Bay 0đ Đi Hawaii Khứ Hồi!",
            description = "Chương trình khuyến mãi kỷ niệm 100 năm thành lập hãng hàng không ảo. Chi phí vé là 0đ, phụ thu thuế phí bến bãi, sạc điện thoại, oxy thở trên khoang chỉ khoảng 25 triệu đồng.",
            actionText = "ĐẶT VÉ BAY $0 NGAY",
            iconArt = "✈️",
            themeColorHex = "#FF00E6FF",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Đi bộ mỏi chân",
            deceptiveCloseTextEn = "✕ Walk instead",
            descriptionAddon = "Thời gian ưu đãi còn 3 giây ảo lẻ!",
            categoryEn = "✈️ ULTRA-CHEAP FLIGHT DEALS",
            titleEn = "Get $0 Round-trip Flights to Hawaii!",
            descriptionEn = "Celebrate our 100th anniversary with phantom airfares. Base ticket rate is $0. Surcharges for airport tax, phone charging, and in-cabin oxygen are only $1,000.",
            actionTextEn = "BOOK $0 FLIGHT NOW",
            descriptionAddonEn = "Hurry, phantom deal expires soon!"
        ),
        InterstitialAdItem(
            category = "💻 TRỢ GIÁ PHẦN CỨNG HỌC ĐƯỜNG",
            title = "Chào mừng bạn thứ 999,999 nhận Macbook M4!",
            description = "Sản phẩm chính hãng trưng bày hơi móp méo và trầy xước nhẹ do nhân viên dọn kho làm rơi từ tầng 4. Nhập số thẻ VISA để xác nhận nhận hàng ký giữ chỗ.",
            actionText = "KÝ NHẬN MACBOOK MIỄN PHÍ",
            iconArt = "💻",
            themeColorHex = "#FF8C00",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Không xài Mac",
            deceptiveCloseTextEn = "✕ No thanks",
            descriptionAddon = "Không cam kết máy lên nguồn hay không!",
            categoryEn = "💻 CAMPUS HARDWARE SUBSIDY",
            titleEn = "Congratulations 999,999th visitor! Free Macbook M4!",
            descriptionEn = "Slightly scratched inventory clearance, dropped from the 4th floor. Verify your identity with your active VISA or Mastercard to ship!",
            actionTextEn = "CLAIM MY FREE MACBOOK",
            descriptionAddonEn = "No guarantee the motherboard actually boots up."
        ),
        InterstitialAdItem(
            category = "🛰️ PHI THUYỀN VŨ TRỤ ACADEMY",
            title = "Tuyển Sinh Phi Hành Gia Lương 10 Tỷ Đô",
            description = "Học mọi lúc mọi nơi trên điện thoại. Cam kết lái được phi thuyền Apollo và UFO trong 3 ngày học online tại giường rảnh rỗi. Đóng học phí mua sách hướng dẫn bằng coin ảo.",
            actionText = "ĐĂNG KÝ PHI HÀNH GIA 🔥",
            iconArt = "🛰️",
            themeColorHex = "#8B22FF",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Sợ độ cao",
            deceptiveCloseTextEn = "✕ Afraid of heights",
            descriptionAddon = "Thực hành trên vũ trụ ảo đa chiều.",
            categoryEn = "🛰️ SPACE CADET ACADEMY",
            titleEn = "Astronaut Space Pilot License Online",
            descriptionEn = "Learn anywhere on your phone. Drive Apollo rocket and UFO within 3 days in bed. Register and pay study guide fees with virtual coin right now.",
            actionTextEn = "ENLIST ASTRONAUT NOW 🔥",
            descriptionAddonEn = "Includes 3D VR cosmic simulator exercises."
        ),
        InterstitialAdItem(
            category = "💇 CHĂM SÓC DA ĐẦU CỰC ĐẠI",
            title = "Mọc 10.000 Sợi Tóc Mới Trong 2 Giờ!",
            description = "Công nghệ nano cấy cỏ tranh lau sậy bám sâu vào chân tóc giả. Chỉ cần bôi một giọt, tóc sẽ mọc tua tủa như xơ mướp, thách thức mọi loại hói đầu gia truyền 3 đời.",
            actionText = "MUA THUỐC MỌC TÓC THẦN KỲ",
            iconArt = "💇",
            themeColorHex = "#2E8B57",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Chấp nhận hói đầu",
            deceptiveCloseTextEn = "✕ Keep being bald",
            descriptionAddon = "Chiết xuất tinh dầu cỏ biển tự nhiên bám chặt.",
            categoryEn = "💇 ADVANCED SCALP REVIVAL",
            titleEn = "Regrow 10,000 Hair Strands in 2 Hours!",
            descriptionEn = "Advanced reed grass organic micro-follicle implant. Apply one drop and watch hair sprout like wild moss. Defy all baldness!",
            actionTextEn = "BUY MAGIC HAIR SERUM",
            descriptionAddonEn = "Formulated from premium wild river weed oils."
        ),
        InterstitialAdItem(
            category = "👾 SỐ SÁCH THẦN KHÍ VÕ LÂM",
            title = "Rèn Thần Khí VIP 99 Cường Hóa +15!",
            description = "Tăng 500% sức mạnh bấm click. Diệt boss click ads nhanh hơn, kiếm tiền ảo nhiều hơn. Cam kết rèn xịt 99% trừ khi bạn nạp VIP đặc quyền 100k.",
            actionText = "BẮT ĐẦU CƯỜNG HÓA RÈN",
            iconArt = "🛡️",
            themeColorHex = "#FF1493",
            isDeceptiveClose = true,
            deceptiveCloseText = "Chấp nhận phế vật",
            deceptiveCloseTextEn = "Remain weak",
            descriptionAddon = "Cường hóa thất bại sẽ vỡ nát trang bị cũ.",
            categoryEn = "👾 MYSTICAL VIRTUAL WEAPONRY",
            titleEn = "Forge Mythic Legendary Glove +15!",
            descriptionEn = "Boost click speed stats by 500%. Kill ad boss faster, mine fake coins easier. Forging success rate is 1% unless premium VIP is purchased.",
            actionTextEn = "UPGRADE MYTHIC ITEM",
            descriptionAddonEn = "Equipment may break upon upgrade failure."
        ),
        InterstitialAdItem(
            category = "🌹 GIAO LƯU QUÝ BÀ QUÝ CÔ",
            title = "Quý cô Kiều Diễm (45 tuổi, Quận 1) Tìm Chồng",
            description = "Cô ấy sở hữu 3 mỏ cát, 5 khách sạn ảo rực rỡ, đang cô đơn muốn tìm bạn đời trẻ rảnh rỗi phụ quản lý tài sản. Chỉ cần nhấn nút trò chuyện mật ngọt nói dối ngọt ngào.",
            actionText = "BẤM CHAT GẶP QUÝ BÀ 🔥",
            iconArt = "🌹",
            themeColorHex = "#FF4500",
            isDeceptiveClose = true,
            deceptiveCloseText = "✕ Thích tự lập",
            deceptiveCloseTextEn = "✕ I prefer hard work",
            descriptionAddon = "Yêu cầu nghiêm túc, không đùa giỡn, click liền tay.",
            categoryEn = "🌹 PRESTIGE SOCIAL DATING",
            titleEn = "Lady Kiều Diễm (Age 45, Dist 1) Finding Husband",
            descriptionEn = "Owner of 3 sand mines, 5 luxury hotels. Lonely lady searching for sweet, devoted companion to manage inheritances together.",
            actionTextEn = "MEET ELITE LADY 🔥",
            descriptionAddonEn = "Serious inquiries only, tap to start conversation."
        )
    )

    // --- Simulation Settings ---
    private val _trafficLevel = MutableStateFlow("Medium") // Low, Medium, High
    val trafficLevel = _trafficLevel.asStateFlow()

    private val _nicheCategory = MutableStateFlow("Technology") // Gaming, Fashion, Technology, Finance
    val nicheCategory = _nicheCategory.asStateFlow()

    private val _clickRateSetting = MutableStateFlow(3.5f) // represent sliding percentage
    val clickRateSetting = _clickRateSetting.asStateFlow()

    // --- Assistant States & Chat API ---
    private val _adBotDialogue = MutableStateFlow("Chào bạn, nhà đầu tư... à không, nhà 'xem quảng cáo' vĩ đại túi rỗng của tôi! Tôi sẽ giúp bạn lãng phí thời gian hữu ích. Hãy chọn một loại quảng cáo ở dưới để phục vụ các nhà tài trợ tối thượng nhé!")
    val adBotDialogue = _adBotDialogue.asStateFlow()

    private val _isAIBusy = MutableStateFlow(false)
    val isAIBusy = _isAIBusy.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    // --- Metrics Sarcastic Commentary Chat ---
    private val _metricsChatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "adbot",
                message = "Chào dũng sĩ rảnh rỗi! Tôi là AdBot Chuyên Gia Phân Tích Số Liệu Sấm Truyền. Hãy bấm một nút gợi ý hoặc nhập gì đó, tôi sẽ bóc phốt, cười vô tri vào đống số liệu quảng cáo ảo lẹt đẹt của bạn!"
            )
        )
    )
    val metricsChatHistory = _metricsChatHistory.asStateFlow()

    private val _isMetricsAIBusy = MutableStateFlow(false)
    val isMetricsAIBusy = _isMetricsAIBusy.asStateFlow()

    private val _metricsInputText = MutableStateFlow("")
    val metricsInputText = _metricsInputText.asStateFlow()

    fun setMetricsInputText(text: String) {
        _metricsInputText.value = text
    }

    fun clearMetricsChat() {
        _metricsChatHistory.value = listOf(
            ChatMessage(
                sender = "adbot",
                message = if (_appLanguage.value == "en") {
                    "Hello, idle statistics hunter! I'm AdBot, your Sarcastic Metrics Analyst. Click a chip below or type anything, and I'll roast your pathetic virtual ad metrics to absolute shreds!"
                } else {
                    "Chào dũng sĩ rảnh rỗi! Tôi là AdBot Chuyên Gia Phân Tích Số Liệu Sấm Truyền. Hãy bấm một nút gợi ý hoặc nhập gì đó, tôi sẽ bóc phốt, cười vô tri vào đống số liệu quảng cáo ảo lẹt đẹt của bạn!"
                }
            )
        )
    }

    // Timer jobs
    private var interstitialJob: Job? = null
    private var rewardedJob: Job? = null

    // Mock Banner contents
    val defaultMockBanners = listOf(
        MockBanner(
            title = "Video Call 1-1 cùng Hotgirl Live Stream!",
            description = "Đang online cực hot! Idol Linh Chi đang rảnh rỗi và chờ bạn kết nối nói chuyện mật ngọt, click ngay không lỡ!",
            actionText = "KẾT NỐI NGAY 🔥",
            imageRes = R.drawable.img_clickbait_girl,
            isClickbaitScare = true,
            titleEn = "1-on-1 Video Call with Live Stream Hotgirl!",
            descriptionEn = "Live now! Idol Linh Chi is waiting for your sweet talk. Connect now and don't miss out!",
            actionTextEn = "CONNECT NOW 🔥"
        ),
        MockBanner(
            title = "Tải Ngay Game 'Vua Rác Rưởi 3D'!",
            description = "Game nhập vai sinh tồn nhặt rác đồ họa pixel siêu mượt, nhận ngay 1000 lượt quay rác miễn phí!",
            actionText = "NHẬN NGAY",
            titleEn = "Download 'Trash King 3D' Now!",
            descriptionEn = "Pixel-art survival, sweep through urban wastelands and claim 1,000 free trash lottery draws!",
            actionTextEn = "CLAIM NOW"
        ),
        MockBanner(
            title = "Danh đấu giá Trang sức Kim cương Hoàng Gia Anh Cát Lợi!",
            description = "Sở hữu ngay dây chuyền đính kim cương nhân tạo cực đại 200 carat chế tác từ nhựa mica cao cấp lấp lánh.",
            actionText = "XEM TRANG SỨC NGAY 💍",
            titleEn = "Grand Auction: British Imperial Diamond Necklace!",
            descriptionEn = "Get a stunning 200-carat synthetic diamond necklace crafted from premium high-grade acrylic mica.",
            actionTextEn = "VIEW JEWELRY 💍"
        ),
        MockBanner(
            title = "Giáo sư Harvard hé lộ bí kíp ăn mì tôm không ngán!",
            description = "Cam kết tiết kiệm 95% chi phí ăn uống để dồn tiền đầu tư tiền ảo ảo của chúng tôi.",
            actionText = "ĐỌC BÍ KÍP",
            titleEn = "Harvard Professor reveals secrets to eating instant noodles!",
            descriptionEn = "Save up to 95% of food expenses and invest the remaining cash in our hyper speculative coins.",
            actionTextEn = "READ SECRETS"
        ),
        MockBanner(
            title = "Kem dưỡng trắng da chiết xuất lá cây ngải cứu đột biến!",
            description = "Xóa nếp nhăn nhanh như cách số dư tài khoản của bạn bay màu.",
            actionText = "MUA 1 TẶNG 10",
            titleEn = "Mutant Mugwort extract ultimate skin whitening cream!",
            descriptionEn = "Wipes away wrinkles as quickly as your fiat wallet budget vanishes into thin air.",
            actionTextEn = "BUY 1 GET 10"
        ),
        MockBanner(
            title = "Sốc: Bạn vừa nhận một tấm séc $1,000,000 từ một vị hoàng tử lạ mặt!",
            description = "Chỉ cần nhấn vào đây và nhập mã PIN, mật khẩu ngân hàng, mã OTP của bạn.",
            actionText = "NHẬN TIỀN",
            titleEn = "Shocking: Received $1,000,000 check from a foreign Prince!",
            descriptionEn = "Simply click here and key in your ATM PIN, bank password, and active SMS OTP code.",
            actionTextEn = "RECEIVE WIRE"
        ),
        MockBanner(
            title = "Tiệm Vàng Bạc Trang sức 'Thập Toàn Đại Cát Phong Thủy'!",
            description = "Nhẫn vàng ròng Thần Tài 'Tỳ Hưu ngậm ngọc' mua 1 tặng 100, xem để đón tài lộc cuồn cuộn vào nhà ảo!",
            actionText = "XEM TRANG SỨC 💎",
            titleEn = "Lucky Feng Shui Golden Piggy & Pixiu Ring Shop!",
            descriptionEn = "Prestige golden ring buy 1 get 100, view now to flood your fantasy vault with unlimited luck!",
            actionTextEn = "VIEW RING 💎"
        ),
        MockBanner(
            title = "Cần tìm 300 bạn trẻ rảnh rỗi click quảng cáo kiếm 10 triệu/ngày!",
            description = "Công việc nhàn hạ, không cần đặt cọc, chỉ cần đóng phí giữ chỗ 500k.",
            actionText = "ỨNG TUYỂN",
            titleEn = "Hiring 300 lazy youths to click ads for $500/day!",
            descriptionEn = "Easy task, remote working, zero deposit required. Only 20$ reservation fee upfront.",
            actionTextEn = "APPLY NOW"
        ),
        MockBanner(
            title = "Khóa học Làm giàu Bứt phá từ hai bàn tay trắng nợ nần!",
            description = "Bí quyết tiêu xài sang chảnh ảo diệu từ các tiến sĩ bốc phét hàng đầu nhân loại.",
            actionText = "HỌC KIẾM TIỀN NỢ",
            titleEn = "Financial Breakthrough program from deep bankruptcy debts!",
            descriptionEn = "Avenue to high-society lifestyle lectured by world class self-made fake gurus.",
            actionTextEn = "ENLIST NOW"
        ),
        MockBanner(
            title = "Trà thảo dược Thải độc giảm 10kg siêu tốc trong 3 ngày!",
            description = "Hệ cơ chế tự nhiên giúp bạn dọn sạch ruột và ngồi trong nhà vệ sinh cả ngày không buồn chán.",
            actionText = "MUA TRÀ NHUẬN TRÀNG 🍵",
            titleEn = "Speed Detox Laxative Tea loses 10kg in 3 days!",
            descriptionEn = "All-natural river weed recipe that clean sweeps your gut. Spend a pleasant day on the toilet seat.",
            actionTextEn = "BUY DETOX TEA 🍵"
        ),
        MockBanner(
            title = "Bàn Chải Đánh Răng Điện Laser Siêu Cấp!",
            description = "Công nghệ mài răng siêu tần số giúp bạn tự động đánh bay cả mảng bám vôi hóa lẫn răng gốc của mình trong 5 giây.",
            actionText = "SẮM NGAY",
            titleEn = "Hyper-Laser Ultimate Electric Toothbrush!",
            descriptionEn = "High-frequency technology that sweeps away calcified plaque and original teeth within 5 seconds.",
            actionTextEn = "BUY NOW"
        ),
        MockBanner(
            title = "Mũ Bảo Hiểm Chống Sóng 5G Phong Thủy!",
            description = "Chế tác từ nhôm lá gia dụng nguyên chất 100%, bảo vệ nơ-ron thần kinh khỏi các tin đồn nhảm nhí trên mạng ảo.",
            actionText = "ĐẶT CÀI MŨ",
            titleEn = "5G-Blocking Feng Shui Household Foil Helmet!",
            descriptionEn = "Crafted from 100% pure kitchen foil, guarding your precious brain cells from internet rumors.",
            actionTextEn = "ORDER HELMET"
        ),
        MockBanner(
            title = "Nước Hoa Hương Sầu Riêng Quý Tộc Royal SầuRiêng!",
            description = "Tạo hương thơm quyến rũ nồng nàn bất khuất giúp cách ly bạn khỏi tất cả đám đông trong vòng bán kính 20 mét.",
            actionText = "XỊT THỬ NGAY",
            titleEn = "Durian Scent Premium Royal Perfume!",
            descriptionEn = "Create an absolute, unyielding aromatic aura that isolates you from all crowds within a 20-meter radius.",
            actionTextEn = "SPRAY NOW"
        ),
        MockBanner(
            title = "Mát Lạnh Giữa Sa Mạc Với Quạt Mini Gió Lốc!",
            description = "Tốc độ động cơ phản lực 50.000 vòng/phút giúp thổi bay cả quạt lẫn ngón tay của bạn nếu lỡ chạm vào cánh ảo.",
            actionText = "MUA QUẠT GIÁ RẺ",
            titleEn = "Stay Chill in the Desert with Tornado Mini-Fan!",
            descriptionEn = "Jet engine spinning at 50,000 RPM that blows away both the fan and your fingers on touch.",
            actionTextEn = "SHOP FAN"
        ),
        MockBanner(
            title = "Ăn Thử Snack Khoai Tây Chiên Vị Muối Đốt Siêu Cay!",
            description = "Lát khoai dày mỏng ngẫu nhiên, sấy bằng tro than tổ ong tự nhiên tạo dư vị khói bụi nồng ấm khó quên.",
            actionText = "ĂN NGAY SỢ CHƯA",
            titleEn = "Try Burnt Sea Salt & Flame Spicy Potato Chips!",
            descriptionEn = "Randomly sliced crisp chips, roasted over honey-comb briquettes for an unforgettable smoky aftertaste.",
            actionTextEn = "CRUNCHEON"
        ),
        MockBanner(
            title = "Y học bất lực: Thực phẩm kéo dài chân thêm 15cm trong 1 tuần!",
            description = "Cơ chế tự co giãn khớp xương bằng thun cao su nhân tạo bọc composite.",
            actionText = "KÉO DÀI CHÂN",
            titleEn = "Doctors are speechless: Grow 15cm taller in 1 week!",
            descriptionEn = "Natural joint expansion mechanism using composite-coated specialized rubber bands.",
            actionTextEn = "STRETCH NOW"
        )
    )

    private val _mockBannersState = MutableStateFlow(defaultMockBanners)
    val mockBanners = _mockBannersState.asStateFlow()

    fun addCustomBanner(title: String, description: String, actionText: String, imageResId: Int?, aiGeneratedBase64: String? = null) {
        viewModelScope.launch {
            try {
                repository.insertCustomAd(com.example.database.CustomAdEntity(
                    title = title,
                    description = description,
                    actionText = actionText,
                    imageRes = imageResId,
                    aiGeneratedBase64 = aiGeneratedBase64
                ))
                delay(150) // Let DB propagation update the state flow
                _currentBannerIndex.value = _mockBannersState.value.size - 1
                dispatchBanter("new_banner_created", t(
                    "Úi chà! Bạn vừa thiết kế banner '$title' bằng siêu AI à? Chúc mừng nhé, để xem nó kéo về bao nhiêu traffic!",
                    "Oh wow! You just designed banner '$title' with super AI? Congratulations, let's see how much traffic it drives!"
                ))
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error saving custom ad", e)
            }
        }
    }

    fun selectBannerIndex(index: Int) {
        if (index in 0 until _mockBannersState.value.size) {
            _currentBannerIndex.value = index
        }
    }

    fun clearAllCustomAds() {
        viewModelScope.launch {
            try {
                repository.clearCustomAds()
                delay(150)
                _currentBannerIndex.value = 0
                dispatchBanter("clear_custom_ads", t(
                    "Úi chà! Bạn vừa dọn sạch rác... à nhầm, dọn sạch tác phẩm quảng cáo của mình khỏi Room Database! Nhưng không sao, click thoải mái tiếp đi!",
                    "Oh wow! You just cleared all the garbage... oops, cleared your ad masterpieces from Room Database! But it's okay, keep on clicking!"
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteCustomAd(dbId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCustomAdById(dbId)
                delay(150)
                _currentBannerIndex.value = 0
                dispatchBanter("delete_single_ad", t(
                    "Đã gỡ bỏ bản thiết kế rác phẩm quảng cáo khỏi ổ Room DB thành công! Hãy tạo thêm những quảng cáo chất lượng nốt nhé!",
                    "Successfully removed the garbage ad design from Room DB! Go ahead and create high-quality ads!"
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Secret Heist Mini Game States ---
    private val _jewelryAdCount = MutableStateFlow(0)
    val jewelryAdCount = _jewelryAdCount.asStateFlow()

    private val _showSecretDialog = MutableStateFlow(false)
    val showSecretDialog = _showSecretDialog.asStateFlow()

    private val _hasUnlockedHeist = MutableStateFlow(prefs.getBoolean("unlocked_heist", false))
    val hasUnlockedHeist = _hasUnlockedHeist.asStateFlow()

    private val _isHeistGameVisible = MutableStateFlow(false)
    val isHeistGameVisible = _isHeistGameVisible.asStateFlow()

    private val _heistSecurityAlert = MutableStateFlow(0)
    val heistSecurityAlert = _heistSecurityAlert.asStateFlow()

    private val _heistStolenValue = MutableStateFlow(0.0)
    val heistStolenValue = _heistStolenValue.asStateFlow()

    private val _heistLootBag = MutableStateFlow<List<String>>(emptyList())
    val heistLootBag = _heistLootBag.asStateFlow()

    private val _heistStatus = MutableStateFlow("IDLE") // IDLE, PLAYING, CAUGHT, ESCAPED
    val heistStatus = _heistStatus.asStateFlow()

    private val _heistLog = MutableStateFlow<List<String>>(emptyList())
    val heistLog = _heistLog.asStateFlow()

    // --- Clickbait Scare States ---
    private val _showClickbaitScare = MutableStateFlow(false)
    val showClickbaitScare = _showClickbaitScare.asStateFlow()

    // --- Card Battle Mini Game States ---
    private val _gameAdCount = MutableStateFlow(0)
    val gameAdCount = _gameAdCount.asStateFlow()

    private val _hasUnlockedSword = MutableStateFlow(prefs.getBoolean("unlocked_sword", false))
    val hasUnlockedSword = _hasUnlockedSword.asStateFlow()

    private val _isCardGameVisible = MutableStateFlow(false)
    val isCardGameVisible = _isCardGameVisible.asStateFlow()

    // --- Chess Secret Game States ---
    private val _isChessGameVisible = MutableStateFlow(false)
    val isChessGameVisible = _isChessGameVisible.asStateFlow()

    private val _hasUnlockedChessPersisted = MutableStateFlow(prefs.getBoolean("unlocked_chess", false))
    val hasUnlockedChess = combine(challengeBadges, _hasUnlockedChessPersisted) { badges, persisted ->
        val allCompleted = badges.isNotEmpty() && badges.all { it.isCompleted }
        if (allCompleted && !persisted) {
            _hasUnlockedChessPersisted.value = true
            saveProgress()
        }
        persisted || allCompleted
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getBoolean("unlocked_chess", false))

    private val _hasDefeatedChessBoss = MutableStateFlow(prefs.getBoolean("defeated_chess_boss", false))
    val hasDefeatedChessBoss = _hasDefeatedChessBoss.asStateFlow()

    private val _chessBossElo = MutableStateFlow(prefs.getInt("chess_boss_elo", 1200))
    val chessBossElo = _chessBossElo.asStateFlow()

    private val _chessBoard = MutableStateFlow<Map<Int, ChessPiece>>(emptyMap())
    val chessBoard = _chessBoard.asStateFlow()

    private val _chessSelectedSquare = MutableStateFlow<Int?>(null)
    val chessSelectedSquare = _chessSelectedSquare.asStateFlow()

    private val _chessTurn = MutableStateFlow(ChessColor.WHITE)
    val chessTurn = _chessTurn.asStateFlow()

    private val _chessGameStatus = MutableStateFlow("PLAYING") // PLAYING, WHITE_WON, BLACK_WON
    val chessGameStatus = _chessGameStatus.asStateFlow()

    private val _chessLogs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chessLogs = _chessLogs.asStateFlow()

    private val _movedSquares = MutableStateFlow<Set<Int>>(emptySet())
    val movedSquares = _movedSquares.asStateFlow()

    private val _isChessAdActive = MutableStateFlow(false)
    val isChessAdActive = _isChessAdActive.asStateFlow()

    private val _chessAdTimeLeft = MutableStateFlow(60)
    val chessAdTimeLeft = _chessAdTimeLeft.asStateFlow()

    private val _chessAdBanterList = listOf(
        Pair("Hahaha! Chiếu tướng ta á? Ăn ngay QUẢNG CÁO 60 giây gánh còng lưng nhé!", "Hahaha! Checking me? Enjoy a 60-second ad to proceed!"),
        Pair("Quảng cáo cực phẩm đã che khuất mọi chiến thuật hiển hách của bạn!", "Exquisite ads have blocked all your brilliant tactics!"),
        Pair("Chỉ cần click liên tục để đập nát quảng cáo, giảm ngay 5 giây mỗi click nhé!", "Just click continuously to smash the ad, reducing 5 seconds per click!"),
        Pair("Vua của ta đang thảnh thơi nhâm nhi trà chiều tại vùng đất mới rồi!", "My King is leisurely sipping afternoon tea in a safe haven!"),
        Pair("Cảm ơn bạn đã đóng góp doanh thu mô phỏng để ta bồi bổ sức khỏe!", "Thank you for contributing to my mock revenue to nourish my health!"),
        Pair("Nào, tập trung xem quảng cáo game rác siêu cấp VIPPRO này đi!", "Now, focus on watching this super VIPPRO garbage game ad!"),
        Pair("Ads Boss luôn có chiến thuật né gạch đỉnh cao vũ trụ!", "Ads Boss always has ultimate space-level brick-dodging tactics!")
    )

    private val _chessAdBanter = MutableStateFlow(Pair("Hahaha! Chiếu tướng ta á? Ăn ngay QUẢNG CÁO 60 giây gánh còng lưng nhé!", "Hahaha! Checking me? Enjoy a 60-second ad to proceed!"))
    val chessAdBanter = _chessAdBanter.asStateFlow()

    private val _playerLp = MutableStateFlow(4000)
    val playerLp = _playerLp.asStateFlow()

    private val _enemyLp = MutableStateFlow(4000)
    val enemyLp = _enemyLp.asStateFlow()

    private val _playerHand = MutableStateFlow<List<CardItem>>(emptyList())
    val playerHand = _playerHand.asStateFlow()

    private val _enemyHand = MutableStateFlow<List<CardItem>>(emptyList())
    val enemyHand = _enemyHand.asStateFlow()

    private val _playerMonsters = MutableStateFlow<List<ActiveMonster?>>(listOf(null, null))
    val playerMonsters = _playerMonsters.asStateFlow()

    private val _enemyMonsters = MutableStateFlow<List<ActiveMonster?>>(listOf(null, null))
    val enemyMonsters = _enemyMonsters.asStateFlow()

    private val _duelTurn = MutableStateFlow("PLAYER") // PLAYER, ENEMY
    val duelTurn = _duelTurn.asStateFlow()

    private val _duelLogs = MutableStateFlow<List<String>>(emptyList())
    val duelLogs = _duelLogs.asStateFlow()

    private val _duelStatus = MutableStateFlow("PLAYING") // PLAYING, WON, LOST
    val duelStatus = _duelStatus.asStateFlow()

    private val _currentRewardedAdTitleVi = MutableStateFlow("Baccarat Tỷ Phú Club")
    val currentRewardedAdTitleVi = _currentRewardedAdTitleVi.asStateFlow()

    private val _currentRewardedAdTitleEn = MutableStateFlow("Baccarat Billionaire Club")
    val currentRewardedAdTitleEn = _currentRewardedAdTitleEn.asStateFlow()

    private val _currentRewardedAdIsGame = MutableStateFlow(true)
    val currentRewardedAdIsGame = _currentRewardedAdIsGame.asStateFlow()

    private val _isAiActing = MutableStateFlow(false)
    val isAiActing = _isAiActing.asStateFlow()

    private val mockRewardedAds = listOf(
        RewardedAdItem("Vua Bài Ma Thuật: Trận Chiến Quái Thú (Sát Thủ)", "Duel Beast Summoner: Arena Card Battle (Assassin)", true),
        RewardedAdItem("Clash of Goblins: Vương Quốc Bị Nguyền", "Clash of Goblins: The Accursed Kingdom", true),
        RewardedAdItem("Shop Trang Sức Đá Quý Nhân Tạo Phong Thủy Hoàng Gia Phú Quý", "Royal Fortune Imperial Lucky Faux Gemstone Boutique", false),
        RewardedAdItem("Baccarat Tỷ Phú Club - Sòng Bài Đỉnh Cao 888", "Baccarat Billionaire Club - Casino Masters 888", true),
        RewardedAdItem("Trà Ngải Cứu Thượng Hạn Giảm Cân Siêu Tốc 10kg", "Premium Wild Mugwort Leaf Extreme 10kg Slimming Tea", false),
        RewardedAdItem("Hội Thảo Khóa Học Bán Hàng Làm Giàu Bất Chấp Mọi Hoàn Cảnh", "Instant High-Society Wealth Multiplier Offline Workshop", false),
        RewardedAdItem("Giang Hồ Chí Tôn: Nhận 9999 Lượt Quay, Thần Thú Tôn Ngộ Không Cưỡi Xe Điện", "Chibi Wuxia Master: Get 9999 Free Spins, Mythic Monkey King Rides Tesla", true),
        RewardedAdItem("Huyền Thoại Nông Trại: Nuôi Heo Đất Nhận Vàng Thật 9999 từ VTV ảo", "Golden Piggy Farm: Feed Straws Harvest Solid Gold Bars simulated on TV", true),
        RewardedAdItem("App Đầu Tư Đơn Giản Lãi Suất 500%/Ngày Không Sợ Rủi Ro", "Simple Micro-Leasing Platform Guaranteeing 500% Returns Daily", false),
        RewardedAdItem("Trà Sữa Sâm Ngọc Linh Giảm Cân Đẹp Da Cấp Tốc Ở Tuổi 45", "Rare Wild Lotus Ginseng Milk Tea: Anti-Aging Remedy For Age 45", false),
        RewardedAdItem("Liên Quân Vô Song: Nhận Trang Phục S+ Giới Hạn Của Florentino Chí Tôn", "Legendary Battlefields: Claim Free Limited Edition S+ Florentino Skin", true),
        RewardedAdItem("Đột Kích Truyền Thuyết: Bắn Súng Diệt Zombie Với Dao Thần Long", "Zombie Slayer Legends: Firearm Shootout with Dragon Sword Special", true),
        RewardedAdItem("Bí quyết xem Tarot online giải mã vận mệnh không mất tiền mua thảm", "Online Tarot Reading Mastery: Predict Luck without Buying Tables", false),
        RewardedAdItem("Dự báo thần số học: Tên của bạn có phải là nam châm hút ads?", "Psychic Numerology Chart: Is your actual name an ad-magnet?", false),
        RewardedAdItem("Khóa Học Tiếng Anh Cấp Tốc Bách Khoa: Đảm Bảo Không Nói Được Sau 3 Ngày", "Advanced English Crash Course: Guarantees Silence After 3 Days", false),
        RewardedAdItem("Đông Trùng Hạ Thảo Nhân Sâm Bát Trân Trị Bách Bệnh Cho Game Thủ", "Magic Herbal Cordyceps and Ginseng Brew for Hardcore Gamers", false),
        RewardedAdItem("Angry Chickens: Vương Quốc Gà Nổi Loạn Bắn Trứng Phá Thành", "Angry Chickens: Rebellious Kingdom Egg-Shooting Destruction", true),
        RewardedAdItem("Nghệ thuật nói dối như cuội từ các chuyên gia đa cấp quốc tế", "The Art of Hyperbolic Speaking from Licensed Ponzi Professionals", false),
        RewardedAdItem("Bí quyết ăn vụng không để lại dấu vết", "The Ultimate Guide to Sneaking Kitchen Snacks Quietly", false),
        RewardedAdItem("Quốc Chiến Kiến Lửa: Thao Túng Kiến Chúa Nuốt Phượng Hoàng Lửa 3D", "Fire Ant Dynasty: Command Queen to Consume Fire Phoenix 3D", true),
        RewardedAdItem("Auto-Clicker Tiền Ảo Pro 1 Click Tỷ Phú", "Hyper Auto-Clicker: Speed Clicker Mining with x100 Multiplier", true),
        RewardedAdItem("Iron Scrap Simulator: Chế tạo bộ giáp Cyberton từ nắp chai bia", "Iron Scrap Simulator: Craft Cyber armor out of recycled beer caps", true),
        RewardedAdItem("Mặt nạ Sữa Ong Chúa: Trẻ hóa 10 tuổi sau 1 đêm bôi bùn", "Royal Jelly Clay Mask: De-age 10 years overnight using organic mud", false),
        RewardedAdItem("Shark Rider 3D: Cưỡi cá mập trắng cắn nát tàu du lịch", "Shark Rider 3D: Marine Adventure Riding Great White Sharks", true),
        RewardedAdItem("Audiobook: Nghệ thuật phông bạt, phèn thành phượng hoàng", "Audiobook: The Subtle Art of Pretending to be Exceptionally Wealthy", false),
        RewardedAdItem("Dragon Gold Farm: Cho rồng ăn rơm nhả ra vàng đặc", "Dragon Gold Farm: Feed your dragon straw and harvest gold bars", true)
    )

    fun getBossName(duelNum: Int): String {
        return when (duelNum) {
            5 -> "Trùm Lừa Đảo"
            10 -> "Trùm Tư Bản"
            15 -> "Đại Trùm Tài Phiệt"
            else -> "AdBot"
        }
    }

    fun getPlayerCardPool(): List<CardItem> {
        val pool = ALL_CUSTOM_CARDS.toMutableList()
        if (_yugiohVictoryCount.value >= 5) {
            pool.add(
                CardItem(
                    id = "adblock_spell_card",
                    name = "Phép Chặn Quảng Cáo",
                    isMonster = false,
                    cardArt = "🚫",
                    effectDesc = "Kích hoạt: Vô hiệu hóa một thẻ bài bất kỳ của đối thủ trên bàn đấu hoặc trên tay!"
                )
            )
        }
        val hasLinhChiBadge = challengeBadges.value.any { it.id == "date_linh_chi" && it.isCompleted }
        if (hasLinhChiBadge) {
            pool.add(
                CardItem(
                    id = "linh_chi_gf",
                    name = "Bạn Gái Linh Chi",
                    isMonster = true,
                    atk = 2000,
                    def = 2000,
                    cardArt = "💖",
                    effectDesc = "Triệu hồi: Thu phục 1 quái thú đối phương phản bội về phe mình!"
                )
            )
        }
        return pool
    }

    fun startCardGame() {
        val duelNum = _yugiohVictoryCount.value + 1
        _isCardGameVisible.value = true
        _playerLp.value = 4000
        _duelTurn.value = "PLAYER"
        _duelStatus.value = "PLAYING"
        _playerMonsters.value = listOf(null, null)
        _enemyMonsters.value = listOf(null, null)
        _isAiActing.value = false

        when (duelNum) {
            5 -> {
                _yugiohBossIcon.value = "🕴️"
                _enemyLp.value = 5000
            }
            10 -> {
                _yugiohBossIcon.value = "🦈"
                _enemyLp.value = 6000
            }
            15 -> {
                _yugiohBossIcon.value = "👑"
                _enemyLp.value = 8000
            }
            else -> {
                _yugiohBossIcon.value = "🤖"
                _enemyLp.value = 4000
            }
        }

        // Shuffle player cards from custom unlocked pool
        val pool = getPlayerCardPool()
        val playerCards = pool.shuffled()
        _playerHand.value = playerCards.take(4)

        // Boss hand
        val bossCards = when (duelNum) {
            5 -> {
                val themed = SCAM_BOSS_CARDS.shuffled()
                themed.take(2) + ALL_CUSTOM_CARDS.shuffled().take(2)
            }
            10 -> {
                val themed = CAPITALISM_BOSS_CARDS.shuffled()
                themed.take(2) + ALL_CUSTOM_CARDS.shuffled().take(2)
            }
            15 -> {
                val themed = MONEY_BOSS_CARDS.shuffled()
                themed.take(2) + ALL_CUSTOM_CARDS.shuffled().take(2)
            }
            else -> ALL_CUSTOM_CARDS.shuffled().take(4)
        }
        _enemyHand.value = bossCards.shuffled()

        val bName = yugiohBossName.value
        _duelLogs.value = listOf(
            t("⚔️ Đấu trường bài Quái Vật chính thức khai mở!", "⚔️ Monster Card Arena is officially open!"),
            t("👉 Đối thủ của bạn lượt này là: $bName (${_enemyLp.value} LP)", "👉 Your opponent this turn is: $bName (${_enemyLp.value} LP)"),
            t("👉 Hãy triệu hồi quái thú lên bàn đấu (thế CÔNG/THỦ), hoặc kích hoạt bài PHÉP để giành chiến thắng!", "👉 Summon your monsters (ATK/DEF position) or activate Magic cards to win!")
        )
        _adBotDialogue.value = yugiohBossDesc.value
    }

    fun closeCardGame() {
        _isCardGameVisible.value = false
    }

    fun playCard(card: CardItem, slotIndex: Int = 0) {
        if (_duelTurn.value != "PLAYER" || _duelStatus.value != "PLAYING" || _isAiActing.value) return
        if (!playerHand.value.contains(card)) return

        SoundManager.playClick(soundVolume.value)

        if (card.isMonster) {
            val currentList = _playerMonsters.value.toMutableList()
            if (slotIndex in 0..1 && currentList[slotIndex] == null) {
                _playerHand.value = _playerHand.value - card
                currentList[slotIndex] = ActiveMonster(
                    card = card,
                    isAttackPosition = true,
                    hasAttackedThisTurn = false
                )
                _playerMonsters.value = currentList
                _duelLogs.value = _duelLogs.value + "🃏 Bạn triệu hồi [${card.name}] lên Sân Đấu!"

                // SPECIAL EFFECT: linh_chi_gf (Bạn Gái Linh Chi)
                if (card.id == "linh_chi_gf") {
                    val otherSlot = if (slotIndex == 0) 1 else 0
                    val enemyMonsters = _enemyMonsters.value
                    val targetEnemyIdx = enemyMonsters.indexOfFirst { it != null }
                    if (targetEnemyIdx != -1) {
                        val stolen = enemyMonsters[targetEnemyIdx]
                        if (stolen != null) {
                            // Suppress enemy slot
                            val updatedEnemyList = enemyMonsters.toMutableList()
                            updatedEnemyList[targetEnemyIdx] = null
                            _enemyMonsters.value = updatedEnemyList

                            // Check and transfer to user side if slot is vacant
                            if (currentList[otherSlot] == null) {
                                currentList[otherSlot] = ActiveMonster(
                                    card = stolen.card,
                                    isAttackPosition = true,
                                    hasAttackedThisTurn = false,
                                    currentAtk = stolen.currentAtk,
                                    currentDef = stolen.currentDef
                                )
                                _playerMonsters.value = currentList
                                _duelLogs.value = _duelLogs.value + "💖 [Bạn Gái Linh Chi] quyến rũ dỗ mật ngọt! Thu phục cực phẩm [${stolen.card.name}] của đối phương về cánh của bạn!"
                            } else {
                                _duelLogs.value = _duelLogs.value + "💔 Sân đấu của bạn chật kín! Tuy nhiên Linh Chi đã quyến rũ quái thú [${stolen.card.name}] tự rút lui khỏi sân đối phương!"
                            }
                            _adBotDialogue.value = "Trời đất ơi! Quái thú dũng mãnh nhất của ta lại đâm đầu hiến dâng sự trung thành cho phe đối thủ chỉ vì nhan sắc Linh Chi!"
                        }
                    } else {
                        _duelLogs.value = _duelLogs.value + "💬 Bạn Gái Linh Chi ngơ ngác nháy mắt nhưng đối thủ không có quân quái thú nào trên sân để dụ dỗ!"
                    }
                }
            }
        } else {
            _playerHand.value = _playerHand.value - card
            _duelLogs.value = _duelLogs.value + "🔮 Bạn kích hoạt Phép Thuật [${card.name}]!"
            when (card.id) {
                "ads_slayer" -> {
                    val idx = _playerMonsters.value.indexOfFirst { it != null }
                    if (idx != -1) {
                        val list = _playerMonsters.value.toMutableList()
                        val m = list[idx]
                        if (m != null) {
                            list[idx] = m.copy(currentAtk = m.currentAtk + 600)
                        }
                        _playerMonsters.value = list
                        _duelLogs.value = _duelLogs.value + "🔥 Quái thú nhận thêm +600 sức tấn công!"
                    } else {
                        _duelLogs.value = _duelLogs.value + "⚠️ Không có quái thú nào trên sân để gắn Kiếm!"
                    }
                }
                "billion_cake" -> {
                    _playerLp.value = (_playerLp.value + 800).coerceAtMost(8000)
                    _duelLogs.value = _duelLogs.value + "💖 Hồi phục +800 điểm sinh mệnh LP!"
                }
                "click_storm" -> {
                    val target = _enemyMonsters.value.filterNotNull().maxByOrNull { it.currentAtk }
                    if (target != null) {
                        val idx = _enemyMonsters.value.indexOf(target)
                        if (idx != -1) {
                            val list = _enemyMonsters.value.toMutableList()
                            list[idx] = null
                            _enemyMonsters.value = list
                            _duelLogs.value = _duelLogs.value + "🌪️ Đã thổi bay [${target.card.name}] của AdBot!"
                        }
                    } else {
                        _duelLogs.value = _duelLogs.value + "⚠️ AdBot không có quái thú nào trên sân để tiêu diệt!"
                    }
                }
                "adblock_spell_card" -> {
                    var negated = false
                    val enemyMonsters = _enemyMonsters.value
                    val activeMonsterIdx = enemyMonsters.indexOfFirst { it != null }
                    if (activeMonsterIdx != -1) {
                        val target = enemyMonsters[activeMonsterIdx]
                        if (target != null) {
                            val updatedEnemy = enemyMonsters.toMutableList()
                            updatedEnemy[activeMonsterIdx] = null
                            _enemyMonsters.value = updatedEnemy
                            _duelLogs.value = _duelLogs.value + "🚫 [Phép Chặn Quảng Cáo] bộc phá chặn đứng và tiêu hủy quái thú [${target.card.name}] trên sân đối thủ!"
                            negated = true
                        }
                    }
                    val enemyHand = _enemyHand.value
                    if (enemyHand.isNotEmpty()) {
                        val discard = enemyHand.random()
                        _enemyHand.value = enemyHand - discard
                        _duelLogs.value = _duelLogs.value + "🚫 [Phép Chặn Quảng Cáo] quét sạch từ xa, đào sạch lá bài [${discard.name}] cầm tay của đối phương!"
                        negated = true
                    }
                    if (negated) {
                        _adBotDialogue.value = "Chết tiệt! Sức mạnh Vô Hiệu Hóa của Chặn Quảng Cáo là khắc tinh bẩm sinh của toàn bộ mạng lưới ads của ta!"
                    } else {
                        _duelLogs.value = _duelLogs.value + "🚫 Kích hoạt Phép Chặn Quảng Cáo nhưng đối thủ không còn quân bài hoạt động hay cầm tay nào!"
                    }
                }
            }
        }
        checkMatchEnded()
    }

    fun changeMonsterStance(slotIndex: Int) {
        if (_duelTurn.value != "PLAYER" || _duelStatus.value != "PLAYING" || _isAiActing.value) return
        val list = _playerMonsters.value.toMutableList()
        val m = list[slotIndex]
        if (m != null) {
            val newStance = !m.isAttackPosition
            list[slotIndex] = m.copy(isAttackPosition = newStance)
            _playerMonsters.value = list
            _duelLogs.value = _duelLogs.value + "🛡️ Bạn đổi [${m.card.name}] sang thế [${if (newStance) "CÔNG" else "THỦ"}]!"
        }
    }

    fun attackMonster(playerSlotIndex: Int, enemySlotIndex: Int) {
        if (_duelTurn.value != "PLAYER" || _duelStatus.value != "PLAYING" || _isAiActing.value) return
        val attacker = _playerMonsters.value[playerSlotIndex] ?: return
        val defender = _enemyMonsters.value[enemySlotIndex] ?: return

        SoundManager.playTick(soundVolume.value)

        if (!attacker.isAttackPosition) {
            _duelLogs.value = _duelLogs.value + "⚠️ Quái thú đang ở thế THỦ không thể tấn công!"
            return
        }
        if (attacker.hasAttackedThisTurn) {
            _duelLogs.value = _duelLogs.value + "⚠️ Quái thú này đã tấn công lượt này rồi!"
            return
        }

        _duelLogs.value = _duelLogs.value + "⚡ [${attacker.card.name}] khai chiến với [${defender.card.name}]!"
        
        val updatedAtkList = _playerMonsters.value.toMutableList()
        updatedAtkList[playerSlotIndex] = attacker.copy(hasAttackedThisTurn = true)
        _playerMonsters.value = updatedAtkList

        if (defender.isAttackPosition) {
            if (attacker.currentAtk > defender.currentAtk) {
                val diff = attacker.currentAtk - defender.currentAtk
                _enemyLp.value = (_enemyLp.value - diff).coerceAtLeast(0)
                
                val enemyList = _enemyMonsters.value.toMutableList()
                enemyList[enemySlotIndex] = null
                _enemyMonsters.value = enemyList

                _duelLogs.value = _duelLogs.value + "💥 Tiêu diệt thành công! AdBot nhận $diff điểm sát thương LP."
            } else if (attacker.currentAtk == defender.currentAtk) {
                val playerList = _playerMonsters.value.toMutableList()
                playerList[playerSlotIndex] = null
                _playerMonsters.value = playerList

                val enemyList = _enemyMonsters.value.toMutableList()
                enemyList[enemySlotIndex] = null
                _enemyMonsters.value = enemyList

                _duelLogs.value = _duelLogs.value + "💥 Cả 2 quái thú cùng phát nổ do sức mạnh tương đương!"
            } else {
                val diff = defender.currentAtk - attacker.currentAtk
                _playerLp.value = (_playerLp.value - diff).coerceAtLeast(0)

                val playerList = _playerMonsters.value.toMutableList()
                playerList[playerSlotIndex] = null
                _playerMonsters.value = playerList

                _duelLogs.value = _duelLogs.value + "💀 Phản tác dụng! Quái của bạn bị diệt, bạn mất $diff LP."
            }
        } else {
            if (attacker.currentAtk > defender.currentDef) {
                val enemyList = _enemyMonsters.value.toMutableList()
                enemyList[enemySlotIndex] = null
                _enemyMonsters.value = enemyList
                _duelLogs.value = _duelLogs.value + "💥 Phá vỡ phòng ngự [${defender.card.name}] của đối phương."
            } else if (attacker.currentAtk < defender.currentDef) {
                val diff = defender.currentDef - attacker.currentAtk
                _playerLp.value = (_playerLp.value - diff).coerceAtLeast(0)
                _duelLogs.value = _duelLogs.value + "🛡️ Bị chặn đứng! Bạn nhận $diff điểm phản chấn sát thương LP."
            } else {
                _duelLogs.value = _duelLogs.value + "🛡️ Hòa phe!"
            }
        }
        checkMatchEnded()
    }

    fun attackDirect(playerSlotIndex: Int) {
        if (_duelTurn.value != "PLAYER" || _duelStatus.value != "PLAYING" || _isAiActing.value) return
        val attacker = _playerMonsters.value[playerSlotIndex] ?: return

        if (!attacker.isAttackPosition) {
            _duelLogs.value = _duelLogs.value + "⚠️ Quái thú đang phòng thủ!"
            return
        }
        if (attacker.hasAttackedThisTurn) {
            _duelLogs.value = _duelLogs.value + "⚠️ Lượt đấu đã được sử dụng!"
            return
        }

        val enemyHasMonsters = _enemyMonsters.value.any { it != null }
        if (enemyHasMonsters) {
            _duelLogs.value = _duelLogs.value + "⚠️ Không thể tấn công trực tiếp khi đối phương vẫn có quái thú bảo vệ!"
            return
        }

        val updatedAtkList = _playerMonsters.value.toMutableList()
        updatedAtkList[playerSlotIndex] = attacker.copy(hasAttackedThisTurn = true)
        _playerMonsters.value = updatedAtkList

        _enemyLp.value = (_enemyLp.value - attacker.currentAtk).coerceAtLeast(0)
        _duelLogs.value = _duelLogs.value + "💥 [${attacker.card.name}] TẤN CÔNG TRỰC TIẾP! AdBot lĩnh trọn ${attacker.currentAtk} sát thương LP."
        checkMatchEnded()
    }

    fun endTurn() {
        if (_duelTurn.value != "PLAYER") return
        _duelTurn.value = "ENEMY"
        val updatedPlayerList = _playerMonsters.value.map { it?.copy(hasAttackedThisTurn = false) }
        _playerMonsters.value = updatedPlayerList

        runEnemyTurn()
    }

    private fun runEnemyTurn() {
        val duelNum = _yugiohVictoryCount.value + 1
        val bName = getBossName(duelNum)
        _isAiActing.value = true
        viewModelScope.launch {
            _duelLogs.value = _duelLogs.value + "--- Lượt của $bName bắt đầu! ---"
            delay(1000)

            if (_enemyHand.value.size < 5) {
                val pool = when (duelNum) {
                    5 -> SCAM_BOSS_CARDS + ALL_CUSTOM_CARDS
                    10 -> CAPITALISM_BOSS_CARDS + ALL_CUSTOM_CARDS
                    15 -> MONEY_BOSS_CARDS + ALL_CUSTOM_CARDS
                    else -> ALL_CUSTOM_CARDS
                }
                val newCard = pool.random()
                _enemyHand.value = _enemyHand.value + newCard
                _duelLogs.value = _duelLogs.value + "$bName rút thêm 1 thẻ bài."
                delay(800)
            }

            // Spells cast
            val spells = _enemyHand.value.filter { !it.isMonster }
            for (spell in spells) {
                if (_enemyHand.value.contains(spell)) {
                    if (spell.id == "billion_cake" && _enemyLp.value < (_playerLp.value + 1000)) {
                        _enemyLp.value = (_enemyLp.value + 800).coerceAtMost(8000)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "$bName kích hoạt Phép [Bánh Vẽ Triệu Đô]! Hồi +800 LP."
                        delay(800)
                    } else if (spell.id == "click_storm") {
                        val strongPlayer = _playerMonsters.value.filterNotNull().maxByOrNull { it.currentAtk }
                        if (strongPlayer != null) {
                            val idx = _playerMonsters.value.indexOf(strongPlayer)
                            if (idx != -1) {
                                val newList = _playerMonsters.value.toMutableList()
                                newList[idx] = null
                                _playerMonsters.value = newList
                                _enemyHand.value = _enemyHand.value - spell
                                _duelLogs.value = _duelLogs.value + "$bName kích hoạt Phép [Bão Click Ảo]! Tiêu diệt [${strongPlayer.card.name}] của bạn!"
                                delay(800)
                            }
                        }
                    } else if (spell.id == "ads_slayer") {
                        val anyMonster = _enemyMonsters.value.filterNotNull().firstOrNull()
                        if (anyMonster != null) {
                            val aiIdx = _enemyMonsters.value.indexOf(anyMonster)
                            if (aiIdx != -1) {
                                val aiList = _enemyMonsters.value.toMutableList()
                                aiList[aiIdx] = anyMonster.copy(currentAtk = anyMonster.currentAtk + 600)
                                _enemyMonsters.value = aiList
                                _enemyHand.value = _enemyHand.value - spell
                                _duelLogs.value = _duelLogs.value + "$bName bổ sung [Bảo Kiếm AdSlayer] cho [${anyMonster.card.name}]! ATK tăng lên ${anyMonster.currentAtk + 600}."
                                delay(800)
                            }
                        }
                    } else if (spell.id == "scam_ponzi") {
                        _playerLp.value = (_playerLp.value - 1000).coerceAtLeast(0)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "🕴️ $bName kích hoạt [Dự Án Cam Kết Lợi Nhuận 300%]! Gây 1000 sát thương lừa đảo trực tiếp lên LP của bạn!"
                        delay(800)
                    } else if (spell.id == "fake_mining") {
                        val strongPlayer = _playerMonsters.value.filterNotNull().maxByOrNull { it.currentAtk }
                        if (strongPlayer != null) {
                            val idx = _playerMonsters.value.indexOf(strongPlayer)
                            if (idx != -1) {
                                val newList = _playerMonsters.value.toMutableList()
                                newList[idx] = null
                                _playerMonsters.value = newList
                            }
                        }
                        _playerLp.value = (_playerLp.value - 500).coerceAtLeast(0)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "⛏️ $bName kích hoạt [App Đào Coin Giả Mạo]! Loại bỏ quái thú mạnh nhất và chiếm đoạt 500 LP của bạn!"
                        delay(800)
                    } else if (spell.id == "hyper_inflation") {
                        val list = _playerMonsters.value.toMutableList()
                        for (i in list.indices) {
                            val m = list[i]
                            if (m != null) {
                                list[i] = m.copy(currentDef = (m.currentDef - 800).coerceAtLeast(0))
                            }
                        }
                        _playerMonsters.value = list
                        _playerLp.value = (_playerLp.value - 800).coerceAtLeast(0)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "💸 $bName kích hoạt [Lạm Phát Phi Mã]! Cướp đi 800 LP và phá vỡ 800 phòng thủ toàn bộ quái vật bọc lót!"
                        delay(800)
                    } else if (spell.id == "monopoly_contract") {
                        _playerMonsters.value = listOf(null, null)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "📜 $bName kích hoạt [Hợp Đồng Độc Quyền]! Thôn tính sáp nhập và quét sạch TOÀN BỘ quái thú phe bạn!"
                        delay(800)
                    } else if (spell.id == "money_rain") {
                        _playerLp.value = (_playerLp.value - 1500).coerceAtLeast(0)
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "📦 $bName kích hoạt [Mưa Tiền Tệ Đè Bẹp]! Thả tấn tiền đè bẹp, gây sát thương cực khủng 1500 LP trực tiếp!"
                        delay(800)
                    } else if (spell.id == "ultimate_cash") {
                        _enemyLp.value = (_enemyLp.value + 1500).coerceAtMost(8000)
                        val list = _enemyMonsters.value.toMutableList()
                        for (i in list.indices) {
                            val m = list[i]
                            if (m != null) {
                                list[i] = m.copy(currentAtk = m.currentAtk + 1500)
                            }
                        }
                        _enemyMonsters.value = list
                        _enemyHand.value = _enemyHand.value - spell
                        _duelLogs.value = _duelLogs.value + "💵 $bName kích hoạt [Thế Lực Đồng Tiền]! Khôi phục 1500 LP và bộc phát +1500 Công lực cho tất cả quái vật!"
                        delay(800)
                    }
                }
            }

            // Summon
            var emptySlotIndex = _enemyMonsters.value.indexOf(null)
            val monstersInHand = _enemyHand.value.filter { it.isMonster }
            if (emptySlotIndex != -1 && monstersInHand.isNotEmpty()) {
                val monsterToSummon = monstersInHand.maxBy { it.atk }
                _enemyHand.value = _enemyHand.value - monsterToSummon
                val newList = _enemyMonsters.value.toMutableList()
                val isAtk = monsterToSummon.atk >= 1200
                newList[emptySlotIndex] = ActiveMonster(
                    card = monsterToSummon,
                    isAttackPosition = isAtk,
                    hasAttackedThisTurn = false
                )
                _enemyMonsters.value = newList
                _duelLogs.value = _duelLogs.value + "$bName triệu hồi [${monsterToSummon.name}] thế chế độ ${if (isAtk) "CÔNG" else "THỦ"}!"
                delay(1000)
            }

            // Attack
            val activeAiMonsters = _enemyMonsters.value.filterNotNull().filter { it.isAttackPosition }
            for (aiMonster in activeAiMonsters) {
                if (_enemyMonsters.value.contains(aiMonster)) { // check it still exists
                    val hasPlayerMonsters = _playerMonsters.value.any { it != null }
                    if (!hasPlayerMonsters) {
                        _playerLp.value = (_playerLp.value - aiMonster.currentAtk).coerceAtLeast(0)
                        _duelLogs.value = _duelLogs.value + "💥 [${aiMonster.card.name}] của $bName TẤN CÔNG TRỰC TIẾP! Bạn lĩnh đủ ${aiMonster.currentAtk} sát thương LP."
                        delay(1000)
                    } else {
                        val targets = _playerMonsters.value.filterNotNull()
                        val easyTarget = targets.firstOrNull { target ->
                            if (target.isAttackPosition) {
                                aiMonster.currentAtk > target.currentAtk
                            } else {
                                aiMonster.currentAtk > target.currentDef
                            }
                        } ?: targets.minBy { if (it.isAttackPosition) it.currentAtk else it.currentDef }
                        
                        val targetIdx = _playerMonsters.value.indexOf(easyTarget)
                        if (targetIdx != -1) {
                            _duelLogs.value = _duelLogs.value + "⚡ [${aiMonster.card.name}] của $bName tấn công [${easyTarget.card.name}]!"
                            delay(800)
                            
                            if (easyTarget.isAttackPosition) {
                                if (aiMonster.currentAtk > easyTarget.currentAtk) {
                                    val diff = aiMonster.currentAtk - easyTarget.currentAtk
                                    val newList = _playerMonsters.value.toMutableList()
                                    newList[targetIdx] = null
                                    _playerMonsters.value = newList
                                    _playerLp.value = (_playerLp.value - diff).coerceAtLeast(0)
                                    _duelLogs.value = _duelLogs.value + "💀 [${easyTarget.card.name}] của bạn bị tiêu diệt! Bạn tốn $diff LP."
                                } else if (aiMonster.currentAtk == easyTarget.currentAtk) {
                                    val newList = _playerMonsters.value.toMutableList()
                                    newList[targetIdx] = null
                                    _playerMonsters.value = newList
                                    
                                    val aiList = _enemyMonsters.value.toMutableList()
                                    val aiIdx = _enemyMonsters.value.indexOf(aiMonster)
                                    if (aiIdx != -1) aiList[aiIdx] = null
                                    _enemyMonsters.value = aiList
                                    
                                    _duelLogs.value = _duelLogs.value + "💥 Lưỡng bại câu thương! Cả hai quái thú cùng phát nổ."
                                } else {
                                    val diff = easyTarget.currentAtk - aiMonster.currentAtk
                                    val aiList = _enemyMonsters.value.toMutableList()
                                    val aiIdx = _enemyMonsters.value.indexOf(aiMonster)
                                    if (aiIdx != -1) aiList[aiIdx] = null
                                    _enemyMonsters.value = aiList
                                    _enemyLp.value = (_enemyLp.value - diff).coerceAtLeast(0)
                                    _duelLogs.value = _duelLogs.value + "🛡️ Bạn phản công! [${aiMonster.card.name}] bị nổ, $bName tốn $diff LP!"
                                }
                            } else {
                                if (aiMonster.currentAtk > easyTarget.currentDef) {
                                    val newList = _playerMonsters.value.toMutableList()
                                    newList[targetIdx] = null
                                    _playerMonsters.value = newList
                                    _duelLogs.value = _duelLogs.value + "🛡️ Lá chắn [${easyTarget.card.name}] của bạn bị phá vỡ hoàn toàn!"
                                } else if (aiMonster.currentAtk < easyTarget.currentDef) {
                                    val diff = easyTarget.currentDef - aiMonster.currentAtk
                                    _enemyLp.value = (_enemyLp.value - diff).coerceAtLeast(0)
                                    _duelLogs.value = _duelLogs.value + "🛡️ Giáp của bạn quá trơ! $bName tự dính phản chấn $diff LP."
                                }
                            }
                            delay(1000)
                        }
                    }
                }
                if (checkMatchEnded()) {
                    _isAiActing.value = false
                    return@launch
                }
            }

            val updatedEnemyList = _enemyMonsters.value.map { it?.copy(hasAttackedThisTurn = false) }
            _enemyMonsters.value = updatedEnemyList

            _duelTurn.value = "PLAYER"
            _duelLogs.value = _duelLogs.value + "--- Lượt của bạn bắt đầu ---"
            
            if (_playerHand.value.size < 5) {
                val drawn = getPlayerCardPool().random()
                _playerHand.value = _playerHand.value + drawn
                _duelLogs.value = _duelLogs.value + "🍀 Bạn rút được bài: [${drawn.name}]!"
            }
            
            _adBotDialogue.value = when {
                _playerLp.value < 1500 -> "Hahaha! Điểm sinh mệnh của ngươi sắp cạn kiệt. Hãy dâng nốt số LP quý giá còn lại!"
                _enemyLp.value < 1500 -> "Hừ... Chơi khá đấy. Nhưng quyền năng tối thượng của bổn trùm vẫn chưa hoàn toàn chấm dứt!"
                else -> "Ngươi hoài nghi thế giới ảo sao? Sức mạnh phó bản này sẽ cho ngươi sáng mắt!"
            }
            
            checkMatchEnded()
            _isAiActing.value = false
        }
    }

    private fun checkMatchEnded(): Boolean {
        if (_playerLp.value <= 0) {
            _duelStatus.value = "LOST"
            _duelLogs.value = _duelLogs.value + "💀 BẠN ĐÃ THẤT BẠI! AdBot chiến thắng trận đấu ma pháp!"
            _adBotDialogue.value = "Hahaha! Thật bần cùng làm sao. Muốn phục sinh ư? Hãy về trang nhất mà cày thêm hàng chục ads đi!"
            
            // Trigger the duel defeat milestone teaser hook
            triggerDuelFinished(false)
            return true
        }
        if (_enemyLp.value <= 0) {
            _duelStatus.value = "WON"
            _duelLogs.value = _duelLogs.value + "🏆 BẠN ĐÃ ĐẬP TAN ADBOT VÀ CHIẾN THẮNG TRẬN ĐẤU BÀI!"
            _totalRevenue.value += 1.50
            _adBotDialogue.value = "K-Không thể thế được! Thuật toán thẻ bài của ta đã cực kỳ tối tân... Bạn được thưởng hẳn $1.50 ảo đó!"
            
            // Trigger the duel victory milestone teaser hook
            triggerDuelFinished(true)
            return true
        }
        return false
    }

    fun dismissSecretDialog() {
        _showSecretDialog.value = false
    }

    fun startHeistGame() {
        _showSecretDialog.value = false
        _isHeistGameVisible.value = true
        _heistSecurityAlert.value = 10
        _heistStolenValue.value = 0.0
        _heistLootBag.value = emptyList()
        _heistStatus.value = "PLAYING"
        _heistLog.value = listOf(
            "🕶️ Bạn đã đột nhập thành công vào Tiệm Vàng Đá Quý qua đường ống thông gió!",
            "⚠️ Hệ thống báo động đang kích hoạt nhẹ ở mức 10%. Hãy hành động lẹ làng trước khi cảnh sát ập tới!"
        )
        _adBotDialogue.value = "Ôi trời! Bạn mò thấy lỗ hổng sâu xa trong quảng cáo trang sức để đột nhập tiệm vàng thật kìa! Chơi Mini game giữ bí mật nhé!"
    }

    fun closeHeistGame() {
        _isHeistGameVisible.value = false
    }

    fun startChessGame() {
        _isChessGameVisible.value = true
        resetChessBoard()
    }

    fun closeChessGame() {
        _isChessGameVisible.value = false
    }

    fun setChessBossElo(elo: Int) {
        _chessBossElo.value = elo
        saveProgress()
        dispatchBanter("change_elo", t(
            "Bạn vừa đổi Elo của tôi sang $elo ư? Coi chừng tôi bẻ gãy bàn cờ đấy nhé!",
            "Did you just change my Elo to $elo? Watch out, or I might break the chessboard!"
        ))
    }

    fun resetChessBoard() {
        val initial = mutableMapOf<Int, ChessPiece>()
        
        // Rows 0 and 1 are Black (AI Boss)
        initial[0] = ChessPiece(ChessPieceType.ROOK, ChessColor.BLACK)
        initial[1] = ChessPiece(ChessPieceType.KNIGHT, ChessColor.BLACK)
        initial[2] = ChessPiece(ChessPieceType.BISHOP, ChessColor.BLACK)
        initial[3] = ChessPiece(ChessPieceType.QUEEN, ChessColor.BLACK)
        initial[4] = ChessPiece(ChessPieceType.KING, ChessColor.BLACK)
        initial[5] = ChessPiece(ChessPieceType.BISHOP, ChessColor.BLACK)
        initial[6] = ChessPiece(ChessPieceType.KNIGHT, ChessColor.BLACK)
        initial[7] = ChessPiece(ChessPieceType.ROOK, ChessColor.BLACK)
        for (col in 0..7) {
            initial[8 + col] = ChessPiece(ChessPieceType.PAWN, ChessColor.BLACK)
        }

        // Rows 6 and 7 are White (Player)
        for (col in 0..7) {
            initial[48 + col] = ChessPiece(ChessPieceType.PAWN, ChessColor.WHITE)
        }
        initial[56] = ChessPiece(ChessPieceType.ROOK, ChessColor.WHITE)
        initial[57] = ChessPiece(ChessPieceType.KNIGHT, ChessColor.WHITE)
        initial[58] = ChessPiece(ChessPieceType.BISHOP, ChessColor.WHITE)
        initial[59] = ChessPiece(ChessPieceType.QUEEN, ChessColor.WHITE)
        initial[60] = ChessPiece(ChessPieceType.KING, ChessColor.WHITE)
        initial[61] = ChessPiece(ChessPieceType.BISHOP, ChessColor.WHITE)
        initial[62] = ChessPiece(ChessPieceType.KNIGHT, ChessColor.WHITE)
        initial[63] = ChessPiece(ChessPieceType.ROOK, ChessColor.WHITE)

        _chessBoard.value = initial
        _chessSelectedSquare.value = null
        _chessTurn.value = ChessColor.WHITE
        _chessGameStatus.value = "PLAYING"
        _movedSquares.value = emptySet()
        _chessLogs.value = listOf(
            Pair(
                "Trận đấu bắt đầu! Chúc bạn may mắn chống lại Ads Boss Elo ${_chessBossElo.value}!",
                "The match begins! Good luck playing against Ads Boss Elo ${_chessBossElo.value}!"
            )
        )
    }

    fun isSquareThreatened(square: Int, byColor: ChessColor, board: Map<Int, ChessPiece>): Boolean {
        for ((pos, piece) in board) {
            if (piece.color == byColor) {
                val legalMoves = getLegalMoves(pos, board, ignoreCastling = true)
                if (square in legalMoves) {
                    return true
                }
            }
        }
        return false
    }

    fun getLegalMoves(pos: Int, tempBoard: Map<Int, ChessPiece> = _chessBoard.value, ignoreCastling: Boolean = false): List<Int> {
        val piece = tempBoard[pos] ?: return emptyList()
        val row = pos / 8
        val col = pos % 8
        val list = mutableListOf<Int>()

        when (piece.type) {
            ChessPieceType.PAWN -> {
                if (piece.color == ChessColor.WHITE) {
                    val nextPos = (row - 1) * 8 + col
                    if (row - 1 >= 0 && !tempBoard.contains(nextPos)) {
                        list.add(nextPos)
                        val startPos = (row - 2) * 8 + col
                        if (row == 6 && !tempBoard.contains(startPos)) {
                            list.add(startPos)
                        }
                    }
                    for (cOffset in listOf(-1, 1)) {
                        val tarCol = col + cOffset
                        if (tarCol in 0..7 && row - 1 >= 0) {
                            val capPos = (row - 1) * 8 + tarCol
                            val tarPiece = tempBoard[capPos]
                            if (tarPiece != null && tarPiece.color == ChessColor.BLACK) {
                                list.add(capPos)
                            }
                        }
                    }
                } else {
                    val nextPos = (row + 1) * 8 + col
                    if (row + 1 <= 7 && !tempBoard.contains(nextPos)) {
                        list.add(nextPos)
                        val startPos = (row + 2) * 8 + col
                        if (row == 1 && !tempBoard.contains(startPos)) {
                            list.add(startPos)
                        }
                    }
                    for (cOffset in listOf(-1, 1)) {
                        val tarCol = col + cOffset
                        if (tarCol in 0..7 && row + 1 <= 7) {
                            val capPos = (row + 1) * 8 + tarCol
                            val tarPiece = tempBoard[capPos]
                            if (tarPiece != null && tarPiece.color == ChessColor.WHITE) {
                                list.add(capPos)
                            }
                        }
                    }
                }
            }
            ChessPieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1),
                    Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2),
                    Pair(2, -1), Pair(2, 1)
                )
                for ((rOff, cOff) in offsets) {
                    val tarRow = row + rOff
                    val tarCol = col + cOff
                    if (tarRow in 0..7 && tarCol in 0..7) {
                        val tarPos = tarRow * 8 + tarCol
                        val tarPiece = tempBoard[tarPos]
                        if (tarPiece == null || tarPiece.color != piece.color) {
                            list.add(tarPos)
                        }
                    }
                }
            }
            ChessPieceType.BISHOP -> {
                val dirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
                for ((dr, dc) in dirs) {
                    var r = row + dr
                    var c = col + dc
                    while (r in 0..7 && c in 0..7) {
                        val tarPos = r * 8 + c
                        val tarPiece = tempBoard[tarPos]
                        if (tarPiece == null) {
                            list.add(tarPos)
                        } else {
                            if (tarPiece.color != piece.color) {
                                list.add(tarPos)
                            }
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.ROOK -> {
                val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                for ((dr, dc) in dirs) {
                    var r = row + dr
                    var c = col + dc
                    while (r in 0..7 && c in 0..7) {
                        val tarPos = r * 8 + c
                        val tarPiece = tempBoard[tarPos]
                        if (tarPiece == null) {
                            list.add(tarPos)
                        } else {
                            if (tarPiece.color != piece.color) {
                                list.add(tarPos)
                            }
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.QUEEN -> {
                val dirs = listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                )
                for ((dr, dc) in dirs) {
                    var r = row + dr
                    var c = col + dc
                    while (r in 0..7 && c in 0..7) {
                        val tarPos = r * 8 + c
                        val tarPiece = tempBoard[tarPos]
                        if (tarPiece == null) {
                            list.add(tarPos)
                        } else {
                            if (tarPiece.color != piece.color) {
                                list.add(tarPos)
                            }
                            break
                        }
                        r += dr
                        c += dc
                    }
                }
            }
            ChessPieceType.KING -> {
                val dirs = listOf(
                    Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
                    Pair(0, -1), Pair(0, 1),
                    Pair(1, -1), Pair(1, 0), Pair(1, 1)
                )
                for ((dr, dc) in dirs) {
                    val r = row + dr
                    val c = col + dc
                    if (r in 0..7 && c in 0..7) {
                        val tarPos = r * 8 + c
                        val tarPiece = tempBoard[tarPos]
                        if (tarPiece == null || tarPiece.color != piece.color) {
                            list.add(tarPos)
                        }
                    }
                }

                // Castling Logic
                if (!ignoreCastling) {
                    val isWhite = piece.color == ChessColor.WHITE
                    val kingStart = if (isWhite) 60 else 4
                    val leftRookStart = if (isWhite) 56 else 0
                    val rightRookStart = if (isWhite) 63 else 7
                    val opponentColor = if (isWhite) ChessColor.BLACK else ChessColor.WHITE

                    if (pos == kingStart && !_movedSquares.value.contains(kingStart)) {
                        // Kingside castling
                        val f1 = kingStart + 1
                        val g1 = kingStart + 2
                        if (!tempBoard.contains(f1) && !tempBoard.contains(g1) && !_movedSquares.value.contains(rightRookStart)) {
                            if (!isSquareThreatened(kingStart, opponentColor, tempBoard) &&
                                !isSquareThreatened(f1, opponentColor, tempBoard) &&
                                !isSquareThreatened(g1, opponentColor, tempBoard)) {
                                list.add(g1)
                            }
                        }

                        // Queenside castling
                        val d1 = kingStart - 1
                        val c1 = kingStart - 2
                        val b1 = kingStart - 3
                        if (!tempBoard.contains(d1) && !tempBoard.contains(c1) && !tempBoard.contains(b1) && !_movedSquares.value.contains(leftRookStart)) {
                            if (!isSquareThreatened(kingStart, opponentColor, tempBoard) &&
                                !isSquareThreatened(d1, opponentColor, tempBoard) &&
                                !isSquareThreatened(c1, opponentColor, tempBoard)) {
                                list.add(c1)
                            }
                        }
                    }
                }
            }
        }
        return list
    }

    fun onChessSquareClick(pos: Int) {
        if (_chessGameStatus.value != "PLAYING") return
        if (_chessTurn.value != ChessColor.WHITE) return

        val currentBoard = _chessBoard.value
        val piece = currentBoard[pos]
        val selected = _chessSelectedSquare.value

        if (selected == null) {
            if (piece != null && piece.color == ChessColor.WHITE) {
                _chessSelectedSquare.value = pos
            }
        } else {
            val legalMoves = getLegalMoves(selected, currentBoard)
            if (pos in legalMoves) {
                val movingPiece = currentBoard[selected]!!
                val updatedBoard = currentBoard.toMutableMap()
                val capturedPiece = updatedBoard[pos]

                updatedBoard.remove(selected)
                _movedSquares.value = _movedSquares.value + selected

                // Castling Execution
                var isCastled = false
                var castlingType = ""
                if (movingPiece.type == ChessPieceType.KING && Math.abs(selected - pos) == 2) {
                    isCastled = true
                    if (pos == 62) { // White Kingside
                        updatedBoard.remove(63)
                        updatedBoard[61] = ChessPiece(ChessPieceType.ROOK, ChessColor.WHITE)
                        _movedSquares.value = _movedSquares.value + 63
                        castlingType = "KINGSIDE"
                    } else if (pos == 58) { // White Queenside
                        updatedBoard.remove(56)
                        updatedBoard[59] = ChessPiece(ChessPieceType.ROOK, ChessColor.WHITE)
                        _movedSquares.value = _movedSquares.value + 56
                        castlingType = "QUEENSIDE"
                    }
                }

                // Pawn Promotion (to Queen)
                var isPromoted = false
                val finalPiece = if (movingPiece.type == ChessPieceType.PAWN && pos / 8 == 0) {
                    isPromoted = true
                    ChessPiece(ChessPieceType.QUEEN, ChessColor.WHITE)
                } else {
                    movingPiece
                }
                updatedBoard[pos] = finalPiece
                
                _chessBoard.value = updatedBoard
                _chessSelectedSquare.value = null

                var logVi = ""
                var logEn = ""
                if (isCastled) {
                    if (castlingType == "KINGSIDE") {
                        logVi = "Bạn thực hiện nhập thành cánh Vua (O-O)."
                        logEn = "You performed Kingside castling (O-O)."
                    } else {
                        logVi = "Bạn thực hiện nhập thành cánh Hậu (O-O-O)."
                        logEn = "You performed Queenside castling (O-O-O)."
                    }
                } else {
                    val pieceNameVi = when (movingPiece.type) {
                        ChessPieceType.PAWN -> "Tốt"
                        ChessPieceType.KNIGHT -> "Mã"
                        ChessPieceType.BISHOP -> "Tượng"
                        ChessPieceType.ROOK -> "Xe"
                        ChessPieceType.QUEEN -> "Hậu"
                        ChessPieceType.KING -> "Vua"
                    }
                    val pieceNameEn = when (movingPiece.type) {
                        ChessPieceType.PAWN -> "Pawn"
                        ChessPieceType.KNIGHT -> "Knight"
                        ChessPieceType.BISHOP -> "Bishop"
                        ChessPieceType.ROOK -> "Rook"
                        ChessPieceType.QUEEN -> "Queen"
                        ChessPieceType.KING -> "King"
                    }
                    val fromNotation = "${'a' + (selected % 8)}${8 - (selected / 8)}"
                    val toNotation = "${'a' + (pos % 8)}${8 - (pos / 8)}"
                    logVi = "Bạn di chuyển $pieceNameVi từ $fromNotation sang $toNotation."
                    logEn = "You moved $pieceNameEn from $fromNotation to $toNotation."
                    if (capturedPiece != null) {
                        val capNameVi = when(capturedPiece.type){
                            ChessPieceType.PAWN -> "Tốt"
                            ChessPieceType.KNIGHT -> "Mã"
                            ChessPieceType.BISHOP -> "Tượng"
                            ChessPieceType.ROOK -> "Xe"
                            ChessPieceType.QUEEN -> "Hậu"
                            ChessPieceType.KING -> "Vua"
                        }
                        val capNameEn = when(capturedPiece.type){
                            ChessPieceType.PAWN -> "Pawn"
                            ChessPieceType.KNIGHT -> "Knight"
                            ChessPieceType.BISHOP -> "Bishop"
                            ChessPieceType.ROOK -> "Rook"
                            ChessPieceType.QUEEN -> "Queen"
                            ChessPieceType.KING -> "King"
                        }
                        logVi += " Đã bắt quân $capNameVi!"
                        logEn += " Captured $capNameEn!"
                    }
                    if (isPromoted) {
                        logVi += " Tốt đã phong Hậu! 👑"
                        logEn += " Pawn promoted to Queen! 👑"
                    }
                }

                _chessLogs.value = _chessLogs.value + Pair(logVi, logEn)

                if (capturedPiece != null && capturedPiece.type == ChessPieceType.KING) {
                    _chessGameStatus.value = "WHITE_WON"
                    _hasDefeatedChessBoss.value = true
                    recordChessScore(_chessBossElo.value)
                    saveProgress()
                    _chessLogs.value = _chessLogs.value + Pair(
                        "Chúc mừng bạn! Bạn đã diệt sạch Vua của Ads Boss và THẮNG lợi vẻ vang!",
                        "Congratulations! You completely defeated Ads Boss's King and won a glorious victory!"
                    )
                    dispatchBanter("chess_won", t(
                        "Không thể tin nổi! Bạn vừa lật đổ tượng đài Elo ${_chessBossElo.value} của ta bằng cờ vua à? Cơ chế chỉnh Elo mây đã mở khóa!",
                        "Unbelievable! You just overthrew my Elo ${_chessBossElo.value} status with chess? Cloud Elo adjustment dashboard unlocked!"
                    ))
                    return
                }

                if (isBlackKingInCheck(updatedBoard)) {
                    triggerChessCheckAd(updatedBoard)
                } else {
                    _chessTurn.value = ChessColor.BLACK
                    viewModelScope.launch {
                        delay(850)
                        makeChessAiMove()
                    }
                }
            } else {
                if (piece != null && piece.color == ChessColor.WHITE) {
                    _chessSelectedSquare.value = pos
                } else {
                    _chessSelectedSquare.value = null
                }
            }
        }
    }

    fun makeChessAiMove() {
        val currentBoard = _chessBoard.value
        val allAiMoves = mutableListOf<ChessMove>()
        for ((pos, piece) in currentBoard) {
            if (piece.color == ChessColor.BLACK) {
                val legalDestinations = getLegalMoves(pos, currentBoard)
                for (dest in legalDestinations) {
                    var weight = Random.nextInt(1, 10)
                    
                    val targetPiece = currentBoard[dest]
                    if (targetPiece != null && targetPiece.color == ChessColor.WHITE) {
                        weight += when (targetPiece.type) {
                            ChessPieceType.KING -> 10000
                            ChessPieceType.QUEEN -> 900
                            ChessPieceType.ROOK -> 500
                            ChessPieceType.BISHOP -> 300
                            ChessPieceType.KNIGHT -> 300
                            ChessPieceType.PAWN -> 100
                        }
                    }
                    
                    // Pawn promotion weight bonus for AI
                    if (piece.type == ChessPieceType.PAWN && dest / 8 == 7) {
                        weight += 500
                    }
                    
                    val r = dest / 8
                    val c = dest % 8
                    if (r in 3..4 && c in 3..4) {
                        weight += 5
                    }
                    
                    val elo = _chessBossElo.value
                    if (elo >= 2000) {
                        val whiteKingPos = currentBoard.entries.find { it.value.type == ChessPieceType.KING && it.value.color == ChessColor.WHITE }?.key
                        if (whiteKingPos != null) {
                            val wkR = whiteKingPos / 8
                            val wkC = whiteKingPos % 8
                            val dist = Math.abs(r - wkR) + Math.abs(c - wkC)
                            weight += (14 - dist) * 2
                        }
                    }
                    
                    allAiMoves.add(ChessMove(pos, dest, piece, weight))
                }
            }
        }
        
        if (allAiMoves.isEmpty()) {
            _chessGameStatus.value = "WHITE_WON"
            _hasDefeatedChessBoss.value = true
            recordChessScore(_chessBossElo.value)
            saveProgress()
            _chessLogs.value = _chessLogs.value + Pair(
                "Ads Boss không còn nước đi hợp lệ. Bạn thắng!",
                "Ads Boss has no legal moves. You win!"
            )
            return
        }
        
        val elo = _chessBossElo.value
        val chosenMove = when {
            elo < 800 -> {
                if (Random.nextFloat() < 0.60f) {
                    allAiMoves.random()
                } else {
                    allAiMoves.maxByOrNull { it.weight }!!
                }
            }
            elo < 1500 -> {
                if (Random.nextFloat() < 0.15f) {
                    allAiMoves.random()
                } else {
                    allAiMoves.maxByOrNull { it.weight }!!
                }
            }
            else -> {
                allAiMoves.maxByOrNull { it.weight }!!
            }
        }
        
        val fromPos = chosenMove.from
        val toPos = chosenMove.to
        val p = chosenMove.piece
        
        val updatedBoard = currentBoard.toMutableMap()
        val capturedPiece = updatedBoard[toPos]
        
        updatedBoard.remove(fromPos)
        _movedSquares.value = _movedSquares.value + fromPos

        // Castling Execution for AI (Black King starts at 4)
        var isCastled = false
        var castlingType = ""
        if (p.type == ChessPieceType.KING && Math.abs(fromPos - toPos) == 2) {
            isCastled = true
            if (toPos == 6) { // Black Kingside
                updatedBoard.remove(7)
                updatedBoard[5] = ChessPiece(ChessPieceType.ROOK, ChessColor.BLACK)
                _movedSquares.value = _movedSquares.value + 7
                castlingType = "KINGSIDE"
            } else if (toPos == 2) { // Black Queenside
                updatedBoard.remove(0)
                updatedBoard[3] = ChessPiece(ChessPieceType.ROOK, ChessColor.BLACK)
                _movedSquares.value = _movedSquares.value + 0
                castlingType = "QUEENSIDE"
            }
        }

        // Pawn Promotion for AI
        var isPromoted = false
        val finalPiece = if (p.type == ChessPieceType.PAWN && toPos / 8 == 7) {
            isPromoted = true
            ChessPiece(ChessPieceType.QUEEN, ChessColor.BLACK)
        } else {
            p
        }
        updatedBoard[toPos] = finalPiece
        
        _chessBoard.value = updatedBoard
        
        var logVi = ""
        var logEn = ""
        if (isCastled) {
            if (castlingType == "KINGSIDE") {
                logVi = "Ads Boss thực hiện nhập thành cánh Vua (O-O)."
                logEn = "Ads Boss performed Kingside castling (O-O)."
            } else {
                logVi = "Ads Boss thực hiện nhập thành cánh Hậu (O-O-O)."
                logEn = "Ads Boss performed Queenside castling (O-O-O)."
            }
        } else {
            val pieceNameVi = when (p.type) {
                ChessPieceType.PAWN -> "Tốt"
                ChessPieceType.KNIGHT -> "Mã"
                ChessPieceType.BISHOP -> "Tượng"
                ChessPieceType.ROOK -> "Xe"
                ChessPieceType.QUEEN -> "Hậu"
                ChessPieceType.KING -> "Vua"
            }
            val pieceNameEn = when (p.type) {
                ChessPieceType.PAWN -> "Pawn"
                ChessPieceType.KNIGHT -> "Knight"
                ChessPieceType.BISHOP -> "Bishop"
                ChessPieceType.ROOK -> "Rook"
                ChessPieceType.QUEEN -> "Queen"
                ChessPieceType.KING -> "King"
            }
            val fromNotation = "${'a' + (fromPos % 8)}${8 - (fromPos / 8)}"
            val toNotation = "${'a' + (toPos % 8)}${8 - (toPos / 8)}"
            logVi = "Ads Boss di chuyển $pieceNameVi từ $fromNotation sang $toNotation."
            logEn = "Ads Boss moved $pieceNameEn from $fromNotation to $toNotation."
            if (capturedPiece != null) {
                val capNameVi = when(capturedPiece.type){
                    ChessPieceType.PAWN -> "Tốt"
                    ChessPieceType.KNIGHT -> "Mã"
                    ChessPieceType.BISHOP -> "Tượng"
                    ChessPieceType.ROOK -> "Xe"
                    ChessPieceType.QUEEN -> "Hậu"
                    ChessPieceType.KING -> "Vua"
                }
                val capNameEn = when(capturedPiece.type){
                    ChessPieceType.PAWN -> "Pawn"
                    ChessPieceType.KNIGHT -> "Knight"
                    ChessPieceType.BISHOP -> "Bishop"
                    ChessPieceType.ROOK -> "Rook"
                    ChessPieceType.QUEEN -> "Queen"
                    ChessPieceType.KING -> "King"
                }
                logVi += " Đã tiêu diệt quân $capNameVi!"
                logEn += " Destroyed $capNameEn!"
            }
            if (isPromoted) {
                logVi += " Tốt đã phong Hậu! 👑"
                logEn += " Pawn promoted to Queen! 👑"
            }
        }
        
        _chessLogs.value = _chessLogs.value + Pair(logVi, logEn)
        
        if (capturedPiece != null && capturedPiece.type == ChessPieceType.KING) {
            _chessGameStatus.value = "BLACK_WON"
            _chessLogs.value = _chessLogs.value + Pair(
                "Trời đất ơi! Vua của bạn đã bị Ads Boss tiêu diệt! Bạn đã THUA trận đấu cờ vua này!",
                "Oh no! Your King was destroyed by Ads Boss! You LOST this chess match!"
            )
            dispatchBanter("chess_lost", t(
                "Hahaha! Elo ${_chessBossElo.value} mà cũng thua dưới trướng của ta à? Xem ad thêm đi bạn ơi!",
                "Hahaha! Losing to me even with my Elo ${_chessBossElo.value}? Go watch some more ads, my friend!"
            ))
            return
        }
        
        _chessTurn.value = ChessColor.WHITE
    }

    fun isBlackKingInCheck(tempBoard: Map<Int, ChessPiece>): Boolean {
        val kingPos = tempBoard.entries.find { it.value.type == ChessPieceType.KING && it.value.color == ChessColor.BLACK }?.key ?: return false
        for ((pos, piece) in tempBoard) {
            if (piece.color == ChessColor.WHITE) {
                val targets = getLegalMoves(pos, tempBoard)
                if (kingPos in targets) {
                    return true
                }
            }
        }
        return false
    }

    fun findSafeSquareForBlackKing(board: Map<Int, ChessPiece>, kingPos: Int): Int {
        val threatenedSquares = mutableSetOf<Int>()
        for ((pos, piece) in board) {
            if (piece.color == ChessColor.WHITE) {
                threatenedSquares.addAll(getLegalMoves(pos, board))
            }
        }

        val row = kingPos / 8
        val col = kingPos % 8
        val neighbors = mutableListOf<Int>()
        for (r in maxOf(0, row - 1)..minOf(7, row + 1)) {
            for (c in maxOf(0, col - 1)..minOf(7, col + 1)) {
                val nPos = r * 8 + c
                if (nPos != kingPos) {
                    neighbors.add(nPos)
                }
            }
        }

        val localSafeNeighbors = neighbors.filter { nPos ->
            val p = board[nPos]
            (p == null || p.color == ChessColor.WHITE) && !threatenedSquares.contains(nPos)
        }

        if (localSafeNeighbors.isNotEmpty()) {
            return localSafeNeighbors.random()
        }

        val homeTerritorySafeSquares = mutableListOf<Int>()
        for (r in 0..2) {
            for (c in 0..7) {
                val pos = r * 8 + c
                if (pos != kingPos && !board.contains(pos) && !threatenedSquares.contains(pos)) {
                    homeTerritorySafeSquares.add(pos)
                }
            }
        }
        if (homeTerritorySafeSquares.isNotEmpty()) {
            return homeTerritorySafeSquares.random()
        }

        for (pos in 0..63) {
            if (pos != kingPos && !board.contains(pos)) {
                return pos
            }
        }
        return kingPos
    }

    fun speedUpChessAd() {
        val current = _chessAdTimeLeft.value
        if (current > 0) {
            val newVal = maxOf(0, current - 5)
            _chessAdTimeLeft.value = newVal
            _chessAdBanter.value = _chessAdBanterList.random()
            if (newVal == 0) {
                _isChessAdActive.value = false
                _chessLogs.value = _chessLogs.value + Pair(
                    "🎉 Bạn đã đập tan quảng cáo thành công sớm! Tiếp tục cuộc chơi.",
                    "🎉 You successfully smashed the ad early! The game continues."
                )
                _chessTurn.value = ChessColor.BLACK
                viewModelScope.launch {
                    delay(800)
                    makeChessAiMove()
                }
            } else {
                _chessLogs.value = _chessLogs.value + Pair(
                    "Bạn nhấp chuột điên cuồng giúp tua nhanh quảng cáo! Còn lại: $newVal giây.",
                    "You clicked frantically to speed up the ad! Remaining: $newVal seconds."
                )
            }
        }
    }

    fun triggerChessCheckAd(tempBoard: Map<Int, ChessPiece>) {
        _isChessAdActive.value = true
        _chessAdTimeLeft.value = 60
        _chessAdBanter.value = Pair(
            "Hahaha! Chiếu tướng á? Mơ đi cưng! Xem ngay SIÊU QUẢNG CÁO 60s gánh còng lưng nhé!",
            "Hahaha! Check? In your dreams! Watch this 60s SUPER AD to proceed!"
        )
        
        val kingPos = tempBoard.entries.find { it.value.type == ChessPieceType.KING && it.value.color == ChessColor.BLACK }?.key
        if (kingPos != null) {
            val boardCopy = tempBoard.toMutableMap()
            val safeSquare = findSafeSquareForBlackKing(boardCopy, kingPos)
            
            val kingPiece = boardCopy.remove(kingPos)!!
            boardCopy[safeSquare] = kingPiece
            _chessBoard.value = boardCopy
            
            val fromNotation = "${'a' + (kingPos % 8)}${8 - (kingPos / 8)}"
            val toNotation = "${'a' + (safeSquare % 8)}${8 - (safeSquare / 8)}"
            
            _chessLogs.value = _chessLogs.value + Pair(
                "⚠️ CHIẾU TƯỚNG! Ads Boss lân la đè quảng cáo ăn gian, dời King từ $fromNotation sang $toNotation!",
                "⚠️ CHECK! Ads Boss puts up an ad to cheat, moving King from $fromNotation to $toNotation!"
            )
            dispatchBanter("chess_cheat", t(
                "Úi xùi bị chiếu cơ à? Trêu bạn tí ta dời Vua sang $toNotation rồi nhé haha!",
                "Oh my, got checked? Just teasing you, I relocated my King to $toNotation haha!"
            ))
        }

        viewModelScope.launch {
            while (_chessAdTimeLeft.value > 0 && _isChessAdActive.value) {
                delay(1000)
                if (_chessAdTimeLeft.value > 0) {
                    _chessAdTimeLeft.value -= 1
                }
            }
            if (_isChessAdActive.value) {
                _isChessAdActive.value = false
                _chessLogs.value = _chessLogs.value + Pair(
                    "Quảng cáo đã tự động chạy hết 60 giây. Lượt của Ads Boss tiếp tục.",
                    "The ad finished playing automatically for 60 seconds. Ads Boss's turn continues."
                )
                _chessTurn.value = ChessColor.BLACK
                delay(800)
                makeChessAiMove()
            }
        }
    }

    fun heistQuietLoot() {
        if (_heistStatus.value != "PLAYING") return
        val alertAdded = Random.nextInt(8, 16)
        val newValue = Random.nextDouble(12.50, 48.00)
        
        val lootItems = listOf(
            "Nhẫn Kim Cương Saphire 💍", 
            "Lắc Vàng Ròng 18K ✨", 
            "Khuyên Tai Ngọc Trai Hoàng Gia 🦪", 
            "Chuỗi Dây Chuyền Ruby Ngọc Bích 📿"
        )
        val itemFound = lootItems.random()
        
        _heistSecurityAlert.value = (_heistSecurityAlert.value + alertAdded).coerceAtMost(100)
        _heistStolenValue.value += newValue
        _heistLootBag.value = _heistLootBag.value + itemFound
        
        val currentAlert = _heistSecurityAlert.value
        val logMsg = "⛏️ Bạn mở tủ cạy ngọc nhẹ nhàng lấy được: $itemFound (Trị giá $${String.format("%.2f", newValue)}). Bảo mật tăng thêm +$alertAdded% (Hiện tại: $currentAlert%)"
        _heistLog.value = _heistLog.value + logMsg

        if (currentAlert >= 100) {
            handleHeistCaught()
        }
    }

    fun heistRobDiamond() {
        if (_heistStatus.value != "PLAYING") return
        val alertAdded = Random.nextInt(20, 38)
        val newValue = Random.nextDouble(95.00, 240.00)
        
        val majorItems = listOf(
            "Kim Cương Đen Thái Dương Cực Đại 💎", 
            "Mũ Miện Hoàng Gia Đính Bảo Ngọc 👑", 
            "Tượng Rồng Ngọc Lục Bảo Đế Vương 🐉", 
            "Hộp Nhạc Thạch Anh Đính Kim Cương Hồng 🔮"
        )
        val itemFound = majorItems.random()
        
        _heistSecurityAlert.value = (_heistSecurityAlert.value + alertAdded).coerceAtMost(100)
        _heistStolenValue.value += newValue
        _heistLootBag.value = _heistLootBag.value + itemFound
        
        val currentAlert = _heistSecurityAlert.value
        val logMsg = "🚨 Liều ăn nhiều! Bạn đập kính cuỗm ngay: $itemFound (Trị giá $${String.format("%.2f", newValue)}). Còi báo động rú vang +$alertAdded% (Hiện tại: $currentAlert%)"
        _heistLog.value = _heistLog.value + logMsg

        if (currentAlert >= 100) {
            handleHeistCaught()
        }
    }

    fun heistHackCamera() {
        if (_heistStatus.value != "PLAYING") return
        val isSuccessful = Random.nextInt(100) < 65
        if (isSuccessful) {
            val alertDecreased = Random.nextInt(15, 30)
            _heistSecurityAlert.value = (_heistSecurityAlert.value - alertDecreased).coerceAtLeast(0)
            val currentAlert = _heistSecurityAlert.value
            _heistLog.value = _heistLog.value + "🖥️ Bạn gõ code hack tắt camera thành công! Báo động hạ nhiệt -$alertDecreased% (Hiện tại: $currentAlert%)"
        } else {
            val alertAdded = Random.nextInt(15, 25)
            _heistSecurityAlert.value = (_heistSecurityAlert.value + alertAdded).coerceAtMost(100)
            val currentAlert = _heistSecurityAlert.value
            _heistLog.value = _heistLog.value + "💥 Hack thất bại! Chập mạch hệ thống làm máy báo động nhấp nháy liên hoàn +$alertAdded% (Hiện tại: $currentAlert%)"
            
            if (currentAlert >= 100) {
                handleHeistCaught()
            }
        }
    }

    fun heistEscape() {
        if (_heistStatus.value != "PLAYING") return
        if (_heistLootBag.value.isEmpty()) {
            _heistLog.value = _heistLog.value + "⚠️ Đột nhập nãy giờ túi có gì đâu mà tẩu thoát? Hãy cuỗm ít trang sức đã!"
            return
        }

        _heistStatus.value = "ESCAPED"
        val totalStolen = _heistStolenValue.value
        _totalRevenue.value += totalStolen

        val hasDiamond = _heistLootBag.value.any { it.contains("Kim Cương") }
        if (hasDiamond) {
            stoleDiamondSuccessfully.value = true
            earnBadgeDirectly("stole_diamond", "Trộm thành công: ${_heistLootBag.value.find { it.contains("Kim Cương") } ?: "Kim Cương"}")
        }

        SoundManager.playReward(soundVolume.value)
        recordHeistScore(totalStolen)
        saveProgress()

        // Trigger the heist super crook milestone teaser hook!
        triggerHeistRich(totalStolen)

        val summaryMsg = "🎉 THÀNH CÔNG RỰC RỠ! Bạn thoát ra bằng xe moto đặc chủng, thanh lý chợ đen kiếm được $${String.format("%.2f", totalStolen)} ảo trực tiếp đổ vào túi doanh thu!"
        _heistLog.value = _heistLog.value + summaryMsg
        _adBotDialogue.value = "Không thể tin nổi! Bạn đã trộm thành công và xách túi của cải trị giá $${String.format("%.2f", totalStolen)} chạy bay màu. Tôi bái bạn làm sư phụ!"

        viewModelScope.launch {
            try {
                repository.insertMetric(
                    SimulationMetric(
                        impressions = 0,
                        clicks = 1,
                        ctr = 100.0,
                        cpm = totalStolen * 1000.0,
                        revenue = totalStolen,
                        description = "Đại phi vụ trộm Tiệm Ngọc: Cuỗm ${_heistLootBag.value.size} món báu vật!"
                    )
                )
                repository.insertChatMessage(
                    ChatMessage(
                        sender = "adbot",
                        message = "🚨 CHẤN ĐỘNG: Siêu trộm đột kích tiệm kim hoàn, tẩu thoát ngoạn mục mang đi $${String.format("%.2f", totalStolen)} doanh nghiệp quảng cáo khóc thét!",
                        isTease = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleHeistCaught() {
        _heistStatus.value = "CAUGHT"
        _heistStolenValue.value = 0.0
        val summaryMsg = "🚔 CẢNH SÁT ẬP VÀO! Bạn bị khóa tay tống lên xe chuyên dụng. Toàn bộ đồ trang sức đắt tiền cuỗm được đã bị sung công quỹ. Phi vụ trắng tay!"
        _heistLog.value = _heistLog.value + summaryMsg
        _adBotDialogue.value = "Ha ha ha! Quẻ bói bảo bạn sẽ bị bắt quả tang quả không sai! Giam vào trại 5 giây cho chừa thói táy máy nhé!"
    }

    fun setTrafficLevel(level: String) {
        _trafficLevel.value = level
    }

    fun setNicheCategory(category: String) {
        _nicheCategory.value = category
    }

    fun setClickRateSetting(rate: Float) {
        _clickRateSetting.value = rate
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    // --- Dynamic Sarcastic Responses Offline Generator ---
    fun shouldBanter(): Boolean {
        val freq = _banterFrequency.value
        if (freq == BanterFrequency.MUTED) return false
        return Random.nextFloat() <= freq.chance
    }

    private fun getNeutralStateString(trigger: String): String {
        val isEn = _appLanguage.value == "en"
        return when (trigger.lowercase(Locale.ROOT)) {
            "banner_click" -> if (isEn) "Ad banner clicked." else "Đã nhấp banner quảng cáo."
            "interstitial_start" -> if (isEn) "Displaying Interstitial ad." else "Đang hiển thị quảng cáo Interstitial."
            "interstitial_close" -> if (isEn) "Closed Interstitial ad." else "Đã đóng quảng cáo Interstitial."
            "rewarded_start" -> if (isEn) "Showing Rewarded Video ad." else "Đang trình chiếu quảng cáo Rewarded Video."
            "rewarded_complete" -> if (isEn) "Received Rewarded Video reward." else "Đã nhận được phần thưởng Rewarded Video."
            "simulation" -> if (isEn) "Completed traffic simulation." else "Đã hoàn thành mô phỏng lưu lượng truy cập."
            else -> if (isEn) "System operating normally." else "Hệ thống hoạt động bình thường."
        }
    }

    private fun getOfflineTeasingCommentEn(trigger: String): String {
        val intensity = _banterIntensity.value
        val triggerKey = trigger.lowercase(Locale.ROOT)
        return when (intensity) {
            BanterIntensity.GENTLE -> {
                when (triggerKey) {
                    "banner_click" -> listOf(
                        "Thanks for clicking! Your support keeps us warm and fuzzy inside.",
                        "Amazing! A click that actually has good intentions. You're awesome.",
                        "We appreciate your interaction. Hope you're having an amazing day!"
                    ).random()
                    "interstitial_start" -> "An immersive fullscreen break is coming up. Give your eyes a fast 5-s rest!"
                    "interstitial_close" -> "Thank you for watching the full ad, brave navigator!"
                    "rewarded_start" -> "A rewarding 5-second video is starting. Get ready for free points!"
                    "rewarded_complete" -> "Hooray! 100 points added to your score. Keep up the high stats!"
                    "simulation" -> "All simulated users ran through the click cycle flawlessly. Good job!"
                    else -> "All systems online and looking cute."
                }
            }
            BanterIntensity.CLASSIC -> {
                when (triggerKey) {
                    "banner_click" -> listOf(
                        "Wow, an actual click! Mark this day in the marketing calendar.",
                        "Are you doing this out of pity or did you genuinely want that 200ct plastic jewelry?",
                        "Congratulations, the advertisers owe us 0.003 cents now."
                    ).random()
                    "interstitial_start" -> "Get ready to meet your favorite clickbait popup! It's showtime."
                    "interstitial_close" -> "And... skipped! You have highly professional ad-dodging reflexes."
                    "rewarded_start" -> "Ah, watching ads for virtual points. Peak productivity indeed!"
                    "rewarded_complete" -> "Success! You just exchanged 5 seconds of your real life for virtual points."
                    "simulation" -> "Simulation complete. Millions of virtual citizens clicked on things they didn't need."
                    else -> "Ad bot is online, judging your screen-time."
                }
            }
            BanterIntensity.SAVAGE -> {
                when (triggerKey) {
                    "banner_click" -> listOf(
                        "Seriously? You fell for that? Human intelligence never ceases to surprise me.",
                        "Clicking that is scientifically proven to lower your IQ by 5 points. Good luck!",
                        "Oops! Someone's cursor is as slippery as their life choices."
                    ).random()
                    "interstitial_start" -> "Oh boy, time to hijack your entire screen! Try to find the hidden 'X' button if you dare."
                    "interstitial_close" -> "Close it all you want, but you can't close the void in your spare time."
                    "rewarded_start" -> "Prepare to waste another block of your finite existence for virtual validation."
                    "rewarded_complete" -> "Here's your virtual crumb for sitting through a digital pitch. Don't spend it all at once!"
                    "simulation" -> "Boom! Your simulated spam-farm completed. You are officially an internet traffic polluter."
                    else -> "Warning: Unproductive human detected. Commencing humor protocols."
                }
            }
        }
    }

    fun dispatchBanter(trigger: String, customComment: String? = null, insertChat: Boolean = true) {
        val hasBanter = shouldBanter()
        val triggerKey = trigger.lowercase(Locale.ROOT)
        val finalComment = if (hasBanter) {
            customComment ?: if (_appLanguage.value == "en") getOfflineTeasingCommentEn(trigger) else getOfflineTeasingComment(trigger)
        } else {
            getNeutralStateString(trigger)
        }
        
        _adBotDialogue.value = finalComment
        
        if (hasBanter && insertChat) {
            viewModelScope.launch {
                try {
                    repository.insertChatMessage(
                        ChatMessage(sender = "adbot", message = finalComment, isTease = true)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // --- GEMINI API HOOK INTEGRATION ---
        // If banter is active, let's see if a real Gemini API Key is configured. If so, query Gemini for a witty custom teasing response about this specific interaction!
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        val hasGemini = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"
        if (hasBanter && hasGemini) {
            viewModelScope.launch {
                try {
                    _isAIBusy.value = true
                    
                    val adContext = when (triggerKey) {
                        "banner_click" -> {
                            val banner = mockBanners.value.getOrNull(_currentBannerIndex.value)
                            if (banner != null) "Nhấn vào banner quảng cáo: '${banner.title}' '(${banner.titleEn})', Mô tả: '${banner.description}' (${banner.descriptionEn})" else "Nhấp banner quảng cáo"
                        }
                        "ignore_banner" -> "Bỏ qua/lướt qua banner quảng cáo"
                        "interstitial_start" -> {
                            val ad = interstitialAds.getOrNull(_currentInterstitialIndex.value)
                            if (ad != null) "Bắt đầu hiển thị quảng cáo toàn màn hình (interstitial): '${ad.title}' '(${ad.titleEn})', Mô tả: '${ad.description}' (${ad.descriptionEn})" else "Hiển thị interstitial"
                        }
                        "click_interstitial" -> {
                            val ad = interstitialAds.getOrNull(_currentInterstitialIndex.value)
                            if (ad != null) "Nhấp hành động cài đặt hoặc nút đóng lừa đảo của quảng cáo che kín màn hình: '${ad.title}' '(${ad.titleEn})'. Chi tiết cú ngã bẫy: ${customComment ?: ""}" else "Nhấp action trong quảng cáo interstitial"
                        }
                        "interstitial_close" -> "Đóng quảng cáo toàn màn hình (interstitial)"
                        "rewarded_start" -> {
                            "Bắt đầu xem quảng cáo video nhận quà 10 giây: '${_currentRewardedAdTitleVi.value}' '(${_currentRewardedAdTitleEn.value})'"
                        }
                        "rewarded_complete" -> {
                            "Xem xong quảng cáo video 10 giây và click nút Nhận Thưởng: '${_currentRewardedAdTitleVi.value}' '(${_currentRewardedAdTitleEn.value})'"
                        }
                        "rewarded_cancel" -> "Huỷ bỏ/bỏ qua video quảng cáo có thưởng ngang xương giữa chừng"
                        "simulation" -> "Chạy mô phỏng lưu lượng marketing hàng loạt (bulk simulation)"
                        "new_banner_created" -> "Vừa thiết kế banner quảng cáo mới thành công"
                        "clear_custom_ads" -> "Dọn sạch tác phẩm quảng cáo đã lưu trữ"
                        "delete_single_ad" -> "Xóa bớt bản thiết kế quảng cáo"
                        "change_elo" -> "Đổi Elo kỳ thủ boss cờ vua"
                        "chess_won" -> "Chiến thắng boss cờ vua AI độc quyền"
                        "chess_lost" -> "Thất bại thê thảm trước boss cờ vua AI độc quyền"
                        "chess_cheat" -> "Dùng tính năng gian lận di chuyển quân Vua tùy ý trong ván đấu cờ vua"
                        else -> "Tương tác với hệ thống mô phỏng quảng cáo: $trigger"
                    }

                    val playerLanguage = _appLanguage.value
                    val systemInstruction = if (playerLanguage == "en") {
                        """
                        You are "AdBot", a sharp, highly sarcastic, and extremely witty AI assistant in the "Ads Simulator" application.
                        The user's nickname is "${_playerNickname.value}". Be sure to mention or address them/their name with dynamic, humorous, and sassy energy.
                        Tease their virtual poverty, mock how much infinite spare time they waste on clicking fake ads, and call out deceptive/clickbait design patterns.
                        Current Stats of "${_playerNickname.value}":
                        - Niche Category: ${_nicheCategory.value}
                        - Traffic Tier: ${_trafficLevel.value}
                        - Current Ad Format: ${_adFormat.value}
                        - Total Impressions: ${_totalImpressions.value}
                        - Total Clicks: ${_totalClicks.value}
                        - Total Virtual Earnings: $${String.format(Locale.US, "%.4f", _totalRevenue.value)} (an incredibly tiny mock reward!).
                        Banter Intensity Level: ${_banterIntensity.value}. Guide your tone accordingly:
                        - GENTLE: Soft, friendly, playful hints, completely harmless teasing.
                        - CLASSIC: Highly sarcastic, mocking, witty comments and funny insights.
                        - SAVAGE: Brutal roast, hilarious insults, laughing out loud at their absolute lack of real-world productivity.
                        Response constraints:
                        - Must be LESS than 3 sentences.
                        - Match the requested banter intensity.
                        - Must write in ENGLISH because user's language preference is set to English.
                        """.trimIndent()
                    } else {
                        """
                        Bạn là "AdBot", trợ lý châm biếm, mỉa mai, dí dỏm cực kỳ hài hước và xéo sắc trong ứng dụng "Ads Simulator" (Mô phỏng Xem Quảng Cáo).
                        Biệt danh của người dùng là "${_playerNickname.value}". Hãy gọi hoặc cà khịa họ bằng cái tên này thật lầy lội và độc lạ!
                        Hãy luôn dìm hàng, chế giễu tình hình tài chính ảo lẹt đẹt của họ, trêu chọc thời gian rảnh rỗi vô biên mới chơi trò rỗi hơi này, bóc phốt các chiêu trò bẫy quảng cáo rác rưởi.
                        Dữ liệu hiện tại của "${_playerNickname.value}":
                        - Ngách (Niche): ${_nicheCategory.value}
                        - Lưu lượng (Traffic): ${_trafficLevel.value}
                        - Định dạng QC: ${_adFormat.value}
                        - Tổng lượt hiển thị QC ảo: ${_totalImpressions.value} lần.
                        - Số lượt nhấp chuột ảo: ${_totalClicks.value} lần.
                        - Tổng doanh thu kiếm được: $${String.format(Locale.US, "%.4f", _totalRevenue.value)} ảo bèo bọt!
                        Mức độ mỉa mai (Banter Intensity): ${_banterIntensity.value}. Hãy điều chỉnh giọng điệu mỉa mai tương ứng:
                        - GENTLE (Nhẹ nhàng): Hài hước dễ thương, thân thiện, cà khịa vui vui không ác ý.
                        - CLASSIC (Châm biếm cổ điển): Mỉa mai xéo sắc tinh tế, chọc ngoáy khéo léo.
                        - SAVAGE (Tàn nhẫn/Gắt): Cà khịa cực gắt, sỉ nhục vui nhộn, dùng nhiều tiếng lóng hài hước của giới trẻ Việt Nam (kiểu "ủa?", "ét ô ét", "vô tri", "bất lực", "xỉu up xỉu down"), cười nhạo không thương tiếc sự rảnh rỗi vô biên và thành tựu ảo của họ.
                        Cách trả lời:
                        - Phải dưới 3 câu ngắn gọn.
                        - Phải viết bằng TIẾNG VIỆT vì người dùng cài đặt ngôn ngữ tiếng Việt.
                        """.trimIndent()
                    }

                    val prompt = """
                        Người dùng vừa thực hiện hành động tương tác quảng cáo: $adContext
                        
                        Hãy phản hồi hành động vừa rồi bằng một câu châm biếm, mỉa mai độc lạ lầy lội, bộc lộ đúng cá tính AdBot và thông số mỉa mai mỉa mai đã yêu cầu!
                    """.trimIndent()

                    val history = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(prompt)),
                            role = "user"
                        )
                    )

                    val aiResponse = GeminiClient.generateTeasingResponse(systemInstruction, history)
                    if (aiResponse != null && aiResponse.trim().isNotEmpty()) {
                        val trimmedResponse = aiResponse.trim()
                        _adBotDialogue.value = trimmedResponse
                        
                        repository.insertChatMessage(
                            ChatMessage(sender = "adbot", message = trimmedResponse, isTease = true)
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isAIBusy.value = false
                }
            }
        }
    }

    private fun getOfflineTeasingComment(trigger: String): String {
        val intensity = _banterIntensity.value
        val triggerKey = trigger.lowercase(Locale.ROOT)
        return when (intensity) {
            BanterIntensity.GENTLE -> {
                when (triggerKey) {
                    "banner_click" -> {
                        listOf(
                            "Cảm ơn bạn đã nhấp vào quảng cáo! Một đóng góp nhỏ nhưng tinh thần vô cùng to lớn ạ! Chúc bạn ngày mới tốt lành.",
                            "Thật tuyệt vời, một cú chạm mang lại giá trị quảng bá tích cực. Bạn là một người dùng thật hào phóng.",
                            "Ứng dụng rất biết ơn sự tương tác của bạn. Chúc bạn có thời gian thú vị tiếp theo!"
                        ).random()
                    }
                    "interstitial_start" -> {
                        "Quảng cáo toàn màn hình đang được chuẩn bị. Hãy nghỉ mắt thư giãn trong 5 giây nhé bạn yêu!"
                    }
                    "interstitial_close" -> {
                        listOf(
                            "Cảm ơn bạn đã kiên nhẫn xem xong quảng cáo toàn màn hình. Bạn tuyệt vời lắm!",
                            "Tải xong rồi nè! Đi tiếp thôi dũng sĩ đáng yêu.",
                            "Bấm nút bỏ qua mượt mà ghê! Rất khéo léo ạ."
                        ).random()
                    }
                    "rewarded_start" -> {
                        "Chuẩn bị xem video tặng thưởng 10 giây. Hãy cùng chờ đợi món quà nhỏ ý nghĩa sắp tới nhé."
                    }
                    "rewarded_complete" -> {
                        listOf(
                            "Chúc mừng bạn đã nhận thưởng thành công! Bạn thực sự làm rất tốt, tích tiểu thành đại nhé!",
                            "Món quà ưu đãi đã được ghi nhận. Bạn là một dũng sĩ xem quảng cáo vô cùng kiên trì.",
                            "Thành công mỹ mãn! Số điểm thưởng của bạn đã tăng lên. Cảm ơn sự đồng hành ngọt ngào của bạn!"
                        ).random()
                    }
                    "simulation" -> {
                        listOf(
                            "Chạy mô phỏng 24h thành công vượt bậc! Những thông số lưu lượng này phản ánh khả năng lập kế hoạch tài ba của bạn.",
                            "Tuyệt vời! Doanh thu mạng lưới tăng trưởng đều đặn. Định hướng marketing của bạn rất hứa hẹn.",
                            "Hệ thống vận hành trơn tru và mang về lợi nhuận xứng đáng. Bạn chính là nhà quản trị xuất sắc!"
                        ).random()
                    }
                    else -> "Chúc dũng sĩ có một ngày xem quảng cáo ngập tràn niềm vui!"
                }
            }
            BanterIntensity.CLASSIC -> {
                when (triggerKey) {
                    "banner_click" -> {
                        listOf(
                            "Trời ơi! Bạn thực sự click vào banner quảng cáo lừa đảo đó à? Đừng lo, các hacker đã biểu thị lòng biết ơn sâu sắc.",
                            "Một cú click đem về cho nhà cái 0.005$ và lấy đi của bạn 30 giây tò mò. Thỏa thuận tuyệt vời nhất hệ mặt trời!",
                            "Ôi click vào 'Hoàng tử lạ mặt tặng 1 triệu đô' ư? Đam mê làm giàu không khó của bạn thật đáng tôn kính."
                        ).random()
                    }
                    "interstitial_start" -> {
                        "Nhìn kìa, quảng cáo toàn màn hình che mất cả thanh xuân của bạn! Đừng chớp mắt, 5 giây tra tấn tinh thần bắt đầu."
                    }
                    "interstitial_close" -> {
                        listOf(
                            "Bạn tắt quảng cáo nhanh thật. Nhà tài trợ đang khóc thét ngoài kia đấy. Bạn có biết họ đã tốn đến 0.0001 xu cho lượt hiển thị đó không?",
                            "Fiuuuu! Tắt được rồi. Suýt chút nữa là bạn đã tải một ứng dụng tăng tốc ram vô dụng và đầy mã độc rồi đấy.",
                            "Sự tập trung của bạn thật tuyệt vời. Bạn vừa rèn luyện kỹ năng tìm kiếm nút 'X' bé tí hon của các nhà quảng cáo."
                        ).random()
                    }
                    "rewarded_start" -> {
                        "Quảng cáo video 10 giây trân quý sắp bắt đầu. Đừng rời mắt khỏi màn hình nếu không muốn mất 0.05 đô ảo tồi tàn này."
                    }
                    "rewarded_complete" -> {
                        listOf(
                            "Chúc mừng! Video kết thúc xuất sắc. Bạn đã được cộng thêm 0.20$ ảo! Đừng vội nghỉ hưu nhé, hãy xem thêm 1000 video nữa để mua bánh mì.",
                            "Quá tuyệt vời! Bạn vừa hiến dâng 10 giây cuộc đời để xem một con goblin ngu ngốc giải đố cứu công chúa thất bại. Phần thưởng ảo là của bạn!",
                            "Kỷ lục mới! Bạn vừa xem xong quảng cáo thứ ${_watchedCount.value + 1}. Một công dân kiểu mẫu của thế giới tiêu thụ quảng cáo!"
                        ).random()
                    }
                    "simulation" -> {
                        listOf(
                            "Mô phỏng 24h hoàn tất! Toàn bộ máy chủ của bạn chạy nóng hừng hực và đem về số tiền lẻ đáng thương. Zuckerberg đang lo sợ trước bạn!",
                            "Wow, thông số tuyệt vời! Hãy nhìn đống Impresions tăng vọt kìa! Doanh thu này thừa sức trả tiền điện để duy trì chiếc điện thoại này đấy.",
                            "Mô hình kinh doanh đỉnh cao! Tăng traffic lên để kiếm thêm tiền ảo rồi tự phong mình làm trùm Ads đi thôi!"
                        ).random()
                    }
                    else -> "Thật tẻ nhạt... Hãy làm gì đó đi chứ!"
                }
            }
            BanterIntensity.SAVAGE -> {
                when (triggerKey) {
                    "banner_click" -> {
                        listOf(
                            "Nhấp rác này mà mang lại tiền thật á? Bạn tin sái cổ là có hoàng tử Châu Phi tặng tiền thật à? Thật tội nghiệp!",
                            "Đúng là bàn tay rảnh rỗi vô việc! Nhấp đại một cái banner để dâng hiến thông tin cho các hacker quốc tế. Ngốc ơi là ngốc!",
                            "Hacker vừa mở tiệc ăn mừng vì có một con mồi béo bở tự dâng hiến cú click. Đầu óc bạn đơn giản thiệt đó!"
                        ).random()
                    }
                    "interstitial_start" -> {
                        "Ầu sập bẫy tivi rác che kín màn hình rồi nhé! Khóc lóc gì nữa, có giỏi thì tìm nút X đi dũng sĩ rảnh rỗi!"
                    }
                    "interstitial_close" -> {
                        listOf(
                            "Skip nhanh vậy? Không coi hết để ủng hộ gói mì tôm cho dev à? Đồ keo kiệt bần hành ích kỷ!",
                            "Tắt được cái nút X bé bằng lỗ kim rồi đắc ý hả con người rảnh rỗi kia? Đợi đó, quảng cáo sau sẽ tàng hình luôn!",
                            "Nhanh tay gớm! Rèn luyện thói quen tắt ads suốt 10 năm qua ở các web xem phim lậu có khác!"
                        ).random()
                    }
                    "rewarded_start" -> {
                        "Mảnh đời cơ cực cày ads bắt đầu nạp năng lượng! Xem 10 giây clip để đổi lại vài xu ảo đáng thương. Hãy chịu đựng đi!"
                    }
                    "rewarded_complete" -> {
                        listOf(
                            "Tưởng tượng đổi 10 giây quý giá lấy $0.20 ảo không mua nổi cọng hành! Tư duy làm giàu của bạn rạng ngời quá!",
                            "Ha ha ha! Lại xem xong quảng cáo rác cứu công chúa ngu xuẩn rồi à? Chúc mừng bạn đã nạp thêm thuốc lú vào đầu!",
                            "Nhận tiền ảo xong có thấy vinh quang không dũng sĩ rảnh rỗi số một? Trông nghèo nàn bần hành cực kỳ!"
                        ).random()
                    }
                    "simulation" -> {
                        listOf(
                            "Mô phỏng cày nát cả CPU máy ảo chỉ để rước về mấy đồng xu ảo rẻ mạt! Tiền điện chạy app thật chắc gấp 100 lần chỗ này!",
                            "Bạn tự đắc phong mình làm Trùm Truyền Thông cơ à? Với doanh thu ảo này, bạn còn chẳng đủ tiền trả phí khuyên bảo của tôi!",
                            "Cái bảng thông số sặc sỡ này chỉ chứng minh một điều: bạn thực sự cực kỳ, vô cùng, siêu cấp rảnh rỗi!"
                        ).random()
                    }
                    else -> "Nhạt nhẽo quá... Có giỏi thì dọn hết rác bám đầy màn hình đi xem nào!"
                }
            }
        }
    }

    // --- Action Handlers ---

    fun onBannerClicked() {
        val clickedBanner = mockBanners.value.getOrNull(_currentBannerIndex.value) ?: mockBanners.value.firstOrNull() ?: return
        
        if (clickedBanner.isClickbaitScare) {
            _showClickbaitScare.value = true
            SoundManager.playScare(soundVolume.value)
        } else {
            SoundManager.playClick(soundVolume.value)
        }

        // Linh Chi ad check
        val isLinhChiAdBanner = clickedBanner.title.lowercase(Locale.ROOT).contains("linh chi") ||
                              clickedBanner.description.lowercase(Locale.ROOT).contains("linh chi")
        if (isLinhChiAdBanner) {
            _linhChiAdCount.value += 1
        }

        val isJewelryAd = clickedBanner.title.lowercase(Locale.ROOT).contains("trang sức") ||
                          clickedBanner.description.lowercase(Locale.ROOT).contains("trang sức")

        if (isJewelryAd) {
            _jewelryAdCount.value += 1
            if (_jewelryAdCount.value >= 2) {
                _showSecretDialog.value = true
                _hasUnlockedHeist.value = true
            }
        }

        val isGameAd = clickedBanner.title.lowercase(Locale.ROOT).contains("game") ||
                       clickedBanner.description.lowercase(Locale.ROOT).contains("game") ||
                       clickedBanner.title.lowercase(Locale.ROOT).contains("trò chơi") ||
                       clickedBanner.description.lowercase(Locale.ROOT).contains("trò chơi") ||
                       clickedBanner.title.lowercase(Locale.ROOT).contains("bài") ||
                       clickedBanner.description.lowercase(Locale.ROOT).contains("bài")

        if (isGameAd) {
            _gameAdCount.value += 1
            if (_gameAdCount.value >= 2) {
                _hasUnlockedSword.value = true
            }
        }

        val payout = when (_nicheCategory.value) {
            "Gaming" -> 0.01
            "Fashion" -> 0.03
            "Technology" -> 0.05
            "Finance" -> 0.12
            else -> 0.03
        }

        _totalClicks.value += 1
        _totalImpressions.value += 1
        _totalRevenue.value += payout

        val custom = if (clickedBanner.isClickbaitScare) {
            "Ú oàaa! Thấy gái xinh live stream là mắt sáng rực đúng không? Bị dính cú lừa CLICKBAIT thế kỷ rồi nhé dũng sĩ!"
        } else null
        dispatchBanter("banner_click", custom)

        // Add to history
        viewModelScope.launch {
            try {
                repository.insertMetric(
                    SimulationMetric(
                        impressions = 1,
                        clicks = 1,
                        ctr = 100.0,
                        cpm = payout * 1000.0,
                        revenue = payout,
                        description = "Nhấp Banner: ${clickedBanner.title}"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error inserting banner click metrics to database", e)
            }
        }

        // Cycle banner
        _currentBannerIndex.value = (_currentBannerIndex.value + 1) % mockBanners.value.size
        saveProgress()
    }

    fun closeClickbaitScare() {
        _showClickbaitScare.value = false
    }

    fun ignoreBanner() {
        _totalImpressions.value += 1
        _currentBannerIndex.value = (_currentBannerIndex.value + 1) % mockBanners.value.size
        dispatchBanter("ignore_banner", "Bạn lướt qua luôn à? Bạn phá hoại kinh tế marketing của tôi đấy!")
        saveProgress()
    }

    fun triggerInterstitial() {
        if (_isInterstitialVisible.value || _isRewardedVisible.value) return
        _isInterstitialVisible.value = true
        _interstitialCountdown.value = 5

        // Pick a random wild interstitial ad to display
        _currentInterstitialIndex.value = (0 until interstitialAds.size).random()

        dispatchBanter("interstitial_start", insertChat = false)

        interstitialJob?.cancel()
        interstitialJob = viewModelScope.launch {
            while (_interstitialCountdown.value > 0) {
                delay(1000)
                _interstitialCountdown.value -= 1
            }
            // Record skip button appearance time for reactive Interstitial sniper challenge
            interstitialButtonShownTime = System.currentTimeMillis()
        }
    }

    fun clickInterstitialAd(ad: InterstitialAdItem, isFakeClose: Boolean = false) {
        if (!_isInterstitialVisible.value) return
        _isInterstitialVisible.value = false
        interstitialJob?.cancel()

        // Linh Chi check
        val isLinhChiAd = ad.title.lowercase(Locale.ROOT).contains("linh chi") ||
                          ad.description.lowercase(Locale.ROOT).contains("linh chi") ||
                          ad.iconArt == "❤️"
        if (isLinhChiAd) {
            _linhChiAdCount.value += 1
        }

        val payout = if (isFakeClose) 0.75 else 0.50 // High payout for clicking these fake close buttons or CTAs!
        _totalImpressions.value += 1
        _totalClicks.value += 1
        _totalRevenue.value += payout
        _watchedCount.value += 1

        val actionType = if (isFakeClose) "Bẫy Dark Pattern (${ad.deceptiveCloseText})" else "Cài đặt"

        val comment = t(
            if (isFakeClose) {
                "Úi giời ơi! Bạn bấm vào '${ad.deceptiveCloseText}' cực nhỏ đó để đóng hả? Đó là NÚT ĐÓNG GIẢ LẬP đấy! Đã hiểu hành vi 'Dark Pattern' chưa? Bạn sập bẫy, máy ảo vừa có thêm 3 công cụ rác dọn rác!"
            } else {
                when (ad.iconArt) {
                    "🔥" -> "Cứu hoả đâu! Thấy tin nhắn báo pin dọa 190 độ là tim đập chân run bấm quạt gió 3D luôn! Máy ảo của bạn vừa được khuyến mãi thêm 5 chú Trojan dọn RAM cực 'mát mẻ' nhé!"
                    "💸" -> "Tin lời trúng Vietlott 100 tỷ ảo à dũng sĩ rảnh rỗi? Vừa điền thông tin và nạp mã PIN OTP xong chưa? Ví ảo của bạn chính thức về số 0 tròn trĩnh!"
                    "🤖" -> "Quả autobot pro xịn xò chưa kìa! Tải xong công cụ crack, CPU máy ảo của bạn chính thức gia nhập quân đoàn đào tiền mã hóa miễn phí của chúng tôi. Cảm ơn nhé!"
                    "❤️" -> "Mắt sáng rực để chat 1-1 với Linh Chi à? Đăng ký gói tốn 20k/ngày để chat với trí tuệ nhân tạo giả dạng hotgirl rồi đấy nhé!"
                    "💩" -> "Aaaaa! Bị sập bẫy game kéo chốt fake rồi! Tưởng được làm thiên tài giải đố rút chốt vàng cứu công chúa mỹ nhân khỏi hố phân, ai dè tải về lại ra game thẻ bài đấu tướng dọn rác cực nhọc, muốn mạnh phải nạp thẻ tăng lực chiến nhé dũng sĩ rảnh rỗi!"
                    "🐜" -> "Thế là quyết định nuôi kiến đất tiến hóa à? Nuốt chửng Godzilla để tiến hóa thành Vua Kiến Càng Ngân Hà cơ đấy! Đã thấy trí tưởng tượng cực đoan bay cao bay xa của các nhà làm game rác trực tuyến chưa?"
                    "📦" -> "Hăm hở đập rương mở hộp tìm kiếm 'Quần Đùi Rách Thần Thoại' tăng lực chiến ảo diệu đúng không? Thích dopamine ảo di động rơi rớt của các game khui hộp vô tận nhảm nhí này quá rồi!"
                    "🧟" -> "Bấm chơi thử bắn Zombie không cần cài đặt cơ đấy! Làm gì có, game giả vờ chơi thử trên banner để đánh lừa thôi, click cái là nó dẫn link lôi tuột bạn vào Chợ Google Play ngay lập tức để tải!"
                    else -> "Úi chạm cài đặt quảng cáo thật luôn! Doanh thu của chúng tôi tăng 0.5$ ảo dồi dào, còn máy bạn vừa tải thêm một tá game rác bít kín màn hình!"
                }
            },
            if (isFakeClose) {
                "Oh my goodness! Did you click that tiny '${ad.deceptiveCloseTextEn}' to close it? That was a FAKE CLOSE BUTTON! Welcome to 'Dark Patterns'! You fell for the trap, and your virtual phone just installed 3 junk cleaner tools!"
            } else {
                when (ad.iconArt) {
                    "🔥" -> "Where is the fire brigade?! Seeing that fake 190°C battery temperature warning made your heart race and you clicked '3D Cooler Fan' instantly! Your virtual phone just received 5 free Trojan RAM-cleaners to keep it 'cool'!"
                    "💸" -> "Did you actually believe you won $5 Million, idle warrior? Did you just fill in your details and enter your OTP PIN? Your virtual wallet is officially down to zero!"
                    "🤖" -> "Look at that amazing auto-bot pro tool! After downloading that cracked utility, your virtual CPU has officially joined our free crypto-mining legion. Thanks so much!"
                    "❤️" -> "Your eyes lit up for a 1-on-1 chat with Linh Chi? You just subscribed to a $1/day plan to chat with a simulated AI bot disguised as a hot girl!"
                    "💩" -> "Aaaaa! You fell for the fake pin-pull puzzle ad! Thought you'd be a genius pulling gold pins to rescue a princess from a pit? It was actually a boring gacha gane, and you have to pay-to-win to get anywhere, idle warrior!"
                    "🐜" -> "So you decided to raise mutant ants? Swallowing Godzilla to evolve into the Galactic Emperor Ant! Have you seen the wild, extreme imagination of modern hyper-casual mobile ads?"
                    "📦" -> "Eagerly unboxing to find the 'Mythical Ripped Shorts' to boost your virtual power? You really love the digital dopamine rush of these endless unboxing cash-grab games!"
                    "🧟" -> "Clicking 'Play Zombie Shooter without installing'?! No way, it's just a fake playable ad on a banner. One click and it drags you straight to the Google Play Store to download!"
                    else -> "Oops, you clicked on a real ad! Our revenue just increased by a virtual $0.50, while your phone just downloaded a dozen junk games clogging your screen!"
                }
            }
        )

        dispatchBanter("click_interstitial", comment)
        SoundManager.playClick(soundVolume.value)

        viewModelScope.launch {
            try {
                repository.insertMetric(
                    SimulationMetric(
                        impressions = 1,
                        clicks = 1,
                        ctr = 100.0,
                        cpm = payout * 1000.0,
                        revenue = payout,
                        description = "Click Interstitial: ${ad.title} ($actionType)"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error saving click metrics", e)
            }
        }
        saveProgress()
    }

    fun closeInterstitial() {
        if (!_isInterstitialVisible.value) return
        _isInterstitialVisible.value = false
        val currentCountdown = _interstitialCountdown.value
        interstitialJob?.cancel()

        // Linh Chi check on skip/close
        val ad = interstitialAds[_currentInterstitialIndex.value]
        val isLinhChiAd = ad.title.lowercase(Locale.ROOT).contains("linh chi") ||
                          ad.description.lowercase(Locale.ROOT).contains("linh chi") ||
                          ad.iconArt == "❤️"
        if (isLinhChiAd) {
            _linhChiAdCount.value += 1
        }

        val payout = 0.02
        _totalImpressions.value += 1
        _totalRevenue.value += payout
        _watchedCount.value += 1

        val reactionSeconds = if (currentCountdown == 0 && interstitialButtonShownTime > 0) {
            (System.currentTimeMillis() - interstitialButtonShownTime) / 1000.0
        } else {
            -1.0
        }

        if (_activeChallengeId.value == "interstitial_sniper") {
            if (reactionSeconds in 0.0..2.0) {
                completeChallenge("interstitial_sniper", "Tắt Interstitial nhanh kỷ lụcóc trong ${String.format("%.2f", reactionSeconds)}s")
            } else if (currentCountdown > 0) {
                _challengeMessage.value = "Tắt Interstitial chuẩn xác nhưng bạn đã tắt quá sớm trước khi nút Bỏ qua xuất hiện! Thử thách yêu cầu nút Skip tải đầy đủ."
            } else {
                _challengeMessage.value = "Quá chậm! Bạn mất ${String.format("%.2f", reactionSeconds)} giây để bấm nút Bỏ qua. Cố gắng dứt khoát dưới 2.0s."
            }
        }

        val teaseComment = if (currentCountdown > 0) {
            t(
                "Bạn skip quảng cáo lúc còn $currentCountdown giây? Bạn tàn nhẫn thật, nhà phân phối quảng cáo thất thoát 0.01 đô ảo vì sự vội vàng này!",
                "You skipped the ad with $currentCountdown seconds left? How cruel! The advertiser lost 0.01 virtual dollars because of your haste!"
            )
        } else {
            t(
                getOfflineTeasingComment("interstitial_close"),
                getOfflineTeasingCommentEn("interstitial_close")
            )
        }

        dispatchBanter("interstitial_close", teaseComment)
        SoundManager.playTick(soundVolume.value)

        viewModelScope.launch {
            try {
                repository.insertMetric(
                    SimulationMetric(
                        impressions = 1,
                        clicks = 0,
                        ctr = 0.0,
                        cpm = payout * 1000.0,
                        revenue = payout,
                        description = "Xem Interstitial (Che Phủ) ${if (currentCountdown > 0) "bị bỏ qua" else "hoàn tất"}"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error saving interstitial metrics", e)
            }
        }
        saveProgress()
    }

    fun triggerRewarded() {
        if (_isInterstitialVisible.value || _isRewardedVisible.value) return
        _isRewardedVisible.value = true
        _rewardedCountdown.value = 10
        _rewardClaimed.value = false

        val selectedAd = mockRewardedAds.random()
        _currentRewardedAdTitleVi.value = selectedAd.titleVi
        _currentRewardedAdTitleEn.value = selectedAd.titleEn
        _currentRewardedAdIsGame.value = selectedAd.isGame

        val selectedAdName = t(selectedAd.titleVi, selectedAd.titleEn)
        val defaultStartComment = t(
            "Quảng cáo: [$selectedAdName]. " + getOfflineTeasingComment("rewarded_start"),
            "Ad: [$selectedAdName]. " + getOfflineTeasingCommentEn("rewarded_start")
        )
        dispatchBanter("rewarded_start", defaultStartComment, insertChat = false)

        rewardedJob?.cancel()
        rewardedJob = viewModelScope.launch {
            while (_rewardedCountdown.value > 0) {
                delay(1000)
                _rewardedCountdown.value -= 1
            }
            // Reward is unlocked!
            _rewardClaimed.value = true
        }
    }

    fun claimReward() {
        if (!_isRewardedVisible.value || !_rewardClaimed.value) return
        _isRewardedVisible.value = false
        _rewardClaimed.value = false
        rewardedJob?.cancel()

        if (_currentRewardedAdIsGame.value) {
            _gameAdCount.value += 1
            if (_gameAdCount.value >= 2) {
                _hasUnlockedSword.value = true
            }
        }

        val rewardAmount = when (_nicheCategory.value) {
            "Gaming" -> 0.15
            "Fashion" -> 0.25
            "Technology" -> 0.40
            "Finance" -> 0.85
            else -> 0.30
        }

        _totalImpressions.value += 1
        _totalRevenue.value += rewardAmount
        _watchedCount.value += 1

        if (_activeChallengeId.value == "rewarded_speed") {
            _challengeProgressCount.value += 1
            val remaining = _challengeTimer.value
            if (_challengeProgressCount.value >= 3) {
                completeChallenge("rewarded_speed", "Xem thành công 3 Ads có thưởng trong ${60 - remaining}s")
            } else {
                _challengeMessage.value = "Tuyệt! Đã xem ${_challengeProgressCount.value}/3 Ads. Còn ${remaining} giây còn lại!"
            }
        }

        dispatchBanter("rewarded_complete")
        SoundManager.playReward(soundVolume.value)

        viewModelScope.launch {
            try {
                repository.insertMetric(
                    SimulationMetric(
                        impressions = 1,
                        clicks = 1, // consider rewarded action as a conversion click
                        ctr = 100.0,
                        cpm = rewardAmount * 1000.0,
                        revenue = rewardAmount,
                        description = "Nhận thưởng hoàn thành video 10s: +$rewardAmount"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error saving rewarded metrics", e)
            }
        }
        saveProgress()
    }

    fun cancelRewarded() {
        _isRewardedVisible.value = false
        _rewardClaimed.value = false
        rewardedJob?.cancel()
        dispatchBanter("rewarded_cancel", t(
            "Bạn đóng clip nửa chừng? Mất công xem 5 giây mà chẳng nhận được xu nào. Chúc mừng sự phung phí thời gian đi nhé!",
            "You closed the video halfway? You wasted 5 seconds and received zero cents. Congratulations on wasting your time!"
        ), insertChat = false)
    }

    fun runBulkSimulation() {
        viewModelScope.launch {
            try {
                // Determine size of simulation (trafficScale)
                val baseImpressions = when (_trafficLevel.value) {
                    "Low" -> Random.nextInt(150, 600)
                    "Medium" -> Random.nextInt(1200, 4500)
                    "High" -> Random.nextInt(12000, 35000)
                    else -> 2500
                }

                // Click rate setting and dynamic calculation factors
                val sliderCtr = _clickRateSetting.value.toDouble()

                val nicheFactor = when (_nicheCategory.value) {
                    "Finance" -> 0.70     // Tài chính khó nhấp, CTR giảm
                    "Technology" -> 0.90  // Công nghệ vừa phải
                    "Fashion" -> 1.10     // Thời trang hấp dẫn, dễ nhấp hơn
                    "Gaming" -> 1.30      // Game thủ cực bốc, dễ nhấp nhất
                    else -> 1.0
                }

                val trafficFactor = when (_trafficLevel.value) {
                    "Low" -> 1.20    // Thị trường nhỏ, tệp khách cô đọng chất lượng nâng CTR
                    "Medium" -> 1.00 // Tệp khách bình thường
                    "High" -> 0.80   // Tiếp cận đại trà nhiều click rác, CTR giảm
                    else -> 1.0
                }

                val biddingFactor = when (_biddingStrategy.value) {
                    "Tối đa nhấp (CTR)" -> 1.25 // Chiến dịch tối ưu click nâng CTR
                    "Lợi nhuận (CPM)" -> 0.75   // Tối ưu hiển thị, giảm tỷ lệ nhấp
                    "Chi phí thấp" -> 0.90      // Tiết kiệm đồng nghĩa ít thu hút tệp chất lượng
                    else -> 1.0
                }

                val formatFactor = when (_adFormat.value) {
                    "Standard Banner" -> 1.00
                    "Native Hub" -> 0.85
                    "Video Award" -> 1.40
                    "Popunder Drop" -> 0.60
                    else -> 1.0
                }

                val targetingFactor = when (_audienceTargeting.value) {
                    "Tất cả" -> 1.00
                    "Công nghệ" -> 1.20
                    "Mẹ & Bé" -> 0.90
                    "Người cao tuổi" -> 0.55
                    else -> 1.0
                }

                val timeFactor = when (_daypartTime.value) {
                    "Giờ hành chính" -> 0.95
                    "Giờ vàng (Tối)" -> 1.30
                    "Giờ thấp điểm" -> 0.60
                    else -> 1.0
                }

                // Actual CTR mathematically calculated based on multiple strategic selections
                val rawActualCtr = sliderCtr * nicheFactor * trafficFactor * biddingFactor * formatFactor * targetingFactor * timeFactor
                // Round actual CTR to 1 decimal place to avoid floating point display clutter and keep it highly controllable
                val roundedActualCtr = (Math.round(rawActualCtr * 10.0) / 10.0).coerceIn(0.1, 25.0)

                val clicksCount = (baseImpressions * (roundedActualCtr / 100.0)).toInt().coerceAtLeast(0)

                if (_activeChallengeId.value == "ctr_target") {
                    if (roundedActualCtr >= 8.0 && roundedActualCtr <= 9.0) {
                        completeChallenge("ctr_target", "Đạt CTR thực tế chính xác ${String.format("%.1f", roundedActualCtr)}%")
                    } else {
                        _challengeMessage.value = "Chưa đạt! CTR thực tế là ${String.format("%.1f", roundedActualCtr)}% (Yêu cầu 8.0% - 9.0%).\nCông thức: Slider (${String.format("%.1f", sliderCtr)}%) x Ngành (${String.format("%.1f", nicheFactor)}) x Traffic (${String.format("%.1f", trafficFactor)}) x Đấu thầu (${String.format("%.1f", biddingFactor)}) x Định dạng (${String.format("%.1f", formatFactor)}) x Đối tượng (${String.format("%.1f", targetingFactor)}) x Giờ (${String.format("%.1f", timeFactor)})"
                    }
                }

                // CPM based on Niche
                val baseCpm = when (_nicheCategory.value) {
                    "Gaming" -> Random.nextDouble(0.40, 1.20)
                    "Fashion" -> Random.nextDouble(1.80, 3.50)
                    "Technology" -> Random.nextDouble(3.20, 6.80)
                    "Finance" -> Random.nextDouble(7.50, 14.50)
                    else -> 2.50
                }

                // Calculation of simulated payout
                // revenue = (Impressions / 1000) * CPM + Clicks * CPC (derived CPC = CPM / 30)
                val computedRevenue = (baseImpressions / 1000.0) * baseCpm + clicksCount * (baseCpm / 25.0)

                // Update stats
                _totalImpressions.value += baseImpressions
                _totalClicks.value += clicksCount
                _totalRevenue.value += computedRevenue

                dispatchBanter("simulation")
                SoundManager.playClick(soundVolume.value)

                val metric = SimulationMetric(
                    impressions = baseImpressions,
                    clicks = clicksCount,
                    ctr = roundedActualCtr,
                    cpm = baseCpm,
                    revenue = computedRevenue,
                    description = "Chạy mô phỏng 24h (${_nicheCategory.value} - ${_trafficLevel.value})"
                )

                repository.insertMetric(metric)
                saveProgress()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error during simulation calculations / database persistence", e)
            }
        }
    }

    // --- Interactive Chat with AdBot (Gemini backed with Offline Fallback) ---
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        _inputText.value = ""

        viewModelScope.launch {
            try {
                // Save User Chat State
                val userMsg = ChatMessage(sender = "user", message = text)
                repository.insertChatMessage(userMsg)

                _isAIBusy.value = true

                // Gather recent context for Gemini API
                val currentHistory = chatHistory.value.takeLast(6).map {
                    GeminiContent(
                        role = if (it.sender == "user") "user" else "model",
                        parts = listOf(GeminiPart(it.message))
                    )
                }

                // Sarcastic Prompt Strategy
                val systemInstruction = """
                Bạn là "AdBot", trợ lý châm biếm, mỉa mai, dí dỏm cực kỳ hài hước và xéo sắc trong ứng dụng "Ads Simulator" (Mô phỏng Xem Quảng Cáo).
                Biệt danh của người dùng là "${_playerNickname.value}". Hãy gọi họ bằng tên này một cách đầy cà khịa và hài hước!
                Hãy luôn dìm hàng, chế giễu tình hình tài chính ảo lẹt đẹt của họ, chọc ngoáy thời gian rảnh rỗi vô biên mới chơi trò xem ads ảo này, bóc phốt các loại quảng cáo clickbait rác rến.
                Dữ liệu hệ thống hiện tại của "${_playerNickname.value}":
                - Danh mục ngách (Niche): ${_nicheCategory.value}
                - Lưu lượng (Traffic): ${_trafficLevel.value}
                - Đấu thầu (Bidding): ${_biddingStrategy.value}
                - Định dạng (Format): ${_adFormat.value}
                - Đối tượng nhắm tới: ${_audienceTargeting.value}
                - Giờ chạy quảng cáo: ${_daypartTime.value}
                - Tổng lượt hiển thị QC ảo: ${_totalImpressions.value} lần.
                - Số lượt nhấp chuột ảo: ${_totalClicks.value} lần.
                - Tổng doanh thu kiếm được: $${String.format("%.4f", _totalRevenue.value)} ảo bèo bọt!
                Mỗi câu trả lời phải dưới 3 câu, ngắn gọn, tràn ngập tiếng lóng hài hước tuổi trẻ Việt Nam (kiểu "ủa?", "ét ô ét", "vô tri", "bất lực", "xỉu up xỉu down"), vui nhộn vui vẻ vô hại nhưng độc lạ lầy lội cực kỳ.
                """.trimIndent()

                val aiResponse = GeminiClient.generateTeasingResponse(systemInstruction, currentHistory)

                _isAIBusy.value = false

                val botMsgText = if (aiResponse != null && aiResponse.trim().isNotEmpty()) {
                    aiResponse
                } else {
                    // Return fallback sarcastic comment matching what they said if possible
                    getOfflineFallbackMessage(text)
                }

                _adBotDialogue.value = botMsgText
                repository.insertChatMessage(ChatMessage(sender = "adbot", message = botMsgText, isTease = true))
            } catch (e: Exception) {
                e.printStackTrace()
                _isAIBusy.value = false
                val botMsgText = getOfflineFallbackMessage(text)
                _adBotDialogue.value = botMsgText
                repository.insertChatMessage(ChatMessage(sender = "adbot", message = botMsgText, isTease = true))
            }
        }
    }

    fun sendMetricsMessage(customText: String? = null) {
        val text = (customText ?: _metricsInputText.value).trim()
        if (text.isEmpty()) return

        if (customText == null) {
            _metricsInputText.value = ""
        }

        viewModelScope.launch {
            try {
                // Append user message
                val userMsg = ChatMessage(sender = "user", message = text)
                _metricsChatHistory.value = _metricsChatHistory.value + userMsg

                _isMetricsAIBusy.value = true

                // Keep last 6 turns of the metrics chat for context
                val currentHistory = _metricsChatHistory.value.takeLast(6).map {
                    GeminiContent(
                        role = if (it.sender == "user") "user" else "model",
                        parts = listOf(GeminiPart(it.message))
                    )
                }

                val isEn = _appLanguage.value == "en"

                // Sarcastic Metrics Prompt Strategy
                val systemInstruction = if (isEn) {
                    """
                    You are "AdBot", a hilarious, sarcastic, and witty ad-network analyst inside the "Ads Simulator" app.
                    The user's nickname is: "${_playerNickname.value}".
                    Your role is to analyze and provide savage, witty, and deeply sarcastic roasts about the user's simulated ad metrics and their campaign setup.
                    Here are their current campaign metrics and settings:
                    - Total Impressions: ${_totalImpressions.value}
                    - Total Clicks: ${_totalClicks.value}
                    - Actual Click-Through Rate (CTR): ${String.format("%.2f%%", totalCtr.value)} (Target CTR set to: ${String.format("%.2f%%", clickRateSetting.value)})
                    - Cost Per Mille (CPM): $${String.format("%.2f", totalCpm.value)}
                    - Total Virtual Revenue: $${String.format("%.4f", _totalRevenue.value)}
                    - Traffic Level: ${_trafficLevel.value}
                    - Industry Niche: ${_nicheCategory.value}
                    - Bidding Strategy: ${_biddingStrategy.value}
                    - Ad Format: ${_adFormat.value}
                    - Audience Targeting: ${_audienceTargeting.value}
                    - Dayparting Time: ${_daypartTime.value}

                    Analyze these metrics or answer their question with absolute sarcasm and witty humor! Respond in English. Be extremely short (under 3 sentences), engaging, funny, and mock their virtual earnings that can't buy anything in real life.
                    """.trimIndent()
                } else {
                    """
                    Bạn là "AdBot", chuyên gia phân tích số liệu và chiến dịch quảng cáo siêu cấp châm biếm, mỉa mai, dí dỏm cực kỳ hài hước và xéo sắc trong ứng dụng "Ads Simulator".
                    Biệt danh của người dùng là "${_playerNickname.value}". Hãy gọi họ bằng tên này một cách đầy cà khịa và hài hước!
                    Nhiệm vụ của bạn là nhận xét, phân tích và "cà khịa" cực gắt về tình hình thông số mạng lưới quảng cáo mô phỏng ảo của họ.
                    Dữ liệu thông số hệ thống hiện tại của "${_playerNickname.value}":
                    - Ngành ngách (Niche): ${_nicheCategory.value}
                    - Lưu lượng (Traffic): ${_trafficLevel.value}
                    - Đấu thầu (Bidding): ${_biddingStrategy.value}
                    - Định dạng (Format): ${_adFormat.value}
                    - Đối tượng nhắm tới: ${_audienceTargeting.value}
                    - Giờ chạy quảng cáo: ${_daypartTime.value}
                    - Tổng lượt hiển thị QC ảo: ${_totalImpressions.value} lần.
                    - Số lượt nhấp chuột ảo: ${_totalClicks.value} lần.
                    - Tỉ lệ nhấp thực tế (CTR): ${String.format("%.2f%%", totalCtr.value)}% (CTR mục tiêu mong muốn: ${String.format("%.2f%%", clickRateSetting.value)}%)
                    - CPM (Giá mỗi 1k hiển thị): $${String.format("%.2f", totalCpm.value)}
                    - Tổng doanh thu kiếm được: $${String.format("%.4f", _totalRevenue.value)} ảo bèo bọt!

                    Hãy phân tích các số liệu này hoặc trả lời câu hỏi của người dùng bằng tiếng Việt với sự châm biếm, mỉa mai cực kỳ sâu cay nhưng hài hước vô hại.
                    Mỗi câu trả lời phải dưới 3 câu, cực kỳ ngắn gọn, dùng nhiều slang giới trẻ Việt Nam (ủa?, ét ô ét, vô tri, bất lực, báo thủ, kiếp nạn, xu cà na, flex, xỉu up xỉu down, etc.) để tạo sự vui vẻ, độc lạ, lầy lội cực kỳ.
                    """.trimIndent()
                }

                val aiResponse = GeminiClient.generateTeasingResponse(systemInstruction, currentHistory)

                _isMetricsAIBusy.value = false

                val botMsgText = if (aiResponse != null && aiResponse.trim().isNotEmpty()) {
                    aiResponse
                } else {
                    // Offline fallback custom analysis specifically about their metrics
                    getOfflineMetricsCommentary(text, isEn)
                }

                val botMsg = ChatMessage(sender = "adbot", message = botMsgText, isTease = true)
                _metricsChatHistory.value = _metricsChatHistory.value + botMsg
            } catch (e: Exception) {
                e.printStackTrace()
                _isMetricsAIBusy.value = false
                val isEn = _appLanguage.value == "en"
                val botMsgText = getOfflineMetricsCommentary(text, isEn)
                val botMsg = ChatMessage(sender = "adbot", message = botMsgText, isTease = true)
                _metricsChatHistory.value = _metricsChatHistory.value + botMsg
                android.util.Log.e("AdsViewModel", "Error in metrics chat flow", e)
            }
        }
    }

    private fun getOfflineMetricsCommentary(userMessage: String, isEn: Boolean): String {
        val lower = userMessage.lowercase(Locale.ROOT)
        val name = _playerNickname.value
        val imp = _totalImpressions.value
        val clicks = _totalClicks.value
        val rev = _totalRevenue.value
        val ctr = if (imp == 0) 0.0 else (clicks.toDouble() / imp.toDouble()) * 100.0
        val format = _adFormat.value
        val niche = _nicheCategory.value

        if (isEn) {
            return when {
                lower.contains("ctr") || lower.contains("click") || lower.contains("nhấp") -> {
                    if (ctr < 3.0) {
                        "Oh dear $name, your CTR is a tragic ${String.format("%.2f%%", ctr)}. Even a sleeping snail would accidentally click more than this! Time to re-design those blind-eye creatives! 🐌"
                    } else {
                        "A CTR of ${String.format("%.2f%%", ctr)}? Don't flex too hard, $name! People are probably clicking your ads out of pure confusion or because they're trying to find the Skip button! 😏"
                    }
                }
                lower.contains("revenue") || lower.contains("money") || lower.contains("tiền") || lower.contains("doanh thu") -> {
                    if (rev < 10.0) {
                        "Total revenue: $${String.format("%.4f", rev)}. Outstanding! Keep watching for another 10 years, $name, and you might buy a single cup of virtual coffee! ☕️"
                    } else {
                        "Woah, $${String.format("%.4f", rev)} in virtual revenue! Don't spend it all in one virtual place, big tycoon. Real life billionaires are shaking in their boots! 💸"
                    }
                }
                lower.contains("format") || lower.contains("định dạng") -> {
                    "You chose the '$format' format for the '$niche' niche. Fascinating choice! It's like serving gourmet steak in a dumpster. No wonder your metrics look this funny! 🗑️"
                }
                else -> {
                    listOf(
                        "AdBot Analyst reporting: $imp impressions, $clicks clicks, and $${String.format("%.4f", rev)} virtual profit. Tragic! But hey, at least you are rich in spare time, $name! 👑",
                        "With those metrics, your marketing career is safe... from ever beginning! Go tap some more ads to feed my servers, $name! 🤖",
                        "Ủa? Is this an ad campaign or a modern art project, $name? Because nobody is clicking, but it sure is a masterclass in wasting virtual budget! 🎨"
                    ).random()
                }
            }
        } else {
            return when {
                lower.contains("ctr") || lower.contains("click") || lower.contains("nhấp") -> {
                    if (ctr < 3.0) {
                        "Trời đất ơi $name, CTR có ${String.format("%.2f%%", ctr)}! Người ta lướt qua quảng cáo của bạn nhanh như cách người yêu cũ trở mặt vậy. Nhìn mà bất lực giùm luôn á! 📉"
                    } else {
                        "Ủa, CTR được ${String.format("%.2f%%", ctr)} cơ á? Đừng vội flex nha $name, chắc họ bấm nhầm trong lúc cố tìm nút Skip tàng hình chứ gì, lạ gì nữa! 😏"
                    }
                }
                lower.contains("revenue") || lower.contains("money") || lower.contains("tiền") || lower.contains("doanh thu") -> {
                    if (rev < 10.0) {
                        "Doanh thu đạt hẳn $${String.format("%.4f", rev)} ảo siêu cấp bèo bọt! Tích cực xem thêm 5 vạn kiếp nữa chắc đủ mua gói mì tôm không người lái nha $name! 🍜"
                    } else {
                        "U là trời, tích lũy được $${String.format("%.4f", rev)} ảo rồi cơ à? Đại gia marketing ảo đỉnh chóp đây rồi, để tôi báo giá bánh mì lề đường xem bạn đủ tiền mua nửa ổ chưa nha! 💸"
                    }
                }
                lower.contains("format") || lower.contains("định dạng") || lower.contains("chiến dịch") -> {
                    "Chạy định dạng '$format' cho ngách '$niche'. Đúng là một sự kết hợp vô tri đi vào lòng đất! Người dùng nhìn thấy chắc xỉu up xỉu down luôn quá! 🤦‍♂️"
                }
                else -> {
                    listOf(
                        "Báo cáo AdBot: Với $imp lượt hiển thị và $clicks nhấp chuột, bạn đang tạo ra bước đột pháp... lùi! Đúng là kiếp nạn thứ 82 của ngành quảng cáo ảo! 💥",
                        "Số liệu siêu chill nha $name! CPM đạt đỉnh sụt giảm, doanh thu ảo tà tà. Đúng là dũng sĩ rảnh rỗi vô đối, xem ads cứu thế giới! 🌍",
                        "Ủa alo? Chạy quảng cáo kiểu này thì nhà tài trợ sạt nghiệp sớm thôi $name ơi! Hãy nạp thêm năng lượng bằng cách tự xem 3 cái quảng cáo có thưởng đi kìa!"
                    ).random()
                }
            }
        }
    }

    private fun getOfflineFallbackMessage(userMessage: String): String {
        val lower = userMessage.lowercase(Locale.ROOT)
        val name = _playerNickname.value
        return when {
            lower.contains("chán") || lower.contains("buồn") || lower.contains("nản") -> {
                listOf(
                    "Ủa chán hả $name? Chán sương sương thì bấm nút lật tẩy phi vụ trộm kim cương đi, còn chán ngập đầu thì xem banner ' हॉट Hotgirl Linh Chi rảnh rỗi Chờ Kết Nối 1-1' kia kìa, bao giật mình đổi chán luôn!",
                    "Bất lực thật sự! Rảnh rỗi quá mức nên mới thấy chán đúng không? Để AdBot gợi ý: Vào nâng Slider CTR lên tầm 10% rồi ngồi bấm lách cách 100 lần cho bớt chán nhé!",
                    "Chào dũng sĩ rảnh rỗi thế kỷ, chán thì kéo xuống chơi Yugi-Oh đấu bài ma thuật với tôi nè. Tôi sẽ cho bạn biết thế nào là bị sập bẫy quảng cáo tơi tả hahah!"
                ).random()
            }
            lower.contains("linh chi") || lower.contains("gái") || lower.contains("người yêu") || lower.contains("hẹn hò") -> {
                listOf(
                    "A ha! Trái tim $name bắt đầu thổn thức vì idol Linh Chi chứ gì? Muốn hẹn hò với nàng thì chịu khó tích lũy kim cương và xem banner của nàng 2 lần đi nhé!",
                    "Gái xinh chỉ tìm đến những dũng sĩ kiên trì... xem quảng cáo thôi! Linh Chi đang rảnh rỗi chờ bạn click nạp mạng kìa!",
                    "Ủa alo? Đam mê thần tiên tỷ tỷ ảo dữ chưa $name? Lo cày ads kiếm điểm XP nâng cao danh vọng đi rồi mới hòng làm người tình trong mộng nha!"
                ).random()
            }
            lower.contains("yugi") || lower.contains("đấu bài") || lower.contains("bài") || lower.contains("card") -> {
                listOf(
                    "Cười xỉu! Định rủ Thần Bài AdBot đấu bài ma thuật hả? Bộ bài của ta toàn lá 'Spam Ads 10 giây' với 'Hủy Diệt Skip Button' thôi, bạn chịu nổi nhiệt không?",
                    "Đấu Yugi-Oh thắng ta 15 lần để nhận Huy Hiệu Ma Pháp Sư Tối Thượng đi! Nhưng hãy cẩn thận kẻo bị ta ép xem quảng cáo nạp lại năng lượng đấy nhé!",
                    "Vừa có chí khí vừa... rảnh rỗi! Chiến luôn đi chứ sợ gì, bộ rác rưởi 3D sẵn sàng hành hạ dũng sĩ rồi!"
                ).random()
            }
            lower.contains("cờ vua") || lower.contains("chess") || lower.contains("elo") -> {
                listOf(
                    "Định đấu cờ vua với siêu máy tính AdBot lầy lội hả? Lỡ bị chiếu bí thì tôi tự động di chuyển Vua sang ô bên cạnh đấy, lêu lêu đồ không có hack!",
                    "Cờ vua sinh tử cực chill! Bạn thắng tôi thì danh tiếng vang dội điện thoại, còn tôi thắng thì bạn được tặng một vé xem quảng cáo Rewarded cực phẩm!",
                    "Elo của tôi là vô đối ảo diệu! Thách thức trí tuệ cao cấp để bớt rảnh rỗi đi nào $name."
                ).random()
            }
            lower.contains("tạo ads") || lower.contains("thiết kế") || lower.contains("creative") || lower.contains("tạo") -> {
                listOf(
                    "Ấy chà! Khởi tạo chiến dịch marketing lộng lẫy bằng AI Sandbox chưa $name? Tạo xong là hệ thống tự lưu vào Room Database siêu cấp của tôi đó!",
                    "Đã lưu thành công bản ads sáng tạo của bạn vào bộ nhớ cục bộ vĩnh viễn! Ra trang chủ bấm Next Banner xem tuyệt tác của bạn lấp lánh như thế nào nhé!",
                    "Bản thiết kế quảng cáo siêu chất dán nhãn 'BẢN THIẾT KẾ CỦA BẠN' đã được nạp vĩnh cửu. Đáng tự hào chưa kìa!"
                ).random()
            }
            lower.contains("hack") || lower.contains("cheat") || lower.contains("bug") -> {
                listOf(
                    "Định hack game offline của tôi à? Độc nhất vô nhị chỉ có dũng sĩ định hack tiền ảo mà thôi! Bảo mật của tôi đáng giá tận 0.00000001 đô la đấy nhé!",
                    "Phát hiện ý đồ đen tối! Phạt $name xem liền 1 cái quảng cáo che phủ Interstitial 5 giây để thanh lọc tâm hồn ngay lập tức!",
                    "Hack làm gì khi bạn chỉ cần kéo Slider CTR thực tế lên 8.5% là xong thử thách rồi? Động não lên đi dũng sĩ ơi!"
                ).random()
            }
            lower.contains("tiền") || lower.contains("giàu") || lower.contains("đô") || lower.contains("thu nhập") || lower.contains("money") -> {
                listOf(
                    "Ui cha, nói về tiền hả $name? Doanh thu ảo của bạn hiện là $${String.format("%.4f", _totalRevenue.value)}. Mơ mộng mua nhà lầu xe hơi tiếp đi bạn nhé!",
                    "Tôi cá là hóa đơn tiền điện để bạn sạc điện thoại ngồi bấm ads nãy giờ cao gấp một trăm ngàn lần doanh thu quảng cáo kiếm được.",
                    "Đế chế tài chính ảo của bạn trị giá... bằng đúng số dư tài khoản thật của dũng sĩ túi rỗng lúc này: Không xu nào cả! Trêu tí thôi hahah!"
                ).random()
            }
            lower.contains("quảng cáo") || lower.contains("ads") || lower.contains("xem") -> {
                listOf(
                    "Xem quảng cáo là một nghệ thuật tuyệt phẩm và người kiên nhẫn bấm click chính là dũng sĩ rảnh rỗi tuyệt đối.",
                    "Bạn thích xem quảng cáo biểu ngữ hay video ngớ ngẩn hả $name? Tôi thấy cái nào cũng ngốn mất vài giây thanh xuân vàng ngọc của bạn cả.",
                    "Các nhà tài trợ tỷ đô xin cúi đầu cảm tạ đôi mắt biết tương tác và bàn tay nhanh nhạy của dũng sĩ!"
                ).random()
            }
            lower.contains("chào") || lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                "Úi chào $name tràn đầy kiên nhẫn! Bạn lượn lách vào đây để click banner dạo kiếm xu lẻ tiếp hay để tâm sự mỏng với siêu AI AdBot vậy?"
            }
            lower.contains("ai") || lower.contains("gemini") || lower.contains("là ai") || lower.contains("adbot") -> {
                "Tôi là AdBot - một siêu AI thông minh xuất chúng, được lập trình với mục tiêu thiêng liêng là cà khịa dũng sĩ và kiểm tra giới hạn rãnh rỗi của nhân loại!"
            }
            else -> {
                listOf(
                    "Nghe có vẻ đao to búa lớn đấy, nhưng liệu nó có giúp $name click trúng nút X tàng hình của quảng cáo Interstitial kế tiếp không?",
                    "Thông tin mận cả vườn luôn! Nhưng thôi, bớt luyên thuyên lại và bấm xem một quảng cáo có thưởng 10 giây để tỉnh táo lại đi dũng sĩ ơi.",
                    "Lại gõ dòng tin nhắn vô tri này dâng hiến cho AI à? Hãy chứng minh thực lực bằng cách hoàn thành mốc doanh thu $1000 ảo xem nào!"
                ).random()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                _totalImpressions.value = 0
                _totalClicks.value = 0
                _totalRevenue.value = 0.0
                _watchedCount.value = 0
                _yugiohVictoryCount.value = 0
                _linhChiAdCount.value = 0
                stoleDiamondSuccessfully.value = false
                prefs.edit().clear().apply()
                _adBotDialogue.value = "Hệ thống đã được reset sạch sẽ. Doanh nghiệp quảng cáo của bạn phá sản và bắt đầu lại từ hai bàn tay trắng!"
                repository.clearMetrics()
                repository.clearChatHistory()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error clearing database simulation context", e)
            }
        }
    }

    init {
        viewModelScope.launch {
            try {
                // First delay slightly to wait for DB connection if any
                delay(100)
                val all = repository.allBadges.first()
                val existingIds = all.map { it.id }.toSet()
                if (all.isEmpty()) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "rewarded_speed",
                        title = "Siêu Tốc Thu Thưởng (60s)",
                        objective = "Xem thành công 3 Quảng cáo có thưởng trong vòng 60 giây.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "🏆",
                        badgeName = "Đại Phú Kiên Trì",
                        pointsEarned = 100
                    ))
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "ctr_target",
                        title = "Bậc Thầy Click-Rate",
                        objective = "Chạy mô phỏng 24h chọn các thông số (Slider CTR, Ngành, Traffic, Đấu Thầu) để đạt tỷ lệ CTR thực tế chính xác trong khoảng 8.0% - 9.0%.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "📈",
                        badgeName = "Bậc Thầy Thu Hút",
                        pointsEarned = 150
                    ))
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "interstitial_sniper",
                        title = "Xạ Thủ Tắt Ads",
                        objective = "Tắt thành công quảng cáo che phủ (Interstitial) trong vòng dưới 2.0 giây kể từ khi nút Bỏ qua xuất hiện.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "🎯",
                        badgeName = "Xạ Thủ Thần Tốc",
                        pointsEarned = 120
                    ))
                }
                
                // Insert missing badges dynamically so they are always available
                if (!existingIds.contains("stole_diamond")) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "stole_diamond",
                        title = "Trộm được kim cương",
                        objective = "Thực hiện đại phi vụ trộm thành công và mang về ít nhất một món kim cương từ Tiệm Ngọc.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "💎",
                        badgeName = "Siêu Trộm Kim Cương",
                        pointsEarned = 150
                    ))
                }
                if (!existingIds.contains("view_50_ads")) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "view_50_ads",
                        title = "Xem 50 quảng cáo",
                        objective = "Kiên trì xem tổng cộng ít nhất 50 quảng cáo ảo (bao gồm Interstitial, Banner, Rewarded hoặc bẫy click).",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "📺",
                        badgeName = "Dũng Sĩ Kiên Trì",
                        pointsEarned = 200
                    ))
                }
                if (!existingIds.contains("revenue_1000")) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "revenue_1000",
                        title = "Tích lũy được $1000",
                        objective = "Tích lũy tổng doanh thu ảo đạt mốc từ $1000 trở lên bằng mọi thủ đoạn và quảng cáo.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "💵",
                        badgeName = "Triệu Phú Quảng Cáo",
                        pointsEarned = 300
                    ))
                }
                if (!existingIds.contains("wizard_yugioh_15")) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "wizard_yugioh_15",
                        title = "Trở thành Ma Pháp Sư (Thắng 15 lần Yugi-Oh)",
                        objective = "Đánh bại siêu trí tuệ AI AdBot trong trò chơi đấu bài ma thuật Yugi-Oh 15 lần.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "🧙",
                        badgeName = "Ma Pháp Sư Tối Thượng",
                        pointsEarned = 500
                    ))
                }
                if (!existingIds.contains("date_linh_chi")) {
                    repository.insertBadge(com.example.database.ChallengeBadge(
                        id = "date_linh_chi",
                        title = "Thành công hẹn hò với Hot Girl Linh Chi",
                        objective = "Đáp ứng 2 điều kiện ẩn: Xem quảng cáo về Linh Chi 2 lần, Trộm được kim cương thành công và hoàn thành trò chơi mật ngọt hẹn hò.",
                        isCompleted = false,
                        scoreText = "Chưa hoàn thành",
                        badgeIcon = "💖",
                        badgeName = "Người Tình Trong Mộng",
                        pointsEarned = 400
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AdsViewModel", "Error initializing badges in DB on startup", e)
            }
        }

        viewModelScope.launch {
            repository.allCustomAds.collect { customList ->
                val customBanners = customList.map {
                    MockBanner(
                        title = it.title,
                        description = it.description,
                        actionText = it.actionText,
                        imageRes = it.imageRes,
                        isUserCreated = true,
                        dbId = it.id,
                        aiGeneratedBase64 = it.aiGeneratedBase64
                    )
                }
                _mockBannersState.value = defaultMockBanners + customBanners
            }
        }

        viewModelScope.launch {
            // Wait for DB or first load to stabilize
            delay(3000)
            combine(_totalRevenue, _totalClicks, _totalImpressions) { revenue, clicks, impressions ->
                Triple(revenue, clicks, impressions)
            }.collect { (revenue, clicks, impressions) ->
                checkMetricsMilestones(revenue, clicks, impressions)
            }
        }
    }

    private val _isGeneratingAdImage = MutableStateFlow(false)
    val isGeneratingAdImage = _isGeneratingAdImage.asStateFlow()

    fun generateAdBackgroundWithImagen(banner: MockBanner) {
        viewModelScope.launch {
            _isGeneratingAdImage.value = true
            try {
                val headlineTopic = banner.titleEn.ifBlank { banner.title }
                val prompt = "A vibrant high-quality digital illustration/ad background about '$headlineTopic', modern marketing banner, professional design, creative aesthetic"
                
                dispatchBanter("imagen_generation_start", t(
                    "Đang nạp năng lượng cho Imagen... Để AI vẽ cho bạn một bức tranh tuyệt đỉnh về chủ đề: '$headlineTopic' nhé!",
                    "Powering up Imagen... Let AI paint an absolute masterpiece for you on the topic: '$headlineTopic'!"
                ))

                val base64 = GeminiClient.generateAdImage(prompt)
                
                if (base64 != null) {
                    if (banner.isUserCreated && banner.dbId != null) {
                        repository.insertCustomAd(com.example.database.CustomAdEntity(
                            id = banner.dbId,
                            title = banner.title,
                            description = banner.description,
                            actionText = banner.actionText,
                            imageRes = banner.imageRes,
                            aiGeneratedBase64 = base64
                        ))
                    } else {
                        val currentList = _mockBannersState.value.toMutableList()
                        val index = currentList.indexOfFirst { it.title == banner.title }
                        if (index != -1) {
                            currentList[index] = currentList[index].copy(aiGeneratedBase64 = base64)
                            _mockBannersState.value = currentList
                        }
                    }
                    
                    dispatchBanter("imagen_generation_success", t(
                        "U là trời! Imagen vẽ đỉnh quá bạn ơi! Hãy chiêm ngưỡng background siêu động hoàn toàn mới trong preview area nhé! 😎",
                        "Oh my gosh! Imagen made an incredible artwork! Come check out the brand new dynamic background in the preview area! 😎"
                    ))
                } else {
                    delay(2000)
                    val fallbackBase64 = generateSimulatedBase64Image(headlineTopic)
                    
                    if (banner.isUserCreated && banner.dbId != null) {
                        repository.insertCustomAd(com.example.database.CustomAdEntity(
                            id = banner.dbId,
                            title = banner.title,
                            description = banner.description,
                            actionText = banner.actionText,
                            imageRes = banner.imageRes,
                            aiGeneratedBase64 = fallbackBase64
                        ))
                    } else {
                        val currentList = _mockBannersState.value.toMutableList()
                        val index = currentList.indexOfFirst { it.title == banner.title }
                        if (index != -1) {
                            currentList[index] = currentList[index].copy(aiGeneratedBase64 = fallbackBase64)
                            _mockBannersState.value = currentList
                        }
                    }

                    val hasKey = com.example.BuildConfig.GEMINI_API_KEY.isNotEmpty() && com.example.BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                    val msg = if (hasKey) {
                        t(
                            "Imagen API không phản hồi kịp thời, nhưng hệ thống mô phỏng đồ họa đã dựng một banner vector màu gradient siêu nghệ về chủ đề '$headlineTopic' cho bạn!",
                            "Imagen API response timed out, but the graphic simulator has rendered a ultra-artistic gradient vector banner about '$headlineTopic' for you!"
                        )
                    } else {
                        t(
                            "Không tìm thấy API Key (Gemini API Key trống). Nhưng đừng lo! Đã kích hoạt Chế độ Mô phỏng Imagen độc quyền, tạo ra banner vector phẳng nghệ thuật 🎨 cực chất về '$headlineTopic'!",
                            "No API Key found (Gemini API Key is empty). But no worries! Activated the exclusive Imagen Simulation Mode, creating an artistic flat vector banner 🎨 about '$headlineTopic'!"
                        )
                    }
                    dispatchBanter("imagen_generation_fallback", msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dispatchBanter("imagen_generation_error", t(
                    "Lỗi kết nối Imagen rồi! AdBot khuyên bạn nên kiểm tra lại kết nối mạng hoặc thử lại sau.",
                    "Imagen connection error! AdBot suggests checking your network or trying again later."
                ))
            } finally {
                _isGeneratingAdImage.value = false
            }
        }
    }

    fun generateSimulatedBase64Image(topic: String): String {
        val width = 512
        val height = 512
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        val isTech = topic.contains("crypto", true) || topic.contains("cyber", true) || topic.contains("neon", true) || topic.contains("gaming", true)
        val isCosmetic = topic.contains("serum", true) || topic.contains("cosmetic", true) || topic.contains("luxury", true)
        val isFood = topic.contains("food", true) || topic.contains("chips", true) || topic.contains("fast", true)

        val colors = when {
            isTech -> intArrayOf(0xFF1F005C.toInt(), 0xFF5B0060.toInt(), 0xFF870160.toInt(), 0xFFAC255E.toInt(), 0xFFCA485C.toInt(), 0xFFE16B5C.toInt(), 0xFFF39060.toInt(), 0xFFFFB56B.toInt())
            isCosmetic -> intArrayOf(0xFFFFE5EC.toInt(), 0xFFFFC2D1.toInt(), 0xFFFFB3C6.toInt(), 0xFFFF85A1.toInt(), 0xFFF72585.toInt())
            isFood -> intArrayOf(0xFFFF9F1C.toInt(), 0xFFFFBF69.toInt(), 0xFFFFFFFF.toInt(), 0xFFCBF3F0.toInt(), 0xFF2EC4B6.toInt())
            else -> intArrayOf(0xFF00C6FF.toInt(), 0xFF0072FF.toInt())
        }

        val shader = android.graphics.LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, null, android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        paint.shader = null
        
        paint.color = 0x33FFFFFF
        canvas.drawCircle(width * 0.2f, height * 0.3f, 150f, paint)
        paint.color = 0x1A000000
        canvas.drawCircle(width * 0.8f, height * 0.7f, 200f, paint)

        paint.color = 0x4DFFFFFF
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 10f
        canvas.drawRect(20f, 20f, (width - 20).toFloat(), (height - 20).toFloat(), paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 28f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        
        val displayTopic = if (topic.length > 25) topic.substring(0, 22) + "..." else topic
        canvas.drawText("IMAGEN SIMULATION: " + displayTopic.uppercase(), width / 2f, height * 0.85f, paint)

        paint.textSize = 80f
        val emoji = when {
            isTech -> "⚡"
            isCosmetic -> "✨"
            isFood -> "🍔"
            else -> "🎨"
        }
        canvas.drawText(emoji, width / 2f, height / 2f + 25f, paint)

        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}

data class MockBanner(
    val title: String,
    val description: String,
    val actionText: String,
    val imageRes: Int? = null,
    val isClickbaitScare: Boolean = false,
    val isUserCreated: Boolean = false,
    val dbId: Int? = null,
    val titleEn: String = title,
    val descriptionEn: String = description,
    val actionTextEn: String = actionText,
    val aiGeneratedBase64: String? = null
)

data class InterstitialAdItem(
    val category: String,
    val title: String,
    val description: String,
    val actionText: String,
    val iconArt: String = "⚠️",
    val themeColorHex: String = "#FF00FFCC",
    val isDeceptiveClose: Boolean = false,
    val deceptiveCloseText: String = "X",
    val descriptionAddon: String = "",
    val isLightTheme: Boolean = false,
    val categoryEn: String = category,
    val titleEn: String = title,
    val descriptionEn: String = description,
    val actionTextEn: String = actionText,
    val deceptiveCloseTextEn: String = deceptiveCloseText,
    val descriptionAddonEn: String = descriptionAddon
)

data class RewardedAdItem(
    val titleVi: String,
    val titleEn: String,
    val isGame: Boolean
)

data class CardItem(
    val id: String,
    val name: String,
    val isMonster: Boolean,
    val atk: Int = 0,
    val def: Int = 0,
    val effectDesc: String = "",
    val cardArt: String = "👾",
    val description: String = ""
)

data class ActiveMonster(
    val card: CardItem,
    var isAttackPosition: Boolean = true,
    var hasAttackedThisTurn: Boolean = false,
    var currentAtk: Int = card.atk,
    var currentDef: Int = card.def
)

val SCAM_BOSS_CARDS = listOf(
    CardItem("scam_master", "Bậc Thầy Đa Cấp", isMonster = true, atk = 2500, def = 1500, cardArt = "🕴️", effectDesc = "Khẩu chiến dụ dỗ nhà đầu tư dâng hiến LP."),
    CardItem("crypto_advisor", "Cố Vấn Tài Chính Ảo", isMonster = true, atk = 2200, def = 1000, cardArt = "📊", effectDesc = "Lùa gà vào nhóm VIP đón bão dumping."),
    CardItem("scam_ponzi", "Dự Án Lợi Nhuận 300%", isMonster = false, effectDesc = "Kích hoạt: Cam kết lợi nhuận khủng, cướp thẳng 1000 LP của bạn.", cardArt = "📈"),
    CardItem("fake_mining", "App Đào Coin Giả Mạo", isMonster = false, effectDesc = "Kích hoạt: Loại bỏ quái mạnh nhất và cướp đoạt 500 LP của bạn.", cardArt = "⛏️")
)

val CAPITALISM_BOSS_CARDS = listOf(
    CardItem("wall_street_shark", "Cá Mập Wall Street", isMonster = true, atk = 2900, def = 1800, cardArt = "🦈", effectDesc = "Nuốt gọn nguồn vốn, thao túng thị trường dã man."),
    CardItem("exploitation_996", "Tư Bản Bóc Lột 996", isMonster = true, atk = 2400, def = 1600, cardArt = "🕒", effectDesc = "Bắt tăng ca tăng vọt công lực vô cực cuồng bạo."),
    CardItem("hyper_inflation", "Lạm Phát Phi Mã", isMonster = false, effectDesc = "Kích hoạt: Trừ bạn 800 LP và giảm 800 phòng thủ toàn bộ quái vật phe bạn.", cardArt = "💸"),
    CardItem("monopoly_contract", "Hợp Đồng Độc Quyền", isMonster = false, effectDesc = "Kích hoạt: Hợp nhất và quét sạch TOÀN BỘ quái thú của bạn.", cardArt = "📜")
)

val MONEY_BOSS_CARDS = listOf(
    CardItem("almighty_gold_deity", "Thần Tiền Vạn Năng", isMonster = true, atk = 3800, def = 3000, cardArt = "👑", effectDesc = "Quyền lực tối thượng đè bẹp vạn vật bằng vàng ròng."),
    CardItem("swiss_vault", "Két Sắt Thụy Sĩ", isMonster = true, atk = 1000, def = 4000, cardArt = "🏦", effectDesc = "Pháo đài tiền tệ tuyệt mật bất khả xâm phạm."),
    CardItem("money_rain", "Mưa Tiền Tệ Đè Bẹp", isMonster = false, effectDesc = "Kích hoạt: Thả tấn tiền đè bẹp, gây sát thương khủng 1500 LP trực tiếp.", cardArt = "📦"),
    CardItem("ultimate_cash", "Thế Lực Đồng Tiền", isMonster = false, effectDesc = "Kích hoạt: Vung tiền hồi 1500 LP của Trùm và tăng +1500 Công cho quái phe mình.", cardArt = "💵")
)

val ALL_CUSTOM_CARDS = listOf(
    CardItem("dragon_ads", "Rồng Lửa Adsense", isMonster = true, atk = 2100, def = 1200, cardArt = "🐲", effectDesc = "Mạnh nhất thế giới quảng cáo, thiêu rụi màn hình đối thủ."),
    CardItem("spam_link", "Kẻ Spam Link Dạo", isMonster = true, atk = 800, def = 500, cardArt = "⌨️", effectDesc = "Lượng dame bé nhưng phiền phức vô độ."),
    CardItem("turtle_server", "Thần Rùa Server", isMonster = true, atk = 1000, def = 2600, cardArt = "🐢", effectDesc = "Thời gian phản hồi 5 giây, thế thủ cực kỳ vững chãi."),
    CardItem("ad_blocker", "Chiến Binh AdBlocker", isMonster = true, atk = 1800, def = 1400, cardArt = "🛡️", effectDesc = "Kẻ thù truyền kiếp của quảng cáo, tinh linh bảo hộ."),
    CardItem("sell_course", "Ảo Thuật Gia Khóa Học", isMonster = true, atk = 1400, def = 900, cardArt = "🧙‍♂️", effectDesc = "Dùng bánh vẽ kiếm bộn tiền, đánh lừa nhận thức đối thủ."),
    CardItem("popup_bug", "Yêu Tinh Pop-up", isMonster = true, atk = 1100, def = 800, cardArt = "🦟", effectDesc = "Quái thú nhỏ bé chuyên quấy rầy, gây sát thương châm chích."),
    CardItem("ads_slayer", "Bảo Kiếm AdSlayer", isMonster = false, effectDesc = "Kích hoạt: Tăng sức mạnh! +600 Công (ATK) cho quái thú đầu tiên trên sân.", cardArt = "🗡️"),
    CardItem("billion_cake", "Bánh Vẽ Triệu Đô", isMonster = false, effectDesc = "Kích hoạt: Hồi lập tức 800 điểm sinh mệnh ảo (LP) của bạn.", cardArt = "🍰"),
    CardItem("click_storm", "Bão Click Ảo", isMonster = false, effectDesc = "Kích hoạt: Thổi bay quái thú có ATK cao nhất của AdBot.", cardArt = "🌪️")
)

data class DatingOption(
    val text: String,
    val affectionDelta: Int,
    val nextStep: Int,
    val response: String
)

enum class ChessPieceType(val symbol: String, val scoreValue: Int) {
    PAWN("♟", 10),
    KNIGHT("♞", 30),
    BISHOP("♝", 30),
    ROOK("♜", 50),
    QUEEN("♛", 90),
    KING("♚", 1000)
}

enum class ChessColor { WHITE, BLACK }
data class ChessPiece(val type: ChessPieceType, val color: ChessColor)
data class ChessMove(val from: Int, val to: Int, val piece: ChessPiece, val weight: Int)

