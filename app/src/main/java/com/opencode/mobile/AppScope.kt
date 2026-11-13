package com.opencode.mobile

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application-lifetime scope for network work.
 *
 * Using rememberCoroutineScope for requests means a screen leaving composition can
 * cancel a job mid-flight and Compose reports "the coroutine scope left the composition".
 * Work launched here survives navigation instead.
 */
val AppScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
