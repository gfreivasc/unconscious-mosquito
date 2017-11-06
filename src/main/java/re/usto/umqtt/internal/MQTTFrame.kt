package re.usto.umqtt.internal

sealed class MQTTFrame {
    abstract val length: Int
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