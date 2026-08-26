package com.perso.jow.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping_sessions WHERE status = :status ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestSessionByStatus(status: ShoppingSessionStatus): Flow<ShoppingSessionEntity?>

    @Query("SELECT * FROM shopping_sessions WHERE status = :status ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSessionByStatus(status: ShoppingSessionStatus): ShoppingSessionEntity?

    @Query("SELECT * FROM shopping_sessions WHERE id = :id")
    suspend fun getSession(id: Long): ShoppingSessionEntity?

    @Insert
    suspend fun insertSession(session: ShoppingSessionEntity): Long

    @Update
    suspend fun updateSession(session: ShoppingSessionEntity)

    @Insert
    suspend fun insertSessionRecipes(items: List<ShoppingSessionRecipeEntity>)

    @Query("DELETE FROM shopping_session_recipes WHERE sessionId = :sessionId")
    suspend fun clearSessionRecipes(sessionId: Long)

    @Transaction
    @Query("SELECT * FROM shopping_session_recipes WHERE sessionId = :sessionId")
    fun observeSessionRecipes(sessionId: Long): Flow<List<ShoppingSessionRecipeDetail>>

    @Query("SELECT * FROM shopping_session_recipes WHERE sessionId = :sessionId")
    suspend fun getSessionRecipes(sessionId: Long): List<ShoppingSessionRecipeEntity>

    @Update
    suspend fun updateSessionRecipe(item: ShoppingSessionRecipeEntity)

    @Insert
    suspend fun insertListItems(items: List<ShoppingListItemEntity>)

    @Query("DELETE FROM shopping_list_items WHERE sessionId = :sessionId")
    suspend fun clearListItems(sessionId: Long)

    @Transaction
    @Query("SELECT * FROM shopping_list_items WHERE sessionId = :sessionId ORDER BY id")
    fun observeListItems(sessionId: Long): Flow<List<ShoppingListItemDetail>>

    @Query("SELECT * FROM shopping_list_items WHERE sessionId = :sessionId ORDER BY id")
    suspend fun getListItems(sessionId: Long): List<ShoppingListItemEntity>

    @Update
    suspend fun updateListItem(item: ShoppingListItemEntity)
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history_entries ORDER BY cookedAt DESC")
    fun observeAll(): Flow<List<HistoryEntryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntryEntity)
}
