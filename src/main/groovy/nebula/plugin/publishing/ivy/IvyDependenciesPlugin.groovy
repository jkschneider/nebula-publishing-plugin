/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nebula.plugin.publishing.ivy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.publish.ivy.IvyPublication

class IvyDependenciesPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.plugins.apply IvyBasePublishPlugin

        project.publishing {
            publications {
                nebulaIvy(IvyPublication) {
                    descriptor.withXml { XmlProvider xml ->
                        if (project.plugins.hasPlugin(WarPlugin)) {
                            def dependencies = xml.asNode().dependencies[0]
                            project.configurations.testRuntime.allDependencies.each { dep ->
                                dependencies.appendNode('dependency', [
                                    org: dep.group,
                                    name: dep.name,
                                    rev: dep.version,
                                    revConstraint: dep.version
                                ])
                            }
                        }
                    }
                }
            }
        }
    }
}