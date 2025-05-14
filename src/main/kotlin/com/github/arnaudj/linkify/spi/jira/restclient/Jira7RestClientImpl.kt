package com.github.arnaudj.linkify.spi.jira.restclient

import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.slf4j.LoggerFactory
import java.net.Proxy

// For Jira 7.2.x - https://docs.atlassian.com/jira/REST/7.2.3/
open class Jira7RestClientImpl(configMap: Map<String, Any>) : JiraRestClient {
    private val logger = LoggerFactory.getLogger(Jira7RestClientImpl::class.java)
    private val cookieStore = CookieStore()
    val jiraAuthUser = configMap[ConfigurationConstants.jiraRestServiceAuthUser] as String
    val jiraAuthPwd = configMap[ConfigurationConstants.jiraRestServiceAuthPassword] as String
    val clientProxyHost = configMap[ConfigurationConstants.clientProxyHost] as String?

    open fun createClientBuilder(): okhttp3.OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()
                .cookieJar(cookieStore)
                .authenticator(object : Authenticator {
                    override fun authenticate(route: Route?, response: Response): Request? {
                        // handle reply code 401 (somehow policy implemented in getRequest was not matched by server)
                        if (response.request.header("Authorization") != null) {
                            logger.info("* authenticate(): already tried Authorization header, bailing")
                            return null
                        }

                        logger.info("* authenticate(): adding Authorization header for authentication challenge & clear cookie store")
                        cookieStore.clearAll() // avoid propagating a stale JSESSIONID
                        return response.request.newBuilder()
                                .header("Authorization", Credentials.basic(jiraAuthUser, jiraAuthPwd))
                                .build()
                    }
                })

        if (clientProxyHost == null || clientProxyHost.isBlank()) {
            builder.proxy(Proxy.NO_PROXY)
        }
        return builder
    }

    open fun getRequest(url: String): okhttp3.Request {
        val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json; charset=utf-8".toMediaTypeOrNull().toString())
                .addHeader("User-Agent", "${this.javaClass.simpleName}/1")
                .get()

        // preemptive session cookie check
        if (!cookieStore.hasValidSessionCookie()) {
            logger.info("* getRequest: no valid session cookie, sending Authorization again - cookieStore: $cookieStore")
            request.addHeader("Authorization", Credentials.basic(jiraAuthUser, jiraAuthPwd))
        } else {
            logger.info("* getRequest: has valid session cookie - cookieStore: $cookieStore")
        }

        return request.build()
    }

    override fun resolve(restBaseUrl: String, jiraIssueBrowseURL: String, jiraId: String): JiraEntity {
        require(!jiraAuthUser.isNullOrEmpty())
        try {
            val url = "$restBaseUrl/rest/api/latest/issue/$jiraId"
            val request: Request = getRequest(url)
            val client = createClientBuilder().build()
            logger.info("> Request: $request -- headers: ${request.headers}")

            client.newCall(request).execute().use { response ->
                logger.info("< Reply code ${response.code} -- headers: ${response.headers}")

                if (!response.isSuccessful) {
                    if (response.code == 401) {
                        cookieStore.clearAll()
                    }
                    return JiraEntity(jiraId, jiraIssueBrowseURL, "Http call unsuccessful: ${response.code}: ${response.message}")
                }

                val payload = response.body?.string() ?: ""
                logger.debug("< Reply: ###$payload###")

                return if (payload.isEmpty())
                    JiraEntity(jiraId, jiraIssueBrowseURL, "Empty reply")
                else
                    decodeEntity(payload, jiraIssueBrowseURL)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            return JiraEntity(jiraId, jiraIssueBrowseURL, "Bot error: " + t.message + " caused by " + t.cause?.message)
        }
    }

    fun decodeEntity(payload: String, jiraIssueBrowseURL: String): JiraEntity {
        // https://docs.atlassian.com/jira/REST/7.2.3/#api/2/issue-getIssue
        // Jira markdown to slack markdown: https://github.com/shaunburdick/jira2slack/blob/master/index.js
        val json = JsonParser().parse(payload).asJsonObject

        val summary = getStringOptional(json, "summary", "")
        val key = getStringOptional(json, "key", "")

        val fieldsMap = mutableMapOf<String, Any>()
        if (json.hasNotNull("fields")) {
            val fields = json.getAsJsonObject("fields")
            fieldsMap["summary"] = getStringOptional(fields, "summary", "")

            fieldsMap["created"] = getStringOptional(fields, "created", "")
            fieldsMap["updated"] = getStringOptional(fields, "updated", "")

            if (fields.hasNotNull("status"))
                fieldsMap["status.name"] = getStringOptional(fields.getAsJsonObject("status"), "name", "")

            if (fields.hasNotNull("priority"))
                fieldsMap["priority.name"] = getStringOptional(fields.getAsJsonObject("priority"), "name", "")

            if (fields.hasNotNull("reporter"))
                fieldsMap["reporter.name"] = getStringOptional(fields.getAsJsonObject("reporter"), "name", "")

            if (fields.hasNotNull("assignee"))
                fieldsMap["assignee.name"] = getStringOptional(fields.getAsJsonObject("assignee"), "name", "")
        }

        return JiraEntity(key, jiraIssueBrowseURL, summary, fieldsMap)
    }

    private fun JsonObject.hasNotNull(name: String): Boolean {
        if (!has(name))
            return false

        val value = get(name)
        return value != null && value !is JsonNull
    }

    private fun getStringOptional(json: JsonObject, name: String, defaultValue: String): String {
        return try {
            if (json.has(name)) json.get(name).asString else defaultValue
        } catch (e: Throwable) {
            "Error while parsing"
        }
    }

}
