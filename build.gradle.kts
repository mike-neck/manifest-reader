plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.0")
    implementation(libs.bundles.jackson.xml)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    test {
        useJUnitPlatform()
        javaToolchains {
            launcherFor {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }
    }
    named<JavaExec>("run").configure {
        args("src/test/resources/manifests/default.xml")
    }
}

application {
    mainClass = "com.example.AsAnArray"
}
