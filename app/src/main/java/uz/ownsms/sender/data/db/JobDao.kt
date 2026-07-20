package uz.ownsms.sender.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(jobs: List<JobEntity>)

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: Long): JobEntity?

    @Query("SELECT * FROM jobs WHERE state = :state ORDER BY id")
    suspend fun byState(state: String): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE state = :state AND reported = 0 ORDER BY id")
    suspend fun unreported(state: String): List<JobEntity>

    // Jobs that successfully left the SIM (sent OR already delivered) but haven't reported "sent" yet.
    // The server only accepts delivered from the "sent" state, so "sent" must be reported first even
    // for a job that raced straight to delivered before the reporter's tick.
    @Query("SELECT * FROM jobs WHERE sentReported = 0 AND state IN ('sent', 'delivered') ORDER BY id")
    suspend fun needsSentReport(): List<JobEntity>

    @Query("UPDATE jobs SET reported = 1 WHERE id = :id")
    suspend fun markReported(id: Long)

    // Only fully-reported rows are prunable; unreported terminal/awaiting-delivery rows are kept
    // so a delivered/sent report is never dropped before it reaches the server.
    @Query("DELETE FROM jobs WHERE reported = 1 AND id NOT IN (SELECT id FROM jobs ORDER BY id DESC LIMIT :keep)")
    suspend fun prune(keep: Int)

    @Query("SELECT * FROM jobs ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<JobEntity>

    @Query("SELECT * FROM jobs ORDER BY id DESC LIMIT :limit")
    fun recentFlow(limit: Int): Flow<List<JobEntity>>

    @Query("UPDATE jobs SET state = :state, errorCode = :errorCode WHERE id = :id")
    suspend fun setState(id: Long, state: String, errorCode: String? = null)

    @Query("UPDATE jobs SET sentReported = :reported WHERE id = :id")
    suspend fun setSentReported(id: Long, reported: Boolean)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun count(): Int
}
