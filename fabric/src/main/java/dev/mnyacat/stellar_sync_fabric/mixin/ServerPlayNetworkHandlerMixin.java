package dev.mnyacat.stellar_sync_fabric.mixin;

import dev.mnyacat.stellar_sync_fabric.model.FabricHolder;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    public abstract ServerPlayerEntity getPlayer();

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void onDisconnected(DisconnectionInfo info, CallbackInfo ci) {
        // 切断時の保存処理
        if (this.getPlayer() != null) {
            FabricHolder.INSTANCE.getStorageWrapper().savePlayerData(player, false, false, false, false);
        }
    }
}
