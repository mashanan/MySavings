package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
}
