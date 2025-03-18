package dev.mnyacat.stellar_sync_fabric.mixin;

import dev.mnyacat.stellar_sync_fabric.model.FabricGlobalContext;
import dev.mnyacat.stellar_sync_fabric.model.FabricStorageContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Unique
    private boolean flag = true;

    @Inject(method = "stop", at = @At("HEAD"))
    private void onServerStop(CallbackInfo info) {
        // サーバーストップ時の保存処理
        if (flag) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            Collection<ServerPlayerEntity> collection = server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity player : collection) {
                FabricStorageContext.INSTANCE.getStorageWrapper().savePlayerData(player, false, false, false, false);
            }
        }
        flag = false;
    }
}
