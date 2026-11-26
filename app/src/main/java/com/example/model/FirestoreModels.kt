package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TripStatus(val labelJa: String) {
    PLANNING("計画中"),
    UPCOMING("予定"),
    ACTIVE("旅行中"),
    COMPLETED("完了");

    companion object {
        fun fromRaw(raw: String?): TripStatus =
            entries.firstOrNull { it.name == raw } ?: PLANNING
    }
}

enum class ItineraryCategory(val labelJa: String) {
    TRANSIT("移動"),
    STAY("宿泊"),
    ACTIVITY("体験・チケット"),
    DINING("食事"),
    SPOT("観光");

    companion object {
        fun fromRaw(raw: String?): ItineraryCategory =
            entries.firstOrNull { it.name == raw } ?: SPOT
    }
}

enum class BookingStatus(val labelJa: String) {
    PLANNED("予定"),
    CONFIRMED("予約済"),
    CHECKED_IN("チェックイン済"),
    CANCELLED("キャンセル");

    companion object {
        fun fromRaw(raw: String?): BookingStatus =
            entries.firstOrNull { it.name == raw } ?: PLANNED
    }
}

@Keep
@IgnoreExtraProperties
data class Trip(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val destination: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val companions: String = "",
    val budgetJpy: Long = 0L,
    val spentJpy: Long = 0L,
    val status: String = TripStatus.PLANNING.name,
    val coverKey: String = "kyoto",
    val latitude: Double = 35.0116,
    val longitude: Double = 135.7681,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: TripStatus get() = TripStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class ItineraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tripId: String = "",
    val dayIndex: Long = 1L,
    val startTime: String = "09:00",
    val endTime: String? = null,
    val title: String = "",
    val category: String = ItineraryCategory.SPOT.name,
    val locationName: String = "",
    val lineName: String? = null,
    val platform: String? = null,
    val carNumber: String? = null,
    val seatNumber: String? = null,
    val providerName: String? = null,
    val bookingRef: String? = null,
    val bookingUrl: String? = null,
    val costJpy: Long = 0L,
    val notes: String? = null,
    val bookingStatus: String = BookingStatus.PLANNED.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val categoryEnum: ItineraryCategory get() = ItineraryCategory.fromRaw(category)
    val bookingStatusEnum: BookingStatus get() = BookingStatus.fromRaw(bookingStatus)
}

@Keep
@IgnoreExtraProperties
data class ChecklistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tripId: String = "",
    val label: String = "",
    val category: String = "持ち物",
    val checked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedRoute(
    @DocumentId val id: String = "",
    val userId: String = "",
    val origin: String = "",
    val destination: String = "",
    val summary: String = "",
    val durationMinutes: Long = 30L,
    val fareJpy: Long = 0L,
    val transfers: Long = 0L,
    val legsSummary: String = "",
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
