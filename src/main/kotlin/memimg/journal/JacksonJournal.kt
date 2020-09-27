package memimg.journal

import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import memimg.Transaction
import memimg.util.PropertyHolder
import mu.KotlinLogging
import java.io.File
import java.io.FileWriter
import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets

interface JacksonJournal<S> : Journal<S> {

    val mapper: ObjectMapper
}

interface IOJacksonJournal<S> : Journal<S>, IOJournal<S>, JacksonJournal<S> {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun readTransactions(): Iterator<Transaction<S, *>> {
        val parser = mapper.createParser(reader)

        @Suppress("UNCHECKED_CAST")
        return mapper.readValues(parser, Transaction::class.java) as MappingIterator<Transaction<S, *>>
    }

    override fun writeTransaction(transaction: Transaction<S, *>) {
        logger.debug { "Writing transaction ${transaction::class.java.canonicalName}" }
        val serializedTransaction = mapper.writeValueAsString(transaction).trim() + "\n"
        writer.write(serializedTransaction)
        writer.flush()
    }
}

interface FileJournal<S> : Journal<S>, IOJournal<S> {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    val file: File

    override val reader: Reader
        get() = memimg.util.PropertyHolder(this, "reader") {
            logger.debug("Creating reader")
            file.reader(StandardCharsets.UTF_8)
        }

    override val writer: Writer
        get() = PropertyHolder(this, "writer") {
            logger.debug("Creating writer")
            FileWriter(file, StandardCharsets.UTF_8, true)
        }
}

class JacksonJsonFileJournal<S>(override val file: File) : FileJournal<S>, IOJacksonJournal<S> {

    override val mapper: ObjectMapper by lazy {
        ObjectMapper()
                .registerModule(KotlinModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTypingAsProperty( // Prepend ["@class":"memimg.RenameSoftwareProduct"]
                        DefaultBaseTypeLimitingValidator(), // TODO Transaction subtypes untrusted; use sandbox
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        "@class")
                .findAndRegisterModules()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.INDENT_OUTPUT)
    }
}

class JacksonYamlFileJournal<S>(override val file: File) : FileJournal<S>, IOJacksonJournal<S> {

    override val mapper: ObjectMapper by lazy {
        ObjectMapper(
                YAMLFactory()
                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
        )
                .registerModule(KotlinModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTypingAsProperty( // Prepend [--- !<somepkg.SomeTransaction>]
                        DefaultBaseTypeLimitingValidator(), // TODO Transaction subtypes untrusted; use sandbox
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        "@class") // Property name ignored for Yaml (--- !<somepkg.SomeTransaction>)
                .registerModule(JavaTimeModule())
                .findAndRegisterModules()

    }
}
