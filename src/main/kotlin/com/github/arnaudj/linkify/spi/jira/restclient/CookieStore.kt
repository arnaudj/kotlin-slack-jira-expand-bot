package com.github.arnaudj.linkify.spi.jira.restclient

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

open class CookieStore : CookieJar {

    private val cookieStore: MutableSet<Cookie> = mutableSetOf()

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        clearCache()
        return cookieStore.toList()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach {
            cookieStore.add(it)
        }
    }

    @Synchronized
    open fun hasValidSessionCookie(): Boolean {
        clearCache()
        return cookieStore.find { it.name() == "JSESSIONID" } != null
    }

    @Synchronized
    private fun clearCache() {
        val now = System.currentTimeMillis()
        cookieStore.removeIf { it.expiresAt() <= now }
    }

    override fun toString(): String {
        return cookieStore.toString()
    }
}