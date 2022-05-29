package com.example

import io.ktor.websocket.*
import java.util.*
import kotlin.collections.LinkedHashSet

data class Client(val session: DefaultWebSocketSession, val pawnPositions: MutableList<Byte> = mutableListOf())

val clientsInQueue = Collections.synchronizedSet(LinkedHashSet<Client>())