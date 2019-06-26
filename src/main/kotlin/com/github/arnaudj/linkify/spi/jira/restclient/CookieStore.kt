package com.github.arnaudj.linkify.spi.jira.restclient

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.TimeUnit

/**
 * Custom [Cookie] wrapper with overridable expiresAt
 */
class CookieEx(val cookie: Cookie, private val expiresAt: Long?) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return if (expiresAt != null)
            expiresAt <= now
        else
            cookie.expiresAt() <= now
    }

    override fun toString(): String {
        return "[expiresAt=$expiresAt, cookie=$cookie]"
    }
}

open class CookieStore : CookieJar {
    private val sessionCookieName = "JSESSIONID"
    private val sessionCookieExpiryHours = 8L
    private val cookieStore: MutableSet<CookieEx> = mutableSetOf()

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        clearExpired()
        return cookieStore.toList().map { it.cookie }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            var expires = cookie.expiresAt()
            if (cookie.name() == sessionCookieName) { // no expiresAt provided, add it
                expires = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(sessionCookieExpiryHours)
            }
            cookieStore.add(CookieEx(cookie, expires))
        }
    }

    @Synchronized
    open fun hasValidSessionCookie(): Boolean {
        clearExpired()
        return cookieStore.find { it.cookie.name() == sessionCookieName } != null
    }

    @Synchronized
    private fun clearExpired() {
        cookieStore.removeIf { it.isExpired() }
    }

    @Synchronized
    fun clearAll() {
        cookieStore.clear()
    }

    override fun toString(): String {
        return cookieStore.toString()
    }
}