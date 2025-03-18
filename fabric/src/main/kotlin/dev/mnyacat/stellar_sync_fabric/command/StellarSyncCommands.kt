package dev.mnyacat.stellar_sync_fabric.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import dev.mnyacat.stellar_sync_fabric.model.FabricStorageContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.apache.logging.log4j.Logger
import java.sql.SQLException

class StellarSyncCommands(private val logger: Logger) : ModInitializer {
    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource?>, _: CommandRegistryAccess?, environment: RegistrationEnvironment ->
            dispatcher.register(
                CommandManager.literal("stellar-sync").requires({ source -> source.hasPermissionLevel(1) })
                    .then(
                        CommandManager.literal("needs-rollback")
                            .then(
                                CommandManager.literal("enable")
                                    .then(
                                        CommandManager.argument("rollback_server", StringArgumentType.string())
                                            .executes({ context ->
                                                val rollbackServer =
                                                    StringArgumentType.getString(context, "rollback_server")
                                                try {
                                                    FabricStorageContext.storageWrapper.updateAllPlayersRollbackStatus(
                                                        true,
                                                        rollbackServer
                                                    )
                                                    context.source.sendFeedback(
                                                        { Text.literal("全てのプレイヤーのロールバックサーバーを\"$rollbackServer\"に設定しました. 設定したサーバーに接続するまで, インベントリの同期を行いません.") },
                                                        false
                                                    )
                                                    1
                                                } catch (e: SQLException) {
                                                    logger.error("/needs-rollback enable: Database operation failed.")
                                                    logger.debug(e.message)
                                                    context.source.sendFeedback(
                                                        {
                                                            Text.literal("データベースの操作に失敗しました: ")
                                                                .formatted(Formatting.RED).append(
                                                                    Text.literal("もう一度コマンドを実行してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.")
                                                                        .formatted(Formatting.WHITE)
                                                                )
                                                        },
                                                        false
                                                    )
                                                    0
                                                }
                                            })
                                            // TODO: PlayersをUuidの可変長引数に変更する
                                            .then(
                                                CommandManager.argument("players", EntityArgumentType.players())
                                                    .executes({ context ->
                                                        val players = EntityArgumentType.getPlayers(context, "players")
                                                            .toTypedArray<ServerPlayerEntity>()
                                                        val rollbackServer =
                                                            StringArgumentType.getString(context, "rollback_server")
                                                        val playerNames =
                                                            players.map { player -> player.name }.joinToString(", ")
                                                        try {
                                                            FabricStorageContext.storageWrapper.updateRollbackStatus(
                                                                players,
                                                                true,
                                                                rollbackServer
                                                            )
                                                            context.source.sendFeedback(
                                                                { Text.literal("${playerNames}のロールバックサーバーを\"$rollbackServer\"に設定しました. 設定したサーバーに接続するまで, インベントリの同期を行いません.") },
                                                                false
                                                            )
                                                            1
                                                        } catch (e: SQLException) {
                                                            logger.error("/needs-rollback enable $playerNames: Database operation failed.")
                                                            logger.debug(e.message)
                                                            context.source.sendFeedback(
                                                                {
                                                                    Text.literal("データベースの操作に失敗しました: ")
                                                                        .formatted(Formatting.RED).append(
                                                                            Text.literal("もう一度コマンドを実行してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.")
                                                                                .formatted(Formatting.WHITE)
                                                                        )
                                                                },
                                                                false
                                                            )
                                                            0
                                                        }
                                                    })
                                            )
                                    )
                            ).then(
                                CommandManager.literal("disable").executes({ context ->
                                    try {
                                        FabricStorageContext.storageWrapper.updateAllPlayersRollbackStatus(false, null)
                                        context.source.sendFeedback(
                                            { Text.literal("全てのプレイヤーのロールバックサーバーの設定を解除しました.") },
                                            false
                                        )
                                        1
                                    } catch (e: SQLException) {
                                        logger.error("/needs-rollback disable: Database operation failed.")
                                        logger.debug(e.message)
                                        context.source.sendFeedback(
                                            {
                                                Text.literal("データベースの操作に失敗しました: ")
                                                    .formatted(Formatting.RED).append(
                                                        Text.literal("もう一度コマンドを実行してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.")
                                                            .formatted(Formatting.WHITE)
                                                    )
                                            },
                                            false
                                        )
                                        0
                                    }
                                })
                                    // TODO: PlayersをUuidの可変長引数に変更する
                                    .then(
                                        CommandManager.argument("players", EntityArgumentType.players())
                                            .executes({ context ->
                                                val players = EntityArgumentType.getPlayers(context, "players")
                                                    .toTypedArray<ServerPlayerEntity>()
                                                val playerNames =
                                                    players.map { player -> player.name }.joinToString(", ")
                                                try {
                                                    FabricStorageContext.storageWrapper.updateRollbackStatus(
                                                        players,
                                                        false,
                                                        null
                                                    )
                                                    context.source.sendFeedback(
                                                        { Text.literal("${playerNames}のロールバックサーバーの設定を解除しました.") },
                                                        false
                                                    )
                                                    1
                                                } catch (e: SQLException) {
                                                    logger.error("/needs-rollback disable $$playerNames: Database operation failed.")
                                                    logger.debug(e.message)
                                                    context.source.sendFeedback(
                                                        {
                                                            Text.literal("データベースの操作に失敗しました: ")
                                                                .formatted(Formatting.RED).append(
                                                                    Text.literal("もう一度コマンドを実行してください. 何度も表示される場合は, サーバー管理者に問い合わせてください.")
                                                                        .formatted(Formatting.WHITE)
                                                                )
                                                        },
                                                        false
                                                    )
                                                    0
                                                }
                                            })
                                    )
                            )
                    )
            )
        })
        logger.info("registered commands!")
    }

}