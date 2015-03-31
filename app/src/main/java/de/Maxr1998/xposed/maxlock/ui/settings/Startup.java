/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

import java.io.File;

import de.Maxr1998.xposed.maxlock.BillingHelper;
import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;
import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;

public class Startup extends AsyncTask<Boolean, Void, Void> {

    private Context mContext;
    private boolean isFirstStart;
    private SharedPreferences prefs;

    public Startup(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    protected Void doInBackground(Boolean... firstStart) {
        isFirstStart = firstStart[0];
        if (isFirstStart) {
            prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
        }
        // Prefs
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences_main, false);
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences_locking_type, false);
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences_locking_ui, false);
        // Pro setup
        if (!prefs.getBoolean(Common.ENABLE_PRO, false)) {
            prefs.edit().putBoolean(Common.ENABLE_LOGGING, false).apply();
            prefs.edit().putBoolean(Common.IMOD_DELAY_APP_ENABLED, false).apply();
            prefs.edit().putBoolean(Common.IMOD_DELAY_GLOBAL_ENABLED, false).apply();
        }
        // Like app dialog
        if (!prefs.getBoolean(Common.DIALOG_SHOW_NEVER, false) && System.currentTimeMillis() - prefs.getLong(Common.FIRST_START_TIME, System.currentTimeMillis()) > 10 * 24 * 3600 * 1000) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            @SuppressLint("InflateParams") View dialogView = ((Activity) mContext).getLayoutInflater().inflate(R.layout.dialog_like_app, null);
            final CheckBox checkBox = (CheckBox) dialogView.findViewById(R.id.dialog_cb_never_again);
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (checkBox.isChecked())
                        prefs.edit().putBoolean(Common.DIALOG_SHOW_NEVER, true).apply();
                    switch (i) {
                        case -3:
                            try {
                                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Common.PKG_NAME)));
                            } catch (android.content.ActivityNotFoundException e) {
                                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + Common.PKG_NAME)));
                            }
                            break;
                        case -1:
                            BillingHelper.showDialog(((SettingsActivity) mContext).getBillingProcessor(), (Activity) mContext);
                            break;
                    }
                    prefs.edit().putLong(Common.FIRST_START_TIME, System.currentTimeMillis()).apply();
                }
            };
            builder.setTitle(R.string.dialog_like_app)
                    .setView(dialogView)
                    .setPositiveButton(R.string.dialog_button_donate, onClickListener)
                    .setNeutralButton(R.string.dialog_button_rate, onClickListener)
                    .setNegativeButton(android.R.string.cancel, onClickListener)
                    .create().show();
        }
        // SnackBar with alert
        @SuppressWarnings("ConstantConditions")
        boolean noLockType = prefs.getString(Common.LOCKING_TYPE, "").equals("");
        boolean noPackages = !new File(Util.dataDir(mContext) + File.separator + "shared_prefs" + File.separator + Common.PREFS_PACKAGES + ".xml").exists();
        String snackBar = (noLockType ? mContext.getString(R.string.no_locking_type) + " " : "") + (noPackages ? mContext.getString(R.string.no_locked_apps) : "");
        if (noPackages || noLockType) {
            SnackbarManager.show(Snackbar.with(mContext).text(snackBar).type(noLockType && noPackages ? SnackbarType.MULTI_LINE : SnackbarType.SINGLE_LINE)
                    .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE).swipeToDismiss(false));
        }
        // Other
        Util.cleanUp(mContext);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (isFirstStart) {
            prefs.edit().putBoolean(Common.FIRST_START, false).apply();
        }
        System.out.println("Startup finished");
    }
}
