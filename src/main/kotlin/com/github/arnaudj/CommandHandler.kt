package com.github.arnaudj

import com.github.arnaudj.dtos.results.ReplyResult

interface CommandHandler {
    fun handleMessage(message: String, channelId: String, userId: String): List<ReplyResult>
}