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

package de.Maxr1998.xposed.maxlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class BillingHelper {

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
        private ProgressDialog progressDialog;
        private boolean networkError = false;
        private BillingProcessor bp;
        private Context mContext;
        private Activity mActivity;

        public ShowDialog(BillingProcessor bp, Activity activity) {
            this.bp = bp;
            mActivity = activity;
            mContext = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(mContext, "", "", true);
            progressDialog.setCanceledOnTouchOutside(false);
        }

        @Override
        protected ListAdapter doInBackground(Void... voids) {
            return new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);
                    String title;
                    int c = 0;
                    do {
                        try {
                            networkError = false;
                            title = bp.getPurchaseListingDetails(productIds[position]).title;
                        } catch (NullPointerException e) {
                            title = productIds[position];
                            networkError = true;
                        }
                        c++;
                    } while ((!networkError && c <= 3));
                    tv.setText(title);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                    else
                        tv.setCompoundDrawablesWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                    int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                    tv.setCompoundDrawablePadding(dp5);
                    return v;
                }
            };
        }

        @Override
        protected void onPostExecute(ListAdapter la) {
            progressDialog.dismiss();
            if (networkError)
                Toast.makeText(mContext, R.string.no_network_connected, Toast.LENGTH_SHORT).show();
            AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setAdapter(la, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    bp.purchase(mActivity, productIds[i]);
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        }
    }
}