package org.gradle.dependency.constrain.lib;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.DependencyConstraint;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies constraints from an XML file to a configuration.
 */
public interface ConfigurationConstrainService {

    /**
     * Applies all constraints from the constraint XML file to the passed configuration.
     */
    void doConstrain(Configuration configuration);

    /**
     * Applies all constraints from the constraint XML file to all configurations within the passed configuration container.
     */
    default void doConstrain(ConfigurationContainer configurations) {
        configurations.configureEach(this::doConstrain);
    }
}
