package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import java.util.UUID

class LoginViewModel : ViewModel() {
    private val _username = mutableStateOf("")
    val username: State<String> = _username

    private val _password = mutableStateOf("")
    val password: State<String> = _password

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    fun setUsername(value: String) {
        _username.value = value
        _errorMessage.value = null
    }

    fun setPassword(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    fun login(onSuccess: (String) -> Unit) {
        if (_username.value.isEmpty() || _password.value.isEmpty()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }

        _isLoading.value = true
        // Simulate authentication delay
        val token = "token_${UUID.randomUUID()}"
        _isLoading.value = false
        onSuccess(token)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
