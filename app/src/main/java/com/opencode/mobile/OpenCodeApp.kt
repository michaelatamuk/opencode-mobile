package com.opencode.mobile

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Keeps [WatchState.appForeground] accurate so finished-run notifications are
 * only suppressed while the user is actually looking at that session.
 */
class OpenCodeApp : Application() {

    private var started = 0

    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannels(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                started++
                WatchState.appForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                if (started == 0) WatchState.appForeground = false
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
