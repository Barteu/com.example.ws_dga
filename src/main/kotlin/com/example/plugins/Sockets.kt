package com.example.plugins

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

import com.example.Client
import com.example.joinGame
import com.example.reconnect
import com.example.throwDice


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/dga") { // websocketSession
            val client = Client(this)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        val bytes = frame.readBytes()
                        //outgoing.send(Frame.Text("YOU SAID: $text"))
                        when (bytes[0].toInt()) {
                            0 -> joinGame(client, bytes)
                            1 -> reconnect(client, bytes)
                            2 -> throwDice(bytes)
                        }
                    }
                    else -> {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
    }
}