package com.fieldcrm.android.ui.roles.executivedirector.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun ExecutiveDirectorSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Executive Decisions","Customer or application reference","Decision dossiers","executive_director",onBack,onOpen)
