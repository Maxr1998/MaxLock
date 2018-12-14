package de.Maxr1998.xposed.maxlock.ui.settings_new

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HiddenSettingsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.startActivity(Intent(context, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}