package com.greencom.android.podcasts.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.ItemSearchPodcastBinding
import com.greencom.android.podcasts.utils.PodcastDiffCallback
import com.greencom.android.podcasts.utils.coverBuilder

// TODO

class SearchResultAdapter(
    private val navigateToPodcast: (String) -> Unit,
) : ListAdapter<Podcast, SearchResultViewHolder>(PodcastDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        return SearchResultViewHolder.create(
            parent = parent,
            navigateToPodcast = navigateToPodcast
        )
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val podcast = getItem(position)
        val isLast = position == itemCount - 1 && itemCount >= 3
        holder.bind(podcast, isLast)
    }
}

class SearchResultViewHolder private constructor(
    private val binding: ItemSearchPodcastBinding,
    private val navigateToPodcast: (String) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val context = binding.root.context

    private lateinit var podcast: Podcast

    init {
        binding.root.setOnClickListener {
            navigateToPodcast(podcast.id)
        }
    }

    fun bind(podcast: Podcast, isLast: Boolean) {
        this.podcast = podcast

        binding.apply {
            cover.load(podcast.image) { coverBuilder(context) }
            title.text = podcast.title
            publisher.text = podcast.publisher
            description.text = HtmlCompat.fromHtml(
                podcast.description,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            ).toString().trim()
            divider.isVisible = !isLast
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            navigateToPodcast: (String) -> Unit,
        ): SearchResultViewHolder {
            val binding = ItemSearchPodcastBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return SearchResultViewHolder(
                binding = binding,
                navigateToPodcast = navigateToPodcast
            )
        }
    }
}