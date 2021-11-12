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

import static java.util.Objects.requireNonNull;

/**
 * Represents a constraint loaded from the constraints.xml file.
 */
public final class LoadedConstraint {
    private final String group;
    private final String name;
    private final String suggestedVersion;
    private final List<String> rejected;

    /**
     * Use {@link LoadedConstraint#builder()} to create an instance.
     */
    LoadedConstraint(String group, String name, String suggestedVersion, List<String> rejected) {
        this.group = group;
        this.name = name;
        this.suggestedVersion = suggestedVersion;
        this.rejected = rejected;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getSuggestedVersion() {
        return suggestedVersion;
    }

    public List<String> getRejected() {
        return rejected;
    }

    public static final class Builder {
        private String group;
        private String name;
        private String suggestedVersion;
        private final List<String> rejected = new ArrayList<>();

        private Builder() {
            // no-op
        }

        public Builder group(String group) {
            this.group = requireNonNull(group, "`group` must not be null");
            return this;
        }

        public Builder name(String name) {
            this.name = requireNonNull(name, "`name` must not be null");
            return this;
        }

        public Builder suggestedVersion(String suggestedVersion) {
            this.suggestedVersion = requireNonNull(suggestedVersion, "`suggestedVersion` must not be null");
            return this;
        }


        public Builder addReject(String rejected) {
            this.rejected.add(requireNonNull(rejected, "`rejected` must not be null"));
            return this;
        }

        public LoadedConstraint build() {
            return new LoadedConstraint(group,
                name,
                suggestedVersion,
                Collections.unmodifiableList(rejected)
            );
        }

    }

    public static Builder builder() {
        return new Builder();
    }
}
