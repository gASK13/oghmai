package net.gask13.oghmai

import DescribeWordScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.gask13.oghmai.network.RetrofitInstance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.app.AlertDialog
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import net.gask13.oghmai.ui.WordListingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            OghmAINavHost(navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OghmAI") },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(onClick = { onNavigate("describeWord") }) {
                Text("Describe Word")
            }

            DisabledButton("Add Word")
            Button(onClick = { onNavigate("listWords") }) {
                Text("List Words")
            }
            DisabledButton("Test")
        }
    }
}

@Composable
fun DisabledButton(text: String) {
    Button(onClick = {}, enabled = false) {
        Text("$text (Not Implemented)")
    }
}

@Composable
fun SettingsScreen(
    version: String = "v0.1-dev",
    user: String = "test",
    language: String = "Italian",
    onDeleteAll: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Version: $version")
        Text("Username: $user")
        Text("Language: $language")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onDeleteAll) {
            Text("Delete All Words")
        }
    }
}

@Composable
fun OghmAINavHost(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(navController, startDestination = "main") {
        composable("main") { MainMenuScreen { navController.navigate(it) } }
        composable("describeWord") { DescribeWordScreen(navController) }
        composable("listWords") { WordListingScreen() }
        composable("settings") {
            SettingsScreen(onDeleteAll = {
            // Calling the purge-words endpoint
//            RetrofitInstance.apiService.purgeWords()
//                .enqueue(object : Callback<Void> {
//                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
//                        if (response.isSuccessful) {
//                            // Handle successful purging of words
//                            showDeleteResult(context, "All words deleted")
//                        } else {
//                            // Handle failure
//                            showDeleteResult( context,"Failed to delete words")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<Void>, t: Throwable) {
//                        showDeleteResult(context, "Error: ${t.message}")
//                    }
//                })
        }) }
    }
}

private fun showDeleteResult(context: Context, result: String) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("DELETE")
        .setMessage(result)
        .setCancelable(true) // Disables touch outside dialog to dismiss it
        // "No" button, dismiss dialog
        .setPositiveButton("Ok") { dialog, id ->
            dialog.dismiss()
        }

    // Create and show the dialog
    val dialog = builder.create()
    dialog.show()
}

