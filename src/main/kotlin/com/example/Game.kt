package com.example

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap


class Game(var client1: Client, var client2: Client) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val id = lastId.getAndIncrement()
    var client1Turn = true

    var dice1 = 0
    var dice2 = 0
}

val games: MutableMap<Int, Game> = Collections.synchronizedMap(HashMap())


//data class Game(var client0: Client, var client1: Client, val id: Int)
//fun getLowestAvailableGameId(): Int {
//    for (id in 0..255) if (!games.containsKey(id)) return id
//    throw IllegalStateException("Game count limit reached")
//}