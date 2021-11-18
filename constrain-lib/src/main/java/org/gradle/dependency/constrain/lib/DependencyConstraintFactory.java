package org.gradle.dependency.constrain.lib;

import org.gradle.api.Action;
import org.gradle.api.artifacts.DependencyConstraint;

/**
 * Factory for creating {@link DependencyConstraint}.
 */
@FunctionalInterface
public interface DependencyConstraintFactory {

  DependencyConstraint create(
      Object dependencyNotation, Action<? super DependencyConstraint> configureAction);
}
