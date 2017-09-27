package com.github.arnaudj.linkify.spi.jira

data class JiraEntity(val key: String, val jiraIssueBrowseURL:String, val summary: String = "", val fieldsMap: Map<String, Any> = mapOf()) {
    fun getURL():String {
        return "$jiraIssueBrowseURL/$key"
    }
}
