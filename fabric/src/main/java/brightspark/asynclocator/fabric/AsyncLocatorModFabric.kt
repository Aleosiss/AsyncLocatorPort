package brightspark.asynclocator.fabric

import brightspark.asynclocator.AsyncLocator.setupExecutorService
import brightspark.asynclocator.AsyncLocator.shutdownExecutorService
import brightspark.asynclocator.AsyncLocatorMod
import brightspark.asynclocator.AsyncLocatorMod.MOD_ID
import brightspark.asynclocator.platform.AsyncLocatorConfigSpec
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopping
import net.neoforged.fml.config.ModConfig

class AsyncLocatorModFabric : ModInitializer {
  override fun onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    // Run our common setup.
    AsyncLocatorMod.init()
    // Register the config spec for Fabric via NeoForge.
    NeoForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.SERVER, AsyncLocatorConfigSpec.SPEC)

    ServerLifecycleEvents.SERVER_STARTING.register(ServerStarting { setupExecutorService() })
    ServerLifecycleEvents.SERVER_STOPPING.register(ServerStopping { shutdownExecutorService() })
  }
}
