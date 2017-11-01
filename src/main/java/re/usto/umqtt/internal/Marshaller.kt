package re.usto.umqtt.internal

import java.nio.file.Files.size
import kotlin.experimental.and
import kotlin.experimental.or


class Marshaller {
    companion object {
        fun unmarshal(message: ByteArray): MQTTFrame {
            if (message.size < 2) {
                throw IllegalArgumentException("Byte array not a proper MQTT frame")
            }
            return when (message[0]) {
                0x02.toByte() -> Connack(message[3].toInt())
                else -> throw IllegalArgumentException("Byte array not a known MQTT frame")
            }
        }

        fun marshall(message: MQTTFrame): ByteArray = when (message) {
            is Connect -> marshallConnectFrame(message)
            else -> TODO("LOG DIS")
        }

        private fun marshallConnectFrame(message: Connect): ByteArray {
            if ((message.willTopic == null) xor (message.willMessage == null)) {
                throw IllegalArgumentException(
                        "There must be either both will topic and message or none"
                )
            }
            val arr = ArrayList<Byte>()
            arr.add((0x01 shl 4).toByte())
            var remainingLength = message.protocol.length + 2 +
                    message.clientId.length + 2 + 4
            if (message.username != null) remainingLength += message.username.length + 2
            if (message.password != null) remainingLength += message.password.length + 2
            if (message.willTopic != null) remainingLength += message.willTopic.length + 2
            if (message.willMessage != null) remainingLength += message.willMessage.length + 2
            arr.addAll(encodedRemainingSize(remainingLength))
            arr.addAll(encodeString(message.protocol))
            arr.add(message.version.toByte())
            var connectFlags = 0.toByte()
            if (message.username != null) connectFlags = connectFlags or (1 shl 7).toByte()
            if (message.password != null) connectFlags = connectFlags or (1 shl 6)
            if (message.willRetain) connectFlags = connectFlags or (1 shl 5)
            connectFlags = connectFlags or (message.willQoS shl 3).toByte()
            if (message.willMessage != null) {
                connectFlags = connectFlags or (1 shl 2)
            }
            if (message.cleanSession) connectFlags = connectFlags or (1 shl 1)
            arr.add(connectFlags)
            arr.addAll(shortToBytes(message.keepAlive))
            arr.addAll(encodeString(message.clientId))
            if (message.willMessage != null) {
                arr.addAll(encodeString(message.willTopic!!))
                arr.addAll(encodeString(message.willMessage))
            }
            message.username?.let { arr.addAll(encodeString(it)) }
            message.password?.let { arr.addAll(encodeString(it)) }
            return arr.toByteArray()
        }

        private fun encodedRemainingSize(initSize: Int): ArrayList<Byte> {
            val bytes = ArrayList<Byte>()
            var size = initSize
            do {
                var digit = (size % 0x80).toByte()
                size /= 0x80
                if (size > 0) {
                    digit = digit or 0x80.toByte()
                }
                bytes.add(digit)
            } while (size > 0)
            return bytes
        }

        private fun shortToBytes(value: Int): Array<Byte> = arrayOf(
                (value ushr 8).toByte(),
                (value and 0xff).toByte()
        )

        private fun encodeString(string: String): Array<Byte> = arrayOf(
                *shortToBytes(string.length),
                *string.toByteArray().toTypedArray()
        )
    }
}