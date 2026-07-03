package com.fieldcrm.android.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fieldcrm.android.data.repository.ApplicationRepository
import com.fieldcrm.android.data.repository.AuthRepository
import com.fieldcrm.android.data.repository.BorrowerRepository
import com.fieldcrm.android.data.repository.ConfigRepository
import com.fieldcrm.android.data.repository.DashboardRepository
import com.fieldcrm.android.data.repository.NotificationsRepository
import com.fieldcrm.android.data.repository.SearchRepository
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.AuditTrailViewModel
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.android.ui.viewmodel.ConfigViewModel
import com.fieldcrm.android.ui.viewmodel.DashboardViewModel
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import com.fieldcrm.android.ui.viewmodel.NotificationsViewModel
import com.fieldcrm.android.ui.viewmodel.SearchViewModel
import com.fieldcrm.android.core.session.SessionStore
import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import io.ktor.client.HttpClient
import com.fieldcrm.android.data.api.MobileApiService
import com.fieldcrm.android.data.api.MobileApiServiceImpl

val appModule = module {
    // SQLite Database Driver
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = androidContext(),
            name = "fieldcrm_offline.db"
        )
    }

    // SQLite AppDatabase instance
    single { AppDatabase(get()) }

    // Session persistence (SharedPreferences, 48-hour TTL)
    single { SessionStore(androidContext()) }

    // Deployed Render production backend URL client
    single { FieldCRMClient("https://fieldcrm.onrender.com") }

    // HttpClient and MobileApiService
    single<MobileApiService> { MobileApiServiceImpl(get<FieldCRMClient>().httpClient, "https://fieldcrm.onrender.com") }

    // Repositories
    single { BorrowerRepository(get(), get()) }
    single { ApplicationRepository(get(), get(), get()) }
    single { AuthRepository(get(), get()) }
    single { NotificationsRepository(get()) }
    single { ConfigRepository(get()) }
    single { DashboardRepository(get()) }
    single { SearchRepository(get()) }

    // ViewModels
    viewModel { AppViewModel(get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { BorrowerViewModel(androidApplication(), get()) }
    viewModel { ApplicationViewModel(androidApplication(), get(), get()) }
    viewModel { NotificationsViewModel(get()) }
    viewModel { ConfigViewModel(get()) }
    viewModel { DashboardViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { AuditTrailViewModel(get()) }
    viewModel { com.fieldcrm.android.ui.viewmodel.DocumentUploadViewModel(get()) }
    viewModel { com.fieldcrm.android.ui.viewmodel.SyncViewModel(get(), get()) }
}
