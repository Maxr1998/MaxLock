package de.Maxr1998.xposed.maxlock.ui.settings_new.implementation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.View
import de.Maxr1998.modernpreferences.preferences.DialogPreference
import de.Maxr1998.xposed.maxlock.R

class ImplementationDialogPreference : DialogPreference("implementation") {

    init {
        titleRes = R.string.ml_implementation
    }

    @SuppressLint("InflateParams")
    override fun createDialog(context: Context): Dialog {
        return AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(View.inflate(context, R.layout.dialog_implementation, null))
                .setNegativeButton(android.R.string.ok, null)
                .setOnDismissListener { requestRebind() }
                .create()
    }
}