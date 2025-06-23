package brightspark.asynclocator.neoforge

import brightspark.asynclocator.AsyncLocator.setupExecutorService
import brightspark.asynclocator.AsyncLocator.shutdownExecutorService
import brightspark.asynclocator.AsyncLocatorMod
import brightspark.asynclocator.platform.AsyncLocatorConfigSpec
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent

@Mod(AsyncLocatorMod.MOD_ID)
class AsyncLocatorModNeoForge(container: ModContainer, ctx: ModLoadingContext) {
  init {
    // Run our common setup.
    AsyncLocatorMod.init()
    // Register the config spec for NeoForge.
    container.registerConfig(ModConfig.Type.SERVER, AsyncLocatorConfigSpec.SPEC)

    // Tells Neoforge that this mod is only required server side
    // ctx.registerExtensionPoint(IExtensionPoint)

    val forgeEventBus: IEventBus = NeoForge.EVENT_BUS
    forgeEventBus.addListener { _: ServerAboutToStartEvent -> setupExecutorService() }
    forgeEventBus.addListener { _: ServerStoppingEvent -> shutdownExecutorService() }
  }
}
