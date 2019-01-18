package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import android_hidden.app.ActivityManager;

public interface IActivityManager {
    void registerProcessObserver(IProcessObserver observer);

    void unregisterProcessObserver(IProcessObserver observer);

    WaitResult startActivityAndWait(IApplicationThread caller, String callingPackage, Intent intent,
                                    String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                                    int flags, ProfilerInfo profilerInfo, Bundle options, int userId);

    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token);

    int getFocusedStackId();

    ActivityManager.StackInfo getStackInfo(int stackId);

    // Since Pie
    ActivityManager.StackInfo getFocusedStackInfo();
}