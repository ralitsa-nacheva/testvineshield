package com.rncoding.testvineshield.core.data.security

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/** Credential-only OS activity for API 24–29. Recreated attempts fail closed. */
class DeviceCredentialFragment : Fragment() {
    internal var credentialIntent: Intent? = null
    internal var onComplete: ((Boolean) -> Unit)? = null
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        finish(it.resultCode == Activity.RESULT_OK)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The continuation belongs to the original activity. Never trust a restored result.
        if (savedInstanceState != null || credentialIntent == null || onComplete == null) {
            finish(false)
            return
        }
        try { launcher.launch(requireNotNull(credentialIntent)) }
        catch (_: Exception) { finish(false) }
    }
    private fun finish(success: Boolean) {
        val callback = onComplete
        onComplete = null
        callback?.invoke(success)
        cancel()
    }
    internal fun cancel() {
        onComplete = null
        credentialIntent = null
        if (isAdded) parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }
    override fun onDestroy() {
        val callback = onComplete
        onComplete = null
        callback?.invoke(false)
        super.onDestroy()
    }
}
