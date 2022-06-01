package com.example

import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.HashMap

val games: MutableMap<Int, Game> = Collections.synchronizedMap(HashMap())

class Game(var client1: Client, var client2: Client) {
    companion object {
        var lastId = AtomicInteger(0)
    }
    val id = lastId.getAndIncrement()
    var client1Turn = true

    var dice1 = 0
    var dice2 = 0

    var c1pawnsPos: MutableList<Byte> = mutableListOf()
    var c2pawnsPos: MutableList<Byte> = mutableListOf()

    fun isPositionFree(pos: Int): Boolean{
        if (c1pawnsPos[0].toInt() == pos ||
            c1pawnsPos[1].toInt() == pos ||
            c2pawnsPos[0].toInt() == pos ||
            c2pawnsPos[1].toInt() == pos
                ){
            return false
        }
        return true
    }

    fun respawnClient1Pawn(pawn: Int){
        for (i in 0..5){
            if (isPositionFree(i)){
                c1pawnsPos[pawn] = i.toByte()
                break
            }
        }
    }

    fun respawnClient2Pawn(pawn: Int){
        for (i in 11..16){
            if (isPositionFree(i)){
                c2pawnsPos[pawn] = i.toByte()
                break
            }
        }
    }

}


