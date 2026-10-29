package com.hanan.strukku.export

import com.hanan.strukku.domain.repository.ReportExporter
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
