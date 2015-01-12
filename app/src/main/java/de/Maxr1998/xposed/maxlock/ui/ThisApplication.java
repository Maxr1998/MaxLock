package de.Maxr1998.xposed.maxlock.ui;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import de.Maxr1998.xposed.maxlock.R;

public class ThisApplication extends Application {

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<>();

    public ThisApplication() {
        super();
    }

    synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.app_tracker) : analytics.newTracker("UA-58429761-1");
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public enum TrackerName {
        APP_TRACKER // Tracker used only in this app.
    }

}
