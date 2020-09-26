package memimg

import kotlinx.serialization.Serializable

interface Command<S, A> {
    fun executeOn(system: S): A
}

@Serializable
abstract class Transaction<S, A> : Command<S, A>

interface Query<S, A> : Command<S, A>
