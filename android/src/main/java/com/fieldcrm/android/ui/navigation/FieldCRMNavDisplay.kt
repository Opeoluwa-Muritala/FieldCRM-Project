package com.fieldcrm.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fieldcrm.android.ui.navigation.impl.adminEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.applicationEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.authEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.borrowerEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.documentEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.onboardingEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.queueEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.reviewEntryBuilder
import com.fieldcrm.android.ui.navigation.impl.shellEntryBuilder
import com.fieldcrm.android.ui.viewmodel.Screen

@Composable
fun FieldCRMNavDisplay(
    backStack: NavBackStack<NavKey>,
    onBack: () -> Unit,
    content: @Composable (Screen) -> Unit
) {
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>()
        ),
        entryProvider = entryProvider(
            fallback = { unknownScreen ->
                NavEntry(unknownScreen) {
                    content(Screen.Login)
                }
            }
        ) {
            authEntryBuilder(content)
            onboardingEntryBuilder(content)
            shellEntryBuilder(content)
            borrowerEntryBuilder(content)
            applicationEntryBuilder(content)
            documentEntryBuilder(content)
            reviewEntryBuilder(content)
            queueEntryBuilder(content)
            committeeEntryBuilder(content)
            adminEntryBuilder(content)
        }
    )
}
