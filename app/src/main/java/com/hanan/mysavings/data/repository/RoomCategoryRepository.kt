package com.hanan.mysavings.data.repository

import com.hanan.mysavings.data.local.dao.CategoryDao
import com.hanan.mysavings.domain.model.Category
import com.hanan.mysavings.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomCategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }
}
