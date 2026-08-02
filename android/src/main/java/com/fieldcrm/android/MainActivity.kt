package com.fieldcrm.android

import android.os.Bundle
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fieldcrm.android.ui.navigation.FieldCRMApp
import com.fieldcrm.android.ui.theme.FieldCRMTheme
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModel()
    private val appViewModel: AppViewModel by viewModel()

    private var pendingApplicationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingApplicationId = intent?.getStringExtra("application_id")
        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            val themePrefs = androidx.compose.runtime.remember { getSharedPreferences("fieldcrm_theme_prefs", MODE_PRIVATE) }
            var themeState by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(themePrefs.getString("theme", "System") ?: "System")
            }

            androidx.compose.runtime.DisposableEffect(themePrefs) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == "theme") {
                        themeState = themePrefs.getString("theme", "System") ?: "System"
                    }
                }
                themePrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    themePrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val isDark = when (themeState) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FieldCRMTheme(role = appUiState.session?.role, darkTheme = isDark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    FieldCRMApp(appViewModel, pendingApplicationId)
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
