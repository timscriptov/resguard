plugins {
    java
    id("maven-publish")
    kotlin("jvm")
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://www.jitpack.io") }
}

dependencies {
    implementation("io.github.reandroid:ARSCLib:1.2.4") {
        exclude(group = "org.xmpull")
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.mcal"
            artifactId = "resguard"
            version = "1.0.0"

            afterEvaluate {
                from(components["java"])
            }
        }
    }
}
