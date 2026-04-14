package com.cylonid.nativealpha.links

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Link history database entity
 */
@Entity(tableName = "link_history")
data class LinkHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appId: Long,
    val url: String,
    val pageTitle: String,
    val action: String, // "copy", "share", "open", etc.
    val format: String,
    val timestamp: Long,
    val referrer: String? = null
)

/**
 * Link history DAO
 */
@Dao
interface LinkHistoryDao {
    @Insert
    suspend fun insertHistory(history: LinkHistoryEntity)

    @Query("SELECT * FROM link_history WHERE appId = :appId ORDER BY timestamp DESC")
    fun getHistoryFlow(appId: Long): Flow<List<LinkHistoryEntity>>

    @Query("SELECT * FROM link_history WHERE appId = :appId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getHistoryRange(appId: Long, startTime: Long, endTime: Long): List<LinkHistoryEntity>

    @Query("DELETE FROM link_history WHERE appId = :appId AND timestamp < :beforeTime")
    suspend fun deleteOlderThan(appId: Long, beforeTime: Long)

    @Query("DELETE FROM link_history WHERE appId = :appId")
    suspend fun clearAppHistory(appId: Long)

    @Query("SELECT DISTINCT pageTitle FROM link_history WHERE appId = :appId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentPageTitles(appId: Long, limit: Int = 10): List<String>

    @Query("SELECT url, COUNT(*) as count FROM link_history WHERE appId = :appId GROUP BY url ORDER BY count DESC LIMIT :limit")
    suspend fun getMostFrequentLinks(appId: Long, limit: Int = 10): List<LinkFrequency>

    @Query("SELECT action, COUNT(*) as count FROM link_history WHERE appId = :appId GROUP BY action")
    suspend fun getActionStats(appId: Long): List<ActionStatistic>
}

data class LinkFrequency(
    val url: String,
    val count: Int
)

data class ActionStatistic(
    val action: String,
    val count: Int
)

/**
 * Link history database
 */
@Database(
    entities = [LinkHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LinkHistoryDatabase : RoomDatabase() {
    abstract fun linkHistoryDao(): LinkHistoryDao

    companion object {
        @Volatile
        private var instance: LinkHistoryDatabase? = null

        fun getInstance(context: Context): LinkHistoryDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    LinkHistoryDatabase::class.java,
                    "link_history.db"
                ).build().also { instance = it }
            }
        }
    }
}

/**
 * Link history tracker
 */
class LinkHistoryTracker(
    private val context: Context,
    private val appId: Long
) {
    private val db = LinkHistoryDatabase.getInstance(context)
    private val dao = db.linkHistoryDao()

    suspend fun recordAction(
        url: String,
        pageTitle: String,
        action: String,
        format: String = "PLAIN_URL",
        referrer: String? = null
    ) {
        val history = LinkHistoryEntity(
            appId = appId,
            url = url,
            pageTitle = pageTitle,
            action = action,
            format = format,
            timestamp = System.currentTimeMillis(),
            referrer = referrer
        )
        dao.insertHistory(history)
    }

    fun getHistoryFlow(): Flow<List<LinkHistoryEntity>> {
        return dao.getHistoryFlow(appId)
    }

    suspend fun getHistoryRange(startTime: Long, endTime: Long): List<LinkHistoryEntity> {
        return dao.getHistoryRange(appId, startTime, endTime)
    }

    suspend fun getMostFrequentLinks(limit: Int = 10): List<LinkFrequency> {
        return dao.getMostFrequentLinks(appId, limit)
    }

    suspend fun getRecentPageTitles(limit: Int = 10): List<String> {
        return dao.getRecentPageTitles(appId, limit)
    }

    suspend fun getActionStatistics(): List<ActionStatistic> {
        return dao.getActionStats(appId)
    }

    suspend fun clearOlderThan(daysBefore: Int) {
        val timeThreshold = System.currentTimeMillis() - (daysBefore * 24 * 60 * 60 * 1000L)
        dao.deleteOlderThan(appId, timeThreshold)
    }

    suspend fun clearAll() {
        dao.clearAppHistory(appId)
    }

    suspend fun generateReport(): LinkHisticsReport {
        val history = dao.getHistoryFlow(appId)
        val actions = dao.getActionStats(appId)
        val frequent = dao.getMostFrequentLinks(appId)
        val recent = dao.getRecentPageTitles(appId)

        return LinkHisticsReport(
            totalActions = actions.sumOf { it.count },
            actionBreakdown = actions.associate { it.action to it.count },
            mostFrequentLinks = frequent,
            recentPages = recent,
            generatedAt = System.currentTimeMillis()
        )
    }
}

data class LinkHisticsReport(
    val totalActions: Int,
    val actionBreakdown: Map<String, Int>,
    val mostFrequentLinks: List<LinkFrequency>,
    val recentPages: List<String>,
    val generatedAt: Long
)
