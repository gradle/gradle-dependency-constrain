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

import org.gradle.dependency.constrain.lib.DependencyConstrainException;
import org.gradle.dependency.constrain.lib.model.LoadedConstraint;
import org.gradle.dependency.constrain.lib.model.LoadedConstraints;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.WillClose;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.ADVISORY;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.BECAUSE;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.CONSTRAINT;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.CONSTRAINTS;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.GROUP;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.NAME;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.REJECT;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.REJECTED;
import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.SUGGESTED_VERSION;

public final class ConstraintsXmlReader {

    /**
     * Reads in the constraints from the passed in input stream.
     */
    public static LoadedConstraints readFromXml(@WillClose InputStream in) {
        LoadedConstraints.Builder builder = LoadedConstraints.builder();
        readFromXml(in, builder);
        return builder.build();
    }

    private static void readFromXml(
        @WillClose InputStream in, LoadedConstraints.Builder constraintsBuilder
    ) {
        try (InputStream inputStream = in) {
            SAXParser saxParser = createSecureParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            ConstraintsHandler handler = new ConstraintsHandler(constraintsBuilder);
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(inputStream));
        } catch (IOException
            | SAXException
            | ParserConfigurationException
            | DependencyConstrainException e) {
            throw new DependencyConstrainException("Unable to read dependency constraints", e);
        }
    }

    private static SAXParser createSecureParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/namespaces", false);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return spf.newSAXParser();
    }

    /**
     * Handler that parses the constraints.xml file building the {@link LoadedConstraints} via the
     * {@link LoadedConstraints.Builder}.
     */
    private static class ConstraintsHandler extends DefaultHandler2 {
        private final LoadedConstraints.Builder constraintsBuilder;
        private LoadedConstraint.Builder currentConstraintBuilder;
        private boolean inConstraints;
        private boolean inConstraint;
        private boolean inGroup;
        private boolean inName;
        private boolean inSuggestedVersion;
        private boolean inRejected;
        private boolean inReject;
        private boolean inBecause;
        private String currentGroup;
        private String currentName;
        private String currentSuggestedVersion;
        private String currentReject;
        private String currentBecause;

        ConstraintsHandler(LoadedConstraints.Builder constraintsBuilder) {
            this.constraintsBuilder = constraintsBuilder;
        }

        @Nonnull
        private static String createOrAppend(@Nullable String current, @Nonnull String append) {
            return current == null ? append : current + append;
        }

        private static void assertContext(boolean test, String innerTag, String outerTag) {
            assertContext(test, "<" + innerTag + "> must be found under the <" + outerTag + "> tag");
        }

        private static void assertContext(boolean test, String message) {
            if (!test) {
                throw new DependencyConstrainException("Invalid dependency constraints file: " + message);
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case CONSTRAINTS:
                    inConstraints = true;
                    break;
                case CONSTRAINT:
                    assertContext(inConstraints, CONSTRAINT, CONSTRAINTS);
                    inConstraint = true;
                    assert currentConstraintBuilder == null : "`currentConstraintBuilder` already defined";
                    currentConstraintBuilder = LoadedConstraint.builder();
                    break;
                case GROUP:
                    assertContext(inConstraint, GROUP, CONSTRAINT);
                    inGroup = true;
                    break;
                case NAME:
                    assertContext(inConstraint, NAME, CONSTRAINT);
                    inName = true;
                    break;
                case SUGGESTED_VERSION:
                    assertContext(inConstraint, SUGGESTED_VERSION, CONSTRAINT);
                    inSuggestedVersion = true;
                    break;
                case REJECTED:
                    assertContext(inConstraint, REJECTED, CONSTRAINT);
                    inRejected = true;
                    break;
                case REJECT:
                    assertContext(inRejected, REJECT, REJECTED);
                    inReject = true;
                    break;
                case BECAUSE:
                    assertContext(inConstraint, BECAUSE, CONSTRAINT);
                    maybeExtractAdvisory(attributes);
                    inBecause = true;
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case CONSTRAINTS:
                    inConstraints = false;
                    break;
                case CONSTRAINT:
                    inConstraint = false;
                    assertConstraintValid();
                    constraintsBuilder.addConstraint(currentConstraintBuilder.build());
                    currentConstraintBuilder = null;
                    break;
                case GROUP:
                    currentConstraintBuilder.group(currentGroup);
                    currentGroup = null;
                    inGroup = false;
                    break;
                case NAME:
                    currentConstraintBuilder.name(currentName);
                    currentName = null;
                    inName = false;
                    break;
                case SUGGESTED_VERSION:
                    currentConstraintBuilder.suggestedVersion(currentSuggestedVersion);
                    currentSuggestedVersion = null;
                    inSuggestedVersion = false;
                    break;
                case REJECTED:
                    inRejected = false;
                    break;
                case REJECT:
                    currentConstraintBuilder.addReject(currentReject);
                    inReject = false;
                    currentReject = null;
                    break;
                case BECAUSE:
                    currentConstraintBuilder.addBecause(currentBecause);
                    inBecause = false;
                    currentBecause = null;
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            // This method could be called multiple times for the same subset of a string
            if (inGroup) {
                currentGroup = createOrAppend(currentGroup, new String(ch, start, length));
            }
            if (inName) {
                currentName = createOrAppend(currentName, new String(ch, start, length));
            }
            if (inSuggestedVersion) {
                currentSuggestedVersion =
                    createOrAppend(currentSuggestedVersion, new String(ch, start, length));
            }
            if (inReject) {
                currentReject = createOrAppend(currentReject, new String(ch, start, length));
            }
            if (inBecause) {
                currentBecause = createOrAppend(currentBecause, new String(ch, start, length));
            }
        }

        /**
         * Asserts that the {@code <constraint>...</constraint>} element is well-formed.
         */
        private void assertConstraintValid() {
            assert currentConstraintBuilder != null : "`currentConstraintBuilder` not defined";
            assertContext(
                currentConstraintBuilder.isGroupSet(),
                String.format("<%s> tag must appear under the <%s> tag", GROUP, CONSTRAINT));
            assertContext(
                currentConstraintBuilder.isNameSet(),
                String.format("<%s> tag must appear under the <%s> tag", NAME, CONSTRAINT));
            assertContext(
                currentConstraintBuilder.isSuggestedVersionSet(),
                String.format("<%s> tag must appear under the <%s> tag", SUGGESTED_VERSION, CONSTRAINT));
            assertContext(
                currentConstraintBuilder.isBecauseSet(),
                String.format("<%s> tag must appear under the <%s> tag", BECAUSE, CONSTRAINT));
        }

        private void maybeExtractAdvisory(Attributes attributes) {
            String advisory = getNullableAttribute(attributes, ADVISORY);
            if (advisory != null) {
                if (currentBecause == null) {
                    currentBecause = advisory + ": ";
                } else {
                    currentBecause = advisory + ": " + currentBecause;
                }
            }
        }

        @Nullable
        private String getNullableAttribute(Attributes attributes, String name) {
            return attributes.getValue(name);
        }
    }
}
