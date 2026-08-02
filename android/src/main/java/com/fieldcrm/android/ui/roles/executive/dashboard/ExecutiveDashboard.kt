package com.fieldcrm.android.ui.roles.executive.dashboard
import androidx.compose.runtime.Composable
import com.fieldcrm.android.data.api.DashboardMetrics
import com.fieldcrm.android.ui.roles.shared.*
import com.fieldcrm.android.ui.viewmodel.Screen
@Composable fun ExecutiveDashboard(metrics:DashboardMetrics?,loading:Boolean,error:String?,onOpen:(Screen)->Unit,onSignOut:()->Unit)=WorkspaceDashboardPage("Executive","Your authorized executive workspace.",emptyList(),emptyList(),loading,error,onOpen,onSignOut)
