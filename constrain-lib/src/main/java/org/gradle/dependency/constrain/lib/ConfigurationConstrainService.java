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
//        DependencyConstraint constraint = handler.getConstraints().create("junit:junit", constrain -> {
//            constrain.version(version -> {
//                version.require("4.13.1");
//                version.reject("[4.7,4.13]");
//            });
//            constrain.because("CVE-2020-15250: TemporaryFolder on unix-like systems does not limit access to created files");
//        });
        configurations.configureEach(this::doConstrain);
    }

    final class Noop implements ConfigurationConstrainService {

        private Noop() {
            // no-op
        }

        @Override
        public void doConstrain(Configuration configuration) {
            // no-op
        }
    }

    /**
     * Creates a {@link ConfigurationConstrainService}.
     */
    abstract class Factory {
        @Inject
        public abstract Logger getLogger();

        public ConfigurationConstrainService create(
                final File projectGradleDirectory,
                final DependencyConstraintFactory constraintFactory
        ) {
//            return loadConstraintsDocument(projectGradleDirectory)
//                    .map(constraintsDocument -> createConstraints(constraintsDocument, constraintFactory))
//                    .<ConfigurationConstrainService>map(DefaultConfigurationConstrainService::new)
//                    .orElseGet(Noop::new);
            return null;
        }

        private List<DependencyConstraint> createConstraints(
                final Document constraintsDocument,
                final DependencyConstraintFactory constraintFactory
        ) {
            NodeList xmlConstraints = constraintsDocument.getElementsByTagName("constraint");
            List<DependencyConstraint> constraints = new ArrayList<>(xmlConstraints.getLength());
            for (int i = 0; i < xmlConstraints.getLength(); i++) {
                Node node = xmlConstraints.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String group = element.getElementsByTagName("group").item(0).getTextContent();
                    String name = element.getElementsByTagName("name").item(0).getTextContent();
                    String require = element.getElementsByTagName("lowest-known-safe").item(0).getTextContent();
                    String reject = element.getElementsByTagName("reject").item(0).getTextContent();
                    String because = element.getElementsByTagName("because").item(0).getTextContent();
                    constraints.add(constraintFactory.create(group + ":" + name, constraint -> {
                        constraint.version(version -> {
                            version.require(require);
                            version.reject(reject);
                        });
                        constraint.because(because);
                    }));
                } else {
                    throw new RuntimeException("Unexpected node type: " + node.getNodeType());
                }
            }
            return constraints;
        }
    }
}
