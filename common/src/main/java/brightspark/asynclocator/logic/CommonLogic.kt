package brightspark.asynclocator.logic

import brightspark.asynclocator.AsyncLocatorMod.MOD_ID
import brightspark.asynclocator.mixins.MapItemAccess
import brightspark.asynclocator.extensions.CustomDataExtensions.hasAsyncLocatorData
import brightspark.asynclocator.extensions.CustomDataExtensions.removeAsyncLocatorData
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.MapItem
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import java.util.UUID
import java.util.function.Consumer

object CommonLogic {
  const val MAP_HOVER_NAME_KEY: String = "menu.working"
  const val KEY_LOCATING: String = "$MOD_ID.locating"
  const val KEY_LOCATING_MANAGED: String = "$KEY_LOCATING.managed"

  /**
   * Creates an empty "Filled Map", with a hover tooltip name stating that it's locating a feature.
   *
   * @return The ItemStack
   */
  @JvmStatic
  fun createEmptyMap(): ItemStack {
    val stack =
      ItemStack(Items.FILLED_MAP)
        .apply { set(DataComponents.ITEM_NAME, Component.translatable(MAP_HOVER_NAME_KEY)) }

    val customData =
      CompoundTag().apply {
        putByte(KEY_LOCATING, 1.toByte())
      }

    return stack.apply { set(DataComponents.CUSTOM_DATA, CustomData.of(customData)) }
  }

  @JvmStatic
  fun createEmptyManagedMap(): ItemStack {
    val stack =
      ItemStack(Items.FILLED_MAP)
        .apply { set(DataComponents.ITEM_NAME, Component.translatable(MAP_HOVER_NAME_KEY)) }

    val customData =
      CompoundTag()
        .apply { putUUID(KEY_LOCATING_MANAGED, UUID.randomUUID()) }

    return stack.apply { set(DataComponents.CUSTOM_DATA, CustomData.of(customData)) }
  }

  /**
   * Returns true if the stack is an empty FILLED_MAP item with the hover tooltip name stating that it's locating a
   * feature.
   *
   * @param stack The stack to check.
   * @return True if the stack is an empty FILLED_MAP awaiting to be populated with location data.
   */
  @JvmStatic
  fun isEmptyPendingMap(stack: ItemStack): Boolean {
    if (!stack.`is`(Items.FILLED_MAP)) {
      return false
    }

    val customData = stack.get(DataComponents.CUSTOM_DATA)
    return customData != null && customData.hasAsyncLocatorData()
  }

  /**
   * Updates the map stack with all the given data.
   *
   * @param mapStack        The map ItemStack to update
   * @param level           The ServerLevel
   * @param pos             The feature position
   * @param scale           The map scale
   * @param destinationType The map feature type
   * @param displayName     The hover tooltip display name of the ItemStack
   */
  @JvmStatic
  fun updateMap(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos,
    scale: Int,
    destinationType: Holder<MapDecorationType>,
    displayName: String?,
  ) {
    updateMap(mapStack, level, pos, scale, destinationType, Component.translatable(displayName))
  }

  /**
   * Updates the map stack with all the given data.
   *
   * @param mapStack        The map ItemStack to update
   * @param level           The ServerLevel
   * @param pos             The feature position
   * @param scale           The map scale
   * @param destinationType The map feature type
   */
  @JvmOverloads
  @JvmStatic
  fun updateMap(
    mapStack: ItemStack,
    level: ServerLevel,
    pos: BlockPos,
    scale: Int,
    destinationType: Holder<MapDecorationType>,
    displayName: Component? = null,
  ) {
    val mapId = MapItemAccess.callCreateNewSavedData(level, pos.x, pos.z, scale, true, true, level.dimension())
    mapStack.set(DataComponents.MAP_ID, mapId)
    MapItem.renderBiomePreviewMap(level, mapStack)
    MapItemSavedData.addTargetDecoration(mapStack, pos, "+", destinationType)
    if (displayName != null) mapStack.set(DataComponents.ITEM_NAME, displayName)

    val currentData = mapStack.get(DataComponents.CUSTOM_DATA)
    if (currentData != null) {
      val newTag = currentData.copyTag()!!
      newTag.removeAsyncLocatorData()

      if (newTag.isEmpty) {
        mapStack.remove(DataComponents.CUSTOM_DATA)
      } else {
        mapStack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag))
      }
    }
  }

  /**
   * Broadcasts slot changes to all players that have the chest container open.
   * Won't do anything if the BlockEntity isn't an instance of [ChestBlockEntity].
   */
  @JvmStatic
  fun broadcastChestChanges(
    level: ServerLevel,
    be: BlockEntity,
  ) {
    if (be !is ChestBlockEntity) return

    level.players().forEach(
      Consumer { player: ServerPlayer ->
        val container = player.containerMenu
        if (container is ChestMenu && container.container === be) {
          container.broadcastChanges()
        }
      },
    )
  }
}
