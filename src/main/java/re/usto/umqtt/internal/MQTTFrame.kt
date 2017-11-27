package re.usto.umqtt.internal

sealed class MQTTFrame {
    abstract val length: Int
}

class NullFrame(): MQTTFrame() {
    override val length: Int = 0
}

data class Connect(val protocol: String, val version: Int, val cleanSession: Boolean,
                   val keepAlive: Int, val clientId: String, val willRetain: Boolean = false,
                   val willTopic: String? = null, val willMessage: String? = null,
                   val username: String? = null, val password: String? = null,
                   val willQoS: Int = 0b00
): MQTTFrame() {
    override val length: Int = 0
}

data class Connack(val returnCode: Int): MQTTFrame() {
    override val length: Int = 4 // 4 Bytes
}

data class Subscribe(val topics: Array<String>, val qosLevels: ByteArray, val packetId: Int = 0
): MQTTFrame() {
    constructor(topic: String, qosLevel: Byte, packetId: Int = 0)
            : this(arrayOf(topic), byteArrayOf(qosLevel), packetId)
    init {
        if (topics.size != qosLevels.size) {
            throw IllegalArgumentException("Different number of topics and QoS levels specified")
        }
    }
    override val length: Int = 0
}

data class Publish(val topic: String, val payload: String, val qos: Byte = 0,
                   val packetId: Int, var dup: Boolean = false, val retain: Boolean = true
): MQTTFrame() {
    override val length: Int = 0
}

data class Puback(val packetId: Int): MQTTFrame() {
    override val length: Int = 4
}

data class Pubrel(val packetId: Int): MQTTFrame() {
    override val length: Int = 4
}

data class Pubcomp(val packetId: Int): MQTTFrame() {
    override val length: Int = 4
}

data class Suback(val packetId: Int, val qosLevels: ByteArray): MQTTFrame() {
    constructor(packetId: Int, qos: Byte): this(packetId, byteArrayOf(qos))
    override val length: Int = 0
}