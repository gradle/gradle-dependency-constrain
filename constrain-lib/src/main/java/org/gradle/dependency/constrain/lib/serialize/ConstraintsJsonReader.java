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

package org.gradle.dependency.constrain.lib.serialize;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.gradle.dependency.constrain.lib.DependencyConstrainException;
import org.gradle.dependency.constrain.lib.model.LoadedConstraint;
import org.gradle.dependency.constrain.lib.model.LoadedConstraints;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gradle.dependency.constrain.lib.serialize.ConstrainFileLoader.DEPENDENCY_CONSTRAINTS_FILE_PATH_RELATIVE;

/**
 * Loads the {@link LoadedConstraints} from an input stream of JSON.
 */
public class ConstraintsJsonReader {
    private static final String CONSTRAINTS_SPECIFICATION = "schema/dependency-constraints-schema.json";

    /**
     * Parses the JSON from the given input stream and returns the {@link LoadedConstraints}.
     */
    public static LoadedConstraints readFromJson(@WillClose InputStream in) {
        try {
            return doReadFromJson(in);
        } catch (Exception ex) {
            throw new DependencyConstrainException("Unable to read dependency constraints", ex);
        }
    }

    /**
     * Performs the primary processing sequence for reading in and converting the JSON.
     */
    private static LoadedConstraints doReadFromJson(@WillClose InputStream in) {
        final JsonSchema schema = createSchemaValidator();
        final ObjectMapper mapper = createObjectMapper();
        // 1. Read the JSON input creating various intermediate object formats to be used later.
        final InputStreamExtractedData extractedData = InputStreamExtractedData.create(mapper, in);
        final JsonNode json = extractedData.json;
        final List<String> inputLines = extractedData.inputLines;
        // 2. Validate the JSON against the schema
        validateAgainstJsonSchema(schema, json);
        // 3. Convert the JSON to a JsonDependencyConstraints format.
        final JsonDependencyConstraints constraints = readJsonDependencyConstraints(mapper, json);
        // 4. Build the LoadedConstraints object, performing any additional validation.
        final LoadedConstraints loadedConstraints = buildLoadedConstraints(constraints);
        // 5. Generate a version of the input that is correctly formatted.
        final List<String> formattedJson = generateFormattedJson(mapper, json);
        // 6. Verify that the formatted JSON is the same as the input.
        verifyNoFormattingDifferences(inputLines, formattedJson);
        return loadedConstraints;
    }

    private static class InputStreamExtractedData {
        private final JsonNode json;
        private final List<String> inputLines;

        private InputStreamExtractedData(JsonNode json, List<String> inputLines) {
            this.json = json;
            this.inputLines = inputLines;
        }

        public static InputStreamExtractedData create(ObjectMapper mapper, @WillClose InputStream in) {
            final JsonNode json;
            final List<String> inputLines;
            try (BufferedReader useIn = new BufferedReader(new InputStreamReader(in))) {
                useIn.mark(1 << 24); // Mark the stream so we can reset it later
                // StreamReadFeature.AUTO_CLOSE_SOURCE was disabled when creating the ObjectFactory
                json = mapper.readTree(useIn);
                useIn.reset();
                inputLines = useIn.lines().collect(Collectors.toList());
                if (inputLines.isEmpty()) {
                    throw new DependencyConstrainException("File is empty");
                }
            } catch (IOException ex) {
                throw new DependencyConstrainException("Unable to read dependency constraints", ex);
            }
            return new InputStreamExtractedData(json, inputLines);
        }
    }

    private static List<String> generateFormattedJson(ObjectMapper objectMapper, JsonNode json) {
        final DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        monkeyPatchDefaultPrettyPrinter(prettyPrinter);
        final DefaultIndenter indenter = new DefaultIndenter();
        prettyPrinter.indentArraysWith(indenter);
        prettyPrinter.indentObjectsWith(indenter);
        final ObjectWriter writer = objectMapper.writer(prettyPrinter);
        final String output;
        try {
            output = writer.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new DependencyConstrainException(
                "Unable to generate formatted JSON for dependency constraints",
                e
            );
        }
        return Arrays.asList(output.split(System.lineSeparator()));
    }

    /**
     * Verifies that the input matches exactly the formatting that we expect.
     * Will generate an exception containing the diff required to make the input match the expected format.
     */
    private static void verifyNoFormattingDifferences(List<String> input, List<String> formatted) {
        final Patch<String> patch = DiffUtils.diff(input, formatted);
        if (!patch.getDeltas().isEmpty()) {
            final List<String> strings = UnifiedDiffUtils.generateUnifiedDiff(
                DEPENDENCY_CONSTRAINTS_FILE_PATH_RELATIVE,
                DEPENDENCY_CONSTRAINTS_FILE_PATH_RELATIVE,
                input,
                patch,
                0
            );
            final String newline = System.lineSeparator();
            final String fixDiffToApply =
                strings
                    .stream()
                    .collect(Collectors.joining(newline + "  ", "  ", ""));
            throw new DependencyConstrainException(
                "The dependency constraints are not formatted correctly. Please apply this patch to fix the formatting:" +
                    newline +
                    fixDiffToApply
            );
        }
    }

