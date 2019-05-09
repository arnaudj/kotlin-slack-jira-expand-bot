package com.github.arnaudj.linkify.spi.jira.restclient

import com.github.arnaudj.linkify.engines.jira.ConfigurationConstants
import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.*
import org.slf4j.LoggerFactory




// For Jira 7.2.x - https://docs.atlassian.com/jira/REST/7.2.3/
open class Jira7RestClientImpl(configMap: Map<String, Any>) : JiraRestClient {
    private val logger = LoggerFactory.getLogger(Jira7RestClientImpl::class.java)
    private final val cookieStore = CookieStore()
    val jiraAuthUser = configMap[ConfigurationConstants.jiraRestServiceAuthUser] as String
    val jiraAuthPwd = configMap[ConfigurationConstants.jiraRestServiceAuthPassword] as String


    open fun createClientBuilder(): okhttp3.OkHttpClient.Builder{
        return OkHttpClient.Builder();
    }

    open fun getRequest(url:String?): okhttp3.Request {
        var request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", MediaType.parse("application/json; charset=utf-8").toString())
                .addHeader("User-Agent", "${this.javaClass.simpleName}/1")
                .get()
                .build()

        if(cookieStore.getCookieStore().isEmpty())
            request = request.newBuilder()
                       .addHeader("Authorization", Credentials.basic(jiraAuthUser, jiraAuthPwd))
                       .get()
                       .build()
        else {
            val cookie:Cookie? = cookieStore.getCookieStore().find { it.name() == "JSESSIONID" }
            if(cookie!= null) {
                request = request.newBuilder()
                        .addHeader("Set-Cookie", "${cookie.name()}:${cookie.value()}; Max-Age=28800")
                        .get()
                        .build()
            }
        }

        return request
    }

    override fun resolve(restBaseUrl: String, jiraIssueBrowseURL: String, jiraId: String): JiraEntity {
        require(!jiraAuthUser.isNullOrEmpty())
        try {
            val url: String? = "$restBaseUrl/rest/api/latest/issue/$jiraId"
            val request : Request = getRequest(url)
            val client = createClientBuilder()
                    .cookieJar(cookieStore)
                    .build()
            logger.info("> Request: $request")

            client.newCall(request).execute().use { response ->
                logger.info("< Reply code ${response.code()}")

                if (!response.isSuccessful) {
                    return JiraEntity(jiraId, jiraIssueBrowseURL, "Http call unsuccessful: ${response.code()}: ${response.message()}")
                }

                val payload = response.body()?.string() ?: ""
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
