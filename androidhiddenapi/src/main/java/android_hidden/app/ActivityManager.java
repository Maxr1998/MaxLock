package android_hidden.app;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.graphics.Bitmap;
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

    public static class RunningTaskInfo {
        /**
         * A unique identifier for this task.
         */
        public int id;

        /**
         * The stack that currently contains this task.
         */
        public int stackId;

        /**
         * The component launched as the first activity in the task.  This can
         * be considered the "application" of this task.
         */
        public ComponentName baseActivity;

        /**
         * The activity component at the top of the history stack of the task.
         * This is what the user is currently doing.
         */
        public ComponentName topActivity;

        /**
         * Thumbnail representation of the task's current state.  Currently
         * always null.
         */
        public Bitmap thumbnail;

        /**
         * Description of the task's current state.
         */
        public CharSequence description;

        /**
         * Number of activities in this task.
         */
        public int numActivities;

        /**
         * Number of activities that are currently running (not stopped
         * and persisted) in this task.
         */
        public int numRunning;

        /**
         * Last time task was run. For sorting.
         */
        public long lastActiveTime;

        /**
         * True if the task can go in the docked stack.
         */
        public boolean supportsSplitScreenMultiWindow;

        /**
         * The resize mode of the task. See ActivityInfo#resizeMode.
         */
        public int resizeMode;
    }
}