package re.usto.umqtt

import io.reactivex.disposables.Disposable
import re.usto.umqtt.internal.*

class Subscription(val subscribe: Subscribe,
                   val connectionManager: ConnectionManager) {
    private var subscription: Disposable? = null
    private var subackDisposable: Disposable? = null
    var subscribed: Boolean = false

    fun listen(listener: MessageReceived<String>) {
        subscription = connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe { message ->
                    listener.onMessageReceived(message.payload.toString())
                }
        if (subackDisposable == null) awaitSuback { subscribed = true }
        connectionManager.sendMessage(subscribe)
    }

    fun listen(listener: OnMessageReceived<String>) {
        subscription = connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe { message ->
                    listener(message.payload.toString())
                }
        if (subackDisposable == null) awaitSuback { subscribed = true }
        connectionManager.sendMessage(subscribe)
    }

    private fun awaitSuback(success: SubscribedSuccessfully) {
        subackDisposable = connectionManager.observeData()
                .filter { it is Suback }
                .map { it as Suback }
                .filter { it.packetId == subscribe.packetId}
                .subscribe {
                    subscribed = true
                    success.onSubscribedSuccessfully()
                    subackDisposable?.dispose()
                }
    }

    private fun awaitSuback(success: OnSubscribedSuccessfully) {
        subackDisposable = connectionManager.observeData()
                .filter { it is Suback }
                .map { it as Suback }
                .filter { it.packetId == subscribe.packetId }
                .subscribe {
                    subscribed = true
                    success()
                    subackDisposable?.dispose()
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
        subscription = connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe({ message ->
                    listener.onMessageReceived(message.payload.toString())
                }, { t ->
                    error.onError(t)
                })
        if (subackDisposable == null) awaitSuback { subscribed = true }
        connectionManager.sendMessage(subscribe)
    }

    fun listen(listener: OnMessageReceived<String>, error: OnError) {
        subscription = connectionManager.observeMessageStream()
                .filter { it.topic in subscribe.topics }
                .subscribe({ message ->
                    listener(message.payload.toString())
                }, { t ->
                    error(t)
                })
        if (subackDisposable == null) awaitSuback { subscribed = true }
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

    fun unlisten() {
        if (subackDisposable != null || subscribed) {
            // TODO: Send unsubscribe to server
            subackDisposable?.dispose()
        }
        subscription?.dispose()
    }
}
