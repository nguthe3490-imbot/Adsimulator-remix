package com.example.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Entity(tableName = "simulation_metrics")
data class SimulationMetric(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val impressions: Int,
    val clicks: Int,
    val ctr: Double,
    val cpm: Double,
    val revenue: Double,
    val description: String
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "adbot"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isTease: Boolean = false
)

@Entity(tableName = "challenge_badges")
data class ChallengeBadge(
    @PrimaryKey val id: String, // "rewarded_speed", "ctr_target", "interstitial_sniper"
    val title: String,
    val objective: String,
    var isCompleted: Boolean = false,
    var scoreText: String = "",
    val badgeIcon: String = "🏅",
    val badgeName: String = "",
    var pointsEarned: Int = 0
)

@Entity(tableName = "custom_ads")
data class CustomAdEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val actionText: String,
    val imageRes: Int?,
    val timestamp: Long = System.currentTimeMillis(),
    val aiGeneratedBase64: String? = null
)

@Dao
interface AdDao {
    @Query("SELECT * FROM simulation_metrics ORDER BY timestamp DESC")
    fun getAllMetrics(): Flow<List<SimulationMetric>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: SimulationMetric)

    @Query("DELETE FROM simulation_metrics")
    suspend fun clearMetrics()

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    @Query("SELECT * FROM challenge_badges")
    fun getAllBadges(): Flow<List<ChallengeBadge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: ChallengeBadge)

    @Query("DELETE FROM challenge_badges")
    suspend fun clearBadges()

    @Query("SELECT * FROM custom_ads ORDER BY timestamp DESC")
    fun getAllCustomAds(): Flow<List<CustomAdEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomAd(ad: CustomAdEntity)

    @Query("DELETE FROM custom_ads WHERE id = :id")
    suspend fun deleteCustomAdById(id: Int)

    @Query("DELETE FROM custom_ads")
    suspend fun clearCustomAds()
}

@Database(entities = [SimulationMetric::class, ChatMessage::class, ChallengeBadge::class, CustomAdEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adDao(): AdDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ads_simulator_database"
                    ).fallbackToDestructiveMigration().build()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    ).fallbackToDestructiveMigration().build()
                }
                INSTANCE = instance
                instance
            }
        }
    }
}

class AdRepository(private val adDao: AdDao) {
    val allMetrics: Flow<List<SimulationMetric>> = adDao.getAllMetrics()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
    val chatHistory: Flow<List<ChatMessage>> = adDao.getAllChatMessages()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
    val allBadges: Flow<List<ChallengeBadge>> = adDao.getAllBadges()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }
    val allCustomAds: Flow<List<CustomAdEntity>> = adDao.getAllCustomAds()
        .catch { e ->
            e.printStackTrace()
            emit(emptyList())
        }

    suspend fun insertMetric(metric: SimulationMetric) = adDao.insertMetric(metric)
    suspend fun clearMetrics() = adDao.clearMetrics()

    suspend fun insertChatMessage(chatMessage: ChatMessage) = adDao.insertChatMessage(chatMessage)
    suspend fun clearChatHistory() = adDao.clearChatHistory()

    suspend fun insertBadge(badge: ChallengeBadge) = adDao.insertBadge(badge)
    suspend fun clearBadges() = adDao.clearBadges()

    suspend fun insertCustomAd(ad: CustomAdEntity) = adDao.insertCustomAd(ad)
    suspend fun deleteCustomAdById(id: Int) = adDao.deleteCustomAdById(id)
    suspend fun clearCustomAds() = adDao.clearCustomAds()
}
