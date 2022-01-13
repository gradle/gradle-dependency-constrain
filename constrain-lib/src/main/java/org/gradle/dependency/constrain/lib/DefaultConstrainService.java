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

package org.gradle.dependency.constrain.lib;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.dependency.constrain.lib.model.LoadedConstraint;
import org.gradle.dependency.constrain.lib.model.LoadedConstraints;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


class DefaultConstrainService implements ConstrainService {
    public final List<DependencyConstraint> constraints;

    DefaultConstrainService(List<DependencyConstraint> constraints) {
        this.constraints = constraints;
    }

    @Override
    public void doConstrain(Configuration configuration) {
        configuration.getDependencyConstraints().addAll(constraints);
    }

    @Override
    public final List<DependencyConstraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    static class Factory implements ConstrainService.Factory {
        private final LoadedConstraints loadedConstraints;

        Factory(LoadedConstraints loadedConstraints) {
            this.loadedConstraints = loadedConstraints;
        }

        @Override
        public ConstrainService create(DependencyConstraintFactory constraintFactory) {
            final List<DependencyConstraint> constraints =
                loadedConstraints.getConstraints().stream()
                    .map(loadedConstraint -> generateConstraint(constraintFactory, loadedConstraint))
                    .collect(Collectors.toList());
            return new DefaultConstrainService(constraints);
        }

        private DependencyConstraint generateConstraint(
            DependencyConstraintFactory constraintFactory, LoadedConstraint loadedConstraint
        ) {
            return constraintFactory.create(
                loadedConstraint.getObjectNotation(),
                gradleConstraint -> {
                    gradleConstraint.version(
                        gradleVersion -> {
                            gradleVersion.strictly(loadedConstraint.getSuggestedVersion());
                            //noinspection RedundantSuppression
                            //noinspection SimplifyStreamApiCallChains - `toArray` on collection is Java 10+ only
                            gradleVersion.reject(
                                loadedConstraint.getRejected().stream().toArray(String[]::new));
                        });
                    gradleConstraint.because(loadedConstraint.getBecause());
                });
        }

        /**
         * Used for testing.
         */
        static Factory empty() {
            return new Factory(LoadedConstraints.empty());
        }
    }
}
