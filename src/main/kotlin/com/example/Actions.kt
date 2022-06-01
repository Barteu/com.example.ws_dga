package com.example
import io.ktor.websocket.*
import kotlin.random.Random


suspend fun joinGame(client: Client, bytes: ByteArray) {
    clientsInQueue += client
    var game: Game? = null
    synchronized(clientsInQueue) {
        if (clientsInQueue.size == 2) {
            game = Game(clientsInQueue.first(), clientsInQueue.last())
            clientsInQueue.clear()
            games[game!!.id] = game!!

            with(game!!.c1pawnsPos) {
                clear()
                add(0.toByte())
                add(1.toByte())
            }
            with(game!!.c2pawnsPos) {
                clear()
                add(11.toByte())
                add(12.toByte())
            }
        }
    }
    game?.let { sendGameStartedMssg(it) }
}


fun reconnect(client: Client, bytes: ByteArray) {
    val gameId = bytes[1].toInt()
    val clientNumber = bytes[2].toInt()
    val game = games[gameId] ?: return
    println("reconnecting GameID: $gameId, clientNumber: $clientNumber")


    if (clientNumber == 1) {
        game.client1 = client
    } else {
        game.client2 = client
    }
}


suspend fun rollDice(bytes: ByteArray){

    val result = Random.nextInt(1, 7)

    val gameId = bytes[1].toInt()
    val game = games[gameId]
    val playerNumber = bytes[2].toInt()

    val message = byteArrayOf(0x03, result.toByte())
    val messageToEnemy = byteArrayOf(0x05, result.toByte())

    if (game!!.client1Turn && playerNumber == 1){
        game.client1.session.send(Frame.Binary(true, message))
        game.dice1 = result
        game.client2.session.send(Frame.Binary(true, messageToEnemy))
    }
    else if(!game!!.client1Turn && playerNumber == 2){
        game.client2.session.send(Frame.Binary(true, message))
        game.dice2 = result
        game.client1.session.send(Frame.Binary(true, messageToEnemy))
    }
    else{
        println("Unfair play in game $gameId")
        return
    }

    println("Dice throw: result:$result, clientId=$playerNumber")
}


suspend fun sendGameStartedMssg(game: Game) {
    // 4, GameID, clientNumber, hasTurn
    val message1 = byteArrayOf(0x04, game.id.toByte(), 1.toByte(), 1.toByte())
    val message2 = byteArrayOf(0x04, game.id.toByte(), 2.toByte(), 0.toByte())

    println("Sending game start messages GameID: ${game.id}")

    game.client1.session.send(Frame.Binary(true, message1))
    game.client2.session.send(Frame.Binary(true, message2))
}

suspend fun movePawn(client: Client, bytes: ByteArray){

    val gameId = bytes[1].toInt()
    val game = games[gameId]
    val playerNumber = bytes[2].toInt()
    val pawnNumber = bytes[3].toInt()

    println("Player $playerNumber used pawn $pawnNumber in GameID: $gameId")



        if ((game!!.client1Turn && playerNumber == 1) &&
            ( game!!.c1pawnsPos[pawnNumber].toInt() !in 22..26)){
            val diceResult = game.dice1
            if (game.c1pawnsPos[pawnNumber].toInt()+diceResult>21){
                if (game.isPositionFree(22)){
                    game.c1pawnsPos[pawnNumber] = 22.toByte()
                }
                else{
                    game.c1pawnsPos[pawnNumber] = 23.toByte()
                }
            }
            else{
                val newPos = (game.c1pawnsPos[pawnNumber].toInt()+diceResult)%22
                if (game.c1pawnsPos[0].toInt()==newPos){
                    game.respawnClient1Pawn(0)
                }
                else if(game.c1pawnsPos[1].toInt()==newPos){
                    game.respawnClient1Pawn(1)
                }
                else if(game.c2pawnsPos[0].toInt()==newPos){
                    game.respawnClient2Pawn(0)
                }
                else if(game.c2pawnsPos[1].toInt()==newPos){
                    game.respawnClient2Pawn(1)
                }
                game.c1pawnsPos[pawnNumber] = (newPos%22).toByte()
            }

        }
        else if((!game!!.client1Turn && playerNumber == 2)&&
            ( game!!.c2pawnsPos[pawnNumber].toInt() !in 22..26)){
            val diceResult = game.dice2
            if ( (game.c2pawnsPos[pawnNumber].toInt()+diceResult >10) &&
                (game.c2pawnsPos[pawnNumber].toInt()) <= 10   ){
                if (game.isPositionFree(24)){
                    game.c2pawnsPos[pawnNumber] = 24.toByte()
                }
                else{
                    game.c2pawnsPos[pawnNumber] = 25.toByte()
                }
            }
            else{
                val newPos = (game.c2pawnsPos[pawnNumber].toInt()+diceResult)%22
                if (game.c1pawnsPos[0].toInt()==newPos){
                    game.respawnClient1Pawn(0)
                }
                else if(game.c1pawnsPos[1].toInt()==newPos){
                    game.respawnClient1Pawn(1)
                }
                else if(game.c2pawnsPos[0].toInt()==newPos){
                    game.respawnClient2Pawn(0)
                }
                else if(game.c2pawnsPos[1].toInt()==newPos){
                    game.respawnClient2Pawn(1)
                }
                game.c2pawnsPos[pawnNumber] = newPos.toByte()
            }
        }


    val message = byteArrayOf(0x07,
                              game.c1pawnsPos[0],
                              game.c1pawnsPos[1],
                              game.c2pawnsPos[0],
                              game.c2pawnsPos[1])

    game.client1.session.send(Frame.Binary(true, message))
    game.client2.session.send(Frame.Binary(true, message))

    game.apply { client1Turn = !client1Turn }

    if (!game.isPositionFree(22) && !game.isPositionFree(23)){
        game.client1.session.send(Frame.Binary(true, byteArrayOf(0x09, 0x01)))
        game.client2.session.send(Frame.Binary(true, byteArrayOf(0x09, 0x01)))
        games.remove(game.id)
    }
    else if(!game.isPositionFree(24) && !game.isPositionFree(25)){
        game.client1.session.send(Frame.Binary(true, byteArrayOf(0x09, 0x02)))
        game.client2.session.send(Frame.Binary(true, byteArrayOf(0x09, 0x02)))
        games.remove(game.id)
    }
    else if (game.client1Turn){
        game.client1.session.send(Frame.Binary(true, byteArrayOf(0x08)))
        println("Now its client1 turn")
    }
    else{
        game.client2.session.send(Frame.Binary(true, byteArrayOf(0x08)))
        println("Now its client2 turn")
    }
}