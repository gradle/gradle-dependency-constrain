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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

class AsyncConstrainService implements ConstrainService {
    private final CompletableFuture<ConstrainService> futureConstrainService;

    AsyncConstrainService(CompletableFuture<ConstrainService> futureConstrainService) {
        this.futureConstrainService = futureConstrainService;
    }

    private ConstrainService joinToLoadConstrainService() {
        try {
            return futureConstrainService.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof DependencyConstrainException) {
                cause.addSuppressed(e);
                throw (DependencyConstrainException) cause;
            }
            throw e;
        }
    }

    @Override
    public void doConstrain(Configuration configuration) {
        joinToLoadConstrainService().doConstrain(configuration);
    }

    @Override
    public List<DependencyConstraint> getConstraints() {
        return joinToLoadConstrainService().getConstraints();
    }

    @Override
    public ConstrainService union(ConstrainService other) {
        final CompletableFuture<ConstrainService> otherFutureConstrainService;
        if (other instanceof AsyncConstrainService) {
            otherFutureConstrainService = ((AsyncConstrainService) other).futureConstrainService;
        } else {
            otherFutureConstrainService = CompletableFuture.completedFuture(other);
        }
        return new AsyncConstrainService(
            futureConstrainService.thenCombine(otherFutureConstrainService, ConstrainService::union)
        );
    }

    static class Factory implements ConstrainService.Factory {
        private final CompletableFuture<ConstrainService.Factory> futureConstrainServiceFactory;

        Factory(CompletableFuture<ConstrainService.Factory> futureConstrainServiceFactory) {
            this.futureConstrainServiceFactory = futureConstrainServiceFactory;
        }

        @Override
        public ConstrainService create(DependencyConstraintFactory constraintFactory) {
            return new AsyncConstrainService(
                futureConstrainServiceFactory.thenApply(factory -> factory.create(constraintFactory))
            );
        }
    }
}
