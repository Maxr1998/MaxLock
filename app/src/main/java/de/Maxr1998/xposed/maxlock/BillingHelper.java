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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;

public class BillingHelper implements BillingProcessor.IBillingHandler {

    private final String[] productIds = {
            "donate_coke",
            "donate_beer",
            "donate_5",
            "donate_10"
    };
    private final int[] productIcons = {
            R.drawable.ic_coke_48dp,
            R.drawable.ic_beer_48dp,
            R.drawable.ic_favorite_red_small_48dp,
            R.drawable.ic_favorite_red_48dp
    };
    private BillingProcessor bp;
    private Context mContext;

    public BillingHelper(Context context) {
        mContext = context;
        bp = new BillingProcessor(context, mContext.getString(R.string.license_key), this);
        bp.loadOwnedPurchasesFromGoogle();
    }

    public void showDialog() {
        new ShowDialog().execute();
    }

    public BillingProcessor getBp() {
        return bp;
    }

    @Override
    public void onBillingInitialized() {

    }

    @Override
    public void onBillingError(int i, Throwable throwable) {

    }

    @Override
    public void onProductPurchased(String s, TransactionDetails transactionDetails) {
        ((SettingsActivity) mContext).restart();
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    public void finish() {
        bp.release();
    }

    private class ShowDialog extends AsyncTask<Void, Void, ListAdapter> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected ListAdapter doInBackground(Void... voids) {
            return new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
                @SuppressLint("NewApi")
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    View v = super.getView(position, convertView, parent);
                    TextView tv = (TextView) v.findViewById(android.R.id.text1);
                    String title;
                    try {
                        title = bp.getPurchaseListingDetails(productIds[position]).title;
                    } catch (NullPointerException e) {
                        title = productIds[position];
                        Toast.makeText(mContext, R.string.no_network_connected, Toast.LENGTH_SHORT).show();
                    }
                    tv.setText(title);
                    if (Util.noGingerbread())
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
            final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
            dialog.setAdapter(la, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    bp.purchase((Activity) mContext, productIds[i]);
                }
            }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialog.create().dismiss();
                }
            }).create().show();
        }
    }
}