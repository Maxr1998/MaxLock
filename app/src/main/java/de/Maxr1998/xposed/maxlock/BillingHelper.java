package de.Maxr1998.xposed.maxlock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import de.Maxr1998.xposed.maxlock.ui.SettingsActivity;

public class BillingHelper implements BillingProcessor.IBillingHandler {

    private final String[] productIds = {
            "donate_coke",
            "donate_beer"
    };
    private final int[] productIcons = {
            R.drawable.ic_coke_48dp,
            R.drawable.ic_beer_48dp
    };
    private BillingProcessor bp;
    private Context mContext;

    public BillingHelper(Context context) {
        mContext = context;
        bp = new BillingProcessor(context, mContext.getString(R.string.license_key), this);
        bp.loadOwnedPurchasesFromGoogle();
    }

    public void showDialog() {
        ListAdapter listAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
            @SuppressLint("NewApi")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                tv.setText(bp.getPurchaseListingDetails(productIds[position]).title);
                if (Util.noGingerbread())
                    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                else
                    tv.setCompoundDrawablesWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                int dp5 = (int) (5 * mContext.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);
                return v;
            }
        };

        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
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
        ((SettingsActivity) mContext).restart(false);
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    public void finish() {
        bp.release();
    }
}
