package com.github.arnaudj.linkify.cqrs

import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class DataStore<T>(val name: String, val handler: (DataStore<T>) -> Unit) {
    val store = ArrayBlockingQueue<T>(30)
    var threadHandle: Thread? = null

    fun start() {
        threadHandle = thread(start = true, name = name) {
            handler.invoke(this)
        }
    }

    fun stop() {
        threadHandle?.let {
            threadHandle?.interrupt()
            threadHandle = null
        }
    }
}
