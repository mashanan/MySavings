package com.hanan.mysavings.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.hanan.mysavings.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id")
    fun observeAll(): Flow<List<CategoryEntity>>
}
