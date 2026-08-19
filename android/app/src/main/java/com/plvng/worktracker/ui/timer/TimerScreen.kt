package com.plvng.worktracker.ui.timer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plvng.worktracker.ui.components.BubbleCard
import com.plvng.worktracker.ui.components.BubbleNavItem
import com.plvng.worktracker.ui.components.BubbleNavRow
import com.plvng.worktracker.util.DurationFormatter
import com.plvng.worktracker.util.HapticHelper
import com.plvng.worktracker.util.MoneyFormatter

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current

    if (state.showTaskSheet) {
        TaskBottomSheet(
            currentName = state.taskName,
            suggestions = state.taskNameSuggestions.filter { it != state.taskName },
            onDismiss = viewModel::dismissTaskSheet,
            onSave = { name ->
                HapticHelper.performConfirm(view)
                viewModel.saveTaskName(name)
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BubbleNavRow(
                items = listOf(
                    BubbleNavItem(
                        icon = Icons.Default.BarChart,
                        label = "Отчёт",
                        onClick = {
                            HapticHelper.performTick(view)
                            onOpenHistory()
                        },
                    ),
                    BubbleNavItem(
                        icon = Icons.Default.Settings,
                        label = "Настройки",
                        onClick = {
                            HapticHelper.performTick(view)
                            onOpenSettings()
                        },
                    ),
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BubbleCard(onClick = {
                HapticHelper.performTick(view)
                viewModel.openTaskSheet()
            }) {
                Text(
                    text = state.taskName,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = DurationFormatter.formatClock(state.elapsedMs),
                fontSize = 56.sp,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(40.dp))

            RecordButton(
                isRecording = state.isRecording,
                onClick = {
                    HapticHelper.performConfirm(view)
                    viewModel.toggleRecording()
                },
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "сегодня: ${DurationFormatter.formatHuman(state.todayDurationMs)} · ${MoneyFormatter.formatRub(state.todayAmountRub)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    val gradient = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .scale(if (isRecording) scale else 1f)
            .clip(CircleShape)
            .background(gradient)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Surface(
            onClick = onClick,
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (isRecording) "Стоп" else "Начать",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
