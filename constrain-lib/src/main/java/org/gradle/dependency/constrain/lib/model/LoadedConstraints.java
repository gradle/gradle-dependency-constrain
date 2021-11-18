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

package org.gradle.dependency.constrain.lib.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LoadedConstraints {
    private static final LoadedConstraints EMPTY = new LoadedConstraints(Collections.emptyList());

    private final List<LoadedConstraint> constraints;

    private LoadedConstraints(List<LoadedConstraint> constraints) {
        this.constraints = constraints;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static LoadedConstraints empty() {
        return EMPTY;
    }

    public List<LoadedConstraint> getConstraints() {
        return constraints;
    }

    public static final class Builder {
        private final List<LoadedConstraint> constraints = new ArrayList<>();

        private Builder() {
            // no-op
        }

        public void addConstraint(LoadedConstraint constraint) {
            constraints.add(constraint);
        }

        public LoadedConstraints build() {
            return new LoadedConstraints(constraints);
        }
    }
}
