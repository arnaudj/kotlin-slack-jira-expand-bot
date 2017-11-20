package com.github.arnaudj.linkify.slackbot.eventdriven.commands

import com.github.arnaudj.linkify.eventdriven.commands.Command
import com.github.arnaudj.linkify.spi.jira.JiraKeyType
import com.github.salomonbrys.kodein.Kodein


class ResolveJiraCommand(val key: JiraKeyType, val sourceId: String, kodein: Kodein) : Command
