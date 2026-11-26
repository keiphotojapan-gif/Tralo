package com.example.model

data class ProposedItineraryStop(
    val dayIndex: Int,
    val startTime: String,
    val title: String,
    val category: ItineraryCategory,
    val locationName: String,
    val lineName: String? = null,
    val costJpy: Long = 0L,
    val notes: String? = null
)

data class ProposedTripPlan(
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val companions: String,
    val budgetJpy: Long,
    val stops: List<ProposedItineraryStop>,
    val checklistSuggestions: List<String>
)

data class ConciergeMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String,
    val proposedTrip: ProposedTripPlan? = null,
    val appliedToCloud: Boolean = false
)

sealed interface ConciergeUiState {
    data object Idle : ConciergeUiState
    data object Loading : ConciergeUiState
    data class Streaming(val partialText: String) : ConciergeUiState
    data class Error(val message: String) : ConciergeUiState
}
