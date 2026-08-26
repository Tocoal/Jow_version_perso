package com.perso.jow.app.data.repository

import com.perso.jow.app.data.db.AppDatabase
import com.perso.jow.app.data.db.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(db: AppDatabase) {
    private val historyDao = db.historyDao()

    fun observeHistory(): Flow<List<HistoryEntryEntity>> = historyDao.observeAll()
}
