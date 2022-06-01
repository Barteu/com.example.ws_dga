package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.example.plugins.*

fun main() {
    embeddedServer(Netty, port = 8888, host = "127.0.0.1") {
        configureSockets()
    }.start(wait = true)
}
