package com.fieldcrm.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fieldcrm.android.core.biometric.BiometricPromptManager
import com.fieldcrm.android.ui.navigation.FieldCRMApp
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by inject()
    private val appViewModel: AppViewModel by viewModel()

    private val promptManager by lazy { BiometricPromptManager(this) }
    private var pendingApplicationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingApplicationId = intent?.getStringExtra("application_id")
        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            FieldCRMTheme(role = appUiState.session?.role, darkTheme = appUiState.isDarkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldCRMApp(appViewModel, promptManager, pendingApplicationId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingApplicationId = intent.getStringExtra("application_id")
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.syncSession(onExpired = {
            appViewModel.setSessionExpired(true)
        })
    }
}
