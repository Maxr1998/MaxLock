package android_hidden.app;

import android.os.Bundle;

public abstract class ActivityOptions {
    public static ActivityOptions makeBasic() {
        return null;
    }

    public abstract int getLaunchStackId();

    public abstract void setLaunchStackId(int id);

    public abstract int getLaunchTaskId();

    public abstract void setLaunchTaskId(int id);

    public abstract Bundle toBundle();
}
