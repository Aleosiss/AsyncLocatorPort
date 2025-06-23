package brightspark.asynclocator.platform

import brightspark.asynclocator.ALConstants
import java.util.ServiceLoader
import java.util.function.Supplier

object Services {
  val PLATFORM: PlatformHelper = load<PlatformHelper>(PlatformHelper::class.java)
  val CONFIG: ConfigHelper = load<ConfigHelper>(ConfigHelper::class.java)
  val EXPLORATION_MAP_FUNCTION_LOGIC: ExplorationMapFunctionLogicHelper = load<ExplorationMapFunctionLogicHelper>(
    ExplorationMapFunctionLogicHelper::class.java
  )

  private fun <T> load(clazz: Class<T>): T {
    val service = ServiceLoader.load(clazz)
      .findFirst()
      .orElseThrow(Supplier { NullPointerException("Failed to load service for " + clazz.name) })
    ALConstants.logDebug("Loaded {} for service {}", service, clazz)
    return service
  }
}
