package com.github.arnaudj.linkify.spi.jira

interface JiraResolutionService {
    fun resolve(jiraId: String): JiraEntity
}