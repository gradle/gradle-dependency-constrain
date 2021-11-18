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

package org.gradle.dependency.constrain.lib

import org.gradle.api.artifacts.DependencyConstraint
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.TempDir

import java.util.concurrent.Executor
import java.util.function.Supplier

class AsyncConstrainServiceTest extends Specification {
    private static Executor inThreadExecutor = { Runnable runnable -> runnable.run() }

    @TempDir
    @Shared
    File tempDirectory

    def setupSpec() {
        new File(tempDirectory, "dependency-constraints.json") << "{ \"version\": \"0.0.0\" }"
    }

    ConstrainService.Factory createFailingAsyncConstrainServiceFactory() {
        ConstrainService.Factory.loadAndCreateAsync(tempDirectory, inThreadExecutor)
    }

    void "loading errors are not thrown when loaded async until constraints are accessed"() {
        when:
        def factory = createFailingAsyncConstrainServiceFactory()
        then:
        noExceptionThrown()
        when:
        def constrainService = factory.create { Mock(DependencyConstraint) }
        then:
        noExceptionThrown()
        when:
        constrainService.constraints
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            message.startsWith("Failed to load constraints from ")
            cause instanceof DependencyConstrainException
            cause.message == """Dependency constraints contains schema violations:
  - \$.dependencyConstraints: is missing but it is required"""
        }
    }

    void "loading errors are not thrown during union when errors are present where #description"(
        Supplier<ConstrainService.Factory> supplierA,
        Supplier<ConstrainService.Factory> supplierB
    ) {
        when:
        def factoryA = supplierA.get()
        def factoryB = supplierB.get()
        then:
        noExceptionThrown()
        when:
        def constrainServiceA = factoryA.create { Mock(DependencyConstraint) }
        def constrainServiceB = factoryB.create { Mock(DependencyConstraint) }
        then:
        noExceptionThrown()
        when:
        def constrainServiceUnion = constrainServiceA.union(constrainServiceB)
        then:
        noExceptionThrown()
        when:
        constrainServiceUnion.constraints
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            message.startsWith("Failed to load constraints from ")
            cause instanceof DependencyConstrainException
            cause.message == """Dependency constraints contains schema violations:
  - \$.dependencyConstraints: is missing but it is required"""
        }
        where:
        description                 | supplierA                                       | supplierB
        "both async"                | { createFailingAsyncConstrainServiceFactory() } | { createFailingAsyncConstrainServiceFactory() }
        "first empty, second async" | { DefaultConstrainService.Factory.empty() }     | { createFailingAsyncConstrainServiceFactory() }
        "first async, second empty" | { createFailingAsyncConstrainServiceFactory() } | { DefaultConstrainService.Factory.empty() }
    }
}
