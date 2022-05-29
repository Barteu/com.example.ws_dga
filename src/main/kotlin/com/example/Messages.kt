package com.example
import io.ktor.websocket.*
import kotlin.random.Random


suspend fun joinGame(client: Client, bytes: ByteArray) {
    clientsInQueue += client
    with(client.pawnPositions) {
        clear()
        add(bytes[1])
        add(bytes[2])
        add(bytes[3])
    }
    //log("Processing join game message. (Pokemon IDs: ${player.pokemonIDs})")

    var game: Game? = null
    synchronized(clientsInQueue) {
        if (clientsInQueue.size == 2) {
            //val newGameId = getLowestAvailableGameId()
            //log("Creating game. (Game ID: $newGameId)")
            game = Game(clientsInQueue.first(), clientsInQueue.last())
            clientsInQueue.clear()
            games[game!!.id] = game!!
        }
    }
    game?.let { sendGameStartMssg(it) }
}


fun reconnect(client: Client, bytes: ByteArray) {
    val gameId = bytes[1].toInt()
    val clientNumber = bytes[2].toInt()
    val game = games[gameId] ?: return

    //log("Processing reconnect message. (Player Number: $playerNumber, Game ID: $gameId)")

    if (clientNumber == 1) {
        game.client1 = client
    } else {
        game.client2 = client
    }
}


suspend fun throwDice(bytes: ByteArray){

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

    game!!.client1Turn = !game!!.client1Turn
    println("Dice throw: result:$result, clientId=$playerNumber")
}


suspend fun sendGameStartMssg(game: Game) {
    // 4, GameID, clientNumber, hasTurn, pawn1pos, pawn2pos, pawn3pos
    val message1 = byteArrayOf(0x04, game.id.toByte(), 1.toByte(), 1.toByte(), 0.toByte(), 1.toByte(), 2.toByte())
    val message2 = byteArrayOf(0x04, game.id.toByte(), 2.toByte(), 0.toByte(), 0.toByte(), 1.toByte(), 2.toByte())

    println("Sending game start messages GameID: ${game.id}")

    game.client1.session.send(Frame.Binary(true, message1))
    game.client2.session.send(Frame.Binary(true, message2))
}
