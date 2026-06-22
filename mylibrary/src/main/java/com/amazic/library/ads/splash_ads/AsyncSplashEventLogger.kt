package com.amazic.library.ads.splash_ads

import android.os.Bundle
import com.amazic.library.Utils.EventTrackingHelper
import java.util.Locale

// ---------------------------------------------------------------------------
// Event-logging helpers — extracted from AsyncSplash
// ---------------------------------------------------------------------------

private const val MAX_EVENT_NAME_LENGTH = 40
private const val DEFAULT_EVENT_NAME = "event_unknown"

/**
 * Sanitises a raw string into a valid Firebase event name:
 *  - lowercase, alphanumeric + underscore only
 *  - must start with a letter
 *  - max 40 characters
 */
fun AsyncSplash.normalizeFirebaseEventName(input: String): String {
    if (input.isBlank()) return DEFAULT_EVENT_NAME

    var name = input.trim().lowercase()
    name = name.replace(Regex("[^a-z0-9_]"), "_")

    if (name.firstOrNull()?.isLetter() != true) {
        name = "e_$name"
    }

    if (name.length > MAX_EVENT_NAME_LENGTH) {
        name = name.take(MAX_EVENT_NAME_LENGTH)
    }

    return name.ifBlank { DEFAULT_EVENT_NAME }
}

/** Logs a step event with time_between_step and time_to_step params. */
internal fun AsyncSplash.logEventStep(step: String) {
    val bundle = Bundle()
    bundle.putString("time_between_step", "${System.currentTimeMillis() - timeLastStep}")
    bundle.putString("time_to_step", "${System.currentTimeMillis() - timeStep1}")
    EventTrackingHelper.logEventWithMultipleParams(
        activity,
        normalizeFirebaseEventName("AsyncSplash_$step"),
        bundle
    )
    timeLastStep = System.currentTimeMillis()
}

/** Logs the combined init-timing event after all async tasks complete. */
internal fun AsyncSplash.logEventDoneInit() {
    val bundle = Bundle()
    val time = String.format(
        Locale.US, "%.1f_%.2f_%.3f_%.4f",
        timeInitAdmobApi / 1000f,
        timeInitRemoteConfig / 1000f,
        timeInitAdsConsentManager / 1000f,
        timeInitTechManager / 1000f
    )
    bundle.putString("time_between_step", time)
    EventTrackingHelper.logEventWithMultipleParams(
        activity,
        normalizeFirebaseEventName("AsyncSplash_doneInit"),
        bundle
    )
    timeLastStep = System.currentTimeMillis()
}
