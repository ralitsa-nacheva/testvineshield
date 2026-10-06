package com.rncoding.testvineshield

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {

    private val activityProvider:
            AndroidActivityProvider by inject()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        activityProvider.setActivity(this)

        // Compose content...
    }

    override fun onDestroy() {
        activityProvider.clearActivity(this)
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}