package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.linkify.spi.jira.JiraResolutionService
import com.github.arnaudj.linkify.spi.jira.JiraResolutionServiceImpl
import com.github.arnaudj.linkify.spi.jira.restclient.Jira7RestClientImpl
import com.github.arnaudj.linkify.spi.jira.restclient.JiraRestClient
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton

class SlackbotModule {
    companion object {
        fun getInjectionBindings(configMap: Map<String, Any>): Kodein.Module {
            return Kodein.Module {
                bind<JiraRestClient>() with singleton { Jira7RestClientImpl(configMap) }
                bind<JiraResolutionService>() with singleton { JiraResolutionServiceImpl(configMap, instance()) }
            }
        }
    }
}