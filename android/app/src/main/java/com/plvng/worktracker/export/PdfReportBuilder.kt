package com.plvng.worktracker.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.plvng.worktracker.data.SessionDetail
import com.plvng.worktracker.data.TaskSummary
import com.plvng.worktracker.util.DurationFormatter
import com.plvng.worktracker.util.MoneyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportBuilder {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val footerHeight = 28f
    private val contentBottom get() = pageHeight - margin - footerHeight

    private val colorPrimary = Color.parseColor("#5B8DEF")
    private val colorPrimaryDark = Color.parseColor("#3A6FD4")
    private val colorBgAlt = Color.parseColor("#F3F6FC")
    private val colorText = Color.parseColor("#1C2333")
    private val colorTextMuted = Color.parseColor("#5A6478")
    private val colorWhite = Color.WHITE

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 22f
        color = colorWhite
    }
    private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 11f
        color = Color.parseColor("#D8E6FF")
    }
    private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
        color = colorText
    }
    private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10f
        color = colorText
    }
    private val bodyBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 10f
        color = colorText
    }
    private val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 9f
        color = colorTextMuted
    }
    private val statValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 16f
        color = colorPrimaryDark
    }
    private val statLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 9f
        color = colorTextMuted
    }
    private val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9f
        color = colorWhite
    }
    private val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 8f
        color = colorTextMuted
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private lateinit var document: PdfDocument
    private lateinit var canvas: Canvas
    private var currentPage: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = margin

    fun build(
        summaries: List<TaskSummary>,
        details: List<SessionDetail>,
        hourlyRate: Int,
    ): PdfDocument {
        document = PdfDocument()
        startNewPage()

        drawHeaderBlock(hourlyRate)

        if (summaries.isEmpty()) {
            drawEmptyState()
        } else {
            drawTotalsCard(summaries, hourlyRate)
            y += 16f
            drawSectionTitle("Сводка по задачам")
            drawSummaryTable(summaries, hourlyRate)
            drawSectionTitle("Хронология")
            drawTimeline(details)
        }

        finishCurrentPage()
        return document
    }

    private fun drawEmptyState() {
        ensureSpace(40f)
        canvas.drawText("Нет данных за период", margin, y + 20f, sectionPaint)
        y += 40f
    }

    private fun finishCurrentPage() {
        currentPage?.let { page ->
            drawFooter()
            document.finishPage(page)
            currentPage = null
        }
    }

    private fun startNewPage() {
        finishCurrentPage()
        pageNumber++
        y = margin
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = document.startPage(pageInfo)
        currentPage = page
        canvas = page.canvas
    }

    private fun ensureSpace(height: Float) {
        if (y + height > contentBottom) {
            startNewPage()
        }
    }

    private fun drawHeaderBlock(hourlyRate: Int) {
        val headerHeight = 72f
        ensureSpace(headerHeight + 8f)
        val rect = RectF(margin, y, pageWidth - margin, y + headerHeight)
        fillPaint.color = colorPrimary
        canvas.drawRoundRect(rect, 12f, 12f, fillPaint)

        canvas.drawText("Отчёт о рабочем времени", margin + 16f, y + 32f, titlePaint)
        val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru", "RU")).format(Date())
        canvas.drawText(dateStr, margin + 16f, y + 52f, subtitlePaint)
        canvas.drawText(
            "Ставка ${MoneyFormatter.formatRub(hourlyRate)}/ч",
            margin + 16f,
            y + 66f,
            subtitlePaint,
        )
        y += headerHeight + 16f
    }

    private fun drawTotalsCard(summaries: List<TaskSummary>, hourlyRate: Int) {
        val totalMs = summaries.sumOf { it.totalDurationMs }
        val totalRub = summaries.sumOf { ((it.totalDurationMs / 3_600_000.0) * hourlyRate).toInt() }
        val cardHeight = 64f
        ensureSpace(cardHeight + 8f)

        val rect = RectF(margin, y, pageWidth - margin, y + cardHeight)
        fillPaint.color = colorBgAlt
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint)

        val colWidth = (pageWidth - margin * 2) / 3f
        drawStat(margin + 8f, colWidth - 16f, y + 22f, DurationFormatter.formatHuman(totalMs), "Всего времени")
        drawStat(margin + colWidth + 8f, colWidth - 16f, y + 22f, MoneyFormatter.formatRub(totalRub), "Сумма")
        drawStat(margin + colWidth * 2 + 8f, colWidth - 16f, y + 22f, summaries.size.toString(), "Задач")
        y += cardHeight + 8f
    }

    private fun drawStat(x: Float, maxWidth: Float, valueY: Float, value: String, label: String) {
        val fittedValue = fitText(value, statValuePaint, maxWidth)
        canvas.drawText(fittedValue, x, valueY, statValuePaint)
        canvas.drawText(label, x, valueY + 18f, statLabelPaint)
    }

    private fun fitText(text: String, paint: TextPaint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var trimmed = text
        while (trimmed.length > 1 && paint.measureText("$trimmed…") > maxWidth) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }

    private fun drawSectionTitle(title: String) {
        ensureSpace(28f)
        y += 8f
        canvas.drawText(title, margin, y + 14f, sectionPaint)
        y += 22f
        fillPaint.color = colorPrimary
        canvas.drawRect(margin, y, margin + 40f, y + 2f, fillPaint)
        y += 10f
    }

    private fun drawSummaryTable(summaries: List<TaskSummary>, hourlyRate: Int) {
        val tableWidth = pageWidth - margin * 2
        val colTask = tableWidth * 0.32f
        val colTime = tableWidth * 0.16f
        val colAmount = tableWidth * 0.18f
        val headerHeight = 22f
        val minRowHeight = 24f

        fun drawTableHeader() {
            ensureSpace(headerHeight)
            val rect = RectF(margin, y, pageWidth - margin, y + headerHeight)
            fillPaint.color = colorPrimary
            canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
            canvas.drawText("Задача", margin + 8f, y + 15f, tableHeaderPaint)
            canvas.drawText("Время", margin + colTask + 4f, y + 15f, tableHeaderPaint)
            canvas.drawText("Сумма", margin + colTask + colTime + 4f, y + 15f, tableHeaderPaint)
            canvas.drawText("Комментарий", margin + colTask + colTime + colAmount + 4f, y + 15f, tableHeaderPaint)
            y += headerHeight
        }

        drawTableHeader()

        summaries.forEachIndexed { index, item ->
            val amount = ((item.totalDurationMs / 3_600_000.0) * hourlyRate).toInt()
            val timeStr = DurationFormatter.formatHuman(item.totalDurationMs)
            val amountStr = MoneyFormatter.formatRub(amount)
            val commentStr = item.taskComment.orEmpty().ifBlank { "—" }

            val taskLines = wrapText(item.taskName, bodyBoldPaint, colTask - 12f)
            val commentLines = wrapText(commentStr, bodyPaint, tableWidth - colTask - colTime - colAmount - 12f)
            val rowHeight = maxOf(minRowHeight, maxOf(taskLines.size, commentLines.size) * 13f + 10f)

            if (y + rowHeight > contentBottom) {
                startNewPage()
                drawTableHeader()
            }

            if (index % 2 == 1) {
                fillPaint.color = colorBgAlt
                canvas.drawRect(margin, y, pageWidth - margin, y + rowHeight, fillPaint)
            }

            var lineY = y + 14f
            taskLines.forEach { line ->
                canvas.drawText(line, margin + 8f, lineY, bodyBoldPaint)
                lineY += 13f
            }
            canvas.drawText(timeStr, margin + colTask + 4f, y + 14f, bodyPaint)
            canvas.drawText(amountStr, margin + colTask + colTime + 4f, y + 14f, bodyBoldPaint)

            lineY = y + 14f
            commentLines.forEach { line ->
                canvas.drawText(line, margin + colTask + colTime + colAmount + 4f, lineY, bodyPaint)
                lineY += 13f
            }

            y += rowHeight
        }
        y += 12f
    }

    private fun drawTimeline(details: List<SessionDetail>) {
        if (details.isEmpty()) {
            ensureSpace(20f)
            canvas.drawText("Нет сессий", margin, y + 12f, mutedPaint)
            y += 24f
            return
        }

        val grouped = details
            .groupBy { DurationFormatter.formatDate(it.session.startedAt) }
            .toSortedMap(reverseOrder())

        for ((dateKey, sessions) in grouped) {
            val dateLabel = formatDateLabel(dateKey)
            ensureSpace(20f)
            canvas.drawText(dateLabel, margin, y + 12f, bodyBoldPaint)
            y += 18f

            val sortedSessions = sessions.sortedByDescending { it.session.startedAt }
            for (item in sortedSessions) {
                val session = item.session
                val end = session.endedAt ?: System.currentTimeMillis()
                val mainLine = "${DurationFormatter.formatTime(session.startedAt)}–${DurationFormatter.formatTime(end)}  ·  ${session.taskName}"
                val subLine = "${DurationFormatter.formatHuman(item.durationMs)}  ·  ${MoneyFormatter.formatRub(item.amountRub)}"

                val mainLines = wrapText(mainLine, bodyPaint, pageWidth - margin * 2 - 16f)
                val rowHeight = mainLines.size * 12f + 22f + 8f
                ensureSpace(rowHeight)

                fillPaint.color = colorBgAlt
                val rect = RectF(margin, y, pageWidth - margin, y + rowHeight)
                canvas.drawRoundRect(rect, 8f, 8f, fillPaint)

                var lineY = y + 14f
                mainLines.forEach { line ->
                    canvas.drawText(line, margin + 12f, lineY, bodyPaint)
                    lineY += 12f
                }
                canvas.drawText(subLine, margin + 12f, lineY, bodyBoldPaint)
                y += rowHeight + 6f
            }
            y += 6f
        }
    }

    private fun drawFooter() {
        val footerText = "Work Tracker · стр. $pageNumber"
        val textWidth = footerPaint.measureText(footerText)
        canvas.drawText(footerText, (pageWidth - textWidth) / 2f, pageHeight - 20f, footerPaint)
    }

    private fun wrapText(text: String, paint: TextPaint, width: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        return (0 until layout.lineCount).map { i ->
            text.substring(layout.getLineStart(i), layout.getLineEnd(i)).trimEnd()
        }
    }

    private fun formatDateLabel(isoDate: String): String {
        return try {
            val parts = isoDate.split("-")
            if (parts.size == 3) {
                "${parts[2].toInt()} ${monthName(parts[1].toInt())} ${parts[0]}"
            } else {
                isoDate
            }
        } catch (_: Exception) {
            isoDate
        }
    }

    private fun monthName(month: Int): String = when (month) {
        1 -> "января"
        2 -> "февраля"
        3 -> "марта"
        4 -> "апреля"
        5 -> "мая"
        6 -> "июня"
        7 -> "июля"
        8 -> "августа"
        9 -> "сентября"
        10 -> "октября"
        11 -> "ноября"
        12 -> "декабря"
        else -> ""
    }
}
