package memimg

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {

    val logger = LoggerFactory.getLogger("memimg.MainTestKt")

    val arguments =
            args
                    .map { it.trim() }
                    .filter { it.startsWith("--") }
                    .map { argument ->
                        val pos = argument.indexOf("=")
                        val argumentName = argument.substring(2, pos + 1)
                        val argumentValue =
                                if (pos >= 0) {
                                    argument.substring(pos + 1)
                                } else {
                                    "true"
                                }
                        argumentName to argumentValue
                    }
                    .toMap()

    val journalFilename = arguments["journal-file"] ?: "/tmp/scratch.yaml"
    val file = File(journalFilename).also { file ->
//        file.delete()
        file.exists() && file.createNewFile()
        require(file.isFile && file.canRead() && file.canWrite()) {
            "Inaccessible transaction journal file: ${file.absolutePath}"
        }
    }

    val memoryImage = MemoryImage(FileYamlStorage(file)) {
        Right<MutableMap<String, SoftwareProduct>>(mutableMapOf())
    }

    listOf(
            RemoveSoftwareProducts(setOf("snakeyml", "snakeyaml", "arrow", "yamltag", "jvmscripter")),
            AddSoftwareProduct("snakeyml", "java"),
            AddSoftwareProduct("arrow", "kotlin"),
            AddSoftwareProduct("jvmscripter", "java"),
            AddSoftwareProduct("yamltag", "kotlin"),
            RenameSoftwareProduct("snakeyml", "snakeyaml"),
            ChangeSoftwareProductLanguage("jvmscripter", "scala")
    )
            .forEach { transaction ->
                memoryImage.executeTransaction(transaction)
                        .map {
                            logger.debug("Software product resulting from ${transaction::class.java.simpleName}: $it")
                        }
                        .mapLeft {
                            logger.debug("Error: ${it.message ?: it.toString()}")
                            logger.debug("Error: ${it.message ?: it.toString()}")
                        }
            }
}

data class SoftwareProduct(val name: String, val language: String)

typealias SoftwareProductSystem = MutableMap<String, SoftwareProduct>

data class RemoveSoftwareProducts(val names: Set<String>)
    : Transaction<SoftwareProductSystem, List<SoftwareProduct>>() {

    override fun executeOn(system: SoftwareProductSystem): Either<Throwable, List<SoftwareProduct>> =
            try {
                val toBeRemoved = names.filter(system::containsKey).map { system[it]!! }
                system -= toBeRemoved.map { it.name }
                Right(toBeRemoved)
            } catch (throwable: Throwable) {
                Left(throwable)
            }
}

data class AddSoftwareProduct(val name: String, val language: String)
    : Transaction<SoftwareProductSystem, SoftwareProduct>() {

    override fun executeOn(system: SoftwareProductSystem): Either<Throwable, SoftwareProduct> =
            try {

                require(system[name] == null) {
                    "Can't create product with existing name: $name"
                }

                val newSoftwareProduct = SoftwareProduct(name, language)
                system[name] = newSoftwareProduct

                Right(newSoftwareProduct)
            } catch (throwable: Throwable) {
                Left(throwable)
            }
}

data class RenameSoftwareProduct(val name: String, val newName: String)
    : Transaction<SoftwareProductSystem, SoftwareProduct?>() {

    override fun executeOn(system: SoftwareProductSystem): Either<Throwable, SoftwareProduct?> =
            try {
                require(system[newName] == null) {
                    "Can't rename to existing product name: $newName"
                }
                val softwareProduct = system[name]
                if (softwareProduct != null) {
                    system -= name
                    system += newName to softwareProduct.copy(name = newName)
                }
                Right(system[newName])
            } catch (throwable: Throwable) {
                Left(throwable)
            }
}

data class ChangeSoftwareProductLanguage(val name: String,
                                         val newLanguage: String)
    : Transaction<SoftwareProductSystem, SoftwareProduct?>() {

    override fun executeOn(system: SoftwareProductSystem): Either<Throwable, SoftwareProduct?> =
            try {
                val softwareProduct = system[name]
                if (softwareProduct != null) {
                    system[name] = softwareProduct.copy(language = newLanguage)
                }
                Right(softwareProduct)
            } catch (throwable: Throwable) {
                Left(throwable)
            }
}
