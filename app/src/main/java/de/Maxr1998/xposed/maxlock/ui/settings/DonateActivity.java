/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2016 Max Rumpf alias Maxr1998
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
import android.content.ComponentName;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.List;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.util.MLPreferences;
import de.Maxr1998.xposed.maxlock.util.Util;

import static de.Maxr1998.xposed.maxlock.util.Util.LOG_TAG_IAB;

public class DonateActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    private static final String[] productIds = {
            "donate_coke",
            "donate_beer",
            "donate_5",
            "donate_10"
    };
    private static final int[] productIcons = {
            R.drawable.ic_coke_48dp,
            R.drawable.ic_beer_48dp,
            R.drawable.ic_favorite_small_48dp,
            R.drawable.ic_favorite_48dp
    };
    private BillingProcessor bp;
    private TextView donationStatusText;

    private CustomTabsServiceConnection mConnection;
    private CustomTabsSession mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        donationStatusText = (TextView) findViewById(R.id.donation_status);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                String licenseKey = getString(R.string.license_key);
                if (BillingProcessor.isIabServiceAvailable(DonateActivity.this) && !licenseKey.equals("DUMMY") && licenseKey.startsWith("M")) {
                    bp = new BillingProcessor(DonateActivity.this, licenseKey, DonateActivity.this);
                }
                return null;
            }
        }.execute();

        Button donatePayPal = (Button) findViewById(R.id.donate_paypal);
        donatePayPal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent intent = new CustomTabsIntent.Builder(mSession)
                        .setShowTitle(true)
                        .enableUrlBarHiding()
                        .setToolbarColor(Color.WHITE)
                        .build();
                intent.launchUrl(DonateActivity.this, Common.PAYPAL_DONATE_URI);
            }
        });

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                customTabsClient.warmup(0);
                mSession = customTabsClient.newSession(new CustomTabsCallback());
                if (mSession == null) {
                    return;
                }
                mSession.mayLaunchUrl(Common.PAYPAL_DONATE_URI, null, null);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
        };
        CustomTabsClient.bindCustomTabsService(this, "com.android.chrome", mConnection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when BillingProcessor was initialized and it's ready to purchase
     */
    @Override
    public void onBillingInitialized() {
        reloadBilling();
        ArrayAdapter<String> productsAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView) v.findViewById(android.R.id.text1);
                String title;
                try {
                    title = bp.getPurchaseListingDetails(productIds[position]).title.replace(" (MaxLock)", "");
                } catch (NullPointerException e) {
                    title = "Error";
                }
                tv.setText(title);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                } else {
                    tv.setCompoundDrawablesWithIntrinsicBounds(productIcons[position], 0, 0, 0);
                }
                tv.setCompoundDrawablePadding(Util.dpToPx(DonateActivity.this, 12));
                return v;
            }
        };
        ListView productsList = (ListView) findViewById(R.id.donate_products_list);
        assert productsList != null;
        productsList.setAdapter(productsAdapter);
        productsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                bp.loadOwnedPurchasesFromGoogle();
                if (!bp.isPurchased(productIds[position])) {
                    bp.purchase(DonateActivity.this, productIds[position]);
                } else {
                    AlertDialog.Builder b = new AlertDialog.Builder(DonateActivity.this);
                    b.setMessage(R.string.dialog_message_already_bought);
                    b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(LOG_TAG_IAB, bp.listOwnedProducts().toString());
                            if (bp.consumePurchase(productIds[position])) {
                                Log.i(LOG_TAG_IAB, bp.listOwnedProducts().toString());
                                bp.purchase(DonateActivity.this, productIds[position]);
                            }
                        }
                    });
                    b.setNegativeButton(android.R.string.cancel, null);
                    b.show();
                }
            }
        });
    }

    /**
     * Called when requested PRODUCT ID was successfully purchased
     */
    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                reloadBilling();
            }
        }, 200);
    }

    /**
     * Called when some error occurred. See Constants class for more details
     */
    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Log.e("ML-iab", "Error!");
    }

    private void reloadBilling() {
        Log.i(LOG_TAG_IAB, "Loading…");
        if (bp.loadOwnedPurchasesFromGoogle()) {
            onPurchaseHistoryRestored();
        }
    }

    /**
     * Called when purchase history was restored and the list of all owned PRODUCT ID's
     * was loaded from Google Play
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onPurchaseHistoryRestored() {
        View progress = findViewById(android.R.id.progress);
        assert progress != null;
        progress.setVisibility(View.GONE);
        Log.i(LOG_TAG_IAB, "Loaded.");
        List<String> products = bp.listOwnedProducts();
        if (products.size() > 0) {
            MLPreferences.getPreferences(this).edit().putBoolean(Common.DONATED, true).apply();
            donationStatusText.setText(R.string.donate_status_donated);
        } else {
            donationStatusText.setText(R.string.donate_status_not_donated);
        }
    }

    @Override
    protected void onDestroy() {
        if (bp != null) {
            bp.release();
        }
        if (mConnection != null) {
            unbindService(mConnection);
        }
        super.onDestroy();
    }
}