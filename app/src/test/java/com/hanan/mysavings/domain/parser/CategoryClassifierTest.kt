package com.hanan.mysavings.domain.parser

import com.hanan.mysavings.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryClassifierTest {

    private val food = category(1, "Makan & Minum", "warung", "kopi", "rumah makan")
    private val groceries = category(2, "Belanja Harian", "indomaret", "alfamart")
    private val transport = category(3, "Transportasi", "pertamina", "tol", "e-toll")
    private val health = category(6, "Kesehatan", "apotek", "kimia farma")
    private val other = category(7, "Lainnya")
    private val categories = listOf(food, groceries, transport, health, other)

    private fun classify(merchant: String?, vararg rows: String) =
        CategoryClassifier.classify(merchant, rows.toList(), categories)

    @Test
    fun `merchant keyword picks the category`() {
        assertEquals(groceries, classify("Indomaret", "INDOMARET", "TOTAL 22.500"))
        assertEquals(transport, classify("Pertamina", "PERTAMINA", "Pertalite"))
    }

    @Test
    fun `matching is case and punctuation insensitive`() {
        assertEquals(health, classify("KIMIA-FARMA", "KIMIA-FARMA"))
        assertEquals(transport, classify(null, "Isi saldo E-TOLL 100.000"))
    }

    @Test
    fun `multi-word keywords match`() {
        assertEquals(food, classify("Rumah Makan Padang", "RUMAH MAKAN PADANG"))
    }

    @Test
    fun `keywords only match whole words`() {
        // "tol" must not match inside "toleransi", "kopi" not inside "kopiah".
        assertNull(classify("Toko Toleransi", "TOKO TOLERANSI", "KOPIAH HITAM 1 35.000"))
    }

    @Test
    fun `a keyword in the store name beats a keyword among the items`() {
        // Pharmacy that also sells instant coffee: the store name decides.
        assertEquals(health, classify("Apotek Sehat", "APOTEK SEHAT", "KOPI SACHET 2 3.000"))
    }

    @Test
    fun `body keyword is used when the store name is unknown`() {
        assertEquals(food, classify(null, "Kopi Susu 1 18.000"))
    }

    @Test
    fun `no keyword match returns null so the caller can fall back`() {
        assertNull(classify("Toko Sumber Rejeki", "TOKO SUMBER REJEKI", "PAKU 1KG 25.000"))
    }

    private fun category(id: Long, name: String, vararg keywords: String) =
        Category(id, name, "icon$id", "#000000", keywords.toList(), isDefault = true)
}
