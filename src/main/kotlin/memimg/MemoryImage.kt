package memimg

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
        journal.readTransactions().forEach { transaction ->
            transaction.executeOn(system)
        }
    }

    // TODO Implement transactions for taking system snapshots for faster recovery
    fun <T> executeTransaction(transaction: Transaction<S, T>): Result<T> =
            synchronized(this) {
                try {
                    val result = transaction.executeOn(system)
                    journal.writeTransaction(transaction)
                    Result.success(result)
                } catch (throwable: Throwable) {
                    Result.failure(throwable)
                }
            }

    // TODO: How to ensure a query doesn't mutate state?
    // TODO: How can a query to see inconsistent results?
    fun <T> executeQuery(query: Command<S, T>): Result<T> =
            runBlocking {
                var result: Result<T>? = null
                val job = launch {
                    result =
                            try {
                                Result.success(query.executeOn(system))
                            } catch (throwable: Throwable) {
                                Result.failure(throwable)
                            }
                }
                job.join()
                result!!
            }
}



