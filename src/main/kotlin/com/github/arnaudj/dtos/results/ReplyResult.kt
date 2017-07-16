package com.github.arnaudj.dtos.results

data class ReplyResult(val action: String, val channelId: String, val message: String)

val noopReply = ReplyResult("noop", "", "")
val sendMessageReply = ReplyResult("sendMessage", "", "")