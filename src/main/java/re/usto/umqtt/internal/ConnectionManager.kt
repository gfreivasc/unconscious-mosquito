package re.usto.umqtt.internal

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import re.usto.umqtt.UMqtt
import re.usto.umqtt.util.awaitFirst
import java.io.IOException
import java.net.*
import kotlin.collections.ArrayList
import kotlin.experimental.and

class ConnectionManager(private val connection: UMqtt.Companion.Connection) {
    private val socket = Socket()
    private val sendQueue = PublishSubject.create<ByteArray>()
    private val dataQueue = PublishSubject.create<ByteArray>()
    private val readBuffer = ByteArray(1024 * 1024)
    private var parseBuffer = ArrayList<Byte>()
    private var parsePos = 0
    private var parseFetchSize = false

    private var readDisposable: Disposable
    private lateinit var parseDisposable: Disposable
    private lateinit var inputReadObservable: ObservableEmitter<Byte>


    init {
        sendQueue.subscribeOn(Schedulers.io())
                .subscribe({
                    frame -> socket.getOutputStream().write(frame)
                }, {
                    error -> /* TODO: Logger!! */
                })
        readDisposable = dataQueue.subscribeOn(Schedulers.io())
                .subscribe({
                    frame -> Log.d("UMqtt", frame.toString())
                }, {
                    error -> /* TODO: Logger!! */
                })
    }

    @Throws(IOException::class)
    private fun openSocket() {
        val ip: InetAddress = try {
            InetAddress.getByName(connection.brokerIp)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Broker IP host is unknown")
        }
        val address: SocketAddress = InetSocketAddress(ip, connection.brokerPort)
        socket.connect(address)
    }

    private fun subscribeInputStream() {
        inputReadObservable = ObservableEmitter { subscriber ->
            try {
                if (readDisposable.isDisposed) return@create
                var m: Int
                do {
                    m = socket.getInputStream().read(readBuffer)
                    if (m > 0) for (i in 0..(m - 1)) subscriber.onNext(readBuffer[i])
                } while (m != -1 && !subscriber.isDisposed)
            } catch (e: IOException) {
                subscriber.onError(e)
            }
            if (!subscriber.isDisposed) subscriber.onComplete()
        }
        parseDisposable = inputReadObservable
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
                    Log.e("UMqtt", "Error", error)
                })
    }

    fun connect(): Completable = Completable.create { s ->
        try {
            openSocket()
            val connectFrame = Connect(
                    connection.protocol,
                    connection.version,
                    connection.cleanSession,
                    connection.keepAlive,
                    connection.willRetain,
                    connection.clientId,
                    connection.willTopic,
                    connection.willMessage,
                    connection.username,
                    connection.password,
                    connection.willQoS
            )
            subscribeInputStream()
            sendMessage(connectFrame)
            async {
                val response = dataQueue.awaitFirst()
                val connack = Marshaller.unmarshal(response) as Connack
                if (connack.returnCode == 0) s.onComplete()
                else s.onError(ConnectException("Could not connect to MQTT Broker"))
            }
        }
        catch (e: Throwable) {
            s.onError(e)
        }
    }

    private fun sendMessage(message: MQTTFrame) {
        sendQueue.onNext(Marshaller.marshall(message))
    }
}