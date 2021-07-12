package com.greencom.android.podcasts.data.datastore

import androidx.datastore.preferences.core.stringPreferencesKey

/** DataStore preferences keys. */
object PreferenceKeys {

    /** String preferences key for the ID of the last episode. */
    val LAST_EPISODE_ID = stringPreferencesKey("LAST_EPISODE_ID")
}