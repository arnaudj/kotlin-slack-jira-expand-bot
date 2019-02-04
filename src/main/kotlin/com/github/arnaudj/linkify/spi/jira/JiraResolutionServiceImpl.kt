package com.github.arnaudj.linkify.spi.jira

import com.github.arnaudj.linkify.config.ConfigurationConstants
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import org.slf4j.LoggerFactory

class JiraResolutionServiceImpl(
        configMap: Map<String, Any>,
        val restClient: JiraRestClient
) : JiraResolutionService {

    private val logger = LoggerFactory.getLogger(JiraResolutionService::class.java)
    val jiraIssueBrowseURL = configMap[ConfigurationConstants.jiraBrowseIssueBaseUrl] as String
    val jiraRestServiceBaseUrl = configMap[ConfigurationConstants.jiraRestServiceBaseUrl] as String
    val resolveViaAPI = !(configMap[ConfigurationConstants.jiraRestServiceBaseUrl] as String).isEmpty() && !(configMap[ConfigurationConstants.jiraRestServiceAuthUser] as String).isEmpty()

    override fun resolve(jiraId: String): JiraEntity {
        val entity = if (resolveViaAPI)
            restClient.resolve(jiraRestServiceBaseUrl, jiraIssueBrowseURL, jiraId)
        else
            JiraEntity(jiraId, jiraIssueBrowseURL)

        logger.debug("< [jira service] Resolved entity: $entity")
        return entity
    }
}
