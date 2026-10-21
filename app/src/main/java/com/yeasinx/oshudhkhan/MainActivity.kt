package com.yeasinx.oshudhkhan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.yeasinx.oshudhkhan.navigation.FloatingBottomNavBar
import com.yeasinx.oshudhkhan.navigation.MedicineNavGraph
import com.yeasinx.oshudhkhan.navigation.NAV_BAR_SPACE
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.yeasinx.oshudhkhan.ui.auth.AuthViewModel
import com.yeasinx.oshudhkhan.ui.auth.AuthViewModelFactory
import com.yeasinx.oshudhkhan.ui.home.HomeViewModel
import com.yeasinx.oshudhkhan.ui.home.HomeViewModelFactory
import com.yeasinx.oshudhkhan.ui.medicine.MedicineViewModel
import com.yeasinx.oshudhkhan.ui.medicine.MedicineViewModelFactory
import com.yeasinx.oshudhkhan.ui.profile.ProfileViewModel
import com.yeasinx.oshudhkhan.ui.profile.ProfileViewModelFactory
import com.yeasinx.oshudhkhan.ui.theme.OshudhKhanTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val app = application as MedicineApp

        enableEdgeToEdge()
        setContent {
            OshudhKhanTheme {
                val navController = rememberNavController()
                val viewModel: MedicineViewModel = viewModel(
                    factory = MedicineViewModelFactory(app.repository, applicationContext)
                )
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(app.repository, app.userRepository, applicationContext)
                )
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(app.userRepository)
                )
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModelFactory(app.authRepository, app.userRepository)
                )
                val hazeState = rememberHazeState()
                // Content scrolls underneath the floating glass bar, so screens reserve this much at the bottom
                val bottomPadding = NAV_BAR_SPACE +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState)
                                .padding(top = innerPadding.calculateTopPadding())
                        ) {
                            MedicineNavGraph(
                                navController = navController,
                                viewModel = viewModel,
                                homeViewModel = homeViewModel,
                                profileViewModel = profileViewModel,
                                authViewModel = authViewModel,
                                bottomPadding = bottomPadding
                            )
                        }
                        FloatingBottomNavBar(
                            navController = navController,
                            hazeState = hazeState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
