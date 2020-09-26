package memimg

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

class MemoryImage<S>(private val storage: Storage, emptySystem: () -> S) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val system: S = emptySystem()

    init {
        storage.readTransactions<S>().forEach { transaction ->
            transaction.executeOn(system)
        }
    }

    // TODO Implement transactions for taking system snapshots for faster recovery
    fun <T> executeTransaction(transaction: Transaction<S, T>): Result<T> =
            synchronized(this) {
                try {
                    val result = transaction.executeOn(system)
                    storage.write(transaction)
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



