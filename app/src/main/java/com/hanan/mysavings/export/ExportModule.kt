package com.hanan.mysavings.export

import com.hanan.mysavings.domain.repository.ReportExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {
    @Binds
    abstract fun bindReportExporter(impl: PdfReportExporter): ReportExporter
}
