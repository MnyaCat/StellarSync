package dev.mnyacat.stellar_sync_common.model

interface MessageFormatter<T> {
    fun literal(text: String): FormattedMessage<T>
    fun color(message: FormattedMessage<T>, color: MessageColor): FormattedMessage<T>
    fun append(vararg messages: FormattedMessage<T>): FormattedMessage<T>
}

class FormattedMessage<T>(val raw: T, private val formatter: MessageFormatter<T>) {
    fun color(color: MessageColor): FormattedMessage<T> = formatter.color(this, color)
    fun append(vararg messages: FormattedMessage<T>): FormattedMessage<T> = formatter.append(this, *messages)
}

enum class MessageColor {
    RED, GREEN, YELLOW, WHITE
}