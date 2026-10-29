package com.hanan.mysavings.domain.parser

import com.hanan.mysavings.domain.model.Category

/**
 * Guesses a category by matching each category's keywords against the receipt.
 *
 * Keywords match whole words only, so "tol" (toll road) does not match "toleransi" and
 * "lab" does not match "label". A hit in the store name counts more than a hit anywhere
 * else on the receipt, because item names are noisier than the store name.
 */
object CategoryClassifier {

    private const val MERCHANT_WEIGHT = 3
    private const val BODY_WEIGHT = 1

    /** @return the best match, or `null` when no keyword matched at all. Ties go to the earlier category. */
    fun classify(merchant: String?, rows: List<String>, categories: List<Category>): Category? {
        val merchantText = normalize(merchant.orEmpty())
        val bodyText = normalize(rows.joinToString(" "))

        return categories
            .map { category ->
                val score = category.keywords.sumOf { keyword ->
                    val term = normalize(keyword)
                    if (term.isBlank()) return@sumOf 0
                    (if (term in merchantText) MERCHANT_WEIGHT else 0) + (if (term in bodyText) BODY_WEIGHT else 0)
                }
                category to score
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    /** Lowercase words separated by single spaces, padded so " term " matches whole words only. */
    private fun normalize(text: String): String =
        " " + text.lowercase().replace(Regex("""[^a-z0-9]+"""), " ").trim() + " "
}
