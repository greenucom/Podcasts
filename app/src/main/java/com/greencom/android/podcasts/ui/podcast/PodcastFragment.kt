package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.data.domain.Episode
import com.greencom.android.podcasts.data.domain.Podcast
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PodcastFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentPodcastBinding? = null
    /** Non-null View binding. */
    private val binding get() = _binding!!

    /** PodcastViewModel. */
    private val viewModel: PodcastViewModel by viewModels()

    /** Navigation Safe Args. */
    private val args: PodcastFragmentArgs by navArgs()

    /** Podcast ID. */
    private var id = ""

    /** The [Podcast] associated with this fragment. */
    private lateinit var podcast: Podcast

    /** The list of podcast episodes associated with this fragment. */
    private var episodes = listOf<Episode>()

    /** RecyclerView adapter. */
    private val adapter: PodcastWithEpisodesAdapter by lazy {
        PodcastWithEpisodesAdapter(viewModel::updateSubscription)
    }

    // TODO
    private val onScrollListener: RecyclerView.OnScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.list.layoutManager as LinearLayoutManager
            var visibleItemCount = 0
            var totalItemCount = 0
            var firstVisibleItemPosition = 0

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                visibleItemCount = layoutManager.childCount
                totalItemCount = layoutManager.itemCount
                firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Show and hide the fab.
                if (firstVisibleItemPosition >= 5 && dy < 0) {
                    binding.scrollToTop.show()
                } else {
                    binding.scrollToTop.hide()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // View binding setup.
        _binding = FragmentPodcastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Postpone and start enter transition.
        postponeEnterTransition(100L, TimeUnit.MILLISECONDS)

        // Get the podcast ID from the navigation arguments.
        id = args.podcastId

        // Load the podcast.
        viewModel.getPodcast(id)

        setupAppBar()
        setupRecyclerView()
        setupViews()

        setObservers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel adapter coroutine scope in onDetachedFromRecyclerView().
        adapter.onDetachedFromRecyclerView(binding.list)
        // Clear View binding.
        _binding = null
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }

    /** App bar setup. */
    private fun setupAppBar() {
        // Disable AppBarLayout dragging behavior.
        if (binding.appBarLayout.layoutParams != null) {
            val appBarParams = binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams
            val appBarBehavior = AppBarLayout.Behavior()
            appBarBehavior.setDragCallback(object : AppBarLayout.Behavior.DragCallback() {
                override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                    return false
                }
            })
            appBarParams.behavior = appBarBehavior
        }
    }

    /** RecyclerView setup. */
    private fun setupRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext(), true)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )
        binding.list.apply {
            adapter = this@PodcastFragment.adapter
            addItemDecoration(divider)
        }
        binding.list.addOnScrollListener(onScrollListener)

        setupSwipeToRefresh(binding.swipeToRefresh, requireContext())
    }

    /** Fragment views setup. */
    private fun setupViews() {
        hideErrorScreen()

        // Handle toolbar back button clicks.
        binding.toolbarBack.setOnClickListener { findNavController().navigateUp() }

        // TODO
        binding.swipeToRefresh.setOnRefreshListener {

        }

        // Scroll to top and hide the fab.
        binding.scrollToTop.setOnClickListener {
            binding.list.smoothScrollToPosition(0)
            binding.appBarLayout.setExpanded(true, true)
            (it as FloatingActionButton).hide()
        }

        // Fetch the podcast from the error screen.
        binding.error.tryAgain.setOnClickListener { viewModel.fetchPodcast(id) }
    }

    /** Set observers for ViewModel observables. */
    private fun setObservers() {
        // Observe UI states.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.uiState.collectLatest { state ->
                handleUiState(state)
            }
        }

        // Observe events.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.event.collect { event ->
                handleEvent(event)
            }
        }

        // Observe podcast episodes.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            viewModel.getEpisodes(id).collectLatest { episodes ->
                submitToAdapter(episodes)
            }
        }
    }

    /** Handle UI states. */
    private fun handleUiState(state: PodcastState) {
        binding.swipeToRefresh.isVisible = state is PodcastState.Success
        binding.error.root.isVisible = state is PodcastState.Error
        binding.loading.isVisible = state is PodcastState.Loading

        when (state) {

            // Show podcast data.
            is PodcastState.Success -> {
                podcast = state.podcast
                submitToAdapter(podcast)
                binding.list.revealCrossfade()
                hideErrorScreen()
            }

            // Show error screen.
            is PodcastState.Error -> {
                binding.error.root.revealCrossfade()
                hideSuccessScreen()
            }

            // Make `when` expression exhaustive.
            is PodcastState.Loading -> {  }
        }
    }

    /** Handle events. */
    private fun handleEvent(event: PodcastEvent) {
        binding.error.tryAgain.isEnabled = event !is PodcastEvent.Fetching
        binding.error.progressBar.isVisible = event is PodcastEvent.Fetching

        // Change 'Try again' button text.
        if (event is PodcastEvent.Fetching) {
            binding.error.tryAgain.text = getString(R.string.explore_loading)
        } else {
            binding.error.tryAgain.text = getString(R.string.explore_try_again)
        }

        when (event) {

            // Show a snackbar.
            is PodcastEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

            // Show UnsubscribeDialog.
            is PodcastEvent.UnsubscribeDialog ->
                UnsubscribeDialog.show(childFragmentManager, podcast.id)

            // Show Loading process.
            is PodcastEvent.Fetching -> binding.error.progressBar.revealCrossfade()

            // Show episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingStarted ->
                binding.episodesProgressBar.revealImmediately()

            // Hide episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingFinished ->
                binding.episodesProgressBar.hideCrossfade()
        }
    }

    /** Submit a podcast data to [adapter]. */
    private fun submitToAdapter(podcast: Podcast) {
        this.podcast = podcast
        adapter.submitHeaderAndList(this.podcast, this.episodes)
    }

    /** Submit an episode list to [adapter]. */
    private fun submitToAdapter(episodes: List<Episode>) {
        this.episodes = episodes
        adapter.submitHeaderAndList(this.podcast, this.episodes)
    }

    /** Set alpha of the success screen to 0. */
    private fun hideSuccessScreen() {
        binding.list.alpha = 0f
    }

    /** Set alpha of the error screen to 0. */
    private fun hideErrorScreen() {
        binding.error.root.alpha = 0f
    }
}