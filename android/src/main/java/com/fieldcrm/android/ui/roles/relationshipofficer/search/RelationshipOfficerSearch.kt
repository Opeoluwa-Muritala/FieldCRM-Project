package com.fieldcrm.android.ui.roles.relationshipofficer.search
import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.dashboard.SearchResultsScreen
@Composable fun RelationshipOfficerSearch(onBack:()->Unit,onOpen:(String)->Unit)=SearchResultsScreen("Find Customers and Applications","Name, phone, BVN, NIN, or reference","Applications","relationship_officer",onBack,onOpen)
