package brightspark.asynclocator.platform

import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.platform.services.ExplorationMapFunctionLogicHelper
import java.util.ServiceLoader
import java.util.function.Supplier

object Services {
  val EXPLORATION_MAP_FUNCTION_LOGIC: ExplorationMapFunctionLogicHelper =
    load(ExplorationMapFunctionLogicHelper::class.java)

  private fun <T> load(clazz: Class<T>): T {
    val service = ServiceLoader.load(clazz)
      .findFirst()
      .orElseThrow(Supplier { NullPointerException("Failed to load service for " + clazz.name) })
    LOG.info("Loaded {} for service {}", service, clazz)
    return service
  }
}
