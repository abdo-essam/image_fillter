package com.ae.movies.presentation.state

import com.ae.movies.domain.model.TestImage

data class PaginationState(
    val isLoading: Boolean = false,
    val items: List<TestImage> = emptyList(),
    val error: String? = null,
    val endReached: Boolean = false,
    val page: Int = 1,
    val isRefreshing: Boolean = false
)

sealed class PaginationEvent {
    object LoadNextPage : PaginationEvent()
    object Refresh : PaginationEvent()
    data class RemoveItem(val item: TestImage) : PaginationEvent()
}