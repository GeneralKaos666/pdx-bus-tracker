package com.trimettransit.tracker.model.domain

enum class ErrorCopyKind { CONNECTION, LOCAL, EMPTY }

fun errorCopyKind(isNetworkError: Boolean, hasCachedData: Boolean): ErrorCopyKind =
    if (isNetworkError && !hasCachedData) ErrorCopyKind.CONNECTION else ErrorCopyKind.LOCAL
