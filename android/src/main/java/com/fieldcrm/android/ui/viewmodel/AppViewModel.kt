package com.fieldcrm.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.fieldcrm.shared.model.BorrowerModel
import com.fieldcrm.shared.model.LoanApplicationModel

sealed class Screen {
    data object Login : Screen()
    data object Dashboard : Screen()
    data object BorrowerList : Screen()
    data object BorrowerDetail : Screen()
    data object CreateBorrower : Screen()
    data object ApplicationList : Screen()
    data object ApplicationDetail : Screen()
    data object CreateApplication : Screen()
}

class AppViewModel : ViewModel() {
    private val _currentScreen = mutableStateOf<Screen>(Screen.Login)
    val currentScreen: State<Screen> = _currentScreen

    private val _authToken = mutableStateOf<String?>(null)
    val authToken: State<String?> = _authToken

    private val _selectedBorrower = mutableStateOf<BorrowerModel?>(null)
    val selectedBorrower: State<BorrowerModel?> = _selectedBorrower

    private val _selectedApplication = mutableStateOf<LoanApplicationModel?>(null)
    val selectedApplication: State<LoanApplicationModel?> = _selectedApplication

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setAuthToken(token: String) {
        _authToken.value = token
    }

    fun setSelectedBorrower(borrower: BorrowerModel?) {
        _selectedBorrower.value = borrower
    }

    fun setSelectedApplication(application: LoanApplicationModel?) {
        _selectedApplication.value = application
    }

    fun logout() {
        _authToken.value = null
        _currentScreen.value = Screen.Login
    }
}
