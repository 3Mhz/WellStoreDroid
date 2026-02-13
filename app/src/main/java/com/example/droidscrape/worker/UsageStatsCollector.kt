package com.example.droidscrape.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.droidscrape.data.remote.dto.RecordDto

class UsageStatsCollector(private val context: Context) {

    private data class EventHolder(val packageName: String?, val timeStamp: Long)

    fun getUsageStats(startTime: Long, endTime: Long): List<RecordDto> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getUsageStatsFromEvents(usageStatsManager, startTime, endTime)
        } else {
            getUsageStatsFromQuery(usageStatsManager, startTime, endTime)
        }
    }

    private fun getUsageStatsFromEvents(usageStatsManager: UsageStatsManager, startTime: Long, endTime: Long): List<RecordDto> {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val usageMap = mutableMapOf<String, Long>()
        var lastEvent: EventHolder? = null

        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)

            if (lastEvent != null && event.packageName != null) {
                if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                    if (lastEvent.packageName == event.packageName) {
                        val usageTime = event.timeStamp - lastEvent.timeStamp
                        usageMap[event.packageName] = (usageMap[event.packageName] ?: 0) + usageTime
                    }
                }
            }
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastEvent = EventHolder(
                    packageName = event.packageName,
                    timeStamp = event.timeStamp
                )
            }
        }

        return usageMap.map {
            RecordDto(
                packageName = it.key,
                usageMillisIncrement = it.value,
                appLabel = getAppLabel(it.key)
            )
        }
    }

    private fun getUsageStatsFromQuery(usageStatsManager: UsageStatsManager, startTime: Long, endTime: Long): List<RecordDto> {
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
        return usageStats.map {
            RecordDto(
                packageName = it.packageName,
                usageMillisIncrement = it.totalTimeInForeground,
                appLabel = getAppLabel(it.packageName)
            )
        }
    }

    private fun getAppLabel(packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}
