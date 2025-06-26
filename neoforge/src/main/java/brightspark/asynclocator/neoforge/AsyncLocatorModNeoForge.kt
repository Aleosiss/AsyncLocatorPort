package brightspark.asynclocator.neoforge

import brightspark.asynclocator.AsyncLocator.setupExecutorService
import brightspark.asynclocator.AsyncLocator.shutdownExecutorService
import brightspark.asynclocator.AsyncLocatorMod
import brightspark.asynclocator.AsyncLocatorMod.MOD_ID
import brightspark.asynclocator.AsyncLocatorModCommon.printConfigs
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.platform.AsyncLocatorConfigSpec
import net.minecraft.client.Minecraft
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(MOD_ID)
class AsyncLocatorModNeoForge(modEventBus: IEventBus, modContainer: ModContainer) {
  companion object {
    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    object ClientModEvents {
      @SubscribeEvent
      fun onClientSetup(event: FMLClientSetupEvent?) {
        // Some client setup code
        LOG.info("HELLO FROM CLIENT SETUP")
        LOG.info("MINECRAFT NAME >> {}", Minecraft.getInstance().user.name)
      }
    }
  }

  init {
    // Run our common setup.
    AsyncLocatorMod.init()
    // Register the config spec for NeoForge.
    modContainer.registerConfig(ModConfig.Type.SERVER, AsyncLocatorConfigSpec.SPEC)

    // Tells Neoforge that this mod is only required server side
    // ctx.registerExtensionPoint(IExtensionPoint)

    NeoForge.EVENT_BUS.register(this)
    modEventBus.addListener(::commonSetup)
    modEventBus.addListener { _: ServerAboutToStartEvent -> setupExecutorService() }
    modEventBus.addListener { _: ServerStoppingEvent -> shutdownExecutorService() }
    modEventBus.addListener { _: ServerAboutToStartEvent -> printConfigs() }
  }

  private fun commonSetup(event: FMLCommonSetupEvent) {
    // Some common setup code
    LOG.info("HELLO FROM COMMON SETUP")
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  fun onServerStarting(event: ServerStartingEvent) {
    // Do something when the server starts
    LOG.info("HELLO from server starting")
  }
}
