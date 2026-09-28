package com.rncoding.testvineshield.core.platform

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

class AndroidActivityProvider {

    private var activityReference:
            WeakReference<FragmentActivity>? = null

    fun setActivity(
        activity: FragmentActivity
    ) {
        activityReference =
            WeakReference(activity)
    }

    fun clearActivity(
        activity: FragmentActivity
    ) {
        if (
            activityReference
                ?.get() === activity
        ) {
            activityReference = null
        }
    }

    fun getActivity():
            FragmentActivity? =
        activityReference?.get()
}