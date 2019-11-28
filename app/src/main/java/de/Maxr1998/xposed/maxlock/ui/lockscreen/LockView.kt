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

package de.Maxr1998.xposed.maxlock.ui.lockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import de.Maxr1998.xposed.maxlock.Common.*
import de.Maxr1998.xposed.maxlock.R
import de.Maxr1998.xposed.maxlock.ui.actions.tasker.TaskerHelper
import de.Maxr1998.xposed.maxlock.util.*
import splitties.dimensions.dip

class LockView private constructor(ctx: Context, attrs: AttributeSet?, defStyleAttrs: Int, nullablePackageName: String?, private val activityName: String?) :
        RelativeLayout(getThemedContext(ctx), attrs, defStyleAttrs), View.OnClickListener, View.OnLongClickListener {
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : this(context, attributeSet, defStyleAttr, null, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context) : this(context, null)

    // Used for Lockscreen
    constructor(context: Context, packageName: String, activityName: String) : this(context, null, 0, packageName, activityName)

    val packageName: String = when (nullablePackageName) {
        MASTER_SWITCH_ON, null -> context.applicationContext.packageName
        else -> nullablePackageName
    }
    private val password: String?
    private val screenSize: Point by lazy {
        Point().also { activity.windowManager.defaultDisplay.getSize(it) }
    }
    private val authenticationSucceededListener: AuthenticationSucceededListener
    private val currentKey = StringBuilder(10)
    private var inputText: TextView
    private val infoMessage: TextView
    private var knockCodeHolder: KnockCodeHelper? = null
    private val fingerprintStub: FrameLayout by lazyView(R.id.fingerprint_stub)
    private var fingerprintView: FingerprintView? = null

    val prefs: SharedPreferences
        get() = context.prefs

    private val timeLeft: Long
        get() = 59000 + prefs.getLong(FAILED_ATTEMPTS_TIMER, 0) - System.currentTimeMillis()

    private val isTimeLeft: Boolean
        get() = timeLeft > 0

    // Helpers
    val activity: AppCompatActivity
        get() = (context as ContextThemeWrapper).baseContext as AppCompatActivity

    val isLandscape: Boolean
        get() = screenSize.x > screenSize.y

    init {
        try {
            authenticationSucceededListener = activity as AuthenticationSucceededListener
        } catch (e: ClassCastException) {
            throw RuntimeException(activity.javaClass.simpleName + " must implement AuthenticationSucceededListener to use this fragment", e)
        }

        val title = if (nullablePackageName == MASTER_SWITCH_ON)
            resources.getString(R.string.unlock_master_switch)
        else Util.getApplicationNameFromPackage(packageName, context)

        val lockingType = context.prefsKeysPerApp.getString(packageName, prefs.getString(LOCKING_TYPE, ""))
        password = if (context.prefsKeysPerApp.contains(packageName)) {
            context.prefsKeysPerApp.getString(packageName + APP_KEY_PREFERENCE, null)
        } else context.prefsKey.getString(KEY_PREFERENCE, "")


        inflate(R.layout.lock_view, true)

        val titleTextView = findViewById<TextView>(R.id.title_view)
        val inputBar = findViewById<ViewGroup>(R.id.input_bar)
        inputText = findViewById(R.id.input_view)
        val deleteButton = findViewById<ImageButton>(R.id.delete_input)
        infoMessage = findViewById(R.id.message_area)
        val container = findViewById<FrameLayout>(R.id.container)

        // Locking type view setup
        when (lockingType) {
            PREF_VALUE_PASSWORD, PREF_VALUE_PASS_PIN -> {
                inputBar.removeAllViews()
                inputText = AppCompatEditText(context).apply {
                    id = R.id.input_view
                    val mInputTextParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
                    mInputTextParams.weight = 1f
                    layoutParams = mInputTextParams
                    setSingleLine()
                    inputType = (if (lockingType == PREF_VALUE_PASS_PIN) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT) or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setOnEditorActionListener { v, actionId, _ ->
                        return@setOnEditorActionListener if (actionId == EditorInfo.IME_ACTION_DONE) {
                            if (checkInput()) {
                                activity.hideIme()
                            } else {
                                setKey(null, false)
                                v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.shake))
                                handleFailedAttempt()
                            }
                            true
                        } else false
                    }
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
                        override fun onTextChanged(charSequence: CharSequence, i: Int, i2: Int, i3: Int) {}
                        override fun afterTextChanged(editable: Editable) {
                            setKey(editable.toString(), false)
                            if (prefs.getBoolean(ENABLE_QUICK_UNLOCK, false)) {
                                if (checkInput()) activity.hideIme()
                            }
                        }
                    })
                }
                inputBar.addView(inputText)
                inputBar.updateLayoutParams<MarginLayoutParams> {
                    setMargins(dip(16), 0, dip(16), 0)
                }
                removeView(fingerprintStub)
                inputBar.addView(fingerprintStub)
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            }
            PREF_VALUE_PIN -> {
                val pinParams = FrameLayout.LayoutParams(getDimension(R.dimen.container_size), getDimension(R.dimen.container_size))
                pinParams.gravity = if (isLandscape) Gravity.CENTER else Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                pinParams.bottomMargin = if (isLandscape) 0 else getDimension(R.dimen.fingerprint_margin)
                container.addView(PinView(context, this), pinParams)
            }
            PREF_VALUE_KNOCK_CODE -> {
                knockCodeHolder = KnockCodeHelper(this, container)
                container.setOnLongClickListener(this)
                container.contentDescription = resources.getString(R.string.content_description_lockscreen_container)
            }
            PREF_VALUE_PATTERN -> {
                inputBar.isVisible = false
                val patternParams = FrameLayout.LayoutParams(getDimension(R.dimen.container_size), getDimension(R.dimen.container_size))
                patternParams.gravity = Gravity.CENTER
                container.addView(PatternView(context, this), patternParams)
            }
            else -> throw UnsupportedOperationException("LockView cannot be created without a locking type set")
        }

        // Title
        if (prefs.getBoolean(HIDE_TITLE_BAR, false)) {
            titleTextView.isVisible = false
        } else {
            titleTextView.apply {
                text = title
                val icon = context.getApplicationIcon(packageName).apply {
                    setBounds(0, 0, getDimension(R.dimen.title_icon_size), getDimension(R.dimen.title_icon_size))
                }
                titleTextView.setCompoundDrawables(icon, null, null, null)
                setOnLongClickListener(this@LockView)
            }
        }

        // Input
        if (lockingType != PREF_VALUE_PASSWORD && lockingType != PREF_VALUE_PASS_PIN) {
            if (prefs.getBoolean(HIDE_INPUT_BAR, false)) {
                inputBar.isVisible = false
            } else {
                inputText.clearText()
                deleteButton.setOnClickListener(this)
                deleteButton.setOnLongClickListener(this)
            }
        }

        fingerprintStub.addView(FingerprintView(context, this).also { fingerprintView = it })

        // Handle timer for previous wrong attempts
        if (isTimeLeft) startTimer()
    }

    fun forceFocus() {
        inputText.requestFocus()
    }

    fun appendToInput(value: String) {
        if (isTimeLeft) return
        inputText.append(value)
    }

    fun setKey(value: String?, append: Boolean) {
        if (isTimeLeft) {
            return
        }
        infoMessage.clearText()
        if (value == null) {
            currentKey.setLength(0)
            if (knockCodeHolder != null) {
                knockCodeHolder!!.clear(true)
            }
            inputText.clearText()
            return
        }
        if (!append) {
            currentKey.setLength(0)
        }
        currentKey.append(value)

    }

    fun setPattern(pattern: List<*>, patternView: PatternView) {
        setKey(pattern.toString(), false)
        if (!checkInput()) {
            patternView.setWrong()
            handleFailedAttempt()
        }
    }

    fun checkInput() = if (!isTimeLeft && currentKey.toString().toSha256() == password || password!!.isEmpty()) {
        handleAuthenticationSuccess()
        true
    } else false

    fun handleAuthenticationSuccess() {
        prefs.edit().putInt(FAILED_ATTEMPTS_COUNTER, 0).apply()
        activity.hideIme()
        authenticationSucceededListener.onAuthenticationSucceeded()
        if (fingerprintView != null)
            fingerprintView!!.cancelAuthentication()
        TaskerHelper.sendQueryRequest(activity, true, packageName)
        // Reset inputs
        post { setKey(null, false) }
    }

    fun handleFailedAttempt() {
        if (isTimeLeft) {
            return
        }
        infoMessage.setText(R.string.message_wrong_password)
        var old = prefs.getInt(FAILED_ATTEMPTS_COUNTER, 0)
        prefs.edit().putInt(FAILED_ATTEMPTS_COUNTER, ++old).apply()
        if (old % MAX_ATTEMPTS == 0) {
            setKey(null, false)
            if (fingerprintView != null)
                fingerprintView!!.cancelAuthentication()
            prefs.edit().putLong(FAILED_ATTEMPTS_TIMER, System.currentTimeMillis()).apply()
            startTimer()
        }
        TaskerHelper.sendQueryRequest(activity, false, packageName)
    }

    private fun startTimer() {
        object : CountDownTimer(timeLeft, 200) {
            override fun onTick(millisUntilFinished: Long) {
                infoMessage.text = resources.getString(R.string.message_try_again_in_seconds, millisUntilFinished / 1000 + 1)
            }

            override fun onFinish() {
                infoMessage.clearText()
            }
        }.start()
    }

    fun allowFingerprint(): Boolean {
        return !isTimeLeft && prefs.getInt(FAILED_ATTEMPTS_COUNTER, 0) < MAX_ATTEMPTS
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.delete_input -> if (currentKey.isNotEmpty()) {
                currentKey.deleteCharAt(currentKey.length - 1)
                if (inputText.text.isNotEmpty()) {
                    inputText.text = inputText.text.subSequence(0, inputText.text.length - 1)
                }
                if (knockCodeHolder != null) {
                    knockCodeHolder!!.clear(false)
                }
            }
        }
    }

    override fun onLongClick(view: View) = when (view.id) {
        R.id.title_view -> {
            Toast.makeText(context, activityName, Toast.LENGTH_SHORT).show()
            true
        }
        else -> {
            setKey(null, false)
            true
        }
    }

    private fun getDimension(@DimenRes id: Int): Int {
        return resources.getDimensionPixelSize(id)
    }

    companion object {
        private const val MAX_ATTEMPTS = 5

        /**
         * Must be used as ContextThemeWrapper context for this LockView
         */
        @SuppressLint("RestrictedApi")
        fun getThemedContext(baseContext: Context): ContextThemeWrapper {
            return ContextThemeWrapper(baseContext, if (MLPreferences.getPreferences(baseContext).getBoolean(INVERT_COLOR, false)) R.style.AppTheme else R.style.AppTheme_Dark)
        }
    }
}