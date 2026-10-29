package com.hanan.strukku.domain.usecase

import com.hanan.strukku.domain.model.Category
import com.hanan.strukku.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** All categories, for pickers such as the category dropdown on the Confirm screen. */
class ObserveCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> = categoryRepository.observeAll()
}
