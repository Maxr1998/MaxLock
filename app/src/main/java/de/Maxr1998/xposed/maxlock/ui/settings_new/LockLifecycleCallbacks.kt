package de.Maxr1998.xposed.maxlock.ui.settings_new

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler

class LockLifecycleCallbacks(private val vm: SettingsViewModel) : Application.ActivityLifecycleCallbacks {
    private val handler = Handler()
    private val lock = Runnable { vm.locked = true }

    override fun onActivityStarted(activity: Activity) {
        handler.postDelayed({
            // Delayed to make sure the previous activity
            // was stopped already
            handler.removeCallbacks(lock)
        }, 3000)
    }

    override fun onActivityStopped(activity: Activity) {
        // Lock in 5 seconds if no other activity removes our callback
        handler.postDelayed(lock, 5000)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}
    override fun onActivityDestroyed(activity: Activity) {}
}