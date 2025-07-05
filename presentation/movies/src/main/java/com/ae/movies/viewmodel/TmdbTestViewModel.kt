package com.ae.movies.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ae.movies.domain.model.ImageType
import com.ae.movies.domain.model.TestImage
import com.ae.movies.presentation.state.PaginationEvent
import com.ae.movies.presentation.state.PaginationState
import com.ae.network.repository.TmdbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TmdbTestViewModel(
    private val repository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaginationState())
    val uiState: StateFlow<PaginationState> = _uiState.asStateFlow()

    private var paginationJob: Job? = null

    init {
        loadNextPage()
    }

    fun onEvent(event: PaginationEvent) {
        when (event) {
            is PaginationEvent.LoadNextPage -> {
                loadNextPage()
            }
            is PaginationEvent.Refresh -> {
                refresh()
            }
            is PaginationEvent.RemoveItem -> {
                removeItem(event.item)
            }
        }
    }

    private fun loadNextPage() {
        // Prevent multiple simultaneous loads
        if (_uiState.value.isLoading || _uiState.value.endReached) return

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentPage = _uiState.value.page

                // Load movies and people in parallel
                val moviesResult = repository.getPopularMoviesPage(currentPage)
                val peopleResult = repository.getPopularPeoplePage(currentPage)

                // Add artificial delay to see loading state (remove in production)
                delay(500)

                val newImages = mutableListOf<TestImage>()

                // Process movies
                moviesResult.getOrNull()?.let { movies ->
                    val movieImages = movies.mapNotNull { movie ->
                        movie.posterPath?.let { path ->
                            TestImage(
                                url = TmdbRepository.getFullImageUrl(path),
                                label = movie.title,
                                type = ImageType.MOVIE_POSTER
                            )
                        }
                    }
                    newImages.addAll(movieImages)
                }

                // Process people
                peopleResult.getOrNull()?.let { people ->
                    val peopleImages = people.mapNotNull { person ->
                        person.profilePath?.let { path ->
                            TestImage(
                                url = TmdbRepository.getFullImageUrl(path, TmdbRepository.PROFILE_SIZE_W500),
                                label = person.name,
                                type = ImageType.ACTOR_PROFILE
                            )
                        }
                    }
                    newImages.addAll(peopleImages)
                }

                // Check if we've reached the end
                val endReached = newImages.isEmpty() || currentPage >= 10 // Limit to 10 pages for demo

                _uiState.update { state ->
                    state.copy(
                        items = state.items + newImages,
                        isLoading = false,
                        page = if (endReached) state.page else state.page + 1,
                        endReached = endReached
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun refresh() {
        paginationJob?.cancel()
        _uiState.value = PaginationState(isRefreshing = true)
        loadNextPage()
    }

    private fun removeItem(item: TestImage) {
        _uiState.update { state ->
            state.copy(items = state.items.filter { it != item })
        }
    }
}