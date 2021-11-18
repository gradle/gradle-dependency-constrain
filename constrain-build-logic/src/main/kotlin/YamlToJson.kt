import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

abstract class BaseYamlToJson : DefaultTask() {
    abstract val yamlFile: RegularFileProperty

    abstract val jsonFile: RegularFileProperty
}

abstract class YamlToJsonConverter : BaseYamlToJson() {
    init {
        description = "Convert YAML to JSON"
    }

    @get:InputFile
    abstract override val yamlFile: RegularFileProperty

    @get:OutputFile
    abstract override val jsonFile: RegularFileProperty

    @TaskAction
    fun convert() {
        val yamlMapper = ObjectMapper(YAMLFactory())
        val tree = yamlMapper.readTree(yamlFile.get().asFile)
        val jsonObjectWriter = createJsonObjectWriter()
        val theJsonFile = jsonFile.get().asFile
        jsonObjectWriter.writeValue(theJsonFile, tree)
        // Ensure that the file ends with a newline.
        theJsonFile.useLines { lines ->
            if (lines.last().isNotEmpty()) {
                theJsonFile.appendText("\n")
            }
        }
    }
}

/**
 * Validate that the YAML file is in sync with the JSON file.
 */
abstract class YamlToJsonChecker : BaseYamlToJson() {
    init {
        description = "Verify that the JSON file matches the YAML file"
    }

    @get:InputFile
    abstract override val yamlFile: RegularFileProperty

    @get:InputFile
    abstract override val jsonFile: RegularFileProperty

    @TaskAction
    fun check() {
        val yamlMapper = ObjectMapper(YAMLFactory())
        val tree = yamlMapper.readTree(yamlFile.get().asFile)
        val jsonObjectWriter = createJsonObjectWriter()
        var jsonConversionOutput = jsonObjectWriter.writeValueAsString(tree)
        if (jsonConversionOutput.lines().last().isNotEmpty()) {
            jsonConversionOutput += "\n"
        }
        val existingJsonText = jsonFile.get().asFile.readText()

        if (jsonConversionOutput != existingJsonText) {
            throw GradleException("The YAML file and the JSON file are not in sync. Execute `yamlToJson` task.")
        }
    }
}


private fun createJsonObjectWriter(): ObjectWriter {
    val prettyPrinter = DefaultPrettyPrinter()
    // This is super hacky, but it's the only way to get the formatting out of Jackson that we want.
    DefaultPrettyPrinter::class.java.getDeclaredField("_objectFieldValueSeparatorWithSpaces").apply {
        isAccessible = true
        set(prettyPrinter, ": ")
    }
    val indenter = DefaultIndenter()
    prettyPrinter.indentObjectsWith(indenter)
    prettyPrinter.indentArraysWith(indenter)
    return ObjectMapper().writer(prettyPrinter)
}
