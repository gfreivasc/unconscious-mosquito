package re.usto.umqtt.internal

import io.reactivex.Observable
import java.io.IOException
import java.net.Socket

class SocketObservable(private val socket: Socket, bufferSize: Int = 1024 * 1024) {
    private val buffer = ByteArray(bufferSize)

    fun get(): Observable<Byte> = Observable.create { subscriber ->
        var n = 0
        try {
            while (!subscriber.isDisposed && n != -1) {
                n = socket.getInputStream().read(buffer)
                if (n > 0) for (i in 0..(n - 1)) subscriber.onNext(buffer[i])
            }
        }
        catch (e: IOException) {
            subscriber.onError(e)
        }
        if (!subscriber.isDisposed) {
            if (n == -1) subscriber.onError(IOException("Broken PIPE"))
            else subscriber.onComplete()
        }
    }
}