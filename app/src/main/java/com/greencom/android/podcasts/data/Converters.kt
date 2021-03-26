package com.greencom.android.podcasts.data

import com.greencom.android.podcasts.data.database.GenreEntity
import com.greencom.android.podcasts.data.database.PodcastEntity
import com.greencom.android.podcasts.data.database.PodcastLocalAttrs
import com.greencom.android.podcasts.data.domain.Genre
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.network.BestPodcastsWrapper
import com.greencom.android.podcasts.network.GenresWrapper

// DTOs converters.
/** Convert [BestPodcastsWrapper] object to a [PodcastEntity] list. */
fun BestPodcastsWrapper.asPodcastEntities(): List<PodcastEntity> {
    return podcasts.map {
        PodcastEntity(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            genreId = this.genreId,
        )
    }
}

/** Convert [BestPodcastsWrapper] object to a [PodcastLocalAttrs] list. */
fun BestPodcastsWrapper.createAttrs(): List<PodcastLocalAttrs> {
    return podcasts.map {
        PodcastLocalAttrs(
            id = it.id,
            subscribed = false
        )
    }
}

/**
 * Convert [GenresWrapper] object to a [GenreEntity] list.
 *
 * If [GenresWrapper.GenresItem.parentId] is `null`, assign [Genre.NO_PARENT_GENRE]
 * value to the [GenreEntity.parentId] property.
 */
fun GenresWrapper.asGenreEntities(): List<GenreEntity> {
    return genres.map {
        GenreEntity(
            id = it.id,
            name = it.name,
            parentId = it.parentId ?: Genre.NO_PARENT_GENRE,
        )
    }
}
// DTOs converters.

// Models converters.
/**
 * Convert a [Podcast] list to a [PodcastEntity] list. Podcasts in the final list
 * will have [PodcastEntity.genreId] properties set to a given [genreId].
 */
fun List<Podcast>.asPodcastEntities(genreId: Int): List<PodcastEntity> {
    return map {
        PodcastEntity(
            id = it.id,
            title = it.title,
            description = it.description,
            image = it.image,
            publisher = it.publisher,
            explicitContent = it.explicitContent,
            episodeCount = it.episodeCount,
            latestPubDate = it.latestPubDate,
            genreId = genreId
        )
    }
}
// Models converters.