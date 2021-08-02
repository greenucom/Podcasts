package com.greencom.android.podcasts.ui.activity

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.ui.activity.history.ActivityHistoryFragment
import com.greencom.android.podcasts.ui.activity.playlist.ActivityPlaylistFragment

/**
 * Adapter used for the ViewPager2 implementation inside [ActivityFragment]. Creates
 * [ActivityPlaylistFragment] and [ActivityHistoryFragment].
 */
class ActivityViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val itemCount = fragment.resources.getStringArray(R.array.activity_tabs).size

    override fun getItemCount(): Int = itemCount

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ActivityPlaylistFragment()
            1 -> ActivityHistoryFragment()
            else -> throw IllegalStateException("Fragment has not been added to ActivityViewPagerAdapter.createFragment()")
        }
    }
}