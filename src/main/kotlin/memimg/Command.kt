package memimg

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

interface Command<S, A> {
    fun executeOn(system: S): A
}

@Serializable
abstract class
Transaction<S, A> : Command<S, A> {
    // TODO Make OffsetDateTime work w/Jackson Yaml
//    @Contextual
//    val timestamp: OffsetDateTime = OffsetDateTime.now()
}

interface Query<S, A> : Command<S, A>
