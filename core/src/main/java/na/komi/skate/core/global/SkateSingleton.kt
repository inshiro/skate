package na.komi.skate.core.global

import na.komi.skate.core.Skate
import java.util.Stack

internal object SkateSingleton {
    @Volatile
    internal var instance: Skate? = null

    @Synchronized
    fun getInstance() = instance ?: synchronized(Skate::class.java) { instance ?: Skate().also { instance = it } }

    @Suppress("unused")
    private fun readResolve() = getInstance()

    internal var stack: Stack<Skate.SkateFragment>? = Stack()

    fun clear() {
        instance = null
        stack = null
    }

}