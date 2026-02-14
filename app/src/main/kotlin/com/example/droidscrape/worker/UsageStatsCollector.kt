package com.example.droidscrape.worker

import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.droidscrape.network.Record

class UsageStatsCollector(private val context: Context) {

    fun getUsageStats(startTime: Long, endTime: Long): List<Record> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        return usageStats.map { 
            val rawLabel = try {
                context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it.packageName, 0)).toString()
            } catch (e: Exception) {
                it.packageName
            }

            Record(
                packageName = it.packageName,
                appLabel = formatAppLabel(rawLabel),
                usageMillisIncrement = it.totalTimeInForeground
            )
        }
    }

    private fun formatAppLabel(label: String): String {
        // If the label looks like a package name (contains periods), extract the last part
        if (label.contains(".")) {
            val segments = label.split(".").filter { it.isNotEmpty() }
            val lastSegment = segments.lastOrNull() ?: return label
            
            // Capitalize the first letter for better readability
            return lastSegment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        return label
    }
}
