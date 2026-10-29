package com.hanan.strukku.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportLayoutTest {

    @Test
    fun `no transactions still produce one page`() {
        assertEquals(listOf(0 until 0), ReportLayout.paginate(count = 0, firstCapacity = 20, otherCapacity = 40))
    }

    @Test
    fun `rows that exactly fill page one do not create an empty second page`() {
        assertEquals(listOf(0 until 20), ReportLayout.paginate(count = 20, firstCapacity = 20, otherCapacity = 40))
    }

    @Test
    fun `one row more than page one spills onto page two`() {
        assertEquals(listOf(0 until 20, 20 until 21), ReportLayout.paginate(count = 21, firstCapacity = 20, otherCapacity = 40))
    }

    @Test
    fun `later pages hold more rows than page one`() {
        assertEquals(
            listOf(0 until 20, 20 until 60, 60 until 100, 100 until 105),
            ReportLayout.paginate(count = 105, firstCapacity = 20, otherCapacity = 40),
        )
    }

    @Test
    fun `a full first page header pushes all rows to later pages`() {
        assertEquals(listOf(0 until 0, 0 until 5), ReportLayout.paginate(count = 5, firstCapacity = 0, otherCapacity = 40))
    }

    @Test
    fun `every row is placed exactly once, in order`() {
        val pages = ReportLayout.pages(categoryCount = 7, transactionCount = 333)

        assertEquals((0 until 333).toList(), pages.flatMap { it.toList() })
    }

    @Test
    fun `real A4 geometry fits a sensible number of rows`() {
        val firstPage = ReportLayout.rowsThatFit(ReportLayout.firstPageHeaderHeight(categoryCount = 7))
        val otherPages = ReportLayout.rowsThatFit(ReportLayout.OTHER_PAGE_HEADER)

        assertTrue("page 1 with 7 categories fits $firstPage rows", firstPage in 10..30)
        assertTrue("later pages fit $otherPages rows", otherPages in 35..45)
    }

    @Test
    fun `more categories leave fewer transaction rows on page one`() {
        val few = ReportLayout.rowsThatFit(ReportLayout.firstPageHeaderHeight(categoryCount = 1))
        val many = ReportLayout.rowsThatFit(ReportLayout.firstPageHeaderHeight(categoryCount = 7))

        assertEquals(6, few - many)
    }
}
