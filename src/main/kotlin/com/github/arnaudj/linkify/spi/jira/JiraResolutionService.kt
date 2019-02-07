package com.github.arnaudj.linkify.spi.jira

import com.github.arnaudj.linkify.engines.jira.entities.JiraEntity

interface JiraResolutionService {
    fun resolve(jiraId: String): JiraEntity
}