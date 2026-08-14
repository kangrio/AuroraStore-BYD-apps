package com.kangrio.extension.shared.version

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.kangrio.extension.shared.Utils
import com.kangrio.extension.shared.network.HttpRequest
import org.json.JSONObject

object VersionChecker {
    const val UPDATER_URL =
        "https://api.github.com/repos/kangrio/AuroraStore-BYD-apps/releases/latest"

    val prefs by lazy {
        Utils.getCurrentActivity()?.getSharedPreferences("version_prefs", Context.MODE_PRIVATE)
    }
    const val IGNORE_UPDATE_TS_PREFS = "ignore_update_ts_prefs_key"

    val callbacks = object : Application.ActivityLifecycleCallbacks {
        private var checked = false
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.d("VersionChecker", "onActivityCreated: ${activity.javaClass.name}")
            val app = Utils.getApplication() ?: return
            val handler = Handler(Looper.getMainLooper())
            val launcherNameList = Utils.getLauncherActivityNames(app)

            if (checked || activity.javaClass.name !in launcherNameList) return
            checked = true

            handler.postDelayed({
                checkUpdate()
                app.unregisterActivityLifecycleCallbacks(this)
            }, 2000)
        }

        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    }

    private var isInitialized = false
    @JvmStatic
    fun init() = runCatching {
        if (isInitialized) return@runCatching
        isInitialized = true
        val app = Utils.getApplication() ?: return
        app.registerActivityLifecycleCallbacks(callbacks)
    }.getOrElse {
        it.printStackTrace()
    }

    private fun checkUpdate() {
        runCatching {
            val prefs = prefs ?: return
            val ignoreUpdateTs = prefs.getLong(IGNORE_UPDATE_TS_PREFS, 0)

            if (System.currentTimeMillis() - ignoreUpdateTs <
                1000 * 60 * 60 * 24 * 7 // 7 days
            ) {
                return
            }
            val currentVersion = getCurrentVersion()
            val latestVersion = getLatestVersion()

            if (currentVersion < latestVersion) {
                showUpdateDialog(currentVersion, latestVersion)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun getLatestVersion(): Int {
        val body = HttpRequest.get(UPDATER_URL).takeIf { it.isNotEmpty() } ?: return -1
        val json = JSONObject(body)
        return json.optString("tag_name").substring(1).replace(".", "").toInt()
    }

    fun getCurrentVersion(): Int {
        val activity = Utils.getCurrentActivity() ?: return Int.MAX_VALUE
        val packageInfo =
            activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_META_DATA
            ) ?: return Int.MAX_VALUE
        val metadata = packageInfo.applicationInfo?.metaData ?: return Int.MAX_VALUE
        return metadata.getInt("morphe_version", Int.MAX_VALUE)
    }

    fun showUpdateDialog(currentVersion: Int, latestVersion: Int) {
        val activity = Utils.getCurrentActivity() ?: return
        activity.runOnUiThread {
            val dialog = UpdateDialog(activity, currentVersion, latestVersion)
            dialog.show()
        }
    }
}