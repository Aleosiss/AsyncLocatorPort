package brightspark.asynclocator.mixins;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.MapManager;
import brightspark.asynclocator.logic.MerchantLogic;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING_MANAGED;
import static brightspark.asynclocator.logic.CommonLogic.MAP_HOVER_NAME_KEY;

@Mixin(MapItem.class)
public class MapItemMixin {
    @Inject(
            method = "inventoryTick",
            at = @At("HEAD")
    )
    private void inventoryTick(ItemStack is, Level level, Entity entity, int $$3, boolean $$4, CallbackInfo ci) {
        if(!level.isClientSide()) {
            // some basic filtering
            if (!(is.getItem() instanceof MapItem)) return;
            var customData = is.get(DataComponents.CUSTOM_DATA);
            if (customData == null) return;
            if (!customData.contains(KEY_LOCATING_MANAGED)) return;

            // get our shit together
            CompoundTag newCustomData = customData.copyTag();
            var asyncId = newCustomData.getUUID(KEY_LOCATING_MANAGED);
            var mapManager = MapManager.getInstance();

            // get our locate
            var locate = mapManager.getLocateOperation(asyncId);
            // if we can't find it, clean up this map
            if (locate == null) {
                ALConstants.logInfo("MapItemMixin#inventoryTick: No locate operation found for asyncId: " + asyncId);
                newCustomData.remove(KEY_LOCATING_MANAGED);
                is.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.map"));
                is.set(DataComponents.CUSTOM_DATA, CustomData.of(newCustomData));
                return;
            }

            // one-time initialization to set the name of the map
            if (!locate.initialized) {
                ALConstants.logInfo("MapItemMixin#inventoryTick: Locate operation not initialized for asyncId: " + asyncId + ", initializing now.");
                mapManager.initializeLocateOperation(asyncId, getName(is));
                is.set(DataComponents.ITEM_NAME, Component.translatable(MAP_HOVER_NAME_KEY));
            }

            // okay now we wait
            if (!locate.completed) {
                return;
            }

            // if we got here, the locate operation is completed
            var success = locate.pos != null;
            if (success) {
                // lfg
                ALConstants.logInfo("MapItemMixin#inventoryTick: Found locate operation for asyncId: " + asyncId);
                CommonLogic.updateMap(is, getServerLevel(level, locate.levelKey), locate.pos, locate.scale, locate.destinationType, locate.displayName);
            } else {
                // if we don't have a position, we invalidate the map
                ALConstants.logInfo("MapItemMixin#inventoryTick: Locate operation completed but no position found for asyncId: " + asyncId);
                newCustomData.remove(KEY_LOCATING_MANAGED);
                is.set(DataComponents.CUSTOM_DATA, CustomData.of(newCustomData));
                if (entity instanceof Villager merchant) { MerchantLogic.invalidateMap(merchant, is); }
            }
            mapManager.removeLocateOperation(asyncId);

            // update my UI please
            if (entity instanceof Villager merchant) {
                if (merchant.getTradingPlayer() instanceof ServerPlayer tradingPlayer) {
                    pushOfferUpdate(merchant, tradingPlayer);
                }
            }
        }
    }

    @Unique
    private static void pushOfferUpdate(Villager merchant, ServerPlayer tradingPlayer) {
        ALConstants.logInfo("Player {} currently trading - updating merchant offers", tradingPlayer);

        tradingPlayer.sendMerchantOffers(
                tradingPlayer.containerMenu.containerId,
                merchant.getOffers(),
                merchant instanceof Villager villager ? villager.getVillagerData().getLevel() : 1,
                merchant.getVillagerXp(),
                merchant.showProgressBar(),
                merchant.canRestock()
        );
    }

    @Unique
    private String getName(ItemStack is) {
        return is.getHoverName().getString();
    }

    @Unique
    private ServerLevel getServerLevel(Level itemLevel, ResourceKey<Level> levelResourceKey) {
        return itemLevel.getServer().getLevel(levelResourceKey);
    }
}
