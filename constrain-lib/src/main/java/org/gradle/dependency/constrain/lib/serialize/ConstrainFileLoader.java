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

package org.gradle.dependency.constrain.lib.serialize;

import org.gradle.api.UncheckedIOException;
import org.gradle.dependency.constrain.lib.DependencyConstrainException;
import org.gradle.dependency.constrain.lib.model.LoadedConstraints;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public final class ConstrainFileLoader {

    private ConstrainFileLoader() {
        // Utility class
    }

    /**
     * Loads the constraints model from the given directory. Wraps {@link
     * DependencyConstrainException} thrown with an error message indicating the file that caused the
     * error.
     */
    public static LoadedConstraints loadConstraintsFromFile(File projectGradleDirectory) {
        final File constraintsFile = new File(projectGradleDirectory, "constraints.xml");
        if (!constraintsFile.exists()) {
            return LoadedConstraints.empty();
        }
        try {
            return ConstraintsXmlReader.readFromXml(new FileInputStream(constraintsFile));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        } catch (DependencyConstrainException e) {
            // Propagate the exception but add the file name to the message
            throw new DependencyConstrainException(
                "Failed to load constraints from " + constraintsFile, e.getCause());
        }
    }
}
