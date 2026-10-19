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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.yeasinx.oshudhkhan.navigation.MedicineNavGraph
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
                    factory = HomeViewModelFactory(app.repository, app.userRepository)
                )
                val profileViewModel: ProfileViewModel = viewModel(
                    factory = ProfileViewModelFactory(app.userRepository)
                )
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        com.yeasinx.oshudhkhan.navigation.FloatingBottomNavBar(navController = navController)
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MedicineNavGraph(
                            navController = navController,
                            viewModel = viewModel,
                            homeViewModel = homeViewModel,
                            profileViewModel = profileViewModel
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
