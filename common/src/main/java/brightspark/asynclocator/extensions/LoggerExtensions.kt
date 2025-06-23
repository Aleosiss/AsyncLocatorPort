package brightspark.asynclocator.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

inline fun <reified T> logger(): Logger = LoggerFactory.getLogger(T::class.java)

private val logCache = ConcurrentHashMap<Class<out Any>, Logger>(128)
val Any.LOG: Logger get() = logCache.getOrPut(this::class.java) { LoggerFactory.getLogger(this::class.java) }
