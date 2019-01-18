package de.Maxr1998.xposed.maxlock.daemon

import android.app.IActivityManager
import android.content.IContentProvider
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import de.Maxr1998.xposed.maxlock.BuildConfig
import de.Maxr1998.xposed.maxlock.Common.PREFERENCE_PROVIDER_AUTHORITY

private val PREF_COLS = arrayOf("type", "value")

class RemotePreferencesHelper(private val activityManager: IActivityManager) {
    private var contentProvider: IContentProvider? = null

    private fun getContentProvider(): IContentProvider? {
        val provider = contentProvider ?: run {
            val token: IBinder = Binder()
            val userHandle = 0 /* UserHandle.USER_SYSTEM */
            return activityManager.getContentProviderExternal(PREFERENCE_PROVIDER_AUTHORITY, userHandle, token).provider.apply {
                contentProvider = this
            }
        }
        return if (provider.asBinder().isBinderAlive) provider else null
    }

    private fun query(preferenceFile: String, key: String) = getContentProvider()
            ?.query(BuildConfig.APPLICATION_ID, createUri(preferenceFile, key), PREF_COLS, null, null)

    fun queryInt(preferenceFile: String, key: String, default: Int): Int {
        query(preferenceFile, key).use { cursor ->
            return if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) != 0) {
                cursor.getInt(1)
            } else default
        }
    }

    private fun createUri(preferenceFile: String, pref: String): Uri = Uri
            .parse("content://$PREFERENCE_PROVIDER_AUTHORITY").buildUpon()
            .appendPath(preferenceFile)
            .appendPath(pref)
            .build()
}