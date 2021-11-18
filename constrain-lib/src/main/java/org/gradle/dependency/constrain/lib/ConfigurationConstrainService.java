package org.gradle.dependency.constrain.lib;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;

/**
 * Applies constraints from an XML file to a configuration.
 */
public interface ConfigurationConstrainService {

  /**
   * Applies all constraints from the constraint XML file to the passed configuration.
   */
  void doConstrain(Configuration configuration);

  /**
   * Applies all constraints from the constraint XML file to all configurations within the passed
   * configuration container.
   */
  default void doConstrain(ConfigurationContainer configurations) {
    configurations.configureEach(this::doConstrain);
  }
}
