package com.hanan.mysavings.ui.components

import com.hanan.mysavings.data.local.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryNamesTest {

    @Test
    fun `every built-in category has a translatable name`() {
        DefaultCategories.all.forEach { category ->
            assertNotNull("No string resource for '${category.iconKey}'", defaultCategoryNameRes(category.iconKey))
        }
    }

    @Test
    fun `built-in categories map to distinct names`() {
        val names = DefaultCategories.all.map { defaultCategoryNameRes(it.iconKey) }

        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `unknown keys have no built-in name, so a user's own name is shown`() {
        assertNull(defaultCategoryNameRes("custom-pets"))
    }
}
