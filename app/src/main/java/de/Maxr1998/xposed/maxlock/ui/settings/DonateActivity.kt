/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018 Max Rumpf alias Maxr1998
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

package de.Maxr1998.xposed.maxlock.ui.settings

import android.app.LoaderManager
import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Loader
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.MLPreferences
import de.Maxr1998.xposed.maxlock.util.Util
import de.Maxr1998.xposed.maxlock.util.Util.LOG_TAG_IAB

class DonateActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<BillingProcessor>, BillingProcessor.IBillingHandler {

    private var unsafeBillingProcessor: BillingProcessor? = null
    private lateinit var bp: BillingProcessor
    private lateinit var donationStatusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        Util.setTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        donationStatusText = findViewById(R.id.donation_status)

        loaderManager.apply {
            if (getLoader<BillingProcessor>(0).let { it != null && it.isReset })
                restartLoader(0, null, this@DonateActivity)
            else initLoader(0, null, this@DonateActivity)
        }
    }

    private fun setDonationStatus(donated: Boolean = bp.listOwnedProducts().size > 0) {
        val progress = findViewById<View>(android.R.id.progress)!!
        progress.visibility = View.GONE
        Log.i(LOG_TAG_IAB, "Loaded.")
        MLPreferences.getPreferences(this).edit().putBoolean(Common.DONATED, donated).apply()
        donationStatusText.setText(if (donated) R.string.donate_status_donated else R.string.donate_status_not_donated)
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<BillingProcessor> {
        return BillingProcessorLoader(this, this)
    }

    override fun onLoadFinished(loader: Loader<BillingProcessor>, processor: BillingProcessor?) {
        unsafeBillingProcessor = processor
        loader.reset()
    }

    override fun onLoaderReset(loader: Loader<BillingProcessor>) {}

    /**
     * Called when BillingProcessor was initialized and it's ready to purchase
     */
    override fun onBillingInitialized() {
        unsafeBillingProcessor?.let {
            bp = it
            setDonationStatus()
            val productsAdapter = object : ArrayAdapter<String>(this, android.R.layout.select_dialog_item, android.R.id.text1, productIds) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    val tv = v.findViewById<TextView>(android.R.id.text1)
                    val productName = bp.getPurchaseListingDetails(productIds[position])?.title?.run { substring(0, Math.abs(indexOf("(MaxLock")) - 1) }.orEmpty()
                    if (productName.isEmpty()) {
                        parent.visibility = View.INVISIBLE
                        donationStatusText.setText(R.string.donate_status_error)
                        return v
                    }
                    tv.text = productName
                    val drawable = ContextCompat.getDrawable(this@DonateActivity, productIcons[position])
                    if (position < 2) {
                        val array = obtainStyledAttributes(intArrayOf(android.R.attr.textColorPrimary))
                        drawable?.setColorFilter(array.getColor(0, Color.BLACK), PorterDuff.Mode.SRC_ATOP)
                        array.recycle()
                    }
                    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                    tv.compoundDrawablePadding = Util.dpToPx(this@DonateActivity, 12)
                    return v
                }
            }
            val productsList = findViewById<ListView>(R.id.donate_products_list)!!
            productsList.adapter = productsAdapter
            productsList.setOnItemClickListener { _, _, position, _ ->
                if (!bp.isPurchased(productIds[position])) {
                    bp.purchase(this@DonateActivity, productIds[position])
                } else {
                    val b = AlertDialog.Builder(this@DonateActivity)
                    b.setMessage(R.string.dialog_message_already_bought)
                    b.setPositiveButton(android.R.string.yes) { _, _ ->
                        Log.i(LOG_TAG_IAB, bp.listOwnedProducts().toString())
                        if (bp.consumePurchase(productIds[position])) {
                            Log.i(LOG_TAG_IAB, bp.listOwnedProducts().toString())
                            bp.purchase(this@DonateActivity, productIds[position])
                        }
                    }
                    b.setNegativeButton(android.R.string.cancel, null)
                    b.show()
                }
            }
            productsList.setOnItemLongClickListener { _, _, _, _ ->
                if (Util.isDevMode()) {
                    setDonationStatus(true)
                    true
                } else false
            }
        }
    }

    /**
     * Called when requested PRODUCT ID was successfully purchased
     */
    override fun onProductPurchased(productId: String, details: TransactionDetails) {
        Handler().postDelayed({
            Log.i(LOG_TAG_IAB, "Loading…")
            if (bp.loadOwnedPurchasesFromGoogle()) {
                setDonationStatus()
            }
        }, 200)
    }

    /**
     * Called when some error occurred. See Constants class for more details
     */
    override fun onBillingError(errorCode: Int, error: Throwable?) {
        Log.e("ML-iab", "Error!")
    }

    /**
     * Called when purchase history was restored and the list of all owned PRODUCT ID's
     * was loaded from Google Play
     */
    override fun onPurchaseHistoryRestored() {}

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        unsafeBillingProcessor?.release()
        super.onDestroy()
    }

    private class BillingProcessorLoader internal constructor(context: Context, internal var billingHandler: BillingProcessor.IBillingHandler) : AsyncTaskLoader<BillingProcessor>(context) {

        override fun onStartLoading() {
            forceLoad()
        }

        override fun loadInBackground(): BillingProcessor? {
            val licenseKey = context.getString(R.string.license_key)
            return if (BillingProcessor.isIabServiceAvailable(context) && licenseKey != "DUMMY" && licenseKey.startsWith("M")) {
                BillingProcessor(context, licenseKey, billingHandler)
            } else null
        }
    }

    companion object {
        private val productIds = arrayOf("donate_coke", "donate_beer", "donate_5", "donate_10")
        private val productIcons = intArrayOf(R.drawable.ic_coffee_36dp, R.drawable.ic_beer_36dp, R.drawable.ic_favorite_small_48dp, R.drawable.ic_favorite_48dp)
    }
}