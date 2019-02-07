package com.github.arnaudj.linkify.engines.jira.entities

import com.github.arnaudj.linkify.eventdriven.commands.Command
import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.salomonbrys.kodein.Kodein


data class ResolveJiraCommand(val key: JiraKeyType, val source: EventSourceData, val kodein: Kodein) : Command
