package com.yeasinx.oshudhkhan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yeasinx.oshudhkhan.data.Medicine
import com.yeasinx.oshudhkhan.notification.NotificationHelper
import com.yeasinx.oshudhkhan.ui.medicine.MedicineViewModel
import com.yeasinx.oshudhkhan.ui.components.MedicineCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    viewModel: MedicineViewModel,
    onAddMedicine: () -> Unit,
    onEditMedicine: (Medicine) -> Unit
) {
    val medicines by viewModel.medicine.collectAsState()
    var medicineToDelete by remember { mutableStateOf<Medicine?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Medicines",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (medicines.isNotEmpty()) {
                            Text(
                                text = "${medicines.count { it.isActive }} active reminder(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedicine,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                text = { Text("Add Medicine") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (medicines.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No medicines yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the button below to add your first medicine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = medicines,
                        key = { it.id }
                    ) { medicine ->
                        MedicineCard(
                            medicine = medicine,
                            onToggleActive = { viewModel.toggleActive(it) },
                            onEdit = onEditMedicine,
                            onDelete = { medicineToDelete = it }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    medicineToDelete?.let { medicine ->
        AlertDialog(
            onDismissRequest = { medicineToDelete = null },
            title = { Text("Delete Medicine") },
            text = { Text("Are you sure you want to delete \"${medicine.name}\"? All reminders will be cancelled.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedicine(medicine)
                        medicineToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { medicineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}