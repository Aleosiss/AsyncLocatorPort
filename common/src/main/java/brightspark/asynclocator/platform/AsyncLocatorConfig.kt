package brightspark.asynclocator.platform

interface Config {
  /**
   * The maximum number of threads in the async locator thread pool.
   */
  fun locatorThreads(): Int

  /**
   * When a merchant's treasure map offer ends up not finding a feature location, whether the offer should be removed
   * or marked as out of stock.
   */
  fun removeOffer(): Boolean

  /**
   * If true, enables asynchronous locating of structures for dolphin treasures.
   */
  fun dolphinTreasureEnabled(): Boolean

  /**
   * If true, enables asynchronous locating of structures when Eyes Of Ender are thrown.
   */
  fun eyeOfEnderEnabled(): Boolean

  /**
   * If true, enables asynchronous locating of structures for exploration maps found in chests.
   */
  fun explorationMapEnabled(): Boolean

  /**
   * If true, enables asynchronous locating of structures for the locate command.
   */
  fun locateCommandEnabled(): Boolean

  /**
   * If true, enables asynchronous locating of structures for villager trades.
   */
  fun villagerTradeEnabled(): Boolean

  /**
   * The number of minutes before the map name cache expires.
   * The cache is used to look up the name of a map once it's been located.
   */
  fun mapNameCacheExpiryMinutes(): Int

  /**
   * The radius in chunks in which to search for biomes when locating them.
   */
  fun biomeSearchRadius(): Int

  /**
   * The radius in chunks in which to search for structures when locating them.
   */
  fun structureSearchRadius(): Int
}
