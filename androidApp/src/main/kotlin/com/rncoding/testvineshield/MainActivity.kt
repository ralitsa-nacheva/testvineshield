package com.rncoding.testvineshield

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val activityProvider: AndroidActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        setContent { App() }
    }

    override fun onDestroy() {
        // The provider clears only this instance, never a newer replacement.
        activityProvider.clearActivity(this)
        super.onDestroy()
    }
}
