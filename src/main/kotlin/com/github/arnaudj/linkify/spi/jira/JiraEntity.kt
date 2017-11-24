package com.github.arnaudj.linkify.spi.jira


typealias JiraKeyType = String

data class JiraEntity(val key: JiraKeyType, val jiraIssueBrowseURL: String = "", val summary: String = "", val fieldsMap: Map<String, Any> = mapOf()) {
    fun getURL(): String {
        return "$jiraIssueBrowseURL/$key"
    }
}
