package com.github.arnaudj.linkify.spi.jira

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient

class JiraResolutionServiceImpl(configMap: Map<String, Any>, val restClient: JiraRestClient) : JiraResolutionService {
    val jiraHostBaseUrl = configMap[ConfigurationConstants.jiraHostBaseUrl] as String
    val resolveViaAPI = configMap[ConfigurationConstants.jiraResolveWithAPI] as Boolean

    override fun resolve(jiraId: String): JiraEntity {
        // TODO Act on resolveViaAPI to hit Jira REST API if needed, or else the below : restClient.resolve(jiraId)
        return JiraEntity(jiraId, "<$jiraHostBaseUrl/$jiraId|$jiraId>")
    }
}