package com.example.droidscrape.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.droidscrape.viewmodel.ConfigViewModel

@Composable
fun ConfigScreen(navController: NavController, viewModel: ConfigViewModel = viewModel()) {
    val endpointUrl by viewModel.endpointUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Endpoint URL")
        TextField(
            value = endpointUrl,
            onValueChange = { viewModel.saveSettings(it, apiKey) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("API Key")
        TextField(
            value = apiKey,
            onValueChange = { viewModel.saveSettings(endpointUrl, it) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedButton(onClick = { viewModel.resetToDefaults() }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset to Defaults")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Text("Done")
        }
    }
}
