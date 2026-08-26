package com.perso.jow.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cooked-recipe record. recipeId is a soft reference (no foreign key / cascade):
 * history must survive a recipe being edited or deleted later, so the name and
 * servings are snapshotted at cooking time.
 */
@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val recipeName: String,
    val servings: Int,
    val cookedAt: Long = System.currentTimeMillis()
)
