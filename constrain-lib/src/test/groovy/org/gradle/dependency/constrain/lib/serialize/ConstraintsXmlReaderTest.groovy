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

import static org.gradle.dependency.constrain.lib.serialize.ConstraintsXmlTags.*

class ConstraintsXmlReaderTest extends Specification {

    LoadedConstraints loadedConstraints;

    List<LoadedConstraint> getConstraints() { loadedConstraints.constraints }

    def "should read in empty constraints xml file"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
</constraints>
"""
        then:
        constraints.size() == 0
    }

    def "should read in constraints xml file"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <group>org.gradle</group>
        <name>gradle-core</name>
        <suggested-version>1.0</suggested-version>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
        <because>Reason</because>
    </constraint>
</constraints>
"""
        then:
        constraints.size() == 1
        constraints[0].group == "org.gradle"
        constraints[0].name == "gradle-core"
        constraints[0].suggestedVersion == "1.0"
        constraints[0].rejected.size() == 1
        constraints[0].rejected[0] == "[0.2,0.8]"
        constraints[0].because == "Reason"
    }

    def "should read in constraints xml file with multiple constraints"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <group>org.a</group>
        <name>a-core</name>
        <suggested-version>1.0</suggested-version>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
        <because>A reason</because>
    </constraint>
    <constraint>
        <group>org.b</group>
        <name>b-core</name>
        <suggested-version>2.0</suggested-version>
        <rejected>
            <reject>[1.2,1.8]</reject>
            <reject>[0.3,0.7]</reject>
        </rejected>
        <because>B reason</because>
    </constraint>
</constraints>
"""
        then:
        constraints.size() == 2
        constraints[0].group == "org.a"
        constraints[0].name == "a-core"
        constraints[0].suggestedVersion == "1.0"
        constraints[0].rejected.size() == 1
        constraints[0].rejected[0] == "[0.2,0.8]"
        constraints[0].because == "A reason"
        constraints[1].group == "org.b"
        constraints[1].name == "b-core"
        constraints[1].suggestedVersion == "2.0"
        constraints[1].rejected.size() == 2
        constraints[1].rejected[0] == "[1.2,1.8]"
        constraints[1].rejected[1] == "[0.3,0.7]"
        constraints[1].because == "B reason"
    }

    def "can read junit example constraint with advisory tag"() {
        when:
        parse """<?xml version="1.0"?>
<constraints>
    <constraint>
        <group>junit</group>
        <name>junit</name>
        <suggested-version>4.13.1</suggested-version>
        <rejected>
            <reject>[4.7,4.13]</reject>
        </rejected>
        <because advisory="CVE-2020-15250">TemporaryFolder on unix-like systems does not limit access to created files</because>
    </constraint>
</constraints>
"""
        then:
        constraints.size() == 1
        constraints[0].group == "junit"
        constraints[0].name == "junit"
        constraints[0].suggestedVersion == "4.13.1"
        constraints[0].rejected.size() == 1
        constraints[0].rejected[0] == "[4.7,4.13]"
        constraints[0].because == "CVE-2020-15250: TemporaryFolder on unix-like systems does not limit access to created files"
    }

    def "missing <constraint>.<group>"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <name>gradle-core</name>
        <suggested-version>1.0</suggested-version>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
        <because>Reason</because>
    </constraint>
</constraints>
"""
        then:
        def ex = thrown(DependencyConstrainException)
        ex.cause instanceof DependencyConstrainException
        ex.cause.getMessage() == "Invalid dependency constraints file: <group> tag must appear under the <constraint> tag"
    }

    def "missing <constraint>.<name>"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <group>org.gradle</group>
        <suggested-version>1.0</suggested-version>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
        <because>Reason</because>
    </constraint>
</constraints>
"""
        then:
        def ex = thrown(DependencyConstrainException)
        ex.cause instanceof DependencyConstrainException
        ex.cause.getMessage() == "Invalid dependency constraints file: <name> tag must appear under the <constraint> tag"
    }

    def "missing <constraint>.<because>"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <group>org.gradle</group>
        <name>gradle-core</name>
        <suggested-version>1.0</suggested-version>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
    </constraint>
</constraints>
"""
        then:
        def ex = thrown(DependencyConstrainException)
        ex.cause instanceof DependencyConstrainException
        ex.cause.getMessage() == "Invalid dependency constraints file: <because> tag must appear under the <constraint> tag"
    }

    def "missing <constraint>.<suggested-version>"() {
        when:
        parse """<?xml version="1.0" encoding="UTF-8"?>
<constraints>
    <constraint>
        <group>org.gradle</group>
        <name>gradle-core</name>
        <rejected>
            <reject>[0.2,0.8]</reject>
        </rejected>
        <because>Reason</because>
    </constraint>
</constraints>
"""
        then:
        def ex = thrown(DependencyConstrainException)
        ex.cause instanceof DependencyConstrainException
        ex.cause.getMessage() == "Invalid dependency constraints file: <suggested-version> tag must appear under the <constraint> tag"
    }

    def "invalid file with misplaced <#issueTag> fails with reasonable error"(String issueTag, String expectedUnderTag, String xml) {
        when:
        parse xml
        then:
        def ex = thrown(DependencyConstrainException)
        ex.cause instanceof DependencyConstrainException
        ex.cause.getMessage() == "Invalid dependency constraints file: <$issueTag> must be found under the <$expectedUnderTag> tag"
        where:
        issueTag          | expectedUnderTag | xml
        CONSTRAINT        | CONSTRAINTS      | "<constraint/>"
        GROUP             | CONSTRAINT       | "<group/>"
        NAME              | CONSTRAINT       | "<name/>"
        SUGGESTED_VERSION | CONSTRAINT       | "<suggested-version/>"
        REJECTED          | CONSTRAINT       | "<rejected/>"
        REJECT            | REJECTED         | "<reject/>"
        REJECT            | REJECTED         | "<constraints><constraint><reject/><constraint/><constraints/>"
    }

    void parse(@Language("XML") String xml) {
        loadedConstraints = ConstraintsXmlReader.readFromXml(new ByteArrayInputStream(xml.getBytes("utf-8")))
    }

}
