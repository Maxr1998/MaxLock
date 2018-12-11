package net.dinglisch.android.tasker

import android.os.Handler
import android.os.ResultReceiver
import androidx.annotation.Keep

@Keep
class PluginResultReceiver(handler: Handler?) : ResultReceiver(handler)