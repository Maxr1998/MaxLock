package android.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import java.util.List;

import android_hidden.app.ActivityManager;

public interface IActivityManager {
    List<ActivityManager.RunningTaskInfo> getTasks(int maxNum);

    void setFocusedTask(int taskId);

    void registerProcessObserver(IProcessObserver observer);

    void unregisterProcessObserver(IProcessObserver observer);

    int startActivity(IApplicationThread caller, String callingPackage, Intent intent,
                      String resolvedType, IBinder resultTo, String resultWho, int requestCode,
                      int flags, ProfilerInfo profilerInfo, Bundle options);

    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token);

    /**
     * Since API 29
     */
    ContentProviderHolder getContentProviderExternal(String name, int userId, IBinder token, String tag);

    int getFocusedStackId();

    ActivityManager.StackInfo getStackInfo(int stackId);

    // Since Pie
    ActivityManager.StackInfo getFocusedStackInfo();
}