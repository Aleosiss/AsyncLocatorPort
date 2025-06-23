package brightspark.asynclocator

import brightspark.asynclocator.AsyncLocatorMod.CONFIG
import brightspark.asynclocator.extensions.LOG

object AsyncLocatorModCommon {
  fun printConfigs() {
    val config = CONFIG
    LOG.info(
      """
            Configs:
            Locator Threads: ${config.locatorThreads.get()}
            Remove Offer: ${config.removeOffer.get()}
            Dolphin Treasure Enabled: ${config.dolphinTreasureEnabled.get()}
            Eye Of Ender Enabled: ${config.eyeOfEnderEnabled.get()}
            Exploration Map Enabled: ${config.explorationMapEnabled.get()}
            Locate Command Enabled: ${config.locateCommandEnabled.get()}
            Villager Trade Enabled: ${config.villagerTradeEnabled.get()}
      """.trimIndent(),
    )
  }
}
