package memimg

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.flatMap
import arrow.core.getOrHandle
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MemoryImage<S>(private val storage: Storage, private val emptySystem: () -> Either<Throwable, S>) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    private fun <T> handleFailure(t: Throwable): T {
        // TODO Add error handling and resumption
        logger.error("Error ${t::class.java.simpleName} instantiating system: ${t.message ?: t.toString()}", t)
        throw t
    }

    // TODO Add [blocking?] system checkpoints
    private val system: S = emptySystem().getOrHandle(::handleFailure)

    init {

        generateSequence {
            @Suppress("unchecked_cast")
            storage.read()
                    .map { it as Transaction<S, *>? }
                    .flatMap { it?.executeOn(system) ?: Right(null) }
        }
                .dropWhile { it is Either.Right && it.b != null }
                .first()
                .getOrHandle(this::handleFailure)
    }

    fun <T> executeTransaction(transaction: Transaction<S, T>): Either<Throwable, T> =
            synchronized(this) {
                // if (transaction is Validatable<*>) {
                //     @Suppress("unchecked_cast")
                //     (transaction as Validatable<S>).validate(system)
                // }
                transaction
                        .executeOn(system)
                        .map { result ->
                            storage.write(transaction).map { result }
                        }
                        .getOrHandle(this::handleFailure)
            }

    // TODO: How to ensure a query doesn't mutate state?
    // TODO: Can a query to see inconsistent results?
    fun <T> executeQuery(query: Command<S, T>): Either<Throwable, T> =
            runBlocking {
                lateinit var result: Either<Throwable, T>
                val job = launch {
                    result = try {
                        query.executeOn(system)
                    } catch (throwable: Throwable) {
                        Left(throwable)
                    }
                }
                job.join()
                result
            }
}



