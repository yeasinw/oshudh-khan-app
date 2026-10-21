package com.yeasinx.oshudhkhan.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.yeasinx.oshudhkhan.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = false, val error: String? = null, val resetSent: Boolean = false)

// Profile email prefill happens in CloudSync, after the cloud merge
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    val accountEmail: StateFlow<String?> = authRepository.currentUser
        .map { it?.email }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun submit(email: String, password: String, createAccount: Boolean) {
        val trimmed = email.trim()
        val invalid = when {
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Enter a valid email"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        if (invalid != null) {
            _uiState.value = AuthUiState(error = invalid)
            return
        }
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = if (createAccount) authRepository.signUp(trimmed, password)
            else authRepository.signIn(trimmed, password)
            result
                .onSuccess { _uiState.value = AuthUiState() }
                .onFailure { _uiState.value = AuthUiState(error = authErrorMessage(it)) }
        }
    }

    fun resetPassword(email: String) {
        val trimmed = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            _uiState.value = AuthUiState(error = "Enter your email to reset the password")
            return
        }
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            authRepository.sendPasswordReset(trimmed)
                .onSuccess { _uiState.value = AuthUiState(resetSent = true) }
                .onFailure { _uiState.value = AuthUiState(error = authErrorMessage(it)) }
        }
    }

    // Local data stays on the device after signing out.
    fun signOut() = authRepository.signOut()

    fun clearMessages() = _uiState.update { it.copy(error = null, resetSent = false) }

    private fun authErrorMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters"
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> "Incorrect email or password"
        is FirebaseAuthUserCollisionException -> "An account with this email already exists"
        is FirebaseNetworkException -> "No internet connection"
        else -> "Something went wrong. Try again."
    }
}

class AuthViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
