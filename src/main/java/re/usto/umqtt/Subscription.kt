package re.usto.umqtt

import re.usto.umqtt.internal.*

class Subscription(val subscribe: Subscribe,
                   val connectionManager: ConnectionManager) {

    fun listen(listener: MessageReceived<String>) {
        connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe { message ->
                    listener.onMessageReceived(message.payload.toString())
                }
        connectionManager.sendMessage(subscribe)
    }

    fun listen(listener: OnMessageReceived<String>) {
        connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe { message ->
                    listener(message.payload.toString())
                }
        connectionManager.sendMessage(subscribe)
    }

    private fun awaitSuback(success: SubscribedSuccessfully) {
        connectionManager.observeData()
                .map { it as? Suback }
                .filter { it != null && it.packetId == subscribe.packetId}
                .subscribe {
                    success.onSubscribedSuccessfully()
                }
    }

    private fun awaitSuback(success: OnSubscribedSuccessfully) {
        connectionManager.observeData()
                .map { it as? Suback }
                .filter { it != null && it.packetId == subscribe.packetId }
                .subscribe {
                    success()
                }
    }

    fun listen(success: SubscribedSuccessfully, listener: MessageReceived<String>) {
        awaitSuback(success)
        listen(listener)
    }

    fun listen(success: OnSubscribedSuccessfully, listener: OnMessageReceived<String>) {
        awaitSuback(success)
        listen(listener)
    }

    fun listen(listener: MessageReceived<String>, error: Error) {
        connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe({ message ->
                    listener.onMessageReceived(message.payload.toString())
                }, { t ->
                    error.onError(t)
                })
        connectionManager.sendMessage(subscribe)
    }

    fun listen(listener: OnMessageReceived<String>, error: OnError) {
        connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe({ message ->
                    listener(message.payload.toString())
                }, { t ->
                    error(t)
                })
        connectionManager.sendMessage(subscribe)
    }

    fun listen(success: SubscribedSuccessfully, listener: MessageReceived<String>, error: Error) {
        awaitSuback(success)
        listen(listener, error)
    }

    fun listen(success: OnSubscribedSuccessfully, listener: OnMessageReceived<String>, error: OnError) {
        awaitSuback(success)
        listen(listener, error)
    }
}
