package brightspark.asynclocator

import brightspark.asynclocator.platform.Services

object AsyncLocatorModCommon {
  fun printConfigs() {
    val config = Services.CONFIG
    ALConstants.logInfo(
      """
            Configs:
            Locator Threads: ${config.locatorThreads()}
            Remove Offer: ${config.removeOffer()}
            Dolphin Treasure Enabled: ${config.dolphinTreasureEnabled()}
            Eye Of Ender Enabled: ${config.eyeOfEnderEnabled()}
            Exploration Map Enabled: ${config.explorationMapEnabled()}
            Locate Command Enabled: ${config.locateCommandEnabled()}
            Villager Trade Enabled: ${config.villagerTradeEnabled()}
            """.trimIndent()
    )
  }
}
