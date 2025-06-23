package brightspark.asynclocator

import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.platform.AsyncLocatorConfigSpec

object AsyncLocatorMod {
  const val MOD_ID: String = "asynclocator"
  const val MOD_NAME: String = "Async Locator"

  val LOGGER = LOG

  val CONFIG = AsyncLocatorConfigSpec.CONFIG

  fun init() {
    // Write common init code here.
  }
}
