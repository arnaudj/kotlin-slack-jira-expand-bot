package com.github.arnaudj.linkify.spi.jira.restclient

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

open class CookieStore : CookieJar {

    private final val cookieStore : Set<Cookie> = hashSetOf<Cookie>();

    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val  validCookies : ArrayList<Cookie> = arrayListOf<Cookie>();
        cookieStore.forEach {
            if(it.expiresAt() > System.currentTimeMillis()) {
                validCookies.add(it)
            }
        }
        return validCookies;
    }

    override open fun saveFromResponse(url : HttpUrl, cookies : List<Cookie>) {
        cookieStore.plus(cookies)
    }
}