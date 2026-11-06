package com.rncoding.testvineshield.core.presentation.root

/** Temporary startup investigation. Call with fixed stage names only, never user data.
 * println appears under System.out on Android and the platform console on iOS.
 */
internal fun traceStartup(stage: String) {
    println("VineShieldStartup: $stage")
}
