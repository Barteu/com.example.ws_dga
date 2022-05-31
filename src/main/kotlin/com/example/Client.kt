package com.example

import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

data class Client(val session: DefaultWebSocketSession)

val clientsInQueue = Collections.synchronizedSet(LinkedHashSet<Client>())