package com.yeasinx.oshudhkhan.ui.medicine

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yeasinx.oshudhkhan.data.FrequencyType
import com.yeasinx.oshudhkhan.data.Medicine

@Composable
fun MedicineCard(
    medicine: Medicine,
    onToggleActive: (Medicine) -> Unit,
    onEdit: (Medicine) -> Unit,
    onDelete: (Medicine) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            contentColor = if (medicine.isActive)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5F)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
           Row(
               verticalAlignment = Alignment.CenterVertically,
               modifier = Modifier.fillMaxWidth()
           ) {
               Icon(
                   imageVector = Icons.Default.Medication,
                   contentDescription = null,
                    tint = if (medicine.isActive)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                   modifier = Modifier.size(28.dp)
               )
               Spacer(modifier = Modifier.width(12.dp))
               Column( modifier = Modifier.weight(1f)) {
                   Text(
                       text = medicine.name,
                       style = MaterialTheme.typography.titleMedium,
                       fontWeight = FontWeight.Bold,
                       color = if (medicine.isActive)
                           MaterialTheme.colorScheme.onSurface
                       else
                           MaterialTheme.colorScheme.outline
                   )
                   Text(
                       text = medicine.dosage,
                       style = MaterialTheme.typography.bodySmall,
                       color = MaterialTheme.colorScheme.onSurfaceVariant
                   )
               }
               Switch(
                   checked = medicine.isActive,
                   onCheckedChange = { onToggleActive(medicine) },
               )
           }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Frequency Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = frequencyLabel(medicine),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder time chips
            val times = medicine.reminderTimes
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                times.forEach { time ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = formatTime(time),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Edit / Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(onClick = { onEdit(medicine) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Edit", fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = { onDelete(medicine) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}

private fun frequencyLabel(medicine: Medicine): String {
    return when (medicine.frequencyType) {
        FrequencyType.DAILY -> when (medicine.timesPerDay) {
            1 -> "Once daily"
            2 -> "Twice daily"
            3 -> "3 times daily"
            else -> "${medicine.timesPerDay} times daily"
        }
        FrequencyType.EVERY_N_DAYS -> "Every ${medicine.intervalDays} days"
    }
}

private fun formatTime(time: String): String {
    return try {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "%d:%02d %s".format(displayHour, minute, amPm)
    } catch (e: Exception) {
        time
    }
}