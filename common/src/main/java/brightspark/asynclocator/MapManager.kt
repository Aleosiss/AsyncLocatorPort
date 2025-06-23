package brightspark.asynclocator

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.UUID

/**
 * Static manager to facilitate map-pull operations, or "locate operations".
 *
 * When a locate task is created, it generates a UUID and stores it in the map and manager.
 *
 * In [brightspark.asynclocator.mixins.MapItemMixin], the UUID is used to retrieve the locate operation. It then stores
 * its own name in the LocateOperation, then sets it's name to the in-progress component.
 *
 * When the locate task completes, it adds the block position to the LocateOperation,
 * and marks it as completed.
 */
class MapManager {
  /**
   * Represents a locate operation in the map manager.
   */
  class LocateOperation(
    var levelKey: ResourceKey<Level>,
    var scale: Int,
    var destinationType: Holder<MapDecorationType>
  ) {
    var initialized: Boolean = false
    var displayName: String? = null

    var completed: Boolean = false
    var pos: BlockPos? = null
  }

  fun getLocateOperation(uuid: UUID): LocateOperation {
    val operation = LOCATE_OPERATIONS[uuid]
    checkNotNull(operation) { "No locate operation found for UUID: $uuid" }

    return operation
  }

  fun removeLocateOperation(uuid: UUID) {
    LOCATE_OPERATIONS.remove(uuid)
  }

  fun initializeLocateOperation(uuid: UUID, displayName: String?) {
    val operation = LOCATE_OPERATIONS[uuid]
    checkNotNull(operation) { "No locate operation found for UUID: $uuid" }

    operation.initialized = true
    operation.displayName = displayName

    LOCATE_OPERATIONS[uuid] = operation
  }

  fun addLocateOperation(uuid: UUID, operation: LocateOperation) {
    check(!LOCATE_OPERATIONS.containsKey(uuid)) { "Locate operation already exists for UUID: $uuid" }
    LOCATE_OPERATIONS[uuid] = operation
  }

  fun completeLocateOperation(asyncId: UUID, pos: BlockPos?) {
    val operation = LOCATE_OPERATIONS[asyncId]
    checkNotNull(operation) { "No locate operation found for UUID: $asyncId" }

    operation.completed = true
    operation.pos = pos

    LOCATE_OPERATIONS[asyncId] = operation
    ALConstants.logInfo("Locate operation completed for UUID: {}, position: {}", asyncId, pos)
  }

  companion object {
    private val LOCATE_OPERATIONS = HashMap<UUID, LocateOperation>()
    val instance: MapManager = MapManager()

    private fun getName(`is`: ItemStack): String {
      return `is`.hoverName.string
    }

    private fun getServerLevel(itemLevel: Level, levelResourceKey: ResourceKey<Level>): ServerLevel? {
      return itemLevel.server!!.getLevel(levelResourceKey)
    }
  }
}