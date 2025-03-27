package dev.mnyacat.stellar_sync_fabric.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.mnyacat.stellar_sync_fabric.model.FabricGlobalContext
import dev.mnyacat.stellar_sync_fabric.model.FabricStorageContext
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.RegistrationEnvironment
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.apache.logging.log4j.Logger

// ref: https://github.com/FabricMC/fabric-docs/blob/main/reference/1.21/src/main/java/com/example/docs/command/FabricDocsReferenceCommands.java
class StellarSyncDebugCommands(private val logger: Logger) : ModInitializer {
    private fun executeSaveCommand(context: CommandContext<ServerCommandSource>): Int {
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.savePlayerData(player, isOnline = false)
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeLoadCommand(context: CommandContext<ServerCommandSource>): Int {
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.loadPlayerData(player)
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateFlagCommand(context: CommandContext<ServerCommandSource>): Int {
        val isOnline = BoolArgumentType.getBool(context, "is_online")
        val hasCrashed = BoolArgumentType.getBool(context, "has_crashed")
        val needsRollback = BoolArgumentType.getBool(context, "needs_rollback")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateFlags(player, isOnline, hasCrashed, needsRollback)
            context.source.sendFeedback(
                { Text.literal("update: is_online = $isOnline, has_crashed = $hasCrashed") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateOnlineFlagCommand(context: CommandContext<ServerCommandSource>): Int {
        val isOnline = BoolArgumentType.getBool(context, "is_online")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateOnlineFlag(player, isOnline)
            context.source.sendFeedback(
                { Text.literal("update: is_online = $isOnline") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateLastServerCommand(context: CommandContext<ServerCommandSource>): Int {
        val lastServer = StringArgumentType.getString(context, "last_server")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateLastServer(player, lastServer)
            context.source.sendFeedback(
                { Text.literal("update: last_server = $lastServer") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateOnlineStatusCommand(context: CommandContext<ServerCommandSource>): Int {
        val isOnline = BoolArgumentType.getBool(context, "is_online")
        val lastServer = StringArgumentType.getString(context, "last_server")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateOnlineStatus(player, isOnline, lastServer)
            context.source.sendFeedback(
                { Text.literal("update: is_online = $isOnline, last_server = $lastServer") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateCrashFlagCommand(context: CommandContext<ServerCommandSource>): Int {
        val hasCrashed = BoolArgumentType.getBool(context, "has_crashed")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateCrashFlag(player, hasCrashed)
            context.source.sendFeedback(
                { Text.literal("update: has_crashed = $hasCrashed") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateRollbackFlagCommand(context: CommandContext<ServerCommandSource>): Int {
        val needsRollback = BoolArgumentType.getBool(context, "needs_rollback")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateRollbackFlag(player, needsRollback)
            context.source.sendFeedback(
                { Text.literal("update: needs_rollback = $needsRollback") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateRollbackServerCommand(context: CommandContext<ServerCommandSource>): Int {
        val rollbackServer = StringArgumentType.getString(context, "rollback_server")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateRollbackServer(player, rollbackServer)
            context.source.sendFeedback(
                { Text.literal("update: rollback_server = $rollbackServer") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    private fun executeUpdateRollbackStatusCommand(context: CommandContext<ServerCommandSource>): Int {
        val needsRollback = BoolArgumentType.getBool(context, "needs_rollback")
        val rollbackServer = StringArgumentType.getString(context, "rollback_server")
        try {
            val player = context.source.playerOrThrow
            FabricStorageContext.storageWrapper.updateRollbackStatus(player, needsRollback, rollbackServer)
            context.source.sendFeedback(
                { Text.literal("update: needs_rollback = $needsRollback, rollback_server = $rollbackServer") },
                false
            )
            return 1
        } catch (e: CommandSyntaxException) {
            context.source.sendFeedback(
                { Text.literal("This command can only be executed by players") },
                false
            )
            return 0
        }
    }

    override fun onInitialize() {
        CommandRegistrationCallback.EVENT.register(CommandRegistrationCallback { dispatcher: CommandDispatcher<ServerCommandSource?>, _: CommandRegistryAccess?, environment: RegistrationEnvironment ->
            dispatcher.register(
                CommandManager.literal("ss-debug")
                    .requires({ source -> source.hasPermissionLevel(1) })
                    .then(
                        CommandManager.literal("save").executes { context: CommandContext<ServerCommandSource> ->
                            executeSaveCommand(context)
                        })
                    .then(CommandManager.literal("load").executes { context: CommandContext<ServerCommandSource> ->
                        executeLoadCommand(context)
                    })
                    .then(
                        CommandManager.literal("update-flags")
                            .then(
                                CommandManager.argument("is_online", BoolArgumentType.bool())
                                    .then(
                                        CommandManager.argument("has_crashed", BoolArgumentType.bool())
                                            .then(CommandManager.argument("needs_rollback", BoolArgumentType.bool())
                                                .executes { context: CommandContext<ServerCommandSource> ->
                                                    executeUpdateFlagCommand(context)
                                                })
                                    )
                            )
                    )
                    .then(
                        CommandManager.literal("update-online-flag")
                            .then(
                                CommandManager.argument("is_online", BoolArgumentType.bool())
                                    .executes({ context: CommandContext<ServerCommandSource> ->
                                        executeUpdateOnlineFlagCommand(context)
                                    })
                            )
                    )
                    .then(
                        CommandManager.literal("update-last-server")
                            .then(CommandManager.argument("last_server", StringArgumentType.string())
                                .executes { context: CommandContext<ServerCommandSource> ->
                                    executeUpdateLastServerCommand(context)
                                })
                    )
                    .then(
                        CommandManager.literal("update-online-status")
                            .then(
                                CommandManager.argument("is_online", BoolArgumentType.bool())
                                    .then(
                                        CommandManager.argument("last_server", StringArgumentType.string())
                                            .executes({ context ->
                                                executeUpdateOnlineStatusCommand(context)
                                            })
                                    )
                            )
                    )
                    .then(
                        CommandManager.literal("update-crash-flag")
                            .then(
                                CommandManager.argument("has_crashed", BoolArgumentType.bool())
                                    .executes({ context: CommandContext<ServerCommandSource> ->
                                        executeUpdateCrashFlagCommand(context)
                                    })
                            )
                    )
                    .then(
                        CommandManager.literal("update-rollback-flag")
                            .then(
                                CommandManager.argument("needs_rollback", BoolArgumentType.bool())
                                    .executes({ context: CommandContext<ServerCommandSource> ->
                                        executeUpdateRollbackFlagCommand(context)
                                    })
                            )
                    )
                    .then(
                        CommandManager.literal("update-rollback-server")
                            .then(
                                CommandManager.argument("rollback_server", StringArgumentType.string())
                                    .executes({ context: CommandContext<ServerCommandSource> ->
                                        executeUpdateRollbackServerCommand(context)
                                    })
                            )
                    )
                    .then(
                        CommandManager.literal("update-rollback-status")
                            .then(
                                CommandManager.argument("needs_rollback", BoolArgumentType.bool())
                                    .then(
                                        CommandManager.argument("rollback_server", StringArgumentType.string())
                                            .executes({ context ->
                                                executeUpdateRollbackStatusCommand(context)
                                            })
                                    )
                            )
                    )
            )
        })
        logger.info("registered debug commands!")
    }
}