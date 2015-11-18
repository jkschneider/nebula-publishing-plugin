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
package nebula.plugin.publishing.publications

import nebula.plugin.publishing.ivy.IvyPublishPlugin
import nebula.plugin.publishing.maven.MavenPublishPlugin
import nebula.test.IntegrationSpec

class JavadocJarPluginIntegrationSpec extends IntegrationSpec {
    File mavenPublishDir
    File ivyPublishDir
    File mavenUnzipDir
    File ivyUnzipDir

    def setup() {
        buildFile << """\
            ${applyPlugin(MavenPublishPlugin)}
            ${applyPlugin(IvyPublishPlugin)}
            ${applyPlugin(JavadocJarPlugin)}

            version = '0.1.0'
            group = 'test.nebula'

            publishing {
                repositories {
                    maven {
                        name = 'testMaven'
                        url = 'testmaven'
                    }
                    ivy {
                        name = 'testIvy'
                        url = 'testivy'
                    }
                }
            }

            task unzipMaven(type: Copy) {
                def zipFile = file('testmaven/test/nebula/javadoctest/0.1.0/javadoctest-0.1.0-javadoc.jar')
                def outputDir = file('unpackedMaven')

                from zipTree(zipFile)
                into outputDir
            }

            unzipMaven.dependsOn 'publishNebulaPublicationToTestMavenRepository'

            task unzipIvy(type: Copy) {
                def zipFile = file('testivy/test.nebula/javadoctest/0.1.0/javadoctest-0.1.0-javadoc.jar')
                def outputDir = file('unpackedIvy')

                from zipTree(zipFile)
                into outputDir
            }

            unzipIvy.dependsOn 'publishNebulaIvyPublicationToTestIvyRepository'
        """.stripIndent()

        settingsFile << '''\
            rootProject.name = 'javadoctest'
        '''.stripIndent()

        mavenPublishDir = new File(projectDir, 'testmaven/test/nebula/javadoctest/0.1.0')
        ivyPublishDir = new File(projectDir, 'testivy/test.nebula/javadoctest/0.1.0')
        mavenUnzipDir = new File(projectDir, 'unpackedMaven')
        ivyUnzipDir = new File(projectDir, 'unpackedIvy')
    }

    def 'javadoc jar is created for maven'() {
        buildFile << '''\
            apply plugin: 'java'
        '''.stripIndent()

        when:
        runTasksSuccessfully('publishNebulaPublicationToTestMavenRepository')

        then:
        new File(mavenPublishDir, 'javadoctest-0.1.0-javadoc.jar').exists()
    }

    def 'javadoc jar has content for maven'() {
        buildFile << '''\
            apply plugin: 'java'
        '''.stripIndent()

        createHelloWorld()

        when:
        runTasksSuccessfully('unzipMaven')

        then:
        new File(mavenUnzipDir, 'example/HelloWorld.html').exists()
    }

    def 'javadoc jar is created for ivy'() {
        buildFile << '''\
            apply plugin: 'java'
        '''.stripIndent()

        when:
        runTasksSuccessfully('publishNebulaIvyPublicationToTestIvyRepository')

        def ivyXmlFile = new File(ivyPublishDir, 'ivy-0.1.0.xml')

        then:
        new File(ivyPublishDir, 'javadoctest-0.1.0-javadoc.jar').exists()
        ivyXmlFile.exists()

        when:
        def ivyXml = new XmlSlurper().parse(ivyXmlFile)

        then:
        ivyXml.publications[0].artifact.find { it.@type == 'javadoc' && it.@conf == 'javadoc' }
    }

    def 'javadoc jar has content for ivy'() {
        buildFile << '''\
            apply plugin: 'java'
        '''.stripIndent()

        createHelloWorld()

        when:
        runTasksSuccessfully('unzipIvy')

        then:
        new File(ivyUnzipDir, 'example/HelloWorld.html').exists()
    }

    private void createHelloWorld() {
        def src = new File(projectDir, 'src/main/java/example')
        src.mkdirs()

        new File(src, 'HelloWorld.java').text = '''\
            package example;

            /**
             * HelloWorld class for test
             */
            public class HelloWorld {
            }
        '''.stripIndent()
    }
}
