package com.ae.movies.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ae.network.repository.TmdbRepository
import com.ae.movies.domain.model.ImageType
import com.ae.movies.domain.model.TestImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TmdbTestViewModel(
    private val repository: TmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TmdbTestUiState())
    val uiState: StateFlow<TmdbTestUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Fetch popular movies
                repository.getPopularMovies()
                    .catch { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message)
                        }
                    }
                    .collect { movies ->
                        val movieImages = movies.mapNotNull { movie ->
                            movie.posterPath?.let { path ->
                                TestImage(
                                    url = TmdbRepository.getFullImageUrl(path),
                                    label = movie.title,
                                    type = ImageType.MOVIE_POSTER
                                )
                            }
                        }

                        _uiState.update { state ->
                            state.copy(
                                images = state.images + movieImages,
                                isLoading = false
                            )
                        }
                    }

                // Fetch popular people
                repository.getPopularPeople()
                    .catch { /* Handle error */ }
                    .collect { people ->
                        val peopleImages = people.mapNotNull { person ->
                            person.profilePath?.let { path ->
                                TestImage(
                                    url = TmdbRepository.getFullImageUrl(path, TmdbRepository.PROFILE_SIZE_W500),
                                    label = person.name,
                                    type = ImageType.ACTOR_PROFILE
                                )
                            }
                        }

                        _uiState.update { state ->
                            state.copy(
                                images = state.images + peopleImages
                            )
                        }
                    }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(images = emptyList(), error = null) }
        loadImages()
    }
}

data class TmdbTestUiState(
    val images: List<TestImage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)