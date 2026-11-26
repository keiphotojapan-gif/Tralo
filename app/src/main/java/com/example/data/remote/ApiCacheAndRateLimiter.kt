package com.example.data.remote

import java.util.concurrent.ConcurrentHashMap

data class ProviderRatePolicy(
    val minRequestIntervalMs: Long,
    val freshTtlMs: Long,
    val maxStaleTtlMs: Long
)

data class CacheEntry<T>(
    val value: T,
    val fetchedAtMs: Long,
    val updatedAtLabel: String
)

object ProviderRatePolicies {
    val odptPositions = ProviderRatePolicy(
        minRequestIntervalMs = 20_000L,
        freshTtlMs = 30_000L,
        maxStaleTtlMs = 15 * 60_000L
    )
    val odptStatus = ProviderRatePolicy(
        minRequestIntervalMs = 45_000L,
        freshTtlMs = 90_000L,
        maxStaleTtlMs = 30 * 60_000L
    )
    val staticCatalog = ProviderRatePolicy(
        minRequestIntervalMs = 60_000L,
        freshTtlMs = 6 * 60 * 60_000L,
        maxStaleTtlMs = 24 * 60 * 60_000L
    )
    val ekispertRoute = ProviderRatePolicy(
        minRequestIntervalMs = 2_000L,
        freshTtlMs = 120_000L,
        maxStaleTtlMs = 30 * 60_000L
    )
    val mixwayRealtime = ProviderRatePolicy(
        minRequestIntervalMs = 20_000L,
        freshTtlMs = 45_000L,
        maxStaleTtlMs = 15 * 60_000L
    )
    val jrRealtime = ProviderRatePolicy(
        minRequestIntervalMs = 30_000L,
        freshTtlMs = 45_000L,
        maxStaleTtlMs = 15 * 60_000L
    )
}

class ApiCacheAndRateLimiter(
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()
    private val lastNetworkAttemptMs = ConcurrentHashMap<String, Long>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getFresh(key: String, policy: ProviderRatePolicy): CacheEntry<T>? {
        val entry = cache[key] ?: return null
        val age = clockMs() - entry.fetchedAtMs
        return if (age <= policy.freshTtlMs) entry as? CacheEntry<T> else null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getStale(key: String, policy: ProviderRatePolicy): CacheEntry<T>? {
        val entry = cache[key] ?: return null
        val age = clockMs() - entry.fetchedAtMs
        return if (age <= policy.maxStaleTtlMs) entry as? CacheEntry<T> else null
    }

    fun canIssueNetworkRequest(key: String, policy: ProviderRatePolicy): Boolean {
        val last = lastNetworkAttemptMs[key] ?: return true
        return (clockMs() - last) >= policy.minRequestIntervalMs
    }

    fun remainingCooldownSeconds(key: String, policy: ProviderRatePolicy): Long {
        val last = lastNetworkAttemptMs[key] ?: return 0L
        val remainingMs = policy.minRequestIntervalMs - (clockMs() - last)
        return if (remainingMs > 0L) ((remainingMs + 999L) / 1000L) else 0L
    }

    fun recordAttempt(key: String) {
        lastNetworkAttemptMs[key] = clockMs()
    }

    fun <T : Any> put(key: String, value: T, updatedAtLabel: String) {
        cache[key] = CacheEntry(
            value = value,
            fetchedAtMs = clockMs(),
            updatedAtLabel = updatedAtLabel
        )
    }

    fun clearAll() {
        cache.clear()
        lastNetworkAttemptMs.clear()
    }
}
