package com.fieldcrm.android.core.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MutationEvents {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val events = _events.asSharedFlow()

    fun triggerMutation(applicationId: String) {
        _events.tryEmit(applicationId)
    }
}
