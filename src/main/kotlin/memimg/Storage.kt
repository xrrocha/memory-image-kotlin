package memimg

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.jsontype.DefaultBaseTypeLimitingValidator
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

interface Storage {
    fun read(): Either<Throwable, Transaction<*, *>?>
    fun write(transaction: Transaction<*, *>): Either<Throwable, Unit>
}

class FileYamlStorage(private val file: File) : Storage {

    companion object {
        private val logger = LoggerFactory.getLogger(FileYamlStorage::class.java)
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

    private val mappingIterator by lazy {
        val parser = mapper.createParser(file.reader())
        mapper.readValues(parser, Transaction::class.java)
    }

    override fun read(): Either<Throwable, Transaction<*, *>?> =
            try {
                val result =
                        if (mappingIterator.hasNext()) {
                            mappingIterator.next()
                        } else {
                            null
                        }
                Right(result)
            } catch (throwable: Throwable) {
                Left(throwable)
            }

    private val writer by lazy {
        val fileWriter = FileWriter(file, StandardCharsets.UTF_8, true)
        PrintWriter(fileWriter, true)
    }

    override fun write(transaction: Transaction<*, *>): Either<Throwable, Unit> =
            try {
                val yamlString = mapper.writeValueAsString(transaction).trim()
                Right(writer.println(yamlString))
            } catch (throwable: Throwable) {
                Left(throwable)
            }
}
