package com.greencom.android.podcasts.ui.podcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.greencom.android.podcasts.R
import com.greencom.android.podcasts.databinding.FragmentPodcastBinding
import com.greencom.android.podcasts.ui.dialogs.UnsubscribeDialog
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastEvent
import com.greencom.android.podcasts.ui.podcast.PodcastViewModel.PodcastState
import com.greencom.android.podcasts.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

// Saving instance state.
private const val STATE_IS_APP_BAR_EXPANDED = "STATE_IS_APP_BAR_EXPANDED"
private const val STATE_IS_SCROLL_TO_TOP_SHOWN = "STATE_IS_SCROLL_TO_TOP_SHOWN"

private const val FAB_DISTANCE_TOP_THRESHOLD = 10
private const val SMOOTH_SCROLL_THRESHOLD = 100

@ExperimentalTime
@AndroidEntryPoint
class PodcastFragment : Fragment(), UnsubscribeDialog.UnsubscribeDialogListener {

    /** Nullable View binding. Only for inflating and cleaning. Use [binding] instead. */
    private var _binding: FragmentPodcastBinding? = null
    private val binding get() = _binding!!

    /** PodcastViewModel. */
    private val viewModel: PodcastViewModel by viewModels()

    // Navigation arguments.
    private val args: PodcastFragmentArgs by navArgs()

    private var podcastId = ""

    /** Is the app bar expanded. `false` means collapsed. */
    private val isAppBarExpanded = MutableStateFlow(true)

    /** RecyclerView adapter. */
    private val adapter: PodcastWithEpisodesAdapter by lazy {
        PodcastWithEpisodesAdapter(
            sortOrder = viewModel.sortOrder,
            updateSubscription = viewModel::updateSubscription,
            changeSortOrder = viewModel::changeSortOrder,
            playEpisode = viewModel::playEpisode,
            play = viewModel::play,
            pause = viewModel::pause,
        )
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
        postponeEnterTransition(100L, TimeUnit.MILLISECONDS)

        // Restore instance state.
        savedInstanceState?.apply {
            binding.appBarLayout.setExpanded(getBoolean(STATE_IS_APP_BAR_EXPANDED), false)
            binding.scrollToTop.apply { if (getBoolean(STATE_IS_SCROLL_TO_TOP_SHOWN)) show() else hide() }
        }

        // Get the podcast ID from the navigation arguments.
        podcastId = args.podcastId
        viewModel.podcastId = podcastId

        // Load a podcast with episodes.
        viewModel.getPodcastWithEpisodes()
        viewModel.fetchEpisodes()

        initAppBar()
        initRecyclerView()
        initViews()
        initObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putBoolean(STATE_IS_APP_BAR_EXPANDED, isAppBarExpanded.value)
            putBoolean(STATE_IS_SCROLL_TO_TOP_SHOWN, binding.scrollToTop.isOrWillBeShown)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel adapter coroutine scope in onDetachedFromRecyclerView().
        binding.list.adapter = null
        // Clear View binding.
        _binding = null
    }

    // Unsubscribe from the podcast if the user confirms in the UnsubscribeDialog.
    override fun onUnsubscribeClick(podcastId: String) {
        viewModel.unsubscribe(podcastId)
    }

