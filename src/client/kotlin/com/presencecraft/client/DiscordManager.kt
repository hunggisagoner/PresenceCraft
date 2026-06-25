package com.presencecraft.client

import com.jagrosh.discordipc.IPCClient
import com.jagrosh.discordipc.entities.DiscordBuild
import com.jagrosh.discordipc.entities.RichPresence
import net.minecraft.client.Minecraft
import kotlin.concurrent.fixedRateTimer

object DiscordManager {

private const val app_id = 1517495928171528252L // discord application id

private lateinit var client: IPCClient

private var connected = false

private var lastDetails = ""
private var lastState = ""

// start elapsed time when mod loaded
private val startTime = System.currentTimeMillis() / 1000

fun init() {

    println("loaded")
    
    client = IPCClient(app_id)
    ConnectToDiscordRPC()

    fixedRateTimer(
        name = "PresenceCraft-RPC",
        daemon = true,
        initialDelay = 0L,
        period = 1000L
    ) {
        if (!connected) {
            ConnectToDiscordRPC()
        }

        UpdatePresence()
    }
}

private fun ConnectToDiscordRPC() {
    try {

        client.connect(DiscordBuild.ANY)

        connected = true

        println("connected")

    } catch (e: Exception) {

        connected = false

        println("cant connect to discord")
        e.printStackTrace()
    }
}

// upd presence func
private fun UpdatePresence() {

val mc = Minecraft.getInstance()

val details: String
val state: String

val level = mc.level

if (level == null) {

    details = "Main Menu"
    state = ""

} else {

    details = when {

        mc.hasSingleplayerServer() ->
            "Singleplayer"

        mc.currentServer != null -> {

            val server = mc.currentServer
            val ip = server?.ip?.lowercase() ?: ""

            if (ip.contains("realm"))
                "Multiplayer (Realm)"
            else
                "Multiplayer (Server)"
        }

        else ->
            "Multiplayer"
    }

    state = when {

        level.dimension() == net.minecraft.world.level.Level.NETHER ->
            "The Nether"

        level.dimension() == net.minecraft.world.level.Level.END ->
            "The End"

        else ->
            "Overworld"
    }
}

if (details == lastDetails &&
    state == lastState) {
    return
}

lastDetails = details
lastState = state

try {

    val presence = RichPresence.Builder()
        .setDetails(details)
        .setState(state)
        .setStartTimestamp(startTime)
        .build()

    client.sendRichPresence(presence)

    println("updated: $details | $state")

} catch (e: Exception) {

    connected = false

    println("cant update rpc bruh")
    e.printStackTrace()
}

}

}