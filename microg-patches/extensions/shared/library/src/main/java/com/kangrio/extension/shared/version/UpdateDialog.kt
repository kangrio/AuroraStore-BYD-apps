package com.kangrio.extension.shared.version

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class UpdateDialog(
    context: Context,
    private val currentVersion: Int,
    private val latestVersion: Int
) : Dialog(context) {
    private val UPDATER_URL =
        "https://github.com/kangrio/AuroraStore-BYD-apps/releases/latest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val padding = (context.resources.displayMetrics.density * 24).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(context).apply {
            text = "Update Available"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
        }

        val message = TextView(context).apply {
            text = buildString {
                append("New version available\n\n")
                append("Current version: $currentVersion\n")
                append("Latest version: $latestVersion")
            }
            textSize = 16f
            setPadding(0, padding / 2, 0, padding / 2)
        }

        val buttonLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val cancel = Button(context).apply {
            text = "Cancel"
            setOnClickListener {
                val prefs = VersionChecker.prefs ?: return@setOnClickListener dismiss()
                prefs.edit()
                    .putLong(VersionChecker.IGNORE_UPDATE_TS_PREFS, System.currentTimeMillis())
                    .apply()
                dismiss()
            }
        }

        val update = Button(context).apply {
            text = "Update"
            setOnClickListener {
                checkUpdate()
                dismiss()
            }
        }

        buttonLayout.addView(cancel)
        buttonLayout.addView(update)

        root.addView(title)
        root.addView(message)
        root.addView(buttonLayout)

        setContentView(root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun checkUpdate() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPDATER_URL)))
    }
}