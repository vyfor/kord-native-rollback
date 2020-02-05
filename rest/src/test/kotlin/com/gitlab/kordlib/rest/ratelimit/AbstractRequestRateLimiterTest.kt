package com.gitlab.kordlib.rest.ratelimit

import com.gitlab.kordlib.common.entity.DiscordGuild
import com.gitlab.kordlib.rest.request.JsonRequest
import com.gitlab.kordlib.rest.route.Route
import io.ktor.util.StringValues
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@ExperimentalTime
@ExperimentalCoroutinesApi
abstract class AbstractRequestRateLimiterTest {

    abstract fun newRequestRateLimiter(clock: Clock) : RequestRateLimiter

    private val timeout = 1000.seconds
    private val instant = Instant.EPOCH
    val RateLimit.Companion.exhausted get() = RateLimit(Total(5), Remaining(0))

    private suspend fun RequestRateLimiter.sendRequest(clock: TestClock, guildId: Long, bucketKey: Long = guildId, rateLimit: RateLimit) {
        val request = JsonRequest<Unit, DiscordGuild>(Route.GuildGet, mapOf(Route.GuildId to guildId.toString()), StringValues.Empty, StringValues.Empty, null)
        val token = await(request)
        when (rateLimit.isExhausted) {
            true -> token.complete(RequestResponse.BucketRateLimit(BucketKey(bucketKey.toString()), rateLimit, Reset(clock.instant().plus(timeout.toJavaDuration()))))
            else -> token.complete(RequestResponse.Accepted(BucketKey(bucketKey.toString()), rateLimit, Reset(clock.instant().plus(timeout.toJavaDuration()))))
        }
    }

    @Test
    fun `a RequestRateLimiter will suspend for rate limited requests with the same identifier`() = runBlockingTest {
        val clock = TestClock(instant, this, ZoneOffset.UTC)
        val rateLimiter = newRequestRateLimiter(clock)

        rateLimiter.sendRequest(clock, 1, rateLimit = RateLimit.exhausted)
        rateLimiter.sendRequest(clock, 1, rateLimit = RateLimit(Total(5), Remaining(5)))

        assertEquals(timeout.inMilliseconds.toLong(), currentTime)
    }

    @Test
    fun `a RequestRateLimiter will suspend for rate limited requests with the same bucket`() = runBlockingTest {
        val clock = TestClock(instant, this, ZoneOffset.UTC)
        val rateLimiter = newRequestRateLimiter(clock)

        rateLimiter.sendRequest(clock, 1, 1, rateLimit = RateLimit.exhausted)
        rateLimiter.sendRequest(clock, 2, 1 , rateLimit = RateLimit(Total(5), Remaining(5))) //discovery
        rateLimiter.sendRequest(clock, 2, 1, rateLimit = RateLimit(Total(5), Remaining(5)))

        assertEquals(timeout.inMilliseconds.toLong(), currentTime)
    }

    @Test
    fun `a RequestRateLimiter will not suspend for rate limited requests that don't share an identifier`() = runBlockingTest {
        val clock = TestClock(instant, this, ZoneOffset.UTC)
        val rateLimiter = newRequestRateLimiter(clock)

        rateLimiter.sendRequest(clock, 1, rateLimit = RateLimit.exhausted)
        rateLimiter.sendRequest(clock, 2, rateLimit = RateLimit(Total(5), Remaining(5))) //discovery
        rateLimiter.sendRequest(clock, 2, rateLimit = RateLimit.exhausted)

        assertEquals(0, currentTime)
    }
}