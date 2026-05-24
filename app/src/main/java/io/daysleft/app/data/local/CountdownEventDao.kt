package io.daysleft.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownEventDao {
    // 使用 Flow 暴露数据，Compose UI 可以直接收集(collect)这些状态并实现响应式刷新
    @Query("SELECT * FROM countdown_events ORDER BY targetDate ASC")
    fun getAllEvents(): Flow<List<CountdownEventEntity>>

    @Query("SELECT * FROM countdown_events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): CountdownEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CountdownEventEntity): Long

    @Update
    suspend fun updateEvent(event: CountdownEventEntity)

    @Delete
    suspend fun deleteEvent(event: CountdownEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CountdownEventEntity>)
}