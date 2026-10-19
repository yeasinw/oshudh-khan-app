package com.yeasinx.oshudhkhan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yeasinx.oshudhkhan.ui.components.HeatmapLegend
import com.yeasinx.oshudhkhan.ui.components.WeekHeatmap
import com.yeasinx.oshudhkhan.ui.home.HomeViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSetUpProfile: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (state.name.isBlank()) state.greeting else "${state.greeting}, ${state.name}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (state.name.isBlank()) {
                TextButton(onClick = onSetUpProfile, contentPadding = PaddingValues(0.dp)) {
                    Text("Set up your profile")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Last 7 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    WeekHeatmap(days = state.week)
                    Spacer(Modifier.height(8.dp))
                    HeatmapLegend(Modifier.align(Alignment.End))
                }
            }
        }

        item {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        if (state.today.isEmpty()) {
            item {
                Text(
                    "No doses scheduled for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.today, key = { "${it.medicine.id}-${it.time}" }) { dose ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = dose.taken,
                            role = Role.Checkbox,
                            onValueChange = { viewModel.setTaken(dose, it) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(dose.medicine.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${dose.medicine.dosage} · ${dose.time.format(timeFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(checked = dose.taken, onCheckedChange = null)
                }
            }
        }
    }
}
