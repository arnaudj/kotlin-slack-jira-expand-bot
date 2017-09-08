package com.github.arnaudj.linkify.spi.jira

import com.github.arnaudj.linkify.config.ConfigurationConstants

class JiraResolutionServiceImpl(configMap: Map<String, Any>) : JiraResolutionService {
    val jiraHostBaseUrl = configMap[ConfigurationConstants.jiraHostBaseUrl] as String
    val resolveViaAPI = configMap[ConfigurationConstants.jiraResolveWithAPI] as Boolean

    override fun resolve(jiraId: String) : JiraEntity {
        // TODO Act on resolveViaAPI to hit Jira REST API if needed, or else the below
        return JiraEntity(jiraId, "<$jiraHostBaseUrl/$jiraId|$jiraId>")
    }
}