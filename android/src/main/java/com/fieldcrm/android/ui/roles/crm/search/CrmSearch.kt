package com.fieldcrm.android.ui.roles.crm.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun CrmSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search CRM Records","Application, customer, or payment reference","CRM records","crm",onBack,onOpen)
