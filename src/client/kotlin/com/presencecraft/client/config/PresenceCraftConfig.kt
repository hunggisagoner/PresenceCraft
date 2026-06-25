package com.presencecraft.client.config

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = "presencecraft")
class PresenceCraftConfig : ConfigData {
    
    var enableRPC: Boolean = true

    @ConfigEntry.Category("privacy")
    var showServerIP: Boolean = false

    @ConfigEntry.Category("privacy")
    var showPlayerCount: Boolean = true

    @ConfigEntry.Category("privacy")
    var showPlayerFace: Boolean = true

    @ConfigEntry.Category("display")
    var showMinecraftVersion: Boolean = true

    @ConfigEntry.Category("display")
    var showDimension: Boolean = true

    @ConfigEntry.Category("display")
    var showSurvivalDay: Boolean = true
}