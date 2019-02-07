package com.github.arnaudj.linkify.engines.jira.entities

typealias JiraKeyType = String

data class JiraEntity(
        val key: JiraKeyType,
        val jiraIssueBrowseURL: String = "",
        val summary: String = "",
        val fieldsMap: Map<String, Any> = mapOf()
)