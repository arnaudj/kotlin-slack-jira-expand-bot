package com.github.arnaudj.linkify.slackbot.listener

import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.handler.BoltEventHandler
import com.slack.api.bolt.response.Response
import com.slack.api.model.event.MessageEvent

class MessageListener(private val bot: BotFacade) : BoltEventHandler<MessageEvent> {

    override fun apply(payload: EventsApiPayload<MessageEvent>, context: EventContext): Response {
        val event = payload.event
        val botUserId = payload.authorizations[0].userId
        val sender = event.user
        val messageBotId = event.botId

        if (botUserId == sender || messageBotId != null && messageBotId.isNotEmpty()) {
            // filter out own and 3rd party bot messages
            return context.ack()
        }

        val text = event.text
        if (text != null) {
            bot.handleChatMessage(text, EventSourceData(event.channel, sender, event.ts, event.threadTs))
        }
        return context.ack()
    }
}