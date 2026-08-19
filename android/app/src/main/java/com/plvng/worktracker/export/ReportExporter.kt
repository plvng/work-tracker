package com.plvng.worktracker.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.plvng.worktracker.data.SessionDetail
import com.plvng.worktracker.data.TaskSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportResult(
    val pdfUri: Uri,
    val pdfName: String,
)

class ReportExporter(private val context: Context) {
    fun export(
        summaries: List<TaskSummary>,
        details: List<SessionDetail>,
        hourlyRate: Int,
    ): ExportResult {
        val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
        val pdfName = "work-report-$stamp.pdf"

        val builder = PdfReportBuilder()
        val document = builder.build(summaries, details, hourlyRate)

        val uri = writePdfFile(pdfName, document)
        document.close()

        return ExportResult(pdfUri = uri, pdfName = pdfName)
    }

    private fun writePdfFile(displayName: String, document: android.graphics.pdf.PdfDocument): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/WorkTracker")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Cannot create PDF")
        resolver.openOutputStream(uri)?.use { document.writeTo(it) }
            ?: error("Cannot write PDF")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }
}
