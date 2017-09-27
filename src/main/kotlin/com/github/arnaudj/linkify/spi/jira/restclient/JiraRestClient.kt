package com.github.arnaudj.linkify.spi.jira.restclient

import com.github.arnaudj.linkify.spi.jira.JiraEntity

interface JiraRestClient {
    fun resolve(restBaseUrl: String, jiraIssueBrowseURL:String, jiraId: String): JiraEntity
}
