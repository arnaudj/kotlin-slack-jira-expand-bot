package com.github.arnaudj.linkify.slackbot.listener

import com.github.arnaudj.linkify.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.slackbot.BotFacade
import com.slack.api.app_backend.events.payload.EventsApiPayload
import com.slack.api.bolt.context.builtin.EventContext
import com.slack.api.bolt.handler.BoltEventHandler
import com.slack.api.bolt.response.Response
import com.slack.api.model.event.MessageChangedEvent

class MessageChangedListener(private val bot: BotFacade) : BoltEventHandler<MessageChangedEvent> {

    companion object {
        private const val ACK_EMOJI = "white_check_mark"
    }

    override fun apply(payload: EventsApiPayload<MessageChangedEvent>, context: EventContext): Response {
        val event = payload.event
        val text = event.message.text
        if (text != null) {
            bot.handleChatMessage(text, EventSourceData(event.channel, "unknownuid", event.message.ts, event.message.threadTs))
        }
        return context.ack()
    }
}