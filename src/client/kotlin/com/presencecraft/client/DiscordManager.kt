package com.presencecraft.client

import com.presencecraft.client.config.PresenceCraftConfig
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import hunggisagoner.discordipc.DiscordIPC
import hunggisagoner.discordipc.RichPresence
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.PauseScreen
import java.time.Instant

object DiscordManager {

    private const val APP_ID = 1517495928171528252L // discord appilication id
    private var connected = false
    private var ticks = 0
    private var cachedDay = 0L

    private val rpc = RichPresence()
    private val startTime = Instant.now().epochSecond
    // get config
    val config: PresenceCraftConfig by lazy {
        AutoConfig.getConfigHolder(PresenceCraftConfig::class.java).config
    }

    fun init() {
        AutoConfig.register(PresenceCraftConfig::class.java) { definition, configClass -> 
            GsonConfigSerializer(definition, configClass) 
        }

        println("[PresenceCraft] Initializing Discord IPC...")
        connect()

        Runtime.getRuntime().addShutdownHook(Thread {
            if (connected) {
                DiscordIPC.stop()
                println("[PresenceCraft] RPC Closed")
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register { mc ->
            if (!config.enableRPC) {
                if (connected) {
                    DiscordIPC.stop()
                    connected = false
                    println("[PresenceCraft] RPC disabled in config")
                }
                return@register
            }

            if (!connected && ticks % 100 == 0) {
                println("[PresenceCraft] Attempting to reconnect...")
                connect()
            }

            if (connected && ticks >= 40) {
                updatePresence(mc)
                ticks = 0
            } else {
                ticks++
            }
        }
    }

    private fun connect() {
        connected = DiscordIPC.start(APP_ID) {
            println("[PresenceCraft] Connected! Logged in as: ${DiscordIPC.getUser()?.username}")
        }
        if (!connected) {
            println("[PresenceCraft] Cannot connect to Discord RPC")
        }
    }

    private fun updatePresence(mc: Minecraft) {
        val player = mc.player
        val level = mc.level
        val currentScreen = mc.screen

        var details = "Main Menu"
        var state = "Browsing Menus"
        var largeImageKey = "overworld"
        var dimensionName = ""

        var partyId: String? = null
        var partySize = 0
        var partyMax = 0

        if (level != null && player != null) {
            if (mc.hasSingleplayerServer()) {
                val integratedServer = mc.singleplayerServer
                if (integratedServer != null && integratedServer.isPublished) {
                    details = "Multiplayer (Hosting LAN)"
                } else {
                    details = "Singleplayer"
                }
                
                if (integratedServer != null && config.showPlayerCount) {
                    partySize = integratedServer.playerCount
                    partyMax = integratedServer.maxPlayers
                    partyId = "singleplayer_${integratedServer.motd.hashCode()}"
                }
            } else if (mc.getCurrentServer() != null) {
                val server = mc.getCurrentServer()
                val ip = server?.ip?.lowercase() ?: ""

                val isLan = server?.isLan == true

                details = when {
                    ip.contains("realm") -> "Multiplayer (Realm)"
                    isLan -> "Multiplayer (LAN)"
                    else -> "Multiplayer (Server)"
                }

                if (config.showPlayerCount) {
                    partySize = mc.connection?.onlinePlayers?.size ?: 1
                    partyMax = 0
                    partyId = "multiplayer_${ip.hashCode()}"
                }
            }

            if (currentScreen != null) {
                state = if (currentScreen is PauseScreen) {
                    "Game Paused"
                } else {
                    "Browsing Menus"
                }
            } else {
                if (mc.getCurrentServer() != null) {
                    val serverIp = mc.getCurrentServer()?.ip?.lowercase() ?: "Unknown"
                    state = if (config.showServerIP) "On $serverIp" else ""
                } else {
                    state = if (config.showSurvivalDay) "Surviving: Day $cachedDay" else ""
                }
            }

            val rawDim = level.dimension().toString()
            if (config.showDimension) {
                when {
                    rawDim.contains("the_nether") -> {
                        largeImageKey = "thenether"
                        dimensionName = "The Nether"
                    }
                    rawDim.contains("the_end") -> {
                        largeImageKey = "theend"
                        dimensionName = "The End"
                    }
                    else -> {
                        largeImageKey = "overworld"
                        dimensionName = "Overworld"
                    }
                }
            } else {
                largeImageKey = "overworld"
                dimensionName = ""
            }

            val rawDay = level.dayTime / 24000L
            if (!(rawDay == 0L && cachedDay > 0L && player.tickCount < 40)) {
                cachedDay = rawDay
            }
            
        } else {
            details = "Main Menu"
            state = "Browsing Menus"
            cachedDay = 0L
        }

        rpc.setStart(startTime)
        rpc.setDetails(if (details.isNotEmpty()) details else null)
        rpc.setState(if (state.isNotEmpty()) state else null)

        if (partyId != null) {
            rpc.party = hunggisagoner.discordipc.RichPresence.Party(partyId, partySize, partyMax)
        } else {
            rpc.party = null
        }

        val mcVersion = SharedConstants.getCurrentVersion().name()
        val largeHoverText = buildString {
            if (config.showMinecraftVersion) append("Minecraft $mcVersion") else append("Minecraft")
            if (dimensionName.isNotEmpty()) append(" - $dimensionName")
        }
        rpc.setLargeImage(largeImageKey, largeHoverText)

        if (config.showPlayerFace) {
            val playerName = mc.user.name
            rpc.setSmallImage(
                "https://minotar.net/helm/$playerName/128.png",
                playerName
            )
        } else {
            rpc.setSmallImage(null, null)
        }

        DiscordIPC.setActivity(rpc)
        println("[PresenceCraft] RPC Updated | Details: $details | State: $state")
    }
}