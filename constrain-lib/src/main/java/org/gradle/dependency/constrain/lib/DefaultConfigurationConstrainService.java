package org.gradle.dependency.constrain.lib;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;

import java.util.List;


class DefaultConfigurationConstrainService implements ConfigurationConstrainService {

    private final List<DependencyConstraint> constraints;

    DefaultConfigurationConstrainService(List<DependencyConstraint> constraints) {
        this.constraints = constraints;
        // DependencyGraphBuilder:syntheticDependenciesOf
    }

    @Override
    public void doConstrain(Configuration configuration) {
        configuration.getDependencyConstraints().addAll(constraints);
    }
}
