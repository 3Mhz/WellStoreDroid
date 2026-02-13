package com.example.droidscrape.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.droidscrape.viewmodel.MainViewModel
import kotlin.system.exitProcess

@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val testResult by viewModel.testConnectionResult.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate("config") }) {
            Text("Configuration")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.sendTestRequest() }) {
            Text("Test Connection")
        }
        testResult?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { exitProcess(0) }) {
            Text("Exit")
        }
    }
}
