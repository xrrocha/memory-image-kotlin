package memimg.journal

import memimg.Transaction

interface Journal<S> {
    fun readTransactions(): Iterator<Transaction<S, *>>
    fun writeTransaction(transaction: Transaction<S, *>): Unit
}
