package com.fieldcrm.android.ui.roles.legal.queue

import androidx.compose.runtime.Composable
import com.fieldcrm.android.ui.screens.admin.LegalWorkspaceScreen

@Composable
fun LegalCaseQueue(onBack: () -> Unit, onOpenApplication: (String) -> Unit) = LegalWorkspaceScreen(onBack, onOpenApplication)
