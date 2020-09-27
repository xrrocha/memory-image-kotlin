package memimg

import memimg.journal.JacksonYamlFileJournal
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

    val journalFilename = arguments["journal-file"] ?: "scratch.yaml"
    val file = File(journalFilename).also { file ->
        file.exists() || file.createNewFile()
        require(file.isFile && file.canRead() && file.canWrite()) {
            "Inaccessible transaction journal file: ${file.absolutePath}"
        }
    }

    val memoryImage: MemoryImage<MutableMap<String, SoftwareProduct>> =
            MemoryImage(JacksonYamlFileJournal(file), ::mutableMapOf)

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
                            logger.info("Software product resulting from ${transaction::class.java.simpleName}: $it")
                        }
                        .mapLeft {
                            logger.warn("Error: ${it.message ?: it.toString()}")
                        }
            }
}

data class SoftwareProduct(val name: String, val language: String)

typealias System = MutableMap<String, SoftwareProduct>

data class RemoveSoftwareProducts(val names: Set<String>)
    : Transaction<System, List<SoftwareProduct>>() {

    override fun executeOn(system: System): List<SoftwareProduct> {
        val toBeRemoved = names.filter(system::containsKey).map { system[it]!! }
        system -= toBeRemoved.map { it.name }
        return toBeRemoved
    }
}

data class AddSoftwareProduct(val name: String, val language: String)
    : Transaction<System, SoftwareProduct>() {

    override fun executeOn(system: System): SoftwareProduct {

        require(!system.containsKey(name)) {
            "Can't create product with existing name: $name"
        }

        val newSoftwareProduct = SoftwareProduct(name, language)
        system[name] = newSoftwareProduct

        return newSoftwareProduct
    }
}

data class RenameSoftwareProduct(val name: String, val newName: String)
    : Transaction<System, SoftwareProduct?>() {

    init {
        require(name != newName) {
            "Can't rename to same existing name"
        }
    }

    override fun executeOn(system: System): SoftwareProduct? {

        require(!system.containsKey(newName)) {
            "Can't rename to existing product name: $newName"
        }

        return system[name].let { softwareProduct ->
            val newSoftwareProduct = softwareProduct!!.copy(name = newName)
            system -= name
            system += newName to newSoftwareProduct
            newSoftwareProduct
        }
    }
}

data class ChangeSoftwareProductLanguage(val name: String, val newLanguage: String)
    : Transaction<System, SoftwareProduct?>() {

    override fun executeOn(system: System): SoftwareProduct? =
            system[name].let { softwareProduct ->
                val newSoftwareProduct = softwareProduct!!.copy(language = newLanguage)
                system[name] = newSoftwareProduct
                newSoftwareProduct
            }
}
