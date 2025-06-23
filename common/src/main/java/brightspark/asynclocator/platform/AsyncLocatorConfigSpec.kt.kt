package brightspark.asynclocator.platform

import net.neoforged.neoforge.common.ModConfigSpec
import org.apache.commons.lang3.tuple.Pair as ApachePair

object AsyncLocatorConfigSpec {

  class AsyncLocatorConfig(builder: ModConfigSpec.Builder) {
    /**
     * The maximum number of threads in the async locator thread pool.
     */
    val locatorThreads: ModConfigSpec.ConfigValue<Int> = builder
      .comment("The maximum number of threads in the async locator thread pool.")
      .defineInRange("locatorThreads", 4, 1, 32)

    /**
     * When a merchant's treasure map offer ends up not finding a feature location, whether the offer should be removed
     * or marked as out of stock.
     */
    val removeOffer: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment(
        "When a merchant's treasure map offer ends up not finding a feature location, whether the offer should " +
          "be removed or marked as out of stock."
      )
      .define("removeOffer", false)

    /**
     * If true, enables asynchronous locating of structures for dolphin treasures.
     */
    val dolphinTreasureEnabled: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment("If true, enables asynchronous locating of structures for dolphin treasures.")
      .define("dolphinTreasureEnabled", true)

    /**
     * If true, enables asynchronous locating of structures when Eyes Of Ender are thrown.
     */
    val eyeOfEnderEnabled: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment("If true, enables asynchronous locating of structures when Eyes Of Ender are thrown.")
      .define("eyeOfEnderEnabled", true)

    /**
     * If true, enables asynchronous locating of structures for exploration maps found in chests.
     */
    val explorationMapEnabled: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment("If true, enables asynchronous locating of structures for exploration maps found in chests.")
      .define("explorationMapEnabled", true)

    /**
     * If true, enables asynchronous locating of structures for the locate command.
     */
    val locateCommandEnabled: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment("If true, enables asynchronous locating of structures for the locate command.")
      .define("locateCommandEnabled", true)

    /**
     * If true, enables asynchronous locating of structures for villager trades.
     */
    val villagerTradeEnabled: ModConfigSpec.ConfigValue<Boolean> = builder
      .comment("If true, enables asynchronous locating of structures for villager trades.")
      .define("villagerTradeEnabled", true)

    /**
     * The number of minutes before the map name cache expires.
     * The cache is used to look up the name of a map once it's been located.
     */
    val mapNameCacheExpiryMinutes: ModConfigSpec.ConfigValue<Int> = builder
      .comment(
        "The number of minutes before the map name cache expires. " +
          "The cache is used to look up the name of a map once it's been located."
      )
      .defineInRange("mapNameCacheExpiryMinutes", 5, 1, 1440)

    /**
     * The radius in chunks in which to search for biomes when locating them.
     */
    val biomeSearchRadius: ModConfigSpec.ConfigValue<Int> = builder
      .comment("The radius in chunks in which to search for biomes when locating them.")
      .defineInRange("biomeSearchRadius", 400, 1, Int.MAX_VALUE)

    /**
     * The radius in chunks in which to search for structures when locating them.
     */
    val structureSearchRadius: ModConfigSpec.ConfigValue<Int> = builder
      .comment("The radius in chunks in which to search for structures when locating them.")
      .defineInRange("structureSearchRadius", 400, 1, Int.MAX_VALUE)
  }

  private val pair: ApachePair<AsyncLocatorConfig, ModConfigSpec> = ModConfigSpec.Builder()
    .configure(AsyncLocatorConfigSpec::AsyncLocatorConfig)
  val CONFIG: AsyncLocatorConfig = pair.left
  val SPEC: ModConfigSpec = pair.right
}
