package com.presencecraft.client

import net.fabricmc.api.ClientModInitializer

class PresenceCraftClient : ClientModInitializer {

    override fun onInitializeClient() {
        println("[PresenceCraft]: Initialized!")
        DiscordManager.init()
    }
}