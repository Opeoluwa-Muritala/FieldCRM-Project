package com.fieldcrm.android.ui.roles.legal.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun LegalSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Legal Dossiers","Customer, application, or collateral reference","Legal dossiers","legal",onBack,onOpen)
