package com.fieldcrm.android.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.fieldcrm.android.data.repository.ApplicationRepository
import com.fieldcrm.android.data.repository.AuthRepository
import com.fieldcrm.android.data.repository.BorrowerRepository
import com.fieldcrm.android.ui.viewmodel.AppViewModel
import com.fieldcrm.android.ui.viewmodel.ApplicationViewModel
import com.fieldcrm.android.ui.viewmodel.BorrowerViewModel
import com.fieldcrm.android.ui.viewmodel.LoginViewModel
import com.fieldcrm.shared.api.FieldCRMClient
import com.fieldcrm.shared.db.AppDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

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

    // Deployed Render production backend URL client
    single { FieldCRMClient("https://fieldcrm.onrender.com") }

    // Repositories
    single { BorrowerRepository(get(), get()) }
    single { ApplicationRepository(get(), get(), get()) }
    single { AuthRepository(get()) }

    // ViewModels
    viewModel { AppViewModel() }
    viewModel { LoginViewModel(get()) }
    viewModel { BorrowerViewModel(androidApplication(), get()) }
    viewModel { ApplicationViewModel(androidApplication(), get()) }
}
