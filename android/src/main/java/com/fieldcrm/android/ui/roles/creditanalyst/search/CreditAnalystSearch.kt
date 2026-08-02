package com.fieldcrm.android.ui.roles.creditanalyst.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun CreditAnalystSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Credit Dossiers","Application reference, customer, or exception","Credit dossiers","credit_analyst",onBack,onOpen)
