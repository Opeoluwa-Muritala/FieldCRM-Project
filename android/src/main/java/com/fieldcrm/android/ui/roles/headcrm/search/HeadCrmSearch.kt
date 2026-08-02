package com.fieldcrm.android.ui.roles.headcrm.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun HeadCrmSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search CRM Oversight Records","Application, customer, or condition","Oversight records","head_crm",onBack,onOpen)
