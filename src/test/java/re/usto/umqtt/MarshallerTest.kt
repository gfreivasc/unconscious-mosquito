package re.usto.umqtt

import re.usto.umqtt.internal.Connect
import re.usto.umqtt.internal.Marshaller
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import re.usto.umqtt.internal.Connack

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

    @Test fun connackFrameUnmarshalingCorrectly() {
        val frame = byteArrayOf(0x20, 0x02, 0x00, 0x03)
        val connack = Marshaller.unmarshal(frame) as Connack
        assertEquals(0x03, connack.returnCode)
    }
}