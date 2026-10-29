package com.hanan.mysavings.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Each test is a receipt as it comes out of [ReceiptRowBuilder]: one string per printed row,
 * with the kind of noise OCR produces (odd spacing, mixed separators, stray symbols).
 */
class ReceiptParserTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun parse(vararg rows: String) = ReceiptParser.parse(rows.toList(), today)

    @Test
    fun `minimarket receipt - Indomaret`() {
        val result = parse(
            "INDOMARET",
            "PT. INDOMARCO PRISMATAMA",
            "JL. RAYA BOGOR NO. 12",
            "NPWP 01.337.994.6-092.000",
            "12.10.2026-14:32 2.0.36 1234/KASIR/01",
            "AQUA 600ML 2 3.500 7.000",
            "INDOMIE GORENG 5 3.100 15.500",
            "HARGA JUAL : 22.500",
            "TOTAL : 22.500",
            "TUNAI : 50.000",
            "KEMBALI : 27.500",
        )

        assertEquals("Indomaret", result.merchant)
        assertEquals(22_500L, result.total)
        assertEquals(LocalDate.of(2026, 10, 12), result.date)
    }

    @Test
    fun `minimarket receipt - Alfamart with total on the item-count row`() {
        val result = parse(
            "ALFAMART",
            "PT.SUMBER ALFARIA TRIJAYA, TBK",
            "Bon 1A2B3C4 Kasir: Siti",
            "SUSU UHT 1 6.900",
            "ROTI TAWAR 1 15.000",
            "Total Item 2 21.900",
            "Tunai 50.000",
            "Kembalian 28.100",
            "Tgl. 05-10-2026 19:21:03",
        )

        assertEquals("Alfamart", result.merchant)
        assertEquals(21_900L, result.total)
        assertEquals(LocalDate.of(2026, 10, 5), result.date)
    }

    @Test
    fun `restaurant receipt - grand total wins over subtotal, service and tax`() {
        val result = parse(
            "RM. SEDERHANA",
            "Jl. Sudirman No. 5, Jakarta",
            "Telp. (021) 5551234",
            "Meja 12 29/10/26 13:05",
            "Nasi Rendang 2 x 35.000 70.000",
            "Es Teh Manis 2 x 8.000 16.000",
            "Subtotal 86.000",
            "Service 5% 4.300",
            "PPN 10% 9.030",
            "Grand Total 99.330",
            "Tunai 100.000",
            "Kembali 670",
        )

        assertEquals("Rm. Sederhana", result.merchant)
        assertEquals(99_330L, result.total)
        assertEquals(LocalDate.of(2026, 10, 29), result.date)
    }

    @Test
    fun `fuel receipt - text month date and Rp prefix`() {
        val result = parse(
            "PERTAMINA",
            "SPBU 34.12345",
            "JL. MARGONDA RAYA",
            "No. Trans: 0098765",
            "Waktu: 28 Okt 2026 07:45",
            "Produk : Pertalite",
            "Harga/Liter : Rp. 10.000",
            "Volume : 15,00 L",
            "Total Harga : Rp. 150.000",
            "Cash : Rp. 150.000",
        )

        assertEquals("Pertamina", result.merchant)
        assertEquals(150_000L, result.total)
        assertEquals(LocalDate.of(2026, 10, 28), result.date)
    }

    @Test
    fun `cafe receipt - comma thousands with cents and ISO date`() {
        val result = parse(
            "Kopi Kenangan",
            "Grand Indonesia Mall",
            "2026-10-27 16:20:11",
            "Kopi Kenangan Mantan 2 48,000.00",
            "Subtotal 48,000.00",
            "GRAND TOTAL 48,000.00",
            "Payment QRIS 48,000.00",
        )

        assertEquals("Kopi Kenangan", result.merchant)
        assertEquals(48_000L, result.total)
        assertEquals(LocalDate.of(2026, 10, 27), result.date)
    }

    @Test
    fun `pharmacy receipt - Jumlah label and dot thousands with comma cents`() {
        val result = parse(
            "APOTEK K-24",
            "Jl. Kaliurang Km 5",
            "Tanggal : 15/09/2026",
            "Paracetamol 500mg 1 12.500,00",
            "Vitamin C 1 32.000,00",
            "Jumlah 44.500,00",
            "Bayar 50.000,00",
            "Kembali 5.500,00",
        )

        assertEquals("Apotek K-24", result.merchant)
        assertEquals(44_500L, result.total)
        assertEquals(LocalDate.of(2026, 9, 15), result.date)
    }

    @Test
    fun `without a total label the largest amount that is not cash or change is used`() {
        val result = parse(
            "WARUNG BU SITI",
            "Nasi Campur 15.000",
            "Es Jeruk 5.000",
            "20.000",
            "Tunai 50.000",
        )

        assertEquals("Warung Bu Siti", result.merchant)
        assertEquals(20_000L, result.total)
        assertNull(result.date)
    }

    @Test
    fun `amount split onto the row below the TOTAL label is still found`() {
        val result = parse("TOKO MAKMUR", "TOTAL", "125.000", "TUNAI", "150.000")

        assertEquals(125_000L, result.total)
    }

    @Test
    fun `plain amount without separators is accepted on a total row`() {
        assertEquals(25_000L, parse("TOTAL Rp25000").total)
    }

    @Test
    fun `subtotal, discount and savings rows are never taken as the total`() {
        val result = parse(
            "SUB TOTAL 50.000",
            "TOTAL DISKON 5.000",
            "TOTAL HEMAT 5.000",
            "TOTAL 45.000",
        )

        assertEquals(45_000L, result.total)
    }

    @Test
    fun `future dates are ignored`() {
        assertNull(parse("TOKO ABC", "Tgl 12/12/2026", "TOTAL 10.000").date)
    }

    @Test
    fun `invalid calendar dates are ignored and the next valid one is used`() {
        assertEquals(LocalDate.of(2026, 2, 28), parse("31/02/2026", "28/02/2026").date)
    }

    @Test
    fun `two-digit years are read as 20xx`() {
        assertEquals(LocalDate.of(2025, 12, 31), parse("31-12-25").date)
    }

    @Test
    fun `address, phone and greeting rows are not taken as the merchant`() {
        val result = parse(
            "SELAMAT DATANG",
            "Jl. Melati No. 3",
            "0812-3456-7890",
            "Toko Sumber Rejeki",
            "TOTAL 10.000",
        )

        assertEquals("Toko Sumber Rejeki", result.merchant)
    }

    @Test
    fun `legal company name is used only when no brand name is found`() {
        assertEquals("Pt Maju Jaya", parse("PT MAJU JAYA", "TOTAL 10.000").merchant)
    }

    @Test
    fun `unreadable noise yields nothing`() {
        val result = parse("@@@ ###", "12", "..", "~")

        assertNull(result.merchant)
        assertNull(result.total)
        assertNull(result.date)
    }
}
