package com.yeasinx.oshudhkhan.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yeasinx.oshudhkhan.data.DoseLog
import com.yeasinx.oshudhkhan.data.DoseSchedule
import com.yeasinx.oshudhkhan.data.Medicine
import com.yeasinx.oshudhkhan.data.MedicineRepository
import com.yeasinx.oshudhkhan.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class DaySummary(val date: LocalDate, val taken: Int, val scheduled: Int)

data class TodayDose(val medicine: Medicine, val time: LocalTime, val taken: Boolean)

data class HomeUiState(
    val greeting: String = "",
    val name: String = "",
    val week: List<DaySummary> = emptyList(),
    val today: List<TodayDose> = emptyList()
)

fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

class HomeViewModel(
    private val medicineRepository: MedicineRepository,
    userRepository: UserRepository
) : ViewModel() {

    private val today = LocalDate.now()
    private val weekStart = today.minusDays(6)

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.profile,
        medicineRepository.allMedicine,
        medicineRepository.doseLogsBetween(weekStart, today)
    ) { profile, medicines, logs ->
        val takenKeys = logs.map { it.key() }.toSet()
        HomeUiState(
            greeting = greetingFor(LocalTime.now().hour),
            name = profile.name,
            week = (0L..6L).map { offset ->
                val date = weekStart.plusDays(offset)
                val scheduled = medicines.sumOf { DoseSchedule.dosesFor(it, date).size }
                val taken = logs.count { it.date == date.toString() }
                // Logs can outlive a medicine's schedule (deleted/paused), so never show >100%.
                DaySummary(date, taken, maxOf(scheduled, taken))
            },
            today = medicines.flatMap { medicine ->
                DoseSchedule.dosesFor(medicine, today).map { time ->
                    TodayDose(medicine, time, Triple(medicine.id, today.toString(), time.toString()) in takenKeys)
                }
            }.sortedBy { it.time }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setTaken(dose: TodayDose, taken: Boolean) {
        viewModelScope.launch {
            if (taken) medicineRepository.markTaken(dose.medicine.id, today, dose.time)
            else medicineRepository.unmarkTaken(dose.medicine.id, today, dose.time)
        }
    }

    private fun DoseLog.key() = Triple(medicineId, date, time)
}

class HomeViewModelFactory(
    private val medicineRepository: MedicineRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(medicineRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
