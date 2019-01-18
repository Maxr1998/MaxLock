package android_hidden.app;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.graphics.Rect;

public class ActivityManager {

    public static IActivityManager getService() {
        throw new UnsupportedOperationException("Stub");
    }

    public static class StackInfo {
        public int stackId;
        public Rect bounds = new Rect();
        public int[] taskIds;
        public String[] taskNames;
        public Rect[] taskBounds;
        public int[] taskUserIds;
        public ComponentName topActivity;
        public int displayId;
        public int userId;
        public boolean visible;
        // Index of the stack in the display's stack list, can be used for comparison of stack order
        public int position;
    }
}