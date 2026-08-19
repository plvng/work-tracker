package com.plvng.worktracker.ui.history

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plvng.worktracker.data.SessionDetail
import com.plvng.worktracker.data.TaskSummary
import com.plvng.worktracker.ui.components.BubbleCard
import com.plvng.worktracker.ui.components.BubbleNavItem
import com.plvng.worktracker.ui.components.BubbleNavRow
import com.plvng.worktracker.ui.timer.EditCommentDialog
import com.plvng.worktracker.util.DurationFormatter
import com.plvng.worktracker.util.HapticHelper
import com.plvng.worktracker.util.MoneyFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(state.exportMessage, state.lastExportPdfUri) {
        val msg = state.exportMessage ?: return@LaunchedEffect
        val uri = state.lastExportPdfUri
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = if (uri != null) "Открыть" else null,
        )
        if (result == SnackbarResult.ActionPerformed && uri != null) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Открыть PDF"))
        }
        viewModel.clearExportMessage()
    }

    state.editingComment?.let { editing ->
        EditCommentDialog(
            taskName = editing.taskName,
            initialComment = editing.currentComment,
            onConfirm = viewModel::saveTaskComment,
            onDismiss = viewModel::dismissCommentEditor,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BubbleNavRow(
                items = listOf(
                    BubbleNavItem(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = "Назад",
                        onClick = {
                            HapticHelper.performTick(view)
                            onBack()
                        },
                        enabled = !state.isExporting,
                    ),
                    BubbleNavItem(
                        icon = Icons.Default.FileDownload,
                        label = "Экспорт PDF",
                        onClick = {
                            HapticHelper.performConfirm(view)
                            viewModel.exportReport()
                        },
                        isLoading = state.isExporting,
                        enabled = !state.isExporting,
                    ),
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            Text(
                text = "Отчёт",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                textAlign = TextAlign.Center,
            )

            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                Tab(
                    selected = state.selectedTab == HistoryTab.Summary,
                    onClick = { viewModel.selectTab(HistoryTab.Summary) },
                    text = { Text("Сводка") },
                )
                Tab(
                    selected = state.selectedTab == HistoryTab.Timeline,
                    onClick = { viewModel.selectTab(HistoryTab.Timeline) },
                    text = { Text("Хронология") },
                )
            }

            when (state.selectedTab) {
                HistoryTab.Summary -> SummaryList(
                    summaries = state.summaries,
                    hourlyRate = state.hourlyRateRub,
                    onEditComment = { name, comment ->
                        viewModel.openCommentEditor(name, comment)
                    },
                )
                HistoryTab.Timeline -> TimelineList(state.details)
            }
        }
    }
}

@Composable
private fun SummaryList(
    summaries: List<TaskSummary>,
    hourlyRate: Int,
    onEditComment: (taskName: String, comment: String?) -> Unit,
) {
    if (summaries.isEmpty()) {
        EmptyState("Пока нет записей")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(summaries, key = { it.taskName }) { item ->
            val amount = ((item.totalDurationMs / 3_600_000.0) * hourlyRate).toInt()
            BubbleCard {
                Text(
                    item.taskName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${DurationFormatter.formatHuman(item.totalDurationMs)} · ${MoneyFormatter.formatRub(amount)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditComment(item.taskName, item.taskComment) }
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    if (item.taskComment.isNullOrBlank()) {
                        Text(
                            "Добавить комментарий",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            item.taskComment,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineList(details: List<SessionDetail>) {
    if (details.isEmpty()) {
        EmptyState("Пока нет сессий")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(details, key = { it.session.id }) { item ->
            val session = item.session
            val end = session.endedAt ?: System.currentTimeMillis()
            BubbleCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        session.taskName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (session.endedAt == null) {
                        Text("идёт", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    "${DurationFormatter.formatDate(session.startedAt)} · ${DurationFormatter.formatTime(session.startedAt)}–${DurationFormatter.formatTime(end)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${DurationFormatter.formatHuman(item.durationMs)} · ${MoneyFormatter.formatRub(item.amountRub)}",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
