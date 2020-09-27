package memimg.journal

import mu.KotlinLogging
import java.io.Reader
import java.io.Writer

interface IOJournal<S> : Journal<S>, AutoCloseable {

    val reader: Reader
    val writer: Writer

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun close() {

        try {
            if (reader.ready()) {
                logger.debug("Closing reader")
                reader.close()
            }
        } catch (e: Exception) {
            logger.warn("Error ${e::class.java.simpleName} closing journal reader: ${e.message ?: e.toString()}")
        }

        try {
            logger.debug("Closing writer")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            logger.warn("Error ${e::class.java.simpleName} closing journal writer: ${e.message ?: e.toString()}")
        }
    }
}
