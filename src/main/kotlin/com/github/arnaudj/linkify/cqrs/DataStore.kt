package com.github.arnaudj.linkify.cqrs

import java.util.concurrent.ArrayBlockingQueue

class DataStore<T> {
    val store = ArrayBlockingQueue<T>(30)
}
