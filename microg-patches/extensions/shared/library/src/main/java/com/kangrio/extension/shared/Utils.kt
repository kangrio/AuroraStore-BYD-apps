package com.kangrio.extension.shared

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
object Utils {
    @Suppress("PrivateApi", "DiscouragedPrivateApi")
    fun getCurrentActivity(): Activity? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod =
                activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentActivityThreadMethod.isAccessible = true
            val activityThread = currentActivityThreadMethod.invoke(null)

            val activitiesField = activityThreadClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val activities = activitiesField.get(activityThread) as Map<*, *>

            for (activityRecord in activities.values) {
                val recordClass = activityRecord!!::class.java

                val pausedField = recordClass.getDeclaredField("paused")
                pausedField.isAccessible = true
                val paused = pausedField.getBoolean(activityRecord)

                if (!paused) {
                    val activityField = recordClass.getDeclaredField("activity")
                    activityField.isAccessible = true
                    return activityField.get(activityRecord) as Activity
                }
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getLauncherActivityNames(context: Context): List<String> {
        val result = ArrayList<String>()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setPackage(context.packageName)

        val activities = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)

        for (resolveInfo in activities) {
            val info = resolveInfo.activityInfo

            val name = info.targetActivity ?: info.name

            if (!result.contains(name)) {
                result.add(name)
            }
        }

        return result
    }

    fun getApplication(): Application? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getDeclaredMethod("currentApplication")
            currentApplicationMethod.isAccessible = true
            currentApplicationMethod.invoke(null) as Application
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}