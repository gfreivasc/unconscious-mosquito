package re.usto.umqtt.internal

interface MessageReceived<T> {
    fun onMessageReceived(message: T)
}

interface SubscribedSuccessfully {
    fun onSubscribedSuccessfully()
}

interface Error {
    fun onError(throwable: Throwable)
}

typealias OnMessageReceived<T> = (T) -> Unit

typealias OnSubscribedSuccessfully = () -> Unit

typealias OnError = (Throwable) -> Unit
