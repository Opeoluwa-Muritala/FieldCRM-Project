package com.fieldcrm.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fieldcrm.android.ui.screens.*
import com.fieldcrm.android.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    FieldCRMApp()
                }
            }
        }
    }
}

@Composable
fun FieldCRMApp() {
    val appViewModel: AppViewModel = viewModel()
    val loginViewModel: LoginViewModel = viewModel()
    val borrowerViewModel: BorrowerViewModel = viewModel()
    val applicationViewModel: ApplicationViewModel = viewModel()

    when (appViewModel.currentScreen.value) {
        Screen.Login -> {
            LoginScreenView(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    appViewModel.setAuthToken(it)
                    appViewModel.navigateTo(Screen.Dashboard)
                }
            )
        }
        Screen.Dashboard -> {
            DashboardScreenView(
                onNavigateToBorrowers = {
                    appViewModel.navigateTo(Screen.BorrowerList)
                },
                onNavigateToApplications = {
                    appViewModel.navigateTo(Screen.ApplicationList)
                },
                onLogout = {
                    appViewModel.logout()
                }
            )
        }
        Screen.BorrowerList -> {
            BorrowerListScreenView(
                viewModel = borrowerViewModel,
                onBorrowerSelected = { borrower ->
                    appViewModel.setSelectedBorrower(borrower)
                    appViewModel.navigateTo(Screen.BorrowerDetail)
                },
                onAddBorrower = {
                    appViewModel.navigateTo(Screen.CreateBorrower)
                },
                onBackClick = {
                    appViewModel.navigateTo(Screen.Dashboard)
                }
            )
        }
        Screen.BorrowerDetail -> {
            appViewModel.selectedBorrower.value?.let { borrower ->
                BorrowerDetailScreenView(
                    borrower = borrower,
                    onBackClick = {
                        appViewModel.setSelectedBorrower(null)
                        appViewModel.navigateTo(Screen.BorrowerList)
                    },
                    onCreateApplication = {
                        applicationViewModel.setSelectedBorrowerForApp(borrower)
                        appViewModel.navigateTo(Screen.CreateApplication)
                    }
                )
            }
        }
        Screen.CreateBorrower -> {
            CreateBorrowerScreenView(
                viewModel = borrowerViewModel,
                onBorrowerCreated = { newBorrower ->
                    appViewModel.navigateTo(Screen.BorrowerList)
                },
                onBackClick = {
                    appViewModel.navigateTo(Screen.BorrowerList)
                }
            )
        }
        Screen.ApplicationList -> {
            ApplicationListScreenView(
                viewModel = applicationViewModel,
                borrowers = borrowerViewModel.borrowers.value,
                onApplicationSelected = { app ->
                    appViewModel.setSelectedApplication(app)
                    appViewModel.navigateTo(Screen.ApplicationDetail)
                },
                onAddApplication = {
                    appViewModel.navigateTo(Screen.CreateApplication)
                },
                onBackClick = {
                    appViewModel.navigateTo(Screen.Dashboard)
                }
            )
        }
        Screen.ApplicationDetail -> {
            appViewModel.selectedApplication.value?.let { app ->
                ApplicationDetailScreenView(
                    application = app,
                    borrower = borrowerViewModel.borrowers.value.find { it.id == app.borrower_id },
                    onBackClick = {
                        appViewModel.setSelectedApplication(null)
                        appViewModel.navigateTo(Screen.ApplicationList)
                    }
                )
            }
        }
        Screen.CreateApplication -> {
            CreateApplicationScreenView(
                viewModel = applicationViewModel,
                borrowers = borrowerViewModel.borrowers.value,
                onApplicationCreated = { newApp ->
                    appViewModel.navigateTo(Screen.ApplicationList)
                },
                onBackClick = {
                    appViewModel.navigateTo(Screen.ApplicationList)
                }
            )
        }
    }
}
