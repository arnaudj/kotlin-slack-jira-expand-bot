package com.github.arnaudj.linkify.spi.jira.restclient

import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity

interface JiraRestClient {
    fun resolve(restBaseUrl: String, jiraIssueBrowseURL:String, jiraId: String): JiraEntity
}
