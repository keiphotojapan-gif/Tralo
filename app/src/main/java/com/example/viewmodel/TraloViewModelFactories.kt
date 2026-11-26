package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.core.AppPreferences
import com.example.data.repository.AiConciergeRepository
import com.example.data.repository.TransitRepository
import com.example.data.repository.TripRepository
import com.example.data.repository.WeatherRepository
import com.google.firebase.firestore.FirebaseFirestore

object TraloViewModelFactories {
    private fun resolveFirestore(app: Application): FirebaseFirestore {
        val dbId = app.getString(R.string.firestore_database_id)
        return FirebaseFirestore.getInstance(dbId)
    }

    fun homeAndTripsFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val db = resolveFirestore(app)
            HomeAndTripsViewModel(
                tripRepository = TripRepository(db),
                weatherRepository = WeatherRepository(),
                transitRepository = TransitRepository(),
                preferences = AppPreferences(app),
                currentUserId = currentUserId
            )
        }
    }

    fun transitFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val db = resolveFirestore(app)
            TransitViewModel(
                transitRepository = TransitRepository(),
                tripRepository = TripRepository(db),
                preferences = AppPreferences(app),
                currentUserId = currentUserId
            )
        }
    }

    fun conciergeFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
            val db = resolveFirestore(app)
            ConciergeViewModel(
                aiRepository = AiConciergeRepository(),
                tripRepository = TripRepository(db),
                currentUserId = currentUserId
            )
        }
    }
}
