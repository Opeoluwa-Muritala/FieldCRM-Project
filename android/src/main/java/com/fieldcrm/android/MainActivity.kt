package com.fieldcrm.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fieldcrm.android.core.biometric.BiometricPromptManager
import com.fieldcrm.android.ui.navigation.FieldCRMApp
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by inject()
    private val appViewModel: AppViewModel by inject()

    private val promptManager by lazy { BiometricPromptManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            FieldCRMTheme(role = appUiState.session?.role, darkTheme = appUiState.isDarkMode) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldCRMApp(appViewModel, promptManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.syncSession(onExpired = {
            appViewModel.setSessionExpired(true)
        })
    }
}
