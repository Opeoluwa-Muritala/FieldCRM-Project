package com.fieldcrm.android.ui.roles.supervisor.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun SupervisorSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Supervised Applications","Customer or application reference","Supervised applications","supervisor",onBack,onOpen)
