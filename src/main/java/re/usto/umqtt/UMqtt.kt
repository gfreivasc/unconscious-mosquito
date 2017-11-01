package re.usto.umqtt

import android.util.Log
import io.reactivex.schedulers.Schedulers
import re.usto.umqtt.internal.ConnectionManager
import java.net.NoRouteToHostException

class UMqtt(private val connection: Connection) {
    private val connManager = ConnectionManager(connection)
    private var listener: OnConnectedListener? = null
    private var onConnected: (() -> Unit)? = null

    interface OnConnectedListener {
        fun onConnected()
    }

    companion object {
        class Connection @JvmOverloads constructor(
                val brokerIp: String,
                var clientId: String,
                val brokerPort: Int = 1883) {
            var protocol: String = "MQTT"
            private set(value) { field = value }
            var version: Int = 4
            private set(value) { field = value }
            var keepAlive: Int = 0
            var username: String? = null
            var password: String? = null
            var willTopic: String? = null
            var willMessage: String? = null
            var willRetain: Boolean = false
            var willQoS: Int = 0b00
            var cleanSession: Boolean = false

            fun setProtocol(protocol: String, version: Int): Connection {
                this.protocol = protocol
                this.version = version
                return this
            }

            fun setCredentials(username: String, password: String): Connection {
                this.username = username
                this.password = password
                return this
            }

            fun create() = UMqtt(this)
        }
    }

    fun connect(listener: OnConnectedListener) {
        this.listener = listener
        connect(listener::onConnected)
    }

    fun connect(listener: (() -> Unit)? = null) {
        onConnected = listener
        connManager.connect()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    onConnected?.invoke()
                }, { error -> when(error) {
                    is NoRouteToHostException -> Log.e(
                            "UMqtt",
                            "Check if information is correct: ${connection.brokerIp}:${connection.brokerPort}",
                            error)
                    is SecurityException -> Log.e(
                            "UMqtt",
                            "Check your permissions, INTERNET must be missing",
                            error
                    )
                    else -> error.printStackTrace()
                }})
    }
}