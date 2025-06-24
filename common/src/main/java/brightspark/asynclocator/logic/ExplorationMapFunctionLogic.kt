package brightspark.asynclocator.logic

import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.AsyncLocatorMod.CONFIG
import brightspark.asynclocator.MapManager
import brightspark.asynclocator.MapManager.LocateOperation
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING_MANAGED
import brightspark.asynclocator.logic.CommonLogic.createEmptyManagedMap
import brightspark.asynclocator.platform.Services
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.time.Duration
import java.util.UUID

object ExplorationMapFunctionLogic {
  // I'd like to think that structure locating shouldn't take *this* long
  val MAP_NAME_CACHE: Cache<ItemStack, Component> =
    CacheBuilder
      .newBuilder()
      .expireAfterWrite(Duration.ofMinutes(CONFIG.mapNameCacheExpiryMinutes.get().toLong()))
      .build()

  @JvmStatic
  fun cacheName(stack: ItemStack, name: Component) = MAP_NAME_CACHE.put(stack, name)

  fun getCachedName(stack: ItemStack, invalidate: Boolean = true): Component {
    val name = MAP_NAME_CACHE.getIfPresent(stack) ?: Component.translatable("item.minecraft.map")
    if(invalidate) { MAP_NAME_CACHE.invalidate(stack) }
    return name
  }

  fun handleLocationFound(asyncId: UUID, pos: BlockPos?) = MapManager.INSTANCE.completeLocateOperation(asyncId, pos)

  fun handleLocationFound(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos?,
    scale: Int,
    destinationType: Holder<MapDecorationType>,
    invPos: BlockPos,
    asyncId: UUID,
  ) {
    if (pos == null) {
      LOG.info("No location found - invalidating map stack")
      Services.EXPLORATION_MAP_FUNCTION_LOGIC.invalidateMap(mapStack, level, invPos, asyncId)
    } else {
      LOG.info("Location found - updating treasure map in chest")
      // complete the operation in MapManager to close the loop
      MapManager.INSTANCE.completeLocateOperation(asyncId, pos)
      Services.EXPLORATION_MAP_FUNCTION_LOGIC.updateMap(
        mapStack,
        level,
        pos,
        scale,
        destinationType,
        invPos,
        getCachedName(mapStack),
        asyncId,
      )
    }
  }

  fun updateMapAsync(
    initialMapItemStack: ItemStack,
    level: ServerLevel,
    blockPos: BlockPos,
    scale: Int,
    searchRadius: Int,
    skipKnownStructures: Boolean,
    destinationType: Holder<MapDecorationType>,
    destination: TagKey<Structure>,
  ): ItemStack {
    val mapStack = createEmptyManagedMap()

    //val originalName = initialMapItemStack.hoverName
    //mapStack.set(DataComponents.ITEM_NAME, originalName)

    val asyncId = mapStack
      .get(DataComponents.CUSTOM_DATA)!!
      .copyTag()
      .getUUID(KEY_LOCATING_MANAGED)

    // If the map is from a chest, we need to handle it differently
    val isFromChest = (level.getBlockEntity(blockPos) is ChestBlockEntity)

    // Even if the map was generated as part of a chest's loot, we still need to add the locate operation
    // in case the chest is broken and the map is picked up later.
    MapManager.INSTANCE.addLocateOperation(
      asyncId,
      LocateOperation(level.dimension(), scale, destinationType),
    )

    locateStructure(level, destination, blockPos, searchRadius, skipKnownStructures)
      .thenOnServerThread { pos: BlockPos? ->
        when {
          isFromChest -> handleLocationFound(mapStack, level, pos, scale, destinationType, blockPos, asyncId)
          else -> handleLocationFound(asyncId, pos)
        }
      }
    return mapStack
  }
}
