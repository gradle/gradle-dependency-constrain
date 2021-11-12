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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class ConstrainFileLoader {

    private static Optional<Document> loadConstraintsDocument(File projectGradleDirectory) {
        final File constraintsFile = new File(projectGradleDirectory, "constraints.xml");
        if (!constraintsFile.exists()) {
            return Optional.empty();
        }
        final DocumentBuilder documentBuilder = createDocumentBuilder();
        final Document document;
        try {
            document = documentBuilder.parse(constraintsFile);
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        document.getDocumentElement().normalize();
        return Optional.of(document);
    }

    private static DocumentBuilder createDocumentBuilder() {
        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // process XML securely, avoid attacks like XML External Entities (XXE)
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        // Create the DocumentBuilder
        try {
            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
