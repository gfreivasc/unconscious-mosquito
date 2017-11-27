package re.usto.umqtt.internal

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class MarshallerTest {
    @Test fun connackPacketsCorrectlyMarshaled() {
        val protocol = "MQTT"
        val cid = "TESTCLIENT"
        val willTopic = "a/b"
        val willMsg = "will"
        val un = "user"
        val pw = "pass"
        val connect = Connect(protocol, 4, true, 360,
                cid, true, willTopic, willMsg, un, pw, 1)
        val marshaled = Marshaller.marshall(connect)

        var i = 0
        assertEquals(0b00010000.toByte(), marshaled[i++])
        val remLen = 2 + protocol.length +  // Protocol string length
                1 +                         // Protocol version length
                1 +                         // Flags length
                2 +                         // Keep alive length
                2 + cid.length +            // Client ID length
                2 + willTopic.length +      // Will topic length
                2 + willMsg.length +        // will message length
                2 + un.length +             // Username length
                2 + pw.length               // Password length
        assertEquals(remLen.toByte(), marshaled[i++])
        assertEquals(protocol.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until protocol.length) assertEquals(protocol[j].toByte(), marshaled[j + i])
        i += protocol.length
        assertEquals(4, marshaled[i++].toInt())
        assertEquals(0b11101110.toByte(), marshaled[i++])
        assertEquals((360/256).toByte(), marshaled[i++])
        assertEquals((360 % 256).toByte(), marshaled[i++])
        assertEquals(cid.length, (marshaled[i] * 256 + marshaled[i + 1]))
        i += 2
        for (j in 0 until cid.length) assertEquals(cid[j].toByte(), marshaled[j + i])
        i += cid.length
        assertEquals(willTopic.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until willTopic.length) assertEquals(willTopic[j].toByte(), marshaled[j + i])
        i += willTopic.length
        assertEquals(willMsg.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until willMsg.length) assertEquals(willMsg[j].toByte(), marshaled[j + i])
        i += willMsg.length
        assertEquals(un.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until un.length) assertEquals(un[j].toByte(), marshaled[j + i])
        i += un.length
        assertEquals(pw.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until pw.length) assertEquals(pw[j].toByte(), marshaled[j + i])
    }

    @Test fun connectFrameMarshalingCorrectlyWithNonSuppliedData() {
        val protocol = "MQTT"
        val cid = "TESTCLIENT"
        val un = "user"
        val pw = "pass"
        val connect = Connect(protocol, 4, true, 360,
                cid, username = un,  password = pw)
        val marshaled = Marshaller.marshall(connect)

        var i = 0
        assertEquals(0b00010000.toByte(), marshaled[i++])
        val remLen = 2 + protocol.length +  // Protocol string length
                1 +                         // Protocol version length
                1 +                         // Flags length
                2 +                         // Keep alive length
                2 + cid.length +            // Client ID length
                2 + un.length +             // Username length
                2 + pw.length               // Password length
        assertEquals(remLen.toByte(), marshaled[i++])
        assertEquals(protocol.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until protocol.length) assertEquals(protocol[j].toByte(), marshaled[j + i])
        i += protocol.length
        assertEquals(4, marshaled[i++].toInt())
        assertEquals(0b11000010.toByte(), marshaled[i++])
        assertEquals((360/256).toByte(), marshaled[i++])
        assertEquals((360 % 256).toByte(), marshaled[i++])
        assertEquals(cid.length, (marshaled[i] * 256 + marshaled[i + 1]))
        i += 2
        for (j in 0 until cid.length) assertEquals(cid[j].toByte(), marshaled[j + i])
        i += cid.length
        assertEquals(un.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until un.length) assertEquals(un[j].toByte(), marshaled[j + i])
        i += un.length
        assertEquals(pw.length, marshaled[i] * 256 + marshaled[i + 1])
        i += 2
        for (j in 0 until pw.length) assertEquals(pw[j].toByte(), marshaled[j + i])
    }

    @Test fun connackFrameUnmarshalingCorrectly() {
        val frame = byteArrayOf(0x20, 0x02, 0x00, 0x03)
        val connack = Marshaller.unmarshal(frame) as Connack
        assertEquals(0x03, connack.returnCode)
    }

    @Test fun subscribeFrameSingleTopicMarshalingCorrectly() {
        val topic = "a/b"
        val qos = 0b01.toByte()
        val pid = 12
        val sub = Subscribe(topic, qos, pid)
        val marshaled = Marshaller.marshall(sub)
        var i = 0
        assertEquals(0b10000010.toByte(), marshaled[i++])
        val remLen = 2 +        // PacketID
            2 + topic.length +  // Topic
            1                   // QoS
        assertEquals(remLen.toByte(), marshaled[i++])
        assertEquals((pid / 256).toByte(), marshaled[i++])
        assertEquals((pid % 256).toByte(), marshaled[i++])
        assertEquals((topic.length / 256).toByte(), marshaled[i++])
        assertEquals((topic.length % 256).toByte(), marshaled[i++])
        for (j in 0 until topic.length) assertEquals(topic[j].toByte(), marshaled[j + i])
        i += topic.length
        assertEquals(qos, marshaled[i])
    }

    @Test fun subscribeFrameMultiTopicMarshalingCorrectly() {
        val topics = arrayOf("a/b", "c/d", "abcde/fghij")
        val qosLevels = byteArrayOf(0b00.toByte(), 0b01.toByte(), 0b10.toByte())
        val pid = 1241
        val sub = Subscribe(topics, qosLevels, pid)
        val marshaled = Marshaller.marshall(sub)
        var i = 0
        assertEquals(0b10000010.toByte(), marshaled[i++])
        val remLen = 2 +       // PacketID
                topics.map { it.length }.fold(0) { acc, topicLen ->
                    acc + topicLen + 2 + 1 // Topic length + qos
                }
        assertEquals(remLen.toByte(), marshaled[i++])
        assertEquals((pid / 256).toByte(), marshaled[i++])
        assertEquals((pid % 256).toByte(), marshaled[i++])
        for (topic in topics) {
            assertEquals((topic.length / 256).toByte(), marshaled[i++])
            assertEquals((topic.length % 256).toByte(), marshaled[i++])
            for (j in 0 until topic.length) assertEquals(topic[j].toByte(), marshaled[j + i])
            i += topic.length
            assertEquals(qosLevels[topics.indexOf(topic)], marshaled[i++])
        }
    }

    @Test fun subackFrameUnmarshalingCorrectly() {
        val frame = byteArrayOf(0x90.toByte(), 0x03, 0x00, 0x01, 0x01)
        val suback = Marshaller.unmarshal(frame) as Suback
        assertEquals(1, suback.packetId)
        assertEquals(1, suback.qosLevels.size)
        assertEquals(0x01.toByte(), suback.qosLevels[0])
    }

    @Test fun publishMarshallingCorrectly() {
        val topic = "a/b"
        val msg = "test"
        val qos = 2
        val pid = 10
        val marshaled = Marshaller.marshall(Publish(topic, msg, qos.toByte(), pid))
        var i = 0
        assertEquals(0b00110101.toByte(), marshaled[i++])
        val remLen = 2 + topic.length + 2 + msg.length
        assertEquals(remLen.toByte(), marshaled[i++])
        assertEquals((topic.length / 256).toByte(), marshaled[i++])
        assertEquals((topic.length % 256).toByte(), marshaled[i++])
        for (j in 0 until topic.length) assertEquals(topic[j].toByte(), marshaled[i + j])
        i += topic.length
        assertEquals((pid / 256).toByte(), marshaled[i++])
        assertEquals((pid % 256).toByte(), marshaled[i++])
        assertEquals(msg.length, marshaled.size - i)
        for (j in 0 until msg.length) assertEquals(msg[j].toByte(), marshaled[i + j])
    }

    @Test fun publishUnmarshallingCorrectly() {
        val topic = "a/b"
        val msg = "test"
        val qos: Byte = 2
        val pid = 10
        val dup = true
        val retain = false
        var publish = Marshaller.unmarshal(
                Marshaller.marshall(Publish(topic, msg, qos.toByte(), pid, dup, retain))
        ) as? Publish
        assertNotNull(publish)
        publish = publish as Publish
        assertEquals(topic, publish.topic)
        assertEquals(msg, publish.payload)
        assertEquals(qos, publish.qos)
        assertEquals(pid, publish.packetId)
        assertEquals(dup, publish.dup)
        assertEquals(retain, publish.retain)
    }

    @Test fun remainingSizeEncodingCorrectly() {
        val size = 16384
        val enc = Marshaller.encodeRemainingSize(size)
        assertEquals(0x80.toByte(), enc[0])
        assertEquals(0x80.toByte(), enc[1])
        assertEquals(0x01.toByte(), enc[2])
    }

    @Test fun remainingSizeDecodingCorrectly() {
        val size = 8219942
        assertEquals(size, Marshaller.decodeRemainingSize(
                Marshaller.encodeRemainingSize(size).toByteArray()
        ))
    }
}