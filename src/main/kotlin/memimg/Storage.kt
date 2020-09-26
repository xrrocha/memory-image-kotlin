package memimg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

interface Storage {
    fun <S> readTransactions(): Iterator<Transaction<S, *>>
    fun <S> write(transaction: Transaction<S, *>): Unit
}

class FileYamlStorage(private val file: File) : Storage {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val mapper: ObjectMapper =
            ObjectMapper(
                    YAMLFactory()
                            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            )
                    .registerModule(KotlinModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .activateDefaultTypingAsProperty(
                            DefaultBaseTypeLimitingValidator(), // TODO Transaction subtypes are untrusted
                            ObjectMapper.DefaultTyping.EVERYTHING,
                            "@class") // Property name ignored by Yaml: --- !<memimg.SomeTransaction>
                    .findAndRegisterModules()

    override fun <S> readTransactions(): Iterator<Transaction<S, *>> {
        val parser = mapper.createParser(file.reader())
        @Suppress("UNCHECKED_CAST")
        return mapper.readValues(parser, Transaction::class.java) as Iterator<Transaction<S, *>>
    }

    private val writer by lazy {
        val fileWriter = FileWriter(file, StandardCharsets.UTF_8, true)
        PrintWriter(fileWriter, true)
    }

    override fun <S> write(transaction: Transaction<S, *>): Unit {
        val yamlString = mapper.writeValueAsString(transaction).trim()
        writer.println(yamlString)
    }
}
