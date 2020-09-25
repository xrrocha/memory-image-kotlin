package memimg

import arrow.core.Either
import kotlinx.serialization.Serializable

interface Command<S, A> {
    fun executeOn(system: S): Either<Throwable, A>
}

@Serializable
abstract class Transaction<S, A> : Command<S, A>

// Orthogonal interface for validating transaction prior to execution
interface Validatable<S> {
    fun validate(system: S): Unit
}

interface Query<S, A> : Command<S, A>
