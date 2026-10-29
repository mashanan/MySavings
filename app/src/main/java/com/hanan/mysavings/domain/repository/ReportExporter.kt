package com.hanan.mysavings.domain.repository

import com.hanan.mysavings.domain.model.MonthlyReport
import java.io.File

/** Turns a report into a shareable file. The format (PDF) is the implementation's business. */
interface ReportExporter {
    suspend fun export(report: MonthlyReport): File
}
