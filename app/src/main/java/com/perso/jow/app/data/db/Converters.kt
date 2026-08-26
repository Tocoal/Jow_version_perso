package com.perso.jow.app.data.db

import androidx.room.TypeConverter

/** Unit separator, extremely unlikely to appear in a hand-typed recipe step. */
private const val STEP_SEPARATOR = ""

class Converters {

    @TypeConverter
    fun fromStepsList(steps: List<String>): String = steps.joinToString(STEP_SEPARATOR)

    @TypeConverter
    fun toStepsList(data: String): List<String> = if (data.isEmpty()) emptyList() else data.split(STEP_SEPARATOR)

    @TypeConverter
    fun fromStatus(status: ShoppingSessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ShoppingSessionStatus = ShoppingSessionStatus.valueOf(value)
}
