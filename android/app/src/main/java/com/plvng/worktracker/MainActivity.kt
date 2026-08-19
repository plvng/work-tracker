package com.plvng.worktracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plvng.worktracker.export.ReportExporter
import com.plvng.worktracker.ui.history.HistoryScreen
import com.plvng.worktracker.ui.history.HistoryViewModel
import com.plvng.worktracker.ui.navigation.Routes
import com.plvng.worktracker.ui.settings.SettingsScreen
import com.plvng.worktracker.ui.settings.SettingsViewModel
import com.plvng.worktracker.ui.theme.WorkTrackerTheme
import com.plvng.worktracker.ui.timer.TimerScreen
import com.plvng.worktracker.ui.timer.TimerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as WorkTrackerApp
        val repository = app.repository
        val exporter = ReportExporter(this)

        setContent {
            WorkTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.TIMER,
                    ) {
                        composable(Routes.TIMER) {
                            val vm: TimerViewModel = viewModel(
                                factory = TimerViewModel.Factory(repository),
                            )
                            TimerScreen(
                                viewModel = vm,
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            )
                        }
                        composable(Routes.HISTORY) {
                            val vm: HistoryViewModel = viewModel(
                                factory = HistoryViewModel.Factory(repository, exporter),
                            )
                            HistoryScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.SETTINGS) {
                            val vm: SettingsViewModel = viewModel(
                                factory = SettingsViewModel.Factory(repository),
                            )
                            SettingsScreen(
                                viewModel = vm,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
