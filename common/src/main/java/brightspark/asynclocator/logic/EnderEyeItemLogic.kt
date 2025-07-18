package brightspark.asynclocator.logic

import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.mixins.EyeOfEnderAccess
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.tags.StructureTags
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.EyeOfEnder
import net.minecraft.world.item.EnderEyeItem
import net.minecraft.world.item.Item

object EnderEyeItemLogic {
  @JvmStatic
  fun locateAsync(
    level: ServerLevel,
    player: Player,
    eyeOfEnder: EyeOfEnder,
    enderEyeItem: EnderEyeItem?,
  ) {
    locateStructure(
      level,
      StructureTags.EYE_OF_ENDER_LOCATED,
      player.blockPosition(),
      100,
      false,
    ).thenOnServerThread { pos: BlockPos? ->
      (eyeOfEnder as EyeOfEnderData).setLocateTaskOngoing(false)
      if (pos != null) {
        LOG.info("Location found - updating eye of ender entity")
        eyeOfEnder.signalTo(pos)
        CriteriaTriggers.USED_ENDER_EYE.trigger(player as ServerPlayer, pos)
        player.awardStat(Stats.ITEM_USED[enderEyeItem as Item])
      } else {
        LOG.info("No location found - killing eye of ender entity")
        // Set the entity's life to long enough that it dies
        (eyeOfEnder as EyeOfEnderAccess).setLife(Int.MAX_VALUE - 100)
      }
    }
    (eyeOfEnder as EyeOfEnderData).setLocateTaskOngoing(true)
  }
}
