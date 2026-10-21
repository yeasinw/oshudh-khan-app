package com.yeasinx.oshudhkhan.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.yeasinx.oshudhkhan.ui.home.DaySummary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeekHeatmap(days: List<DaySummary>, modifier: Modifier = Modifier) {
    val today = LocalDate.now()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val shape = RoundedCornerShape(10.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = if (day.scheduled == 0) "$label: no doses scheduled"
                        else "$label: ${day.taken} of ${day.scheduled} doses taken"
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxWidth()
                        .background(cellColor(day), shape)
                        .then(
                            if (day.date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape)
                            else Modifier
                        )
                )
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun cellColor(day: DaySummary): Color {
    if (day.scheduled == 0) return MaterialTheme.colorScheme.surfaceVariant
    val ratio = day.taken.toFloat() / day.scheduled
    val alpha = when {
        ratio == 0f -> 0.12f
        ratio < 0.5f -> 0.35f
        ratio < 1f -> 0.65f
        else -> 1f
    }
    return MaterialTheme.colorScheme.primary.copy(alpha = alpha)
}

@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0.12f, 0.35f, 0.65f, 1f).forEach { alpha ->
            Box(
                Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(3.dp))
            )
        }
        Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
