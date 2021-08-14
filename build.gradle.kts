import org.jetbrains.changelog.date
import org.jetbrains.changelog.markdownToHTML

plugins {
    java
    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.intellij") version "1.1.4"
    id("org.jetbrains.changelog") version "1.2.1"
}
fun properties(key: String) = project.findProperty(key).toString()
val javaVersion = "11"

group = "top.shenluw.intellij"
version = "0.1.8"


repositories {
    mavenCentral()
}

dependencies {
//    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
    implementation("io.github.tigerbrokers:openapi-java-sdk:1.0.12")
    implementation("io.netty:netty-tcnative-boringssl-static:2.0.40.Final")
    implementation("org.bouncycastle:bcprov-jdk16:1.46")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.5.1") {
        exclude(module = "kotlin-stdlib")
        exclude(module = "kotlinx-coroutines-core")
    }
}

intellij {
    version.set("2021.2")
    pluginName.set("stock-watch")
    type.set("IC")
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)
}

changelog {
    headerParserRegex.set("\\[?\\d(\\.\\d+)+\\]?.*".toRegex())
    header.set(provider { "[${version.get()}](https://github.com/shenluw/stock-watch/tree/${version.get()}) - ${date()}" })
    path.set("${project.projectDir}/CHANGELOG.md")
    itemPrefix.set("-")
}


tasks {
    compileJava {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion

        options.encoding = "UTF-8"
    }
    compileKotlin {
        kotlinOptions {
            jvmTarget = javaVersion
            apiVersion = "1.3"
//            allWarningsAsErrors = true
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = javaVersion
            apiVersion = "1.3"
//            allWarningsAsErrors = true
        }
    }
    patchPluginXml {
        sinceBuild.set("211")
        untilBuild.set("213.*")

        pluginDescription.set(
            File(projectDir, "README.md").readText().run { markdownToHTML(this) }
        )

        changeNotes.set(provider { changelog.get(version.get()).toHTML() })
    }

    publishPlugin {
        token.set(properties("intellijPublishToken"))
    }

    runPluginVerifier {
        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
    }
}