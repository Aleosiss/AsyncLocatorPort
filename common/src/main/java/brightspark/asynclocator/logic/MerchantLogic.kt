package brightspark.asynclocator.logic

import brightspark.asynclocator.AsyncLocator.locateStructure
import brightspark.asynclocator.AsyncLocatorMod.CONFIG
import brightspark.asynclocator.extensions.LOG
import brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING
import brightspark.asynclocator.logic.CommonLogic.updateMap
import brightspark.asynclocator.mixins.MerchantOfferAccess
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import java.util.Optional

object MerchantLogic {

  @JvmStatic
  fun invalidateMap(merchant: AbstractVillager, mapStack: ItemStack) {
    mapStack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.map"))
    merchant.offers
      .firstOrNull { offer: MerchantOffer -> offer.result == mapStack }
      ?.let { removeOffer(merchant, it) }
      ?: run { LOG.warn("No merchant map offer found for stack {}", mapStack) }
  }

  private fun removeOffer(merchant: AbstractVillager, offer: MerchantOffer) {
    if (CONFIG.removeOffer.get()) {
      if (merchant.offers.remove(offer)) {
        LOG.info("Removed merchant map offer")
      } else {
        LOG.warn("Failed to remove merchant map offer")
      }
    } else {
      (offer as MerchantOfferAccess).setMaxUses(0)
      offer.setToOutOfStock()
      LOG.info("Marked merchant map offer as out of stock")
    }
  }

  @JvmStatic
  fun tickMerchantOffers(level: ServerLevel, merchant: AbstractVillager) = merchant.offers
    .map { it.result }
    .filter { it.`is`(Items.FILLED_MAP) }
    .filter { it.get(DataComponents.CUSTOM_DATA)?.contains(KEY_LOCATING) == true }
    .forEach { it.inventoryTick(level, merchant, -1, false) }

  private fun handleLocationFound(
    level: ServerLevel,
    merchant: AbstractVillager,
    mapStack: ItemStack,
    displayName: String?,
    destinationType: Holder<MapDecorationType>,
    pos: BlockPos?
  ) {
    if (pos == null) {
      LOG.info("No location found - invalidating merchant offer")

      invalidateMap(merchant, mapStack)
    } else {
      LOG.info("Location found - updating treasure map in merchant offer")
      updateMap(mapStack, level, pos, 2, destinationType, displayName)
    }

    val tradingPlayer = merchant.tradingPlayer
    if (tradingPlayer is ServerPlayer) {
      LOG.info("Player {} currently trading - updating merchant offers", tradingPlayer)

      tradingPlayer.sendMerchantOffers(
        tradingPlayer.containerMenu.containerId,
        merchant.offers,
        if (merchant is Villager) merchant.villagerData.level else 1,
        merchant.villagerXp,
        merchant.showProgressBar(),
        merchant.canRestock()
      )
    }
  }

  fun updateMapAsync(
    pTrader: Entity,
    emeraldCost: Int,
    displayName: String?,
    destinationType: Holder<MapDecorationType>,
    maxUses: Int,
    villagerXp: Int,
    destination: TagKey<Structure>
  ): MerchantOffer? {
    return updateMapAsyncInternal(
      pTrader,
      emeraldCost,
      maxUses,
      villagerXp
    ) { level: ServerLevel, merchant: AbstractVillager, mapStack: ItemStack ->
      locateStructure(level, destination, merchant.blockPosition(), 100, true)
        .thenOnServerThread { handleLocationFound(level, merchant, mapStack, displayName, destinationType, it) }
    }
  }

  fun updateMapAsync(
    pTrader: Entity,
    emeraldCost: Int,
    displayName: String?,
    destinationType: Holder<MapDecorationType>,
    maxUses: Int,
    villagerXp: Int,
    structureSet: HolderSet<Structure>
  ): MerchantOffer? {
    return updateMapAsyncInternal(
      pTrader,
      emeraldCost,
      maxUses,
      villagerXp
    ) { level: ServerLevel, merchant: AbstractVillager, mapStack: ItemStack ->
      locateStructure(level, structureSet, merchant.blockPosition(), 100, true)
        .thenOnServerThread { handleLocationFound(level, merchant, mapStack, displayName, destinationType, it?.first) }
    }
  }

  private fun updateMapAsyncInternal(
    trader: Entity, emeraldCost: Int, maxUses: Int, villagerXp: Int, task: MapUpdateTask
  ): MerchantOffer? {
    if (trader is AbstractVillager) {
      val mapStack = CommonLogic.createEmptyManagedMap()
      task.apply(trader.level() as ServerLevel, trader, mapStack)

      return MerchantOffer(
        ItemCost(Items.EMERALD, emeraldCost),
        Optional.of(ItemCost(Items.COMPASS)),
        mapStack,
        maxUses,
        villagerXp,
        0.2f
      )
    } else {
      LOG.info("Merchant is not of type {} - not running async logic", AbstractVillager::class.java.simpleName)
      return null
    }
  }

  fun interface MapUpdateTask {
    fun apply(level: ServerLevel, merchant: AbstractVillager, mapStack: ItemStack)
  }
}
