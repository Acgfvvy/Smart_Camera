package com.abhyanshchannelac.smartcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.abhyanshchannelac.smartcamera.ui.MainScreen
import com.abhyanshchannelac.smartcamera.ui.theme.SmartCameraTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase services
        firebaseAnalytics = Firebase.analytics
        crashlytics = Firebase.crashlytics
        
        // Enable Crashlytics data collection
        crashlytics.setCrashlyticsCollectionEnabled(true)
        
        // Set user identifier for better crash reports
        crashlytics.setUserId(System.currentTimeMillis().toString())
        
        // Log app_open event
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
        
        enableEdgeToEdge()
        setContent {
            SmartCameraTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCameraTheme {
        Greeting("Android")
    }
}