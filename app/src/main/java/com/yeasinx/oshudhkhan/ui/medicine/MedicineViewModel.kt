package com.yeasinx.oshudhkhan.ui.medicine

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yeasinx.oshudhkhan.data.local.Medicine
import com.yeasinx.oshudhkhan.data.MedicineRepository
import com.yeasinx.oshudhkhan.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MedicineViewModel(
    private val repository: MedicineRepository,
    private val appContext: Context
) : ViewModel() {

    val medicine: StateFlow<List<Medicine>> = repository.allMedicine
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    fun addMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val medicineId = repository.insert(medicine)
            val saved = medicine.copy(id = medicineId.toInt())
            if (saved.isActive) {
                AlarmScheduler.scheduleMedicineAlarms(appContext, saved)
            }
        }
    }

    suspend fun getMedicine(id: Int): Medicine? = repository.getById(id)

    fun updateMedicine(original: Medicine, edited: Medicine) {
        viewModelScope.launch {
            // Cancel with the original: its reminder slots may differ from the edited ones
            AlarmScheduler.cancelMedicineAlarms(appContext, original)
            repository.update(edited)
            if (edited.isActive) {
                AlarmScheduler.scheduleMedicineAlarms(appContext, edited)
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            AlarmScheduler.cancelMedicineAlarms(appContext, medicine)
            repository.delete(medicine)
        }
    }

    fun toggleActive(medicine: Medicine) {
        viewModelScope.launch {
            val updated = medicine.copy(isActive = !medicine.isActive)
            repository.update(updated)
            if (updated.isActive) {
                AlarmScheduler.scheduleMedicineAlarms(appContext, updated)
            } else {
                AlarmScheduler.cancelMedicineAlarms(appContext, updated)
            }
        }
    }
}

class MedicineViewModelFactory(
    private val repository: MedicineRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
       if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
           return MedicineViewModel(repository, context) as T
       }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}