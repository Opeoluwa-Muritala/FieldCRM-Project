package com.fieldcrm.android.ui.roles.teamlead.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun TeamLeadSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Search Team Applications","Customer, officer, or application reference","Team applications","team_lead",onBack,onOpen)
