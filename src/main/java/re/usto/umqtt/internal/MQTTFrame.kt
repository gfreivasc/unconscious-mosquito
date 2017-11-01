package re.usto.umqtt.internal

sealed class MQTTFrame {
    abstract val length: Int
}
data class Connect(val protocol: String, val version: Int, val cleanSession: Boolean,
                   val keepAlive: Int, val clientId: String, val willRetain: Boolean = false,
                   val willTopic: String? = null, val willMessage: String? = null,
                   val username: String? = null, val password: String? = null,
                   val willQoS: Int = 0b00, override val length: Int = 0
): MQTTFrame()
data class Connack(val returnCode: Int): MQTTFrame() {
    override val length: Int = 4 // 4 Bytes
}