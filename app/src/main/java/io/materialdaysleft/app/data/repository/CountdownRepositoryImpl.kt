package io.materialdaysleft.app.data.repository

import io.materialdaysleft.app.data.local.CountdownEventDao
import io.materialdaysleft.app.data.local.CountdownEventEntity
import io.materialdaysleft.app.domain.repository.CountdownRepository
import kotlinx.coroutines.flow.Flow

/**
 * 倒数日数据仓库的具体实现类。
 * 负责调度底层 DAO，向上层（ViewModel）屏蔽具体的数据存储细节。
 * * @param dao 通过构造函数注入的 Room DAO 实例
 */
class CountdownRepositoryImpl(
    private val dao: CountdownEventDao
) : CountdownRepository {

    override fun getAllEvents(): Flow<List<CountdownEventEntity>> {
        // 直接返回 DAO 提供的 Flow 流，Compose 侧 collect 后即可实现 UI 自动刷新
        return dao.getAllEvents()
    }

    override suspend fun getEventById(id: Long): CountdownEventEntity? {
        // 挂起函数，在后台协程中执行查询
        return dao.getEventById(id)
    }

    override suspend fun insertEvent(event: CountdownEventEntity): Long {
        // 挂起函数，执行插入并返回生成的 ID
        return dao.insertEvent(event)
    }

    override suspend fun updateEvent(event: CountdownEventEntity) {
        // 挂起函数，执行更新
        dao.updateEvent(event)
    }

    override suspend fun deleteEvent(event: CountdownEventEntity) {
        // 挂起函数，执行删除
        dao.deleteEvent(event)
    }

    override suspend fun insertEvents(events: List<CountdownEventEntity>){
        dao.insertEvents(events)
    }
}