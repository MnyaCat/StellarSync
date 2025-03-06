package dev.mnyacat.stellar_sync_fabric.mixin;

import dev.mnyacat.stellar_sync_fabric.model.FabricHolder;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerJoin(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
        // 接続時のロード処理
        if (!FabricHolder.INSTANCE.getPluginEnable()) {
            player.sendMessage(Text.literal("StellarSyncが無効化されているため, プレイヤーデータの同期が行われません: サーバー管理者に問い合わせてください.").formatted(Formatting.RED));
            return;
        }
        FabricHolder.INSTANCE.getStorageWrapper().loadPlayerData(player);

    }
}
