package com.yeasinx.oshudhkhan.ui.profile

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yeasinx.oshudhkhan.data.UserProfile
import com.yeasinx.oshudhkhan.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileErrors(val name: String? = null, val age: String? = null, val email: String? = null) {
    val isEmpty get() = name == null && age == null && email == null
}

class ProfileViewModel(private val repository: UserRepository) : ViewModel() {

    val profile: StateFlow<UserProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    // Returns validation errors; saves only when there are none.
    fun save(name: String, age: String, email: String): ProfileErrors {
        val parsedAge = age.trim().toIntOrNull()
        val errors = ProfileErrors(
            name = if (name.isBlank()) "Name is required" else null,
            age = if (age.isNotBlank() && (parsedAge == null || parsedAge !in 1..120)) "Enter an age between 1 and 120" else null,
            email = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Enter a valid email" else null
        )
        if (errors.isEmpty) {
            viewModelScope.launch {
                repository.save(UserProfile(name.trim(), parsedAge, email.trim()))
            }
        }
        return errors
    }
}

class ProfileViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
