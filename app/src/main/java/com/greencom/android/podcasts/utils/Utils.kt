package com.greencom.android.podcasts.utils

import android.content.Context

/**
 * Convert `dp` to `px`.
 * @param dp dp in `Int`.
 */
fun Context.convertDpToPx(dp: Int): Float {
    return dp * resources.displayMetrics.density
}

/**
 * Convert `dp` to `px`.
 * @param dp dp in `Float`.
 */
fun Context.convertDpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}