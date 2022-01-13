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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import static java.util.Objects.requireNonNull;

/**
 * Represents a constraint loaded from the constraints.xml file.
 */
public final class LoadedConstraint {
    public static final Comparator<LoadedConstraint> GROUP_NAME_SUGGESTED_VERSION_COMPARATOR =
        Comparator.comparing(LoadedConstraint::getGroupNameSuggestedVersion);
    public static final BiPredicate<LoadedConstraint, LoadedConstraint> GROUP_NAME_SUGGESTED_VERSION_EQUALITY =
        (a, b) -> a.getGroupNameSuggestedVersion().equals(b.getGroupNameSuggestedVersion());
    private final String group;
    private final String name;
    private final String suggestedVersion;
    private final List<String> rejected;
    private final String because;

    /**
     * Use {@link LoadedConstraint#builder()} to create an instance.
     */
    LoadedConstraint(
        String group, String name, String suggestedVersion, List<String> rejected, String because
    ) {
        this.group = requireNonNull(group, "`group` must not be null");
        this.name = requireNonNull(name, "`name` must not be null");
        this.suggestedVersion = requireNonNull(suggestedVersion, "`suggestedVersion` must not be null");
        this.rejected = requireNonNull(rejected, "`rejected` must not be null");
        this.because = requireNonNull(because, "`because` must not be null");
    }

    public static Builder builder() {
        return new Builder();
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

    public String getBecause() {
        return because;
    }

    public Map<String, String> getObjectNotation() {
        final Map<String, String> objectNotation = new HashMap<>(2);
        objectNotation.put("group", group);
        objectNotation.put("name", name);
        return Collections.unmodifiableMap(objectNotation);
    }

    private String getGroupNameSuggestedVersion() {
        return group + ":" + name + ":" + suggestedVersion;
    }

    public static final class Builder {
        private final List<String> rejected = new ArrayList<>();
        private String group;
        private String name;
        private String suggestedVersion;
        private String because;

        private Builder() {
            // no-op
        }

        public Builder group(String group) {
            this.group = requireNonNull(group, "`group` must not be null");
            return this;
        }

        public boolean isGroupSet() {
            return group != null;
        }

        public Builder name(String name) {
            this.name = requireNonNull(name, "`name` must not be null");
            return this;
        }

        public boolean isNameSet() {
            return name != null;
        }

        public Builder suggestedVersion(String suggestedVersion) {
            this.suggestedVersion =
                requireNonNull(suggestedVersion, "`suggestedVersion` must not be null");
            return this;
        }

        public boolean isSuggestedVersionSet() {
            return suggestedVersion != null;
        }

        public Builder addReject(String rejected) {
            this.rejected.add(requireNonNull(rejected, "`rejected` must not be null"));
            return this;
        }

        public Builder because(String because) {
            this.because = requireNonNull(because, "`because` must not be null");
            return this;
        }

        public boolean isBecauseSet() {
            return because != null;
        }

        public LoadedConstraint build() {
            return new LoadedConstraint(
                group, name, suggestedVersion, Collections.unmodifiableList(rejected), because);
        }
    }
}
