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

package org.gradle.dependency.constrain.lib.serializer;


import org.gradle.dependency.constrain.lib.DependencyConstrainException;
import org.gradle.dependency.constrain.lib.model.LoadedConstraint;
import org.gradle.dependency.constrain.lib.model.LoadedConstraints;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;

import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.CONSTRAINT;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.CONSTRAINTS;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.GROUP;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.NAME;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.REJECT;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.REJECTED;
import static org.gradle.dependency.constrain.lib.serializer.ConstraintsXmlTags.SUGGESTED_VERSION;

public final class ConstraintsXmlReader {

    private static void readFromXml(InputStream in, LoadedConstraints.Builder constraintsBuilder) {
        try (InputStream inputStream = in) {
            SAXParser saxParser = createSecureParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            ConstraintsHandler handler = new ConstraintsHandler(constraintsBuilder);
            xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            xmlReader.setContentHandler(handler);
            xmlReader.parse(new InputSource(inputStream));
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new DependencyConstrainException("Unable to read dependency constraints", e);
        }
    }

    public static LoadedConstraints readFromXml(InputStream in) {
        LoadedConstraints.Builder builder = new LoadedConstraints.Builder();
        readFromXml(in, builder);
        return builder.build();
    }

    private static SAXParser createSecureParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/namespaces", false);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return spf.newSAXParser();
    }

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
        private String currentReject;


        ConstraintsHandler(LoadedConstraints.Builder constraintsBuilder) {
            this.constraintsBuilder = constraintsBuilder;
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
                    constraintsBuilder.addConstraint(currentConstraintBuilder.build());
                    currentConstraintBuilder = null;
                    break;
                case GROUP:
                    inGroup = false;
                    break;
                case NAME:
                    inName = false;
                    break;
                case SUGGESTED_VERSION:
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
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            // This method could be called multiple times for the same subset of a string
            if (inGroup) {
                currentConstraintBuilder.group(new String(ch, start, length));
            }
            if (inName) {
                currentConstraintBuilder.name(new String(ch, start, length));
            }
            if (inSuggestedVersion) {
                currentConstraintBuilder.suggestedVersion(new String(ch, start, length));
            }
            if (inReject) {
                if (currentReject == null) {
                    currentReject = new String(ch, start, length);
                } else {
                    currentReject += new String(ch, start, length);
                }
            }
        }

        private static void assertContext(boolean test, String innerTag, String outerTag) {
            assertContext(test, "<" + innerTag + "> must be found under the <" + outerTag + "> tag");
        }

        private static void assertContext(boolean test, String message) {
            if (!test) {
                throw new DependencyConstrainException("Invalid dependency constraints file: " + message);
            }
        }
    }

}
