/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2018  Max Rumpf alias Maxr1998
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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import de.Maxr1998.xposed.maxlock.Common
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.util.*
import kotlinx.android.synthetic.main.fragment_lock_setup.view.*
import kotlinx.android.synthetic.main.split_button.view.*
import java.util.*


class LockSetupFragment : Fragment(), View.OnClickListener {

    private val lockType by lazy { arguments?.getString(Common.LOCKING_TYPE, null) }
    private val customApp: String  by lazy { arguments?.getString(Common.INTENT_EXTRAS_CUSTOM_APP, "") ?: "" }
    private var mUiStage = "first"
    private var currentValue
        get() = if (isPin()) pinInputView.text.toString() else key.toString()
        set(value) = if (isPin()) pinInputView.setText(value) else key.run { setLength(0); append(value); Unit }
    private val key: StringBuilder by lazy { StringBuilder("") }
    private lateinit var keyFromFirst: String
    private lateinit var descriptionText: TextView
    private lateinit var pinInputView: EditText
    private lateinit var knockCodeText: TextView
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        assert(arguments != null)
        assert(lockType != null)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_lock_setup, container, false) as ViewGroup
        descriptionText = rootView.description

        if (isPin()) setupPin(rootView) else setupKnockCode(rootView)

        rootView.button_cancel.setOnClickListener(this)
        nextButton = rootView.button_positive
        nextButton.setOnClickListener(this)

        updateUi()
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.title = if (lockType == Common.LOCKING_TYPE_PIN) getString(R.string.pref_locking_type_pin) else getString(R.string.pref_locking_type_knockcode)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupPin(rootView: ViewGroup) {
        pinInputView = rootView.findViewById(R.id.pin_input_view)
        pinInputView.visibility = VISIBLE
        pinInputView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {
                updateUi()
            }

            override fun afterTextChanged(editable: Editable) {
            }
        })
        pinInputView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                if (pinInputView.text.length > 3) {
                    handleStage()
                }
                true
            } else false
        }
    }

    private fun setupKnockCode(rootView: ViewGroup) {
        val knockCodeGroup = rootView.findViewById<View>(R.id.knock_code_group)
        knockCodeGroup.visibility = VISIBLE
        knockCodeText = rootView.findViewById(R.id.knock_code_text)
        knockCodeText.setOnClickListener(this)
        val knockButtons = arrayOf(R.id.knock_button_1, R.id.knock_button_2, R.id.knock_button_3, R.id.knock_button_4).map { rootView.findViewById<Button>(it) }
        for (i in knockButtons.indices) {
            knockButtons[i].apply {
                setOnClickListener(this@LockSetupFragment)
                setOnLongClickListener {
                    currentValue = ""
                    updateUi()
                    true
                }
            }
        }
    }

    private fun isPin(): Boolean = lockType == Common.LOCKING_TYPE_PIN

    private fun <T> selectByType(pin: T, knockCode: T) = if (isPin()) pin else knockCode

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_positive -> handleStage()
            R.id.button_cancel -> {
                activity?.run {
                    hideIme()
                    onBackPressed()
                }
            }
            else -> {
                if (currentValue.length < 20 || view.id == R.id.knock_code_text)
                    when (view.id) {
                        R.id.knock_code_text -> currentValue = ""
                        R.id.knock_button_1 -> key.append(1)
                        R.id.knock_button_2 -> key.append(2)
                        R.id.knock_button_3 -> key.append(3)
                        R.id.knock_button_4 -> key.append(4)
                        else -> return
                    }
                updateUi()
            }
        }
    }

    private fun updateUi() {
        if (!isPin()) knockCodeText.text = genPass(key)
        when (mUiStage) {
            "first" ->
                when {
                    currentValue.length > 3 -> {
                        descriptionText.setText(R.string.continue_done)
                        nextButton.isEnabled = true
                    }
                    currentValue.isNotEmpty() -> {
                        descriptionText.setText(selectByType(R.string.pin_too_short, R.string.knock_code_too_short))
                        nextButton.isEnabled = false
                    }
                    else -> {
                        descriptionText.setText(selectByType(R.string.choose_pin, R.string.choose_knock_code))
                        nextButton.isEnabled = false
                    }
                }
            "second" -> {
                descriptionText.setText(selectByType(R.string.confirm_pin, R.string.confirm_knock_code))
                nextButton.setText(android.R.string.ok)
                if (currentValue.isNotEmpty()) nextButton.isEnabled = true
            }
        }
    }

    private fun handleStage() {
        if (mUiStage == "first") {
            keyFromFirst = currentValue
            currentValue = ""
            mUiStage = "second"
            updateUi()
        } else if (mUiStage == "second") {
            if (currentValue == keyFromFirst) {
                if (customApp.isEmpty()) {
                    activity!!.prefs.edit().putString(Common.LOCKING_TYPE, selectByType(Common.PREF_VALUE_PIN, Common.PREF_VALUE_KNOCK_CODE)).apply()
                    activity!!.prefsKey.edit().putString(Common.KEY_PREFERENCE, Util.shaHash(currentValue)).apply()
                } else {
                    activity!!.prefsKeysPerApp.edit().putString(customApp, selectByType(Common.PREF_VALUE_PIN, Common.PREF_VALUE_KNOCK_CODE))
                            .putString(customApp + Common.APP_KEY_PREFERENCE, Util.shaHash(currentValue)).apply()
                }
            } else {
                Toast.makeText(activity, activity!!.resources.getString(R.string.toast_password_inconsistent), Toast.LENGTH_SHORT).show()
            }
            activity?.run {
                hideIme()
                onBackPressed()
            }
        }
    }

    private fun genPass(str: StringBuilder): String = Collections.nCopies(str.length, "\u2022").joinToString("")
}