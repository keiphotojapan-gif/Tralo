package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.PartnerLinks
import com.example.core.handleFirestoreError
import com.example.model.BookingStatus
import com.example.model.ChecklistItem
import com.example.model.ItineraryCategory
import com.example.model.ItineraryItem
import com.example.model.ProposedItineraryStop
import com.example.model.ProposedTripPlan
import com.example.model.SavedRoute
import com.example.model.TransitRouteOption
import com.example.model.Trip
import com.example.model.TripStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TripRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireMatchedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Googleサインインが必要です。")
        check(uid == expectedUserId) { "アカウントが切り替わりました。" }
        return uid
    }

    fun observeTrips(userId: String): Flow<List<Trip>> {
        val path = "users/$userId/trips"
        return db.collection("users").document(userId).collection("trips")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Trip::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeTripItems(userId: String, tripId: String): Flow<List<ItineraryItem>> {
        val path = "users/$userId/trips/$tripId/items"
        return db.collection("users").document(userId)
            .collection("trips").document(tripId)
            .collection("items")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ItineraryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<ItineraryItem> { it.dayIndex }.thenBy { it.startTime })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeChecklist(userId: String, tripId: String): Flow<List<ChecklistItem>> {
        val path = "users/$userId/trips/$tripId/checklist"
        return db.collection("users").document(userId)
            .collection("trips").document(tripId)
            .collection("checklist")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChecklistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<ChecklistItem> { it.checked }.thenBy { it.category }.thenBy { it.label })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeSavedRoutes(userId: String): Flow<List<SavedRoute>> {
        val path = "users/$userId/savedRoutes"
        return db.collection("users").document(userId).collection("savedRoutes")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedRoute::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareByDescending<SavedRoute> { it.favorite }.thenByDescending { it.updatedAt?.seconds ?: 0L })
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createTrip(
        userId: String,
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        companions: String,
        budgetJpy: Long,
        status: TripStatus = TripStatus.PLANNING,
        coverKey: String = "kyoto",
        latitude: Double = 35.0116,
        longitude: Double = 135.7681
    ): Result<String> {
        var docPath = "users/$userId/trips"
        return try {
            val uid = requireMatchedUser(userId)
            val cleanTitle = title.trim().take(120)
            val cleanDest = destination.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "旅行タイトルを入力してください。" }
            require(cleanDest.isNotEmpty()) { "目的地を入力してください。" }
            val docRef = db.collection("users").document(uid).collection("trips").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "destination" to cleanDest,
                "startDate" to startDate.trim().take(20),
                "endDate" to endDate.trim().take(20),
                "companions" to companions.trim().take(120).ifBlank { null },
                "budgetJpy" to budgetJpy.coerceIn(0L, 100_000_000L),
                "spentJpy" to 0L,
                "status" to status.name,
                "coverKey" to coverKey.take(60),
                "latitude" to latitude.coerceIn(-90.0, 90.0),
                "longitude" to longitude.coerceIn(-180.0, 180.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTripStatusAndSpent(
        userId: String,
        tripId: String,
        status: TripStatus,
        spentJpy: Long
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank())
            db.collection("users").document(uid).collection("trips").document(tripId)
                .update(
                    mapOf(
                        "status" to status.name,
                        "spentJpy" to spentJpy.coerceIn(0L, 100_000_000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(userId: String, tripId: String): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank())
            db.collection("users").document(uid).collection("trips").document(tripId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addItineraryItem(
        userId: String,
        tripId: String,
        dayIndex: Long,
        startTime: String,
        endTime: String?,
        title: String,
        category: ItineraryCategory,
        locationName: String,
        lineName: String? = null,
        platform: String? = null,
        carNumber: String? = null,
        seatNumber: String? = null,
        providerName: String? = null,
        bookingRef: String? = null,
        bookingUrl: String? = null,
        costJpy: Long = 0L,
        notes: String? = null,
        bookingStatus: BookingStatus = BookingStatus.PLANNED
    ): Result<String> {
        var docPath = "users/$userId/trips/$tripId/items"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank())
            val cleanTitle = title.trim().take(140)
            val cleanLoc = locationName.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "予定タイトルを入力してください。" }
            require(cleanLoc.isNotEmpty()) { "場所・駅名を入力してください。" }

            val docRef = db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "tripId" to tripId,
                "dayIndex" to dayIndex.coerceIn(1L, 60L),
                "startTime" to startTime.trim().ifBlank { "09:00" }.take(16),
                "endTime" to endTime?.trim()?.take(16)?.ifBlank { null },
                "title" to cleanTitle,
                "category" to category.name,
                "locationName" to cleanLoc,
                "lineName" to lineName?.trim()?.take(100)?.ifBlank { null },
                "platform" to platform?.trim()?.take(40)?.ifBlank { null },
                "carNumber" to carNumber?.trim()?.take(40)?.ifBlank { null },
                "seatNumber" to seatNumber?.trim()?.take(40)?.ifBlank { null },
                "providerName" to providerName?.trim()?.take(60)?.ifBlank { null },
                "bookingRef" to bookingRef?.trim()?.take(80)?.ifBlank { null },
                "bookingUrl" to bookingUrl?.trim()?.take(500)?.ifBlank { null },
                "costJpy" to costJpy.coerceIn(0L, 100_000_000L),
                "notes" to notes?.trim()?.take(500)?.ifBlank { null },
                "bookingStatus" to bookingStatus.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateItemBookingStatus(
        userId: String,
        tripId: String,
        itemId: String,
        bookingStatus: BookingStatus,
        bookingRef: String? = null,
        notes: String? = null
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/items/$itemId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank() && itemId.isNotBlank())
            val updates = mutableMapOf<String, Any>(
                "bookingStatus" to bookingStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!bookingRef.isNullOrBlank()) {
                updates["bookingRef"] = bookingRef.trim().take(80)
            }
            if (!notes.isNullOrBlank()) {
                updates["notes"] = notes.trim().take(500)
            }
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document(itemId)
                .update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteItineraryItem(userId: String, tripId: String, itemId: String): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/items/$itemId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank() && itemId.isNotBlank())
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("items").document(itemId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addChecklistItem(
        userId: String,
        tripId: String,
        label: String,
        category: String = "持ち物"
    ): Result<Unit> {
        var docPath = "users/$userId/trips/$tripId/checklist"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank())
            val cleanLabel = label.trim().take(120)
            require(cleanLabel.isNotEmpty()) { "持ち物名を入力してください。" }
            val docRef = db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("checklist").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "tripId" to tripId,
                    "label" to cleanLabel,
                    "category" to category.trim().ifBlank { "持ち物" }.take(60),
                    "checked" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleChecklistItem(
        userId: String,
        tripId: String,
        checkId: String,
        checked: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/checklist/$checkId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank() && checkId.isNotBlank())
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("checklist").document(checkId)
                .update(
                    mapOf(
                        "checked" to checked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChecklistItem(userId: String, tripId: String, checkId: String): Result<Unit> {
        val docPath = "users/$userId/trips/$tripId/checklist/$checkId"
        return try {
            val uid = requireMatchedUser(userId)
            require(tripId.isNotBlank() && checkId.isNotBlank())
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .collection("checklist").document(checkId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveRoute(
        userId: String,
        option: TransitRouteOption,
        favorite: Boolean
    ): Result<Unit> {
        var docPath = "users/$userId/savedRoutes"
        return try {
            val uid = requireMatchedUser(userId)
            val docRef = db.collection("users").document(uid).collection("savedRoutes").document()
            docPath = docRef.path
            val summary = option.legs
                .filter { it.mode.name != "WALK" }
                .joinToString(" → ") { it.lineName }
                .ifBlank { "${option.origin} → ${option.destination}" }
                .take(180)
            val legsSummary = option.legs.joinToString(" / ") {
                "${it.departureStation}→${it.arrivalStation}(${it.lineName})"
            }.take(400)
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "origin" to option.origin.take(100),
                    "destination" to option.destination.take(100),
                    "summary" to summary,
                    "durationMinutes" to option.totalMinutes.toLong().coerceIn(1L, 10080L),
                    "fareJpy" to option.totalFareJpy.toLong().coerceIn(0L, 10_000_000L),
                    "transfers" to option.transferCount.toLong().coerceIn(0L, 30L),
                    "legsSummary" to legsSummary,
                    "favorite" to favorite,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteRoute(userId: String, routeId: String, favorite: Boolean): Result<Unit> {
        val docPath = "users/$userId/savedRoutes/$routeId"
        return try {
            val uid = requireMatchedUser(userId)
            require(routeId.isNotBlank())
            db.collection("users").document(uid).collection("savedRoutes").document(routeId)
                .update(
                    mapOf(
                        "favorite" to favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedRoute(userId: String, routeId: String): Result<Unit> {
        val docPath = "users/$userId/savedRoutes/$routeId"
        return try {
            val uid = requireMatchedUser(userId)
            require(routeId.isNotBlank())
            db.collection("users").document(uid).collection("savedRoutes").document(routeId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createTripFromProposalBatch(
        userId: String,
        proposal: ProposedTripPlan
    ): Result<String> {
        var docPath = "users/$userId/trips"
        return try {
            val uid = requireMatchedUser(userId)
            val batch = db.batch()
            val tripRef = db.collection("users").document(uid).collection("trips").document()
            docPath = tripRef.path
            val totalCost = proposal.stops.sumOf { it.costJpy }
            val coverKey = when {
                proposal.destination.contains("京都") -> "kyoto"
                proposal.destination.contains("箱根") || proposal.destination.contains("富士") -> "hakone"
                else -> "tokyo"
            }

            batch.set(
                tripRef,
                mapOf(
                    "userId" to uid,
                    "title" to proposal.title.trim().take(120),
                    "destination" to proposal.destination.trim().take(100),
                    "startDate" to proposal.startDate.trim().take(20),
                    "endDate" to proposal.endDate.trim().take(20),
                    "companions" to proposal.companions.trim().take(120).ifBlank { null },
                    "budgetJpy" to proposal.budgetJpy.coerceIn(0L, 100_000_000L),
                    "spentJpy" to totalCost.coerceIn(0L, 100_000_000L),
                    "status" to TripStatus.PLANNING.name,
                    "coverKey" to coverKey,
                    "latitude" to 35.0116,
                    "longitude" to 135.7681,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            )

            proposal.stops.take(20).forEach { stop ->
                val itemRef = tripRef.collection("items").document()
                batch.set(
                    itemRef,
                    mapOf(
                        "userId" to uid,
                        "tripId" to tripRef.id,
                        "dayIndex" to stop.dayIndex.toLong().coerceIn(1L, 60L),
                        "startTime" to stop.startTime.trim().ifBlank { "09:00" }.take(16),
                        "title" to stop.title.trim().take(140),
                        "category" to stop.category.name,
                        "locationName" to stop.locationName.trim().take(120),
                        "lineName" to stop.lineName?.trim()?.take(100)?.ifBlank { null },
                        "costJpy" to stop.costJpy.coerceIn(0L, 100_000_000L),
                        "notes" to stop.notes?.trim()?.take(500)?.ifBlank { null },
                        "bookingStatus" to BookingStatus.PLANNED.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                )
            }

            proposal.checklistSuggestions.take(12).forEach { label ->
                if (label.isNotBlank()) {
                    val chkRef = tripRef.collection("checklist").document()
                    batch.set(
                        chkRef,
                        mapOf(
                            "userId" to uid,
                            "tripId" to tripRef.id,
                            "label" to label.trim().take(120),
                            "category" to "持ち物",
                            "checked" to false,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(tripRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createKyotoStarterTrip(userId: String): Result<String> {
        val proposal = ProposedTripPlan(
            title = "京都・嵐山 Autumn Escape",
            destination = "京都",
            startDate = "2026-12-05",
            endDate = "2026-12-07",
            companions = "2名",
            budgetJpy = 95000L,
            stops = listOf(
                ProposedItineraryStop(
                    dayIndex = 1,
                    startTime = "08:30",
                    title = "東海道新幹線 のぞみ21号 東京 → 京都",
                    category = ItineraryCategory.TRANSIT,
                    locationName = "東京駅 16番線",
                    lineName = "東海道新幹線（7号車・12E）",
                    costJpy = 14170L,
                    notes = "スマートEX予約・中央乗換改札利用"
                ),
                ProposedItineraryStop(
                    dayIndex = 1,
                    startTime = "15:00",
                    title = "Ace Hotel Kyoto チェックイン",
                    category = ItineraryCategory.STAY,
                    locationName = "中京区烏丸御池",
                    costJpy = 34000L,
                    notes = "Booking.comで予約状況を確認"
                ),
                ProposedItineraryStop(
                    dayIndex = 2,
                    startTime = "09:15",
                    title = "嵯峨野トロッコ列車＆天龍寺",
                    category = ItineraryCategory.ACTIVITY,
                    locationName = "嵐山・嵯峨駅",
                    costJpy = 3800L,
                    notes = "Klook体験チケット連携対応"
                )
            ),
            checklistSuggestions = listOf(
                "スマートEX QRコード / 交通系ICカード",
                "宿泊確認書 (Booking.com)",
                "折りたたみ傘・防寒ストール",
                "モバイルバッテリー"
            )
        )
        val result = createTripFromProposalBatch(userId, proposal)
        if (result.isSuccess) {
            val tripId = result.getOrNull().orEmpty()
            if (tripId.isNotBlank()) {
                val uid = auth.currentUser?.uid
                if (uid == userId) {
                    runCatching {
                        db.collection("users").document(uid).collection("trips").document(tripId)
                            .update(
                                mapOf(
                                    "status" to TripStatus.ACTIVE.name,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                    }
                }
            }
        }
        return result
    }

    fun bookingLinkFor(destination: String, startDate: String, endDate: String): String =
        PartnerLinks.bookingComSearchUrl(destination, startDate, endDate)
}
