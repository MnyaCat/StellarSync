package dev.mnyacat.stellar_sync_fabric.model

import dev.mnyacat.stellar_sync_common.model.FormattedMessage
import dev.mnyacat.stellar_sync_common.model.MessageColor
import dev.mnyacat.stellar_sync_common.model.MessageFormatter
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting

class FabricMessageFormatter : MessageFormatter<Text> {
    override fun literal(text: String): FormattedMessage<Text> {
        return FormattedMessage(Text.literal(text), this)
    }

    override fun color(message: FormattedMessage<Text>, color: MessageColor): FormattedMessage<Text> {
        val formattedText = when (color) {
            MessageColor.RED -> (message.raw as MutableText).formatted(Formatting.RED)
            MessageColor.GREEN -> (message.raw as MutableText).formatted(Formatting.GREEN)
            MessageColor.YELLOW -> (message.raw as MutableText).formatted(Formatting.YELLOW)
            MessageColor.WHITE -> (message.raw as MutableText).formatted(Formatting.WHITE)
        }
        return FormattedMessage(formattedText, this)
    }

    override fun append(vararg messages: FormattedMessage<Text>): FormattedMessage<Text> {
        val base = messages.firstOrNull()?.raw as? MutableText ?: Text.literal("")
        val result = messages.drop(1).fold(base) { acc, fm ->
            acc.append(fm.raw as Text)
        }
        return FormattedMessage(result, this)
    }
}
