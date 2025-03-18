package dev.mnyacat.stellar_sync_paper.model

import dev.mnyacat.stellar_sync_common.model.FormattedMessage
import dev.mnyacat.stellar_sync_common.model.MessageColor
import dev.mnyacat.stellar_sync_common.model.MessageFormatter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

// Paper向けの実装例（BungeeCordのComponent APIやAdventureを使用する場合）
class PaperMessageFormatter : MessageFormatter<Component> {
    override fun literal(text: String): FormattedMessage<Component> {
        return FormattedMessage(Component.text(text), this)
    }

    override fun color(message: FormattedMessage<Component>, color: MessageColor): FormattedMessage<Component> {
        val formattedText = when(color) {
            MessageColor.RED -> (message.raw as Component).color(NamedTextColor.RED)
            MessageColor.GREEN -> (message.raw as Component).color(NamedTextColor.GREEN)
            MessageColor.YELLOW -> (message.raw as Component).color(NamedTextColor.YELLOW)
            MessageColor.WHITE -> (message.raw as Component).color(NamedTextColor.WHITE)
        }
        return FormattedMessage(formattedText, this)
    }

    override fun append(vararg messages: FormattedMessage<Component>): FormattedMessage<Component> {
        // Paper向けの場合、Component.text()に複数のコンポーネントを連結
        val components = messages.map { it.raw as Component }
        val result = Component.empty().append(Component.join(JoinConfiguration.noSeparators(), components))
        return FormattedMessage(result, this)
    }
}
