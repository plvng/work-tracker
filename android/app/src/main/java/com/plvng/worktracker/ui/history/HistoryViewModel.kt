package com.plvng.worktracker.ui.history

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.plvng.worktracker.data.SessionDetail
import com.plvng.worktracker.data.TaskSummary
import com.plvng.worktracker.data.WorkRepository
import com.plvng.worktracker.export.ExportResult
import com.plvng.worktracker.export.ReportExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditingComment(
    val taskName: String,
    val currentComment: String,
)

data class HistoryUiState(
    val summaries: List<TaskSummary> = emptyList(),
    val details: List<SessionDetail> = emptyList(),
    val hourlyRateRub: Int = 1000,
    val selectedTab: HistoryTab = HistoryTab.Summary,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val lastExportPdfUri: Uri? = null,
    val editingComment: EditingComment? = null,
)

enum class HistoryTab {
    Summary,
    Timeline,
}

class HistoryViewModel(
    private val repository: WorkRepository,
    private val exporter: ReportExporter,
) : ViewModel() {
    private val selectedTab = MutableStateFlow(HistoryTab.Summary)
    private val exportState = MutableStateFlow(ExportUi())
    private val editingComment = MutableStateFlow<EditingComment?>(null)

    private data class ExportUi(
        val isExporting: Boolean = false,
        val message: String? = null,
        val pdfUri: Uri? = null,
    )

    val uiState: StateFlow<HistoryUiState> = combine(
        combine(
            repository.allSessions,
            repository.taskNotes,
            repository.hourlyRateRub,
        ) { sessions, notes, rate -> Triple(sessions, notes, rate) },
        selectedTab,
        exportState,
        editingComment,
    ) { data, tab, export, editing ->
        val (sessions, notes, rate) = data
        val notesMap = notes.associate { it.taskName to it.comment }
        val now = System.currentTimeMillis()
        val summaries = sessions
            .groupBy { it.taskName }
            .map { (name, group) ->
                TaskSummary(
                    taskName = name,
                    totalDurationMs = group.sumOf { it.durationMs(now) },
                    taskComment = notesMap[name],
                )
            }
            .sortedByDescending { it.totalDurationMs }

        val details = sessions
            .sortedByDescending { it.startedAt }
            .map { session ->
                val duration = session.durationMs(now)
                SessionDetail(
                    session = session,
                    durationMs = duration,
                    amountRub = repository.amountRub(duration, rate),
                )
            }

        HistoryUiState(
            summaries = summaries,
            details = details,
            hourlyRateRub = rate,
            selectedTab = tab,
            isExporting = export.isExporting,
            exportMessage = export.message,
            lastExportPdfUri = export.pdfUri,
            editingComment = editing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectTab(tab: HistoryTab) {
        selectedTab.value = tab
    }

    fun openCommentEditor(taskName: String, currentComment: String?) {
        editingComment.value = EditingComment(taskName, currentComment.orEmpty())
    }

    fun dismissCommentEditor() {
        editingComment.value = null
    }

    fun saveTaskComment(comment: String?) {
        viewModelScope.launch {
            val editing = editingComment.value ?: return@launch
            repository.updateTaskComment(editing.taskName, comment)
            editingComment.value = null
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            exportState.update { it.copy(isExporting = true, message = null) }
            try {
                val rate = repository.hourlyRateRub.first()
                val summaries = repository.buildSummaries(rate)
                val details = repository.buildDetails(rate)
                val result: ExportResult = exporter.export(summaries, details, rate)
                exportState.update {
                    it.copy(
                        isExporting = false,
                        pdfUri = result.pdfUri,
                        message = "PDF сохранён в Downloads/WorkTracker",
                    )
                }
            } catch (e: Exception) {
                exportState.update {
                    it.copy(isExporting = false, message = "Ошибка экспорта: ${e.message}")
                }
            }
        }
    }

    fun clearExportMessage() {
        exportState.update { it.copy(message = null) }
    }

    class Factory(
        private val repository: WorkRepository,
        private val exporter: ReportExporter,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository, exporter) as T
        }
    }
}
