package dev.mnyacat.stellar_sync_common.config

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.set
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

class ConfigManager(configPath: Path) {
    private val loader: YamlConfigurationLoader =
        YamlConfigurationLoader.builder().path(configPath).defaultOptions { options ->
            options.serializers { builder ->
                builder.registerAnnotatedObjects(objectMapperFactory())
            }
        }.indent(2).nodeStyle(NodeStyle.BLOCK).build()
    private var configNode: ConfigurationNode = loader.load()
    var config: Config
    private val mapper = ObjectMapper.factory().get(Config::class.java)

    init {
        config = mapper.load(configNode)
        if (!configNode.virtual()) {
            configNode.set(Config::class, config)
        }
        loader.save(configNode)
    }

    fun save() {
        val newNode = loader.createNode()
        mapper.save(config, newNode)
        loader.save(configNode)
    }

    fun reload() {
        configNode = loader.load()
        config = mapper.load(configNode)
    }

    fun reload(newConfigNode: ConfigurationNode) {
        configNode = newConfigNode
        config = mapper.load(configNode)
    }
}