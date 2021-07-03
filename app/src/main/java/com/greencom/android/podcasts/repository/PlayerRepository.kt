package com.greencom.android.podcasts.repository

import com.greencom.android.podcasts.data.domain.Episode

/** Interface that defines player repository that contains player-related use cases. */
interface PlayerRepository {

    /** Get the episode by ID. */
    suspend fun getEpisode(episodeId: String): Episode?

    /** Get the episode's last position by ID. */
    suspend fun getEpisodePosition(episodeId: String): Long?

    /** Update episode state depending on the last position. */
    suspend fun updateEpisodeState(episodeId: String, position: Long, duration: Long)
}