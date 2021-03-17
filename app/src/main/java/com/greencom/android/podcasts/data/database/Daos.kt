package com.greencom.android.podcasts.data.database

import androidx.room.*

/** Interface to interact with `podcasts` table. */
@Dao
interface PodcastDao {

    /**
     * Insert the given [PodcastEntity] list into the `podcasts` table.
     * [OnConflictStrategy.IGNORE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(podcasts: List<PodcastEntity>)

    /**
     * Used to update `inSubscription` property of the single podcast entry in the
     * `podcasts` table with the given [PodcastEntityUpdateSubscription] object
     * without editing other properties.
     */
    @Update(entity = PodcastEntity::class)
    suspend fun update(podcast: PodcastEntityUpdateSubscription)

    /**
     * Used to update the podcast entry in the `podcasts` table without editing
     * `inSubscription` property with the given [PodcastEntityUpdate]
     * object.
     */
    @Update(entity = PodcastEntity::class)
    suspend fun update(podcast: PodcastEntityUpdate)

    /**
     * Used to update the podcasts entries in the `podcasts` table with the given list of
     * [PodcastEntityUpdate] objects without editing
     * `inSubscription` property.
     */
    @Update(entity = PodcastEntity::class)
    suspend fun update(podcasts: List<PodcastEntityUpdate>)

    /** Return a [PodcastEntity] from the `podcasts` table for a given ID. */
    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcast(id: String): PodcastEntity?

    /**
     * Get a list of best podcasts for the provided genre ID from the
     * `podcasts` table.
     */
    @Query("SELECT * FROM podcasts WHERE in_best_for_genre = :genreId")
    suspend fun getBestPodcasts(genreId: Int): List<PodcastEntity>
}

/** Interface to interact with `episodes` table. */
@Dao
interface EpisodeDao {

}

/** Interface to interact with `genres` table. */
@Dao
interface GenreDao {

    /** Get the number of entries in the `genres` table. */
    @Query("SELECT count(*) FROM genres")
    suspend fun getSize(): Int

    /**
     * Insert the given [GenreEntity] list into the `genres` table.
     * [OnConflictStrategy.REPLACE] on conflict.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(genres: List<GenreEntity>)
}