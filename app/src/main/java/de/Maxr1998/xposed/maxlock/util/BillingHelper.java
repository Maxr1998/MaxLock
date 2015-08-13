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

package de.Maxr1998.xposed.maxlock.util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import de.Maxr1998.xposed.maxlock.R;

public abstract class BillingHelper {

    private static final String[] productIds = {
            "donate_coke",
            "donate_beer",
            "donate_5",
            "donate_10"
    };
    private static final int[] productIcons = {
            R.drawable.ic_coke_48dp,
            R.drawable.ic_beer_48dp,
            R.drawable.ic_favorite_red_small_48dp,
            R.drawable.ic_favorite_red_48dp
    };

    public static void showDialog(BillingProcessor bp, Activity activity) {
        if (bp != null && GooglePlayServiceAvailable(activity)) {
            new ShowDialog(bp, activity).execute();
        } else {
            Toast.makeText(activity, "Google Play Services unavailable", Toast.LENGTH_LONG).show();
        }
    }

    public static boolean GooglePlayServiceAvailable(Context c) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(c) == ConnectionResult.SUCCESS;
    }

    public static boolean donated(Context c, BillingProcessor bp) {
        return bp != null && GooglePlayServiceAvailable(c) && !bp.listOwnedProducts().isEmpty();
    }

    private static class ShowDialog extends AsyncTask<Void, Void, ListAdapter> {
        private final ProgressDialog progressDialog;
        private final BillingProcessor bp;
        private final Activity mContext;

        public ShowDialog(BillingProcessor bp, Activity activity) {
            this.bp = bp;
            mContext = activity;
            progressDialog = new ProgressDialog(activity);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected ListAdapter doInBackground(Void... voids) {
            return new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);
                    String title;
                    try {
                        title = bp.getPurchaseListingDetails(productIds[position]).title;
                    } catch (NullPointerException e) {
                        return null;
                    }
                    tv.setText(title);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                    } else {
                        tv.setCompoundDrawablesWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                    }
                    int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);
                    return v;
                }
            };
        }

        @Override
        protected void onPostExecute(final ListAdapter la) {
            if (la == null) {
                Toast.makeText(mContext, R.string.toast_no_network_connected, Toast.LENGTH_SHORT).show();
                return;
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    progressDialog.dismiss();
                    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                    dialog.setAdapter(la, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            bp.purchase(mContext, productIds[i]);
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    }).create().show();
                }
            }, 500);
        }
    }
}