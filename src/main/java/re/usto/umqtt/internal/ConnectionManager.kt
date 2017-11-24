package re.usto.umqtt.internal

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import re.usto.umqtt.Subscription
import re.usto.umqtt.UMqtt
import re.usto.umqtt.util.awaitFirst
import re.usto.umqtt.util.hexString
import java.io.IOException
import java.net.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

class ConnectionManager(private val connection: UMqtt.Companion.Connection) {
    private lateinit var socket: Socket
    private val sendQueue = PublishSubject.create<ByteArray>()
    private val dataQueue = PublishSubject.create<ByteArray>()
    private var parseBuffer = ArrayList<Byte>()
    private var parsePos = 0
    private var parseFetchSize = false
    private var preConnectionQueue: ArrayList<MQTTFrame>? = ArrayList()

    private lateinit var parseDisposable: Disposable
    private var sendDisposable: Disposable

    init {
        sendDisposable = sendQueue.subscribeOn(Schedulers.io())
                .subscribe({
                    frame -> socket.getOutputStream()?.write(frame)
                }, {
                    error -> /* TODO: Logger!! */
                })
    }

    @Throws(Throwable::class)
    private fun openSocket() {
        socket = Socket()
        val ip: InetAddress = try {
            InetAddress.getByName(connection.brokerIp)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Broker IP host is unknown")
        }
        val address: SocketAddress = InetSocketAddress(ip, connection.brokerPort)
        socket.connect(address)
    }

    private fun subscribeInputStream() {
        parseDisposable = SocketObservable(socket).get()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    parseBuffer.add(it)
                    if (parsePos == 0) {
                        parsePos = 1
                        parseFetchSize = true
                    }
                    else if (parseFetchSize) {
                        parsePos *= it % 0x80.toByte()
                        if ((it and 0x80.toByte()) == 0.toByte()) parseFetchSize = false
                    }
                    else if (parsePos > 1) parsePos--
                    else {
                        parsePos = 0
                        dataQueue.onNext(parseBuffer.toByteArray())
                        parseBuffer.clear()
                    }
                }, { error ->
                    Log.e("UMqtt", "Socket Error", error)
                    socket.close()
                    parseDisposable.dispose()
                    if (error is SocketException) connect()
                })
    }

    fun connect(): Completable = Completable.create { s ->
        try {
            openSocket()
            subscribeInputStream()
            val connectFrame = Connect(
                    connection.protocol,
                    connection.version,
                    connection.cleanSession,
                    connection.keepAlive,
                    connection.clientId,
                    connection.willRetain,
                    connection.willTopic,
                    connection.willMessage,
                    connection.username,
                    connection.password,
                    connection.willQoS
            )
            async {
                val response = dataQueue.awaitFirst()
                val connack = parseFrame(response) as? Connack
                if (connack != null && connack.returnCode == 0) {
                    s.onComplete()
                    val postConn = ArrayList<MQTTFrame>()
                    postConn.addAll(preConnectionQueue!!)
                    preConnectionQueue = null
                    postConn.forEach { sendMessage(it) }
                }
                else s.onError(ConnectException("Could not connect to MQTT Broker"))
            }
            sendMessage(connectFrame)
        }
        catch (e: Throwable) {
            s.onError(e)
        }
    }

    fun sendMessage(message: MQTTFrame) {
        if (preConnectionQueue != null && message !is Connect) preConnectionQueue?.add(message)
        else sendQueue.onNext(Marshaller.marshall(message))
    }

    fun observeMessageStream(): Observable<Publish> = dataQueue.subscribeOn(Schedulers.io())
            .map { parseFrame(it) }
            .filter { it is Publish }
            .map { it as Publish }

    fun observeData(): Observable<MQTTFrame> = dataQueue.subscribeOn(Schedulers.io())
            .map { parseFrame(it) }
            .filter { it !is NullFrame }

    fun parseFrame(frame: ByteArray): MQTTFrame {
        try {
            return Marshaller.unmarshal(frame)
        }
        catch (e: IllegalArgumentException) {
            val type = (frame[0].toInt() ushr 4).toByte()
            Log.e("UMqtt", "Unable to read unknown packet type 0x${type.hexString()}")
            return NullFrame()
        }
    }
}