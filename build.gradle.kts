plugins {
    id("java")
    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.intellij") version "1.1.4"
}

fun properties(key: String) = project.findProperty(key).toString()

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
}
val javaVersion = "11"

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
    }

    publishPlugin {
        token.set(properties("intellijPublishToken"))
    }
}