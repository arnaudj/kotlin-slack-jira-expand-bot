package com.github.arnaudj.linkify.spi.jira.restclient

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.time.Duration
import kotlin.system.measureNanoTime

open class CookieStore : CookieJar {

    private final val cookieStore : Set<Cookie> = hashSetOf<Cookie>();

    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val  validCookies : ArrayList<Cookie> = arrayListOf<Cookie>();
        cookieStore.forEach {
            if(it.expiresAt() >  Duration.ofHours(8).toMillis()) {
                validCookies.add(it)
            }
        }
        return validCookies;
    }

    override open fun saveFromResponse(url : HttpUrl, cookies : List<Cookie>) {
        cookieStore.plus(cookies)
    }

    open fun getCookieStore(): Set<Cookie>{
        return cookieStore;
    }
}