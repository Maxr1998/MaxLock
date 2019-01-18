package android.app;

public interface IProcessObserver {
    void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities);

    void onProcessDied(int pid, int uid);

    class Stub implements IProcessObserver {
        @Override
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            // Stub
        }

        @Override
        public void onProcessDied(int pid, int uid) {
            // Stub
        }
    }
}