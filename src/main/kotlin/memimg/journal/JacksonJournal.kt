package memimg.journal

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import memimg.Transaction
import mu.KotlinLogging
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.*
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY
import com.fasterxml.jackson.annotation.PropertyAccessor.FIELD
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

interface JacksonJournal<S> : Journal<S> {

    val mapper: ObjectMapper
}

interface IOJacksonJournal<S> : Journal<S>, IOJournal<S>, JacksonJournal<S> {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun readTransactions(): Iterator<Transaction<S, *>> {
        logger.info { "Reading transactions" }
        val parser = mapper.createParser(reader)
        @Suppress("UNCHECKED_CAST")
        return mapper.readValues(parser, Transaction::class.java) as Iterator<Transaction<S, *>>
    }

    override fun writeTransaction(transaction: Transaction<S, *>) {
        logger.debug { "Writing transaction ${transaction::class.java}" }
        val serializedTransaction = mapper.writeValueAsString(transaction).trim() + "\n"
        writer.write(serializedTransaction)
        writer.flush()
    }
}

abstract class FileJournal<S>(file: File) : Journal<S>, IOJournal<S> {

    override val reader: Reader by lazy {
        file.reader(StandardCharsets.UTF_8)
    }

    override val writer: Writer by lazy {
        val fileWriter = FileWriter(file, StandardCharsets.UTF_8, true)
        PrintWriter(fileWriter, true)
    }
}

class JacksonJsonFileJournal<S>(file: File) : FileJournal<S>(file), IOJacksonJournal<S> {

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

class JacksonYamlFileJournal<S>(file: File) : FileJournal<S>(file), IOJacksonJournal<S> {

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
