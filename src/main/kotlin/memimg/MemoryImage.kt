package memimg

import arrow.core.Either
import arrow.core.Right
import arrow.core.computations.either
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import memimg.journal.Journal
import mu.KotlinLogging

class MemoryImage<S>(private val journal: Journal<S>, emptySystem: () -> S) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val system: S = emptySystem()

    init {
        logger.info("Reading transactions")
        val transactionCount = journal.readTransactions().asSequence().fold(0) { index, transaction ->
            transaction.executeOn(system)
            index + 1
        }
        logger.info("Applied $transactionCount transactions")
    }

    // TODO Implement transactions for taking system snapshots for faster recovery
    fun <T> executeTransaction(transaction: Transaction<S, T>): Either<Throwable, T> =
            synchronized(this) {
                runBlocking {
                    either {
                        // TODO Restrict transactions to only use whitelisted classes / distrust!
                        val result = !Right(transaction.executeOn(system))
                        !Right(journal.writeTransaction(transaction))
                        result
                    }
                }
            }

    // TODO: How can queries be disallowed from mutating state state?
    // TODO: How can a query to see inconsistent results?
    fun <T> executeQuery(query: Command<S, T>): Either<Throwable, T> =
            runBlocking {
                var result: Either<Throwable, T>? = null
                val job = launch {
                    result = either {
                        !Right(query.executeOn(system))
                    }
                }
                job.join()
                result!!
            }
}



