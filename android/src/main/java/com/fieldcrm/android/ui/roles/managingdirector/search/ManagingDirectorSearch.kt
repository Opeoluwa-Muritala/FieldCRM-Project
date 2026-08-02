package com.fieldcrm.android.ui.roles.managingdirector.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun ManagingDirectorSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Final Decisions","Customer or application reference","Decision dossiers","managing_director",onBack,onOpen)
