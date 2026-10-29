package com.hanan.strukku.domain.repository

import com.hanan.strukku.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}
