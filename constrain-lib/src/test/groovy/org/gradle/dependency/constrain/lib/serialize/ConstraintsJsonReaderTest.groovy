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

package org.gradle.dependency.constrain.lib.serialize

import org.gradle.dependency.constrain.lib.DependencyConstrainException
import org.gradle.dependency.constrain.lib.model.LoadedConstraint
import org.gradle.dependency.constrain.lib.model.LoadedConstraints
import org.intellij.lang.annotations.Language
import spock.lang.Specification

class ConstraintsJsonReaderTest extends Specification {

    LoadedConstraints loadedConstraints;

    List<LoadedConstraint> getConstraints() { loadedConstraints.constraints }

    def "able to load a file with an empty dependency constraints block"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [ ]
}
"""
        then:
        constraints.size() == 0
    }

    def "fails with error when version is missing"() {
        when:
        parse """
{
  "dependencyConstraints": []
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        ex.getMessage() == "Unable to read dependency constraints"
        ex.cause.getMessage() == """
Dependency constraints contains schema violations:
  - \$.version: is missing but it is required
""".trim()
    }

    def "fails with error when the input is empty"() {
        when:
        parse ""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause.getMessage() == "File is empty"
        }
    }

    def "fails with error when json object is empty"() {
        when:
        parse "{}"
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Dependency constraints contains schema violations:
  - \$.version: is missing but it is required
  - \$.dependencyConstraints: is missing but it is required
""".trim()
        }
    }

    def "fails with error when dependencyConstraints[0].because.reason is missing"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [
    {
      "group": "org.gradle",
      "name": "gradle-core",
      "suggestedVersion": "1.42",
      "rejectedVersions": [
        "1.41"
      ],
      "because": {
      }
    }
  ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Dependency constraints contains schema violations:
  - \$.dependencyConstraints[0].because.reason: is missing but it is required
""".trim()
        }

    }

    def "fails when version does not match 1.0.0"() {
        when:
        parse """
{
  "version": "0.9.0",
  "dependencyConstraints": [ ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == "Unsupported dependency constraints version: 0.9.0"
        }
    }

    def "fails with error when constraints aren't sorted lexicographically by group & name"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [
    {
      "group": "com.b",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    },
    {
      "group": "com.a",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    }
  ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Constrains were not sorted by group:name:suggestedVersion in lexicographical order:
  - Remove constraint at position 0:
    - {"group":"com.b", "name":"aaa", "suggestedVersion":"1.0.0"}
  - Insert constraint at position 2:
    - {"group":"com.b", "name":"aaa", "suggestedVersion":"1.0.0"}
""".trim()
        }
    }

    def "fails with error when constraints aren't sorted lexicographically by version"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [
    {
      "group": "com.a",
      "name": "aaa",
      "suggestedVersion": "1.0.1",
      "because": {
        "reason": "Reason"
      }
    },
    {
      "group": "com.a",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    }
  ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Constrains were not sorted by group:name:suggestedVersion in lexicographical order:
  - Remove constraint at position 0:
    - {"group":"com.a", "name":"aaa", "suggestedVersion":"1.0.1"}
  - Insert constraint at position 2:
    - {"group":"com.a", "name":"aaa", "suggestedVersion":"1.0.1"}
""".trim()
        }
    }

    def "fails with error when sequences of constraints aren't sorted lexicographically by group:name"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [
    {
      "group": "com.c",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    },
    {
      "group": "com.d",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    },
    {
      "group": "com.a",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    },
    {
      "group": "com.b",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    }
  ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Constrains were not sorted by group:name:suggestedVersion in lexicographical order:
  - Remove constraints at position 0 through 1:
    - {"group":"com.c", "name":"aaa", "suggestedVersion":"1.0.0"}
    - {"group":"com.d", "name":"aaa", "suggestedVersion":"1.0.0"}
  - Insert constraints at position 4 through 5:
    - {"group":"com.c", "name":"aaa", "suggestedVersion":"1.0.0"}
    - {"group":"com.d", "name":"aaa", "suggestedVersion":"1.0.0"}
""".trim()
        }
    }

    def "parsing input not formatting throws an error"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [{
      "group": "com.c",
          "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": { "reason": "Reason"
      }
    }
    ]
    }
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
The dependency constraints are not formatted correctly. Please apply this patch to fix the formatting:
  --- gradle/dependency-constraints.json
  +++ gradle/dependency-constraints.json
  @@ -3,1 +3,2 @@
  -  "dependencyConstraints": [{
  +  "dependencyConstraints": [
  +    {
  @@ -5,1 +6,1 @@
  -          "name": "aaa",
  +      "name": "aaa",
  @@ -7,1 +8,2 @@
  -      "because": { "reason": "Reason"
  +      "because": {
  +        "reason": "Reason"
  @@ -10,2 +12,2 @@
  -    ]
  -    }
  +  ]
  +}
""".trim()
        }
    }

    def "sorting errors are presented before formatting errors"() {
        when:
        parse """
{
  "version": "1.0.0",
  "dependencyConstraints": [
    {
      "group": "com.b",
      "name": "aaa",
      "suggestedVersion": "1.0.0",
      "because": {
        "reason": "Reason"
      }
    },
    { "group": "com.a", "name": "aaa", "suggestedVersion": "1.0.0", "because": { "reason": "Reason" } }
  ]
}
"""
        then:
        def ex = thrown(DependencyConstrainException)
        verifyAll(ex) {
            getMessage() == "Unable to read dependency constraints"
            cause instanceof DependencyConstrainException
            cause.getMessage() == """
Constrains were not sorted by group:name:suggestedVersion in lexicographical order:
  - Remove constraint at position 0:
    - {"group":"com.b", "name":"aaa", "suggestedVersion":"1.0.0"}
  - Insert constraint at position 2:
    - {"group":"com.b", "name":"aaa", "suggestedVersion":"1.0.0"}
""".trim()
        }
    }

    def "parsing succeeds when ending with trailing newline"() {
        when:
        def fileContents = [
                "{",
                "  \"version\": \"1.0.0\",",
                "  \"dependencyConstraints\": [ ]",
                "}",
                ""
        ]
        parseNoTrim(fileContents.join(System.lineSeparator()))
        then:
        constraints.isEmpty()
    }

    def "parse example-junit-CVE-2020-15250.json"() {
        when:
        parseExampleFile("examples/example-junit-CVE-2020-15250.json")
        then:
        constraints.size() == 1
        verifyAll(constraints) {
            verifyAll(get(0)) {
                group == "junit"
                name == "junit"
                objectNotation == [name: "junit", group: "junit"]
                suggestedVersion == "4.13.1"
                rejected.size() == 1
                rejected.get(0) == "[4.7,4.13]"
                because == "[CVE-2020-15250, GHSA-269g-pwp5-87pp]: TemporaryFolder on unix-like systems does not limit access to created files"
            }
        }
    }

    def "parse example-jetty-CVE-2020-27216.json"() {
        when:
        parseExampleFile("examples/example-jetty-CVE-2020-27216.json")
        then:
        constraints.size() == 2
        verifyAll(constraints) {
            verifyAll(get(0)) {
                group == "org.eclipse.jetty"
                name == "jetty-webapp"
                objectNotation == [group: "org.eclipse.jetty", name: "jetty-webapp"]
                suggestedVersion == "9.4.33.v20201020"
                rejected.size() == 3
                rejected.get(0) == "(,9.4.32.v20200930]"
                rejected.get(1) == "[10.0.0.a,10.0.0.beta2]"
                rejected.get(2) == "[11.0.0.a,11.0.0.beta2]"
                because == "[CVE-2020-27216, GHSA-g3wg-6mcf-8jj6]: Local Temp Directory Hijacking Vulnerability"
            }
            verifyAll(get(1)) {
                group == "org.mortbay.jetty"
                name == "jetty-webapp"
                objectNotation == [group: "org.mortbay.jetty", name: "jetty-webapp"]
                suggestedVersion == "9.4.33.v20201020"
                rejected.size() == 1
                rejected.get(0) == "(,9.4.32.v20200930]"
                because == "[CVE-2020-27216, GHSA-g3wg-6mcf-8jj6]: Local Temp Directory Hijacking Vulnerability"
            }
        }
    }

    private void parseExampleFile(String filePath) {
        loadedConstraints = ConstraintsJsonReader.readFromJson(getClass().getClassLoader().getResourceAsStream(filePath))
    }

    private void parse(@Language("json") String json) {
        parseNoTrim(json.trim())
    }

    private void parseNoTrim(@Language("json") String json) {
        loadedConstraints = ConstraintsJsonReader.readFromJson(new ByteArrayInputStream(json.getBytes("utf-8")))
    }
}
