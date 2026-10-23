package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.ui.DashboardScreen
import com.example.ui.LoginScreen
import com.example.ui.theme.AttendanceWageTrackerTheme
import com.example.viewmodel.AttendanceViewModel
import com.example.viewmodel.AttendanceViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = AttendanceRepository(database.attendanceDao())
        AttendanceViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AttendanceWageTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
                    val isAuthReady by viewModel.isAuthReady.collectAsState()

                    if (isAuthReady) {
                        AnimatedContent(
                            targetState = isLoggedIn,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "AuthNavigationTransition"
                        ) { loggedIn ->
                            if (loggedIn) {
                                DashboardScreen(viewModel = viewModel)
                            } else {
                                LoginScreen(viewModel = viewModel)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF4F7FC)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppReopened()
    }
}
