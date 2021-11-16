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
import org.gradle.dependency.constrain.lib.serialize.ConstrainFileLoader;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ServiceScope(value = Scopes.Build.class)
public class ConstrainService implements ConfigurationConstrainService {
    public final List<DependencyConstraint> constraints;

    private ConstrainService(List<DependencyConstraint> constraints) {
        this.constraints = constraints;
    }

    @Override
    public void doConstrain(Configuration configuration) {
        configuration.getDependencyConstraints().addAll(constraints);
    }

    public final List<DependencyConstraint> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    @ServiceScope(value = Scopes.BuildTree.class)
    public static final class Factory {

        private final LoadedConstraints loadedConstraints;

        private Factory(LoadedConstraints loadedConstraints) {
            this.loadedConstraints = loadedConstraints;
        }

        public ConstrainService create(DependencyConstraintFactory constraintFactory) {
            final List<DependencyConstraint> constraints =
                loadedConstraints
                    .getConstraints()
                    .stream()
                    .map(loadedConstraint -> generateConstraint(constraintFactory, loadedConstraint))
                    .collect(Collectors.toList());
            return new ConstrainService(constraints);
        }

        private DependencyConstraint generateConstraint(DependencyConstraintFactory constraintFactory, LoadedConstraint loadedConstraint) {
            return constraintFactory.create(loadedConstraint.getObjectNotation(), gradleConstraint -> {
                gradleConstraint.version(gradleVersion -> {
                    gradleVersion.strictly(loadedConstraint.getSuggestedVersion());
                    //noinspection RedundantSuppression
                    //noinspection SimplifyStreamApiCallChains - Can't simplify as `toArray` on collection is Java 10+ only
                    gradleVersion.reject(loadedConstraint.getRejected().stream().toArray(String[]::new));
                });
                gradleConstraint.because(loadedConstraint.getBecause());
            });
        }


        /**
         * Loads the constraints from the constraints file and generates the constraints model.
         *
         * @param projectGradleDirectory The directory containing the constraints file.
         */
        public static ConstrainService.Factory loadAndCreate(File projectGradleDirectory) {
            return new Factory(ConstrainFileLoader.loadConstraintsFromFile(projectGradleDirectory));
        }
    }

    /**
     * An empty {@link ConstrainService} useful for testing.
     */
    public static ConstrainService empty() {
        return new ConstrainService(Collections.emptyList());
    }
}
