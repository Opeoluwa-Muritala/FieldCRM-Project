package com.fieldcrm.android.ui.roles.audit.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun AuditSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Auditable Records","Actor, action, resource, or reference","Audit records","audit",onBack,onOpen)
