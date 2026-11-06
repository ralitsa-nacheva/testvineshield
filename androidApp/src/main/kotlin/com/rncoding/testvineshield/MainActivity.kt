package com.rncoding.testvineshield

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val activityProvider: AndroidActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("VineShieldStartup", "ACTIVITY onCreate entered")
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        Log.i("VineShieldStartup", "ACTIVITY host registered")
        setContent {
            Log.i("VineShieldStartup", "COMPOSE content entered")
            App()
        }
        Log.i("VineShieldStartup", "ACTIVITY setContent returned")
    }

    override fun onResume() {
        super.onResume()
        Log.i("VineShieldStartup", "ACTIVITY onResume")
    }

    override fun onDestroy() {
        // The provider clears only this instance, never a newer replacement.
        activityProvider.clearActivity(this)
        super.onDestroy()
    }
}
