package com.example.droidscrape.worker

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.droidscrape.network.Record

class UsageStatsCollector(private val context: Context) {

    fun getUsageStats(startTime: Long, endTime: Long): List<Record> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        return usageStats.map { 
            Record(
                packageName = it.packageName,
                appLabel = try {
                    context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it.packageName, 0)).toString()
                } catch (e: Exception) {
                    it.packageName
                },
                usageMillisIncrement = it.totalTimeInForeground
            )
        }
    }
}
