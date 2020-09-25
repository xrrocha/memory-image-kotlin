package memimg

import arrow.core.Either
import arrow.core.Left
import arrow.core.flatMap
import arrow.core.getOrHandle
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Command<S, A> {
    fun executeOn(system: S): Either<Throwable, A>
}

@Serializable
abstract class Transaction<S, A> : Command<S, A>

interface Query<S, A> : Command<S, A>

interface Storage<C> {
    fun read(): Either<Throwable, C?>
    fun write(content: C): Either<Throwable, Unit>
}

interface SerDeser<C> {
    fun <T> serialize(obj: T): Either<Throwable, C>
    fun <T> deserialize(content: C): Either<Throwable, T>
}

class MemoryImage<S, C>(private val storage: Storage<C>,
                        private val serDeser: SerDeser<C>,
                        private val emptySystem: () -> Either<Throwable, S>) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private fun <T> handleLeft(t: Throwable): T {
        // TODO Add error handling and resumption
        logger.error("Error instantiating system: ${t.message ?: t.toString()}")
        throw t
    }

    // TODO Fail fast on empty system creation error
    private val system: S = emptySystem().getOrHandle(::handleLeft)

    init {

        fun <T> nullRight() = Either.Right<T?>(null)

        generateSequence {
            storage.read()
                    .flatMap {
                        if (it == null) nullRight()
                        else serDeser.deserialize<Transaction<S, *>>(it)
                    }
                    .flatMap { it?.executeOn(system) ?: nullRight() }
        }
                .dropWhile { it is Either.Right && it.b != null }
                .first()
                .getOrHandle(this::handleLeft)
    }

    fun <T> executeTransaction(transaction: Command<S, T>): Either<Throwable, T> =
            synchronized(this) {
                transaction.executeOn(system).also {
                    it.map { serDeser.serialize(transaction).flatMap(storage::write) }
                            .getOrHandle(this::handleLeft)
                }
            }

    // TODO: How to ensure a query doesn't mutate state?
    // TODO: Can a query to see inconsistent results?
    fun <T> executeQuery(query: Command<S, T>): Either<Throwable, T> =
            runBlocking {
                lateinit var result: Either<Throwable, T>
                val job = launch {
                    try {
                        result = query.executeOn(system)
                    } catch (throwable: Throwable) {
                        result = Left(throwable)
                    }
                }
                job.join()
                result
            }
}