    private static void monkeyPatchDefaultPrettyPrinter(DefaultPrettyPrinter prettyPrinter) {
        try {
            final Field objectFieldValueSeparatorWithSpaces =
                DefaultPrettyPrinter.class.getDeclaredField("_objectFieldValueSeparatorWithSpaces");
            objectFieldValueSeparatorWithSpaces.setAccessible(true);
            objectFieldValueSeparatorWithSpaces.set(prettyPrinter, ": ");
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(
                "Unable to find field `_objectFieldValueSeparatorWithSpaces` in DefaultPrettyPrinter",
                e
            );
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                "Unable to access field `_objectFieldValueSeparatorWithSpaces` in DefaultPrettyPrinter",
                e
            );
        }
    }

    private static void validateAgainstJsonSchema(JsonSchema schema, JsonNode json) {
        final Set<ValidationMessage> validationMessages = schema.validate(json);
        if (!validationMessages.isEmpty()) {
            final String newline = System.lineSeparator();
            final String violationMessage =
                validationMessages
                    .stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining(newline + "  - ", "  - ", ""));
            throw new DependencyConstrainException("Dependency constraints contains schema violations:" + newline + violationMessage);
        }
    }

    private static JsonDependencyConstraints readJsonDependencyConstraints(ObjectMapper mapper, JsonNode json) {
        final ObjectReader objectReader = mapper.readerFor(JsonDependencyConstraints.class);
        try {
            return objectReader.readValue(json);
        } catch (IOException ex) {
            throw new DependencyConstrainException("Unable to read dependency constraints", ex);
        }
    }

    private static LoadedConstraints buildLoadedConstraints(JsonDependencyConstraints constraints) {
        if (!"1.0.0".equals(constraints.version)) {
            throw new DependencyConstrainException("Unsupported dependency constraints version: " + constraints.version);
        }
        final LoadedConstraints.Builder builder = LoadedConstraints.builder();
        constraints.dependencyConstraints.forEach(constraint -> {
            final String because =
                constraint
                    .because
                    .advisoryIdentifiers
                    .map(identifiers ->
                        identifiers
                            .stream()
                            .collect(Collectors.joining(", ", "[", "]: "))
                    ).orElse("") +
                    constraint.because.reason;
            final LoadedConstraint.Builder constraintBuilder =
                LoadedConstraint
                    .builder()
                    .group(constraint.group)
                    .name(constraint.name)
                    .suggestedVersion(constraint.suggestedVersion)
                    .because(because);
            constraint.rejectedVersions.ifPresent(rejections -> rejections.forEach(constraintBuilder::addReject));
            builder.addConstraint(constraintBuilder.build());
        });
        return builder.build();
    }

    private static JsonSchema createSchemaValidator() {
        final JsonSchemaFactory factory =
            JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909)).build();
        try {
            return factory
                .getSchema(
                    ConstraintsJsonReader.class.getClassLoader().getResourceAsStream(CONSTRAINTS_SPECIFICATION)
                );
        } catch (JsonSchemaException ex) {
            throw new DependencyConstrainException(
                "Unable to load dependency constraints schema (resource: " + CONSTRAINTS_SPECIFICATION + ")", ex);
        }
    }

    static final class JsonDependencyConstraints {
        private final String version;
        private final List<JsonDependencyConstraint> dependencyConstraints;

        @JsonCreator
        JsonDependencyConstraints(
            String version,
            List<JsonDependencyConstraint> dependencyConstraints
        ) {
            this.version = version;
            this.dependencyConstraints = dependencyConstraints;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static final class JsonDependencyConstraint {
        final String group;
        final String name;
        final String suggestedVersion;
        final Optional<List<String>> rejectedVersions;
        final JsonBecause because;

        @JsonCreator
        JsonDependencyConstraint(
            String group,
            String name,
            String suggestedVersion,
            @Nullable List<String> rejectedVersions,
            JsonBecause because
        ) {
            this.group = group;
            this.name = name;
            this.suggestedVersion = suggestedVersion;
            this.rejectedVersions = Optional.ofNullable(rejectedVersions);
            this.because = because;
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static final class JsonBecause {
        final Optional<List<String>> advisoryIdentifiers;
        final Optional<List<String>> moreInformationUrls;
        final String reason;

        @JsonCreator
        JsonBecause(
            @Nullable List<String> advisoryIdentifiers,
            @Nullable List<String> moreInformationUrls,
            String reason
        ) {
            this.advisoryIdentifiers = Optional.ofNullable(advisoryIdentifiers);
            this.moreInformationUrls = Optional.ofNullable(moreInformationUrls);
            this.reason = reason;
        }
    }


    private static ObjectMapper createObjectMapper() {
        final JsonFactory factory =
            JsonFactory
                .builder()
                .disable(StreamReadFeature.AUTO_CLOSE_SOURCE)
                .build();
        final ObjectMapper mapper = new ObjectMapper(factory)
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        mapper.registerSubtypes(
            JsonDependencyConstraints.class,
            JsonDependencyConstraint.class,
            JsonBecause.class
        );
        return mapper;
    }
}
