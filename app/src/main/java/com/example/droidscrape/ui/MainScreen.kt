package com.example.droidscrape.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.droidscrape.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val endpointUrl by viewModel.endpointUrl.collectAsState()
    val collectionEnabled by viewModel.collectionEnabled.collectAsState()
    val testConnectionStatus by viewModel.testConnectionStatus.collectAsState()
    val lastCollectionTime by viewModel.lastCollectionTime.collectAsState()
    val lastSuccessfulUploadTime by viewModel.lastSuccessfulUploadTime.collectAsState()
    val queueSize by viewModel.queueSize.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WellStore Droid") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = endpointUrl ?: "",
                onValueChange = { coroutineScope.launch { viewModel.setEndpointUrl(it) } },
                label = { Text("Endpoint URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable background collection")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = collectionEnabled,
                    onCheckedChange = { viewModel.toggleCollection(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { viewModel.testConnection() }) {
                Text("Test connection / Test upload")
            }

            testConnectionStatus?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it)
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusInfo(
                lastCollectionTime = lastCollectionTime,
                lastSuccessfulUploadTime = lastSuccessfulUploadTime,
                queueSize = queueSize,
                lastError = lastError
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasUsageStatsPermission(context)) {
                Button(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
                    Text("Grant Usage Access Permission")
                }
            }
        }
    }
}

@Composable
fun StatusInfo(
    lastCollectionTime: Long?,
    lastSuccessfulUploadTime: Long?,
    queueSize: Int,
    lastError: String?
) {
    Column {
        Text("Last collection: ${formatDate(lastCollectionTime)}")
        Text("Last successful upload: ${formatDate(lastSuccessfulUploadTime)}")
        Text("Queue size: $queueSize")
        lastError?.let {
            Text("Last error: $it")
        }
    }
}

private fun formatDate(timestamp: Long?): String {
    if (timestamp == null) return "Never"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}