    /** App bar setup. */
    private fun initAppBar() {
        // Disable AppBarLayout dragging behavior.
        setAppBarLayoutCanDrag(binding.appBarLayout, false)

        // Track app bar state.
        binding.appBarLayout.addOnOffsetChangedListener(object : AppBarLayoutStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout, newState: Int) {
                when (newState) {
                    EXPANDED -> isAppBarExpanded.value = true
                    COLLAPSED -> isAppBarExpanded.value = false
                    else -> {  }
                }
            }
        })
    }

    /** RecyclerView setup. */
    private fun initRecyclerView() {
        val divider = CustomDividerItemDecoration(requireContext(), true)
        divider.setDrawable(
            ResourcesCompat.getDrawable(resources, R.drawable.shape_divider, context?.theme)!!
        )

        val onScrollListener = object : RecyclerView.OnScrollListener() {
            val layoutManager = binding.list.layoutManager as LinearLayoutManager
            var totalItemCount = 0
            var firstVisibleItemPosition = 0
            var firstCompletelyVisibleItemPosition = 0
            var lastVisibleItemPosition = 0
            var scrollToTopInitialCheckSkipped = false

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                totalItemCount = layoutManager.itemCount
                firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Fetch more episodes.
                if (totalItemCount >= 10 && lastVisibleItemPosition >= totalItemCount - 10 && dy > 0) {
                    viewModel.fetchMoreEpisodes()
                }

                // Show and hide the podcast title in the app bar.
                binding.appBarTitle.apply {
                    if (firstVisibleItemPosition >= 1) revealImmediately() else hideCrossfade()
                }

                // Show and hide app bar divider.
                binding.appBarDivider.apply {
                    if (firstCompletelyVisibleItemPosition > 0) revealImmediately() else hideCrossfade()
                }

                // Show and hide the fab. Skip the initial check to restore instance state.
                if (scrollToTopInitialCheckSkipped) {
                    binding.scrollToTop.apply {
                        if (firstVisibleItemPosition >= FAB_DISTANCE_TOP_THRESHOLD && dy < 0) {
                            show()
                        } else {
                            hide()
                        }
                    }
                } else {
                    scrollToTopInitialCheckSkipped = true
                }
            }
        }

        binding.list.apply {
            adapter = this@PodcastFragment.adapter
            adapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            addItemDecoration(divider)
            addOnScrollListener(onScrollListener)
        }

        setupSwipeToRefresh(binding.swipeToRefresh, requireContext())
    }

    /** Fragment views setup. */
    private fun initViews() {
        // Hide all screens to then reveal them with crossfade animations.
        hideScreens()

        // Handle toolbar back button clicks.
        binding.appBarBack.setOnClickListener { findNavController().navigateUp() }

        // Force episodes fetching on swipe-to-refresh.
        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.fetchEpisodes(true)
        }

        // Scroll to top.
        binding.scrollToTop.setOnClickListener {
            // Do smooth scroll only if the user has not scrolled far enough.
            if ((binding.list.layoutManager as LinearLayoutManager)
                    .findFirstVisibleItemPosition() <= SMOOTH_SCROLL_THRESHOLD) {
                binding.list.smoothScrollToPosition(0)
                binding.appBarLayout.setExpanded(true, true)
            } else {
                binding.list.scrollToPosition(0)
                binding.appBarLayout.setExpanded(true, true)
            }
        }

        // Fetch the podcast from the error screen.
        binding.error.tryAgain.setOnClickListener {
            viewModel.fetchPodcast()
        }
    }

    /** Set observers for ViewModel observables. */
    private fun initObservers() {
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

        // Observe app bar state to run title animation.
        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.STARTED) {
            isAppBarExpanded.collectLatest {
                if (it) {
                    delay(1000) // Delay animation.
                    binding.appBarTitle.isSelected = true
                } else {
                    binding.appBarTitle.isSelected = false
                }
            }
        }
    }

    /** Handle UI states. */
    private fun handleUiState(state: PodcastState) {
        binding.swipeToRefresh.isVisible = state is PodcastState.Success

        when (state) {
            // Show success screen.
            is PodcastState.Success -> {
                showSuccessScreen()
                binding.appBarTitle.text = state.podcastWithEpisodes.podcast.title
                adapter.submitHeaderAndList(
                    state.podcastWithEpisodes.podcast,
                    state.podcastWithEpisodes.episodes
                )
            }

            // Show error screen.
            is PodcastState.Error -> showErrorScreen()

            // Show loading screen.
            is PodcastState.Loading -> showLoadingScreen()
        }
    }

    /** Handle events. */
    private fun handleEvent(event: PodcastEvent) {
        binding.error.tryAgain.isEnabled = event !is PodcastEvent.Fetching
        binding.error.progressBar.isVisible = event is PodcastEvent.Fetching

        // Change 'Try again' button text.
        if (event is PodcastEvent.Fetching) {
            binding.error.tryAgain.text = getString(R.string.loading)
        } else {
            binding.error.tryAgain.text = getString(R.string.try_again)
        }

        when (event) {
            // Show a snackbar.
            is PodcastEvent.Snackbar -> showSnackbar(binding.root, event.stringRes)

            // Show UnsubscribeDialog.
            is PodcastEvent.UnsubscribeDialog -> {
                UnsubscribeDialog.show(childFragmentManager, podcastId)
            }

            // Show Loading process.
            is PodcastEvent.Fetching -> binding.error.progressBar.revealCrossfade()

            // Show episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingStarted -> {
                binding.episodesProgressBar.revealImmediately()
            }

            // Stop episodes fetching progress bar.
            is PodcastEvent.EpisodesFetchingFinished -> {
                binding.episodesProgressBar.hideCrossfade()
            }

            // Stop episodes swipe-to-refresh indicator.
            PodcastEvent.EpisodesForcedFetchingFinished -> {
                binding.swipeToRefresh.isRefreshing = false
            }
        }
    }

    /** Show success screen and hide all others. */
    private fun showSuccessScreen() {
        binding.list.revealCrossfade()
        binding.error.root.hideImmediately()
        binding.loading.hideImmediately()
    }

    /** Show loading screen and hide all others. */
    private fun showLoadingScreen() {
        binding.loading.revealImmediately()
        binding.list.hideImmediately()
        binding.error.root.hideImmediately()
    }

    /** Show error screen and hide all others. */
    private fun showErrorScreen() {
        binding.error.root.revealCrossfade()
        binding.loading.hideImmediately()
        binding.list.hideImmediately()
    }

    /** Hide all screens. */
    private fun hideScreens() {
        binding.list.hideImmediately()
        binding.error.root.hideImmediately()
        binding.loading.hideImmediately()
    }
}