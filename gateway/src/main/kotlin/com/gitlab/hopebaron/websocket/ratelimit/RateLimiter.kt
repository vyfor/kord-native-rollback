package com.gitlab.hopebaron.websocket.ratelimit

/**
 * A rate limiter that limits the amount of [consume] invocations that can be made over a certain period.
 */
interface RateLimiter {

    /**
     * Acquires a permit for a single action. Suspends if necessary until the permit would not exceed
     * the maximum frequency of permits.
     */
    suspend fun consume()
}

/**
 * Acquires a permit for a single [action]. Suspends if necessary until the permit would not exceed
 * the maximum frequency of permits.
 *
 * @param action The action that correlates to a single permit.
 */
suspend inline fun <T> RateLimiter.consume(action: () -> T): T {
    consume()
    return action()
}

