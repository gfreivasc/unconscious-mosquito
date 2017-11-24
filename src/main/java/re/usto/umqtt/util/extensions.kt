package re.usto.umqtt.util

import io.reactivex.ObservableSource
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.experimental.suspendCancellableCoroutine

suspend fun <T> ObservableSource<T>.awaitFirst(): T = suspendCancellableCoroutine { cont ->
    subscribe(object : Observer<T> {
        private lateinit var subscription: Disposable
        private var value: T? = null
        private var seenValue = false

        override fun onSubscribe(sub: Disposable) {
            subscription = sub
            cont.invokeOnCompletion { sub.dispose() }
        }

        override fun onNext(value: T) {
            if (!seenValue) {
                seenValue = true
                cont.resume(value)
                subscription.dispose()
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun onComplete() {
            if (seenValue) {
                if (cont.isActive) cont.resume(value as T)
                return
            }
            if (cont.isActive) {
                cont.resumeWithException(
                        NoSuchElementException("No value received via onNext for await")
                )
            }
        }

        override fun onError(e: Throwable) {
            cont.resumeWithException(e)
        }
    })
}

fun Byte.hexString() = java.lang.String.format("%02x", this)