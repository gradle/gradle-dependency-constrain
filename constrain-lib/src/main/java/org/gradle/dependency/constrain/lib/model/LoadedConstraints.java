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

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import org.gradle.dependency.constrain.lib.DependencyConstrainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        private final String LINE_SEPARATOR = System.lineSeparator();
        private final List<LoadedConstraint> constraints = new ArrayList<>();

        private Builder() {
            // no-op
        }

        public void addConstraint(LoadedConstraint constraint) {
            constraints.add(constraint);
        }

        private void ensureConstraintsSorted() {
            List<LoadedConstraint> sortedConstraints =
                constraints.stream().sorted(LoadedConstraint.GROUP_NAME_SUGGESTED_VERSION_COMPARATOR).collect(Collectors.toList());
            final Patch<LoadedConstraint> patch =
                DiffUtils.diff(constraints, sortedConstraints, LoadedConstraint.GROUP_NAME_SUGGESTED_VERSION_EQUALITY);
            if (!patch.getDeltas().isEmpty()) {
                String deltas =
                    patch
                        .getDeltas()
                        .stream()
                        .map(this::toConstraintSortErrorMessage)
                        .collect(Collectors.joining(LINE_SEPARATOR + "  - ", "  - ", ""));
                throw new DependencyConstrainException(
                    "Constrains were not sorted by group:name:suggestedVersion in lexicographical order:" + LINE_SEPARATOR + deltas
                );
            }
        }

        private String toConstraintSortErrorMessage(AbstractDelta<LoadedConstraint> delta) {
            final List<LoadedConstraint> lines;
            final String qualifier;
            switch (delta.getType()) {
                case DELETE:
                    qualifier = "Remove";
                    lines = delta.getSource().getLines();
                    break;
                case INSERT:
                    qualifier = "Insert";
                    lines = delta.getTarget().getLines();
                    break;
                default:
                    throw new IllegalStateException("Unexpected delta: " + delta);
            }
            final String constraintPluralized = lines.size() > 1 ? "constraints" : "constraint";
            String deltaFixMessage =
                qualifier + " " + constraintPluralized + " at position " + delta.getSource().getPosition();
            if (lines.size() > 1) {
                deltaFixMessage += " through " + (delta.getSource().getPosition() + lines.size() - 1);
            }
            final String linesMessage =
                lines
                    .stream()
                    .map(Builder::toJsonMapNotation)
                    .collect(Collectors.joining(LINE_SEPARATOR + "    - ", "    - ", ""));
            return deltaFixMessage + ':' + LINE_SEPARATOR + linesMessage;
        }

        private static String toJsonMapNotation(LoadedConstraint loadedConstraint) {
            return String.format("{\"group\":\"%s\", \"name\":\"%s\", \"suggestedVersion\":\"%s\"}",
                loadedConstraint.getGroup(),
                loadedConstraint.getName(),
                loadedConstraint.getSuggestedVersion()
            );
        }

        public LoadedConstraints build() {
            ensureConstraintsSorted();
            return new LoadedConstraints(constraints);
        }
    }
}
