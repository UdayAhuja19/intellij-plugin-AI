plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

group = "com.aiclient"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Ktor client for HTTP requests
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Kotlinx serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // IntelliJ Platform dependencies
    intellijPlatform {
        val platformType = providers.gradleProperty("platformType").get()
        val platformVersion = providers.gradleProperty("platformVersion").get()
        
        create(platformType, platformVersion)
        
        bundledPlugins(
            "com.intellij.java"
        )
        
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        
        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null }
        }
    }
    
    signing {
        // Configure signing if publishing to JetBrains Marketplace
    }
    
    publishing {
        // Configure publishing if releasing to JetBrains Marketplace
    }
}

tasks {
    wrapper {
        gradleVersion = "8.5"
    }
    
    buildSearchableOptions {
        enabled = false
    }
}
