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

import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.dependency.constrain.lib.serialize.ConstrainFileLoader;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ServiceScope(value = Scopes.Build.class)
public interface ConstrainService extends ConfigurationConstrainService {

    List<DependencyConstraint> getConstraints();

    /**
     * Creates a new {@link ConstrainService} which is a union between this and the passed {@link ConstrainService}.
     * Loading error will not be thrown by this method.
     * The returned {@link ConstrainService} will inherit loading exceptions from this and the other.
     */
    default ConstrainService union(ConstrainService other) {
        assert !(this instanceof AsyncConstrainService) :
            "The default implementation of ConstrainService.union must not be used for AsyncConstrainService";
        if (other instanceof AsyncConstrainService) {
            return new AsyncConstrainService(CompletableFuture.completedFuture(this)).union(other);
        }
        final List<DependencyConstraint> union =
            Stream
                .concat(getConstraints().stream(), other.getConstraints().stream())
                .collect(Collectors.toList());
        return new DefaultConstrainService(union);
    }

    /**
     * An empty {@link ConstrainService} useful for testing.
     */
    static ConstrainService empty() {
        return new DefaultConstrainService(Collections.emptyList());
    }


    @ServiceScope(value = Scopes.Build.class)
    interface Factory {

        ConstrainService create(DependencyConstraintFactory constraintFactory);

        /**
         * Loads the constraints from the constraints file and generates the constraints model.
         *
         * @param projectGradleDirectory The directory containing the constraints file.
         */
        static ConstrainService.Factory loadAndCreate(File projectGradleDirectory) {
            return new DefaultConstrainService.Factory(ConstrainFileLoader.loadConstraintsFromFile(projectGradleDirectory));
        }

        /**
         * Loads the constraints from the constraints file and generates the constraints model asynchronously.
         * Exceptions will be thrown by the calls to {@link ConstrainService} if they occur.
         */
        static ConstrainService.Factory loadAndCreateAsync(File projectGradleDirectory, Executor executor) {
            final CompletableFuture<Factory> factoryCompletableFuture =
                CompletableFuture.supplyAsync(() -> loadAndCreate(projectGradleDirectory), executor);
            return new AsyncConstrainService.Factory(factoryCompletableFuture);
        }
    }
}
